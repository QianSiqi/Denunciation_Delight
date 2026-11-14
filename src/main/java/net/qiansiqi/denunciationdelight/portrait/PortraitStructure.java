package net.qiansiqi.denunciationdelight.portrait;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.qiansiqi.denunciationdelight.block.WallPortraitBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 壁挂画像的多方块结构：同朝向的上下左右相邻画像会合并为一个矩形画面。
 * 最大 8×8（64 块），避免 BFS 和渲染包围盒无限膨胀。
 */
public final class PortraitStructure {
    public static final int MAX_BLOCKS = 64;

    private PortraitStructure() {
    }

    public record Layout(BlockPos origin, Direction facing, int width, int height, List<BlockPos> blocks) {
    }

    public static Layout scan(Level level, BlockPos start) {
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof WallPortraitBlock)) return null;

        Direction facing = startState.getValue(WallPortraitBlock.FACING);
        Direction right = facing.getCounterClockWise();
        Direction left = right.getOpposite();
        Direction[] neighbors = {right, left, Direction.UP, Direction.DOWN};

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();
            for (Direction direction : neighbors) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next) || !level.isLoaded(next)) continue;
                BlockState state = level.getBlockState(next);
                if (!(state.getBlock() instanceof WallPortraitBlock)) continue;
                if (state.getValue(WallPortraitBlock.FACING) != facing) continue;
                visited.add(next);
                queue.add(next);
            }
        }

        int minRight = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        BlockPos origin = start;

        for (BlockPos pos : visited) {
            int projectedRight = pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
            minRight = Math.min(minRight, projectedRight);
            maxRight = Math.max(maxRight, projectedRight);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }

        for (BlockPos pos : visited) {
            int projectedRight = pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
            if (projectedRight == minRight && pos.getY() == maxY) {
                origin = pos;
                break;
            }
        }

        int width = Math.max(1, maxRight - minRight + 1);
        int height = Math.max(1, maxY - minY + 1);
        return new Layout(origin, facing, width, height, List.copyOf(new ArrayList<>(visited)));
    }
}