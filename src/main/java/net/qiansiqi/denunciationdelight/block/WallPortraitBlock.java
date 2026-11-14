package net.qiansiqi.denunciationdelight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.client.BlockClientInteractions;
import org.jetbrains.annotations.Nullable;

public class WallPortraitBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    protected static final VoxelShape SHAPE_NORTH = Block.box(1.0D, 1.0D, 14.5D, 15.0D, 15.0D, 16.0D);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(1.0D, 1.0D, 0.0D, 15.0D, 15.0D, 1.5D);
    protected static final VoxelShape SHAPE_WEST = Block.box(14.5D, 1.0D, 1.0D, 16.0D, 15.0D, 15.0D);
    protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 1.0D, 1.0D, 1.5D, 15.0D, 15.0D);

    public WallPortraitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WallPortraitBlockEntity portraitBE)) return;

        Direction facing = state.getValue(FACING);
        Direction right = facing.getCounterClockWise();
        Direction left = right.getOpposite();
        // 吸附到已有画像时继承图片，让新方块立即成为整组画面的一部分
        for (Direction direction : new Direction[]{right, left, Direction.UP, Direction.DOWN}) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof WallPortraitBlock)
                    || neighborState.getValue(FACING) != facing) {
                continue;
            }
            if (level.getBlockEntity(neighborPos) instanceof WallPortraitBlockEntity neighborBE
                    && !neighborBE.getPortraitId().isEmpty()) {
                portraitBE.setPortraitId(neighborBE.getPortraitId());
                break;
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WallPortraitBlockEntity portraitBE) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BlockClientInteractions.openWallPortrait(portraitBE));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallPortraitBlockEntity(pos, state);
    }
}
