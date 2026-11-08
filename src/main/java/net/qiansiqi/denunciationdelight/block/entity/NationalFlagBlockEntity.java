package net.qiansiqi.denunciationdelight.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NationalFlagBlockEntity extends BlockEntity {
    public static final Set<String> VALID_COUNTRIES = Set.of(
            "china", "ussr", "russia", "usa", "uk", "france", "germany", "un"
    );

    private String countryId = "china";

    public NationalFlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NATIONAL_FLAG.get(), pos, state);
    }

    public static boolean isValidCountry(String countryId) {
        return countryId != null && VALID_COUNTRIES.contains(countryId);
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        if (!isValidCountry(countryId)) return;
        this.countryId = countryId;
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
            String loaded = tag.getString("CountryId");
            this.countryId = isValidCountry(loaded) ? loaded : "china";
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
