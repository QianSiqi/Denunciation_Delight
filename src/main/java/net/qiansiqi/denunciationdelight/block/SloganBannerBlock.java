package net.qiansiqi.denunciationdelight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.qiansiqi.denunciationdelight.block.entity.SloganBannerBlockEntity;
import net.qiansiqi.denunciationdelight.client.BlockClientInteractions;
import org.jetbrains.annotations.Nullable;

public class SloganBannerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 薄薄贴在墙面或方块边缘的碰撞体积 (整整一格宽高: 16x16 像素)
    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 14.5D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.5D);
    protected static final VoxelShape SHAPE_WEST = Block.box(14.5D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 0.0D, 0.0D, 1.5D, 16.0D, 16.0D);

    public SloganBannerBlock(Properties properties) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SloganBannerBlockEntity bannerBE)) return;

        // 1. 优先读取物品本身的自定义 NBT
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        boolean hasCustom = false;
        if (tag != null) {
            net.minecraft.nbt.CompoundTag beTag = tag.contains("BlockEntityTag") ? tag.getCompound("BlockEntityTag") : tag;
            if (beTag.contains("Text")) {
                bannerBE.setText(beTag.getString("Text"));
                hasCustom = true;
            }
            if (beTag.contains("BgColor")) bannerBE.setBackgroundColor(beTag.getInt("BgColor"));
            if (beTag.contains("TextColor")) bannerBE.setTextColor(beTag.getInt("TextColor"));
            if (beTag.contains("FontSize")) bannerBE.setFontSize(beTag.getFloat("FontSize"));
        }
        if (stack.hasCustomHoverName() && !hasCustom) {
            bannerBE.setText(stack.getHoverName().getString());
            hasCustom = true;
        }

        // 2. 多方块结构智能继承：放置到相邻横幅旁边时，自动继承相连横幅的文字、字号和颜色！
        if (!hasCustom) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getCounterClockWise();
            Direction left = right.getOpposite();
            Direction[] neighbors = {left, right, Direction.UP, Direction.DOWN};

            for (Direction d : neighbors) {
                BlockPos nPos = pos.relative(d);
                BlockState nState = level.getBlockState(nPos);
                if (nState.getBlock() instanceof SloganBannerBlock && nState.getValue(FACING) == facing) {
                    BlockEntity nBE = level.getBlockEntity(nPos);
                    if (nBE instanceof SloganBannerBlockEntity neighborBanner) {
                        bannerBE.setText(neighborBanner.getText());
                        bannerBE.setBackgroundColor(neighborBanner.getBackgroundColor());
                        bannerBE.setTextColor(neighborBanner.getTextColor());
                        bannerBE.setFontSize(neighborBanner.getFontSize());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (level.isClientSide()) return;

        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);

        if (player.isCreative()) return;

        // 自己接管掉落，避免 playerWillDestroy 的自定义掉落与默认掉落叠加成两个横幅
        ItemStack drop = new ItemStack(this);
        if (blockEntity instanceof SloganBannerBlockEntity bannerBE) {
            CompoundTag beTag = new CompoundTag();
            bannerBE.saveAdditional(beTag);
            drop.addTagElement("BlockEntityTag", beTag);
        }
        popResource(level, pos, drop);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SloganBannerBlockEntity bannerBE) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BlockClientInteractions.openSloganBanner(bannerBE));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SloganBannerBlockEntity(pos, state);
    }
}
