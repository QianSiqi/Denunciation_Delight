package net.qiansiqi.denunciationdelight.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class WallPortraitBlockEntity extends BlockEntity {
    private String portraitId = "";

    public WallPortraitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALL_PORTRAIT.get(), pos, state);
    }

    public String getPortraitId() {
        return portraitId;
    }

    /**
     * 多方块画像可能延伸到周围 8 格，扩大渲染包围盒避免视锥剔除。
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(8.0D);
    }

    public void setPortraitId(String portraitId) {
        this.portraitId = portraitId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("PortraitId", this.portraitId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PortraitId")) {
            this.portraitId = tag.getString("PortraitId");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
