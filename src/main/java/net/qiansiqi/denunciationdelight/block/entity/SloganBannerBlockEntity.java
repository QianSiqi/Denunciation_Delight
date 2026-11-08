package net.qiansiqi.denunciationdelight.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SloganBannerBlockEntity extends BlockEntity {
    private String text = "抓紧生产 厉行节约";
    private int backgroundColor = 0xCC1111; // 经典红底
    private int textColor = 0xFFEE00;       // 鲜亮黄字
    private float fontSize = 1.0F;          // 字号缩放比例

    public SloganBannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOGAN_BANNER.get(), pos, state);
    }

    public float getFontSize() {
        return sanitizeFontSize(fontSize);
    }

    public void setFontSize(float fontSize) {
        this.fontSize = sanitizeFontSize(fontSize);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Text", this.text);
        tag.putInt("BgColor", this.backgroundColor);
        tag.putInt("TextColor", this.textColor);
        tag.putFloat("FontSize", this.fontSize);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Text")) this.text = tag.getString("Text");
        if (tag.contains("BgColor")) this.backgroundColor = tag.getInt("BgColor");
        if (tag.contains("TextColor")) this.textColor = tag.getInt("TextColor");
        if (tag.contains("FontSize")) this.fontSize = sanitizeFontSize(tag.getFloat("FontSize"));
    }

    private static float sanitizeFontSize(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Mth.clamp(value, 0.5F, 2.5F);
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
