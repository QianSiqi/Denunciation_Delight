package net.qiansiqi.denunciationdelight.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.item.ConeHatItem;
import net.qiansiqi.denunciationdelight.item.ModItems;

import java.util.function.Supplier;

public class UpdateConeHatPacket {
    private final InteractionHand hand;
    private final String text;
    private final boolean equipNow;
    private final float fontSize;

    public UpdateConeHatPacket(InteractionHand hand, String text, boolean equipNow, float fontSize) {
        this.hand = hand;
        this.text = text;
        this.equipNow = equipNow;
        this.fontSize = fontSize;
    }

    public UpdateConeHatPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.text = buf.readUtf(64);
        this.equipNow = buf.readBoolean();
        this.fontSize = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeUtf(this.text, 64);
        buf.writeBoolean(this.equipNow);
        buf.writeFloat(this.fontSize);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(this.hand);
            if (stack.is(ModItems.CONE_HAT.get())) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString(ConeHatItem.TAG_HAT_TEXT, this.text.trim());
                float safeFontSize = Float.isFinite(this.fontSize) ? Mth.clamp(this.fontSize, 0.5F, 2.5F) : 1.0F;
                tag.putFloat(ConeHatItem.TAG_FONT_SIZE, safeFontSize);

                if (this.equipNow) {
                    ItemStack currentHelmet = player.getItemBySlot(EquipmentSlot.HEAD);
                    player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
                    player.setItemInHand(this.hand, currentHelmet);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
