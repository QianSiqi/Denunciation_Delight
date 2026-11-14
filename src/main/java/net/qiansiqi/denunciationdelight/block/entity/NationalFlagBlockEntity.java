package net.qiansiqi.denunciationdelight.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.qiansiqi.denunciationdelight.flag.FlagRegistry;
import org.jetbrains.annotations.Nullable;

public class NationalFlagBlockEntity extends BlockEntity {
    private String countryId = "cn";

    public NationalFlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NATIONAL_FLAG.get(), pos, state);
    }

    public static boolean isValidCountry(String countryId) {
        return FlagRegistry.isValidCountry(countryId);
    }

    public String getCountryId() {
        return countryId;
    }

    /**
     * 旗面伸出方块本体约 1.6 格，默认的 1×1×1 渲染包围盒会让旗帜在转动视角时被剔除。
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0D);
    }

    public void setCountryId(String countryId) {
        String canonical = FlagRegistry.canonicalize(countryId);
        if (canonical == null) return;
        this.countryId = canonical;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("CountryId", this.countryId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CountryId")) {
            String loaded = FlagRegistry.canonicalize(tag.getString("CountryId"));
            this.countryId = loaded == null ? "cn" : loaded;
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
