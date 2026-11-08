package net.qiansiqi.denunciationdelight.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;

import java.util.function.Supplier;

public class UpdatePlacardPacket {
    private final InteractionHand hand;
    private final String line1;
    private final String line2;
    private final String line3;
    private final boolean redCross;
    private final boolean equipNow;
    private final float fontSize;

    public UpdatePlacardPacket(InteractionHand hand, String line1, String line2, String line3, boolean redCross, boolean equipNow, float fontSize) {
        this.hand = hand;
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.redCross = redCross;
        this.equipNow = equipNow;
        this.fontSize = fontSize;
    }

    public UpdatePlacardPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.line1 = buf.readUtf(256);
        this.line2 = buf.readUtf(256);
        this.line3 = buf.readUtf(256);
        this.redCross = buf.readBoolean();
        this.equipNow = buf.readBoolean();
        this.fontSize = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeUtf(this.line1, 256);
        buf.writeUtf(this.line2, 256);
        buf.writeUtf(this.line3, 256);
        buf.writeBoolean(this.redCross);
        buf.writeBoolean(this.equipNow);
        buf.writeFloat(this.fontSize);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(this.hand);
            if (stack.is(ModItems.NECK_PLACARD.get())) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString(NeckPlacardItem.TAG_LINE1, this.line1.trim());
                tag.putString(NeckPlacardItem.TAG_LINE2, this.line2.trim());
                tag.putString(NeckPlacardItem.TAG_LINE3, this.line3.trim());
                tag.putBoolean(NeckPlacardItem.TAG_RED_CROSS, this.redCross);
                float safeFontSize = Float.isFinite(this.fontSize) ? Mth.clamp(this.fontSize, 0.5F, 2.5F) : 1.0F;
                tag.putFloat(NeckPlacardItem.TAG_FONT_SIZE, safeFontSize);

                if (this.equipNow) {
                    ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
                    player.setItemSlot(EquipmentSlot.CHEST, stack.copy());
                    player.setItemInHand(this.hand, currentChest);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
