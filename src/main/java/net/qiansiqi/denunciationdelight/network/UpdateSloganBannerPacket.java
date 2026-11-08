package net.qiansiqi.denunciationdelight.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.block.SloganBannerBlock;
import net.qiansiqi.denunciationdelight.block.entity.SloganBannerBlockEntity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

public class UpdateSloganBannerPacket {
    private final BlockPos pos;
    private final String text;
    private final int bgColor;
    private final int textColor;
    private final float fontSize;

    public UpdateSloganBannerPacket(BlockPos pos, String text, int bgColor, int textColor, float fontSize) {
        this.pos = pos;
        this.text = text;
        this.bgColor = bgColor;
        this.textColor = textColor;
        this.fontSize = fontSize;
    }

    public UpdateSloganBannerPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.text = buf.readUtf(256);
        this.bgColor = buf.readInt();
        this.textColor = buf.readInt();
        this.fontSize = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.text, 256);
        buf.writeInt(this.bgColor);
        buf.writeInt(this.textColor);
        buf.writeFloat(this.fontSize);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level();
            if (!level.isLoaded(this.pos)) return;
            if (player.distanceToSqr(Vec3.atCenterOf(this.pos)) > 64.0D) return;
            if (!level.mayInteract(player, this.pos)) return;

            BlockEntity targetBE = level.getBlockEntity(this.pos);
            if (!(targetBE instanceof SloganBannerBlockEntity bannerBE)) return;

            Direction facing = bannerBE.getBlockState().getValue(SloganBannerBlock.FACING);
            float safeFontSize = Float.isFinite(this.fontSize) ? Mth.clamp(this.fontSize, 0.5F, 2.5F) : 1.0F;

            // 广度优先搜索同步整个多方块连通横幅结构
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(this.pos);
            visited.add(this.pos);

            Direction right = facing.getClockWise();
            Direction left = right.getOpposite();
            Direction[] dirs = {right, left, Direction.UP, Direction.DOWN};

            while (!queue.isEmpty() && visited.size() <= 64) {
                BlockPos current = queue.poll();
                BlockEntity be = level.getBlockEntity(current);
                if (be instanceof SloganBannerBlockEntity sbe) {
                    sbe.setText(this.text);
                    sbe.setBackgroundColor(this.bgColor);
                    sbe.setTextColor(this.textColor);
                    sbe.setFontSize(safeFontSize);
                }

                for (Direction d : dirs) {
                    BlockPos neighbor = current.relative(d);
                    if (!visited.contains(neighbor) && level.isLoaded(neighbor)) {
                        BlockState nState = level.getBlockState(neighbor);
                        if (nState.getBlock() instanceof SloganBannerBlock
                                && nState.getValue(SloganBannerBlock.FACING) == facing) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
