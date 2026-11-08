package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.item.ConeHatItem;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.UpdateConeHatPacket;

public class ConeHatEditScreen extends Screen {
    private final ItemStack hatStack;
    private final InteractionHand hand;
    private EditBox textBox;
    private float fontSize = 1.0F;

    public ConeHatEditScreen(ItemStack hatStack, InteractionHand hand) {
        super(Component.translatable("gui.denunciationdelight.cone_hat_edit.title"));
        this.hatStack = hatStack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        CompoundTag tag = this.hatStack.getTag();
        String initialText = tag != null ? tag.getString(ConeHatItem.TAG_HAT_TEXT) : "";
        this.fontSize = (tag != null && tag.contains(ConeHatItem.TAG_FONT_SIZE)) ? tag.getFloat(ConeHatItem.TAG_FONT_SIZE) : 1.0F;
        this.fontSize = sanitizeFontSize(this.fontSize);

        this.textBox = new EditBox(this.font, cx - 100, cy - 25, 200, 20, Component.translatable("gui.denunciationdelight.cone_hat_edit.text"));
        this.textBox.setMaxLength(16);
        this.textBox.setValue(initialText);
        this.addRenderableWidget(this.textBox);

        // 字号调节按钮
        this.addRenderableWidget(Button.builder(Component.literal("A-"), b -> {
            this.fontSize = Math.max(0.5F, Math.round((this.fontSize - 0.1F) * 10.0F) / 10.0F);
        }).bounds(cx + 20, cy + 2, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("A+"), b -> {
            this.fontSize = Math.min(2.5F, Math.round((this.fontSize + 0.1F) * 10.0F) / 10.0F);
        }).bounds(cx + 60, cy + 2, 35, 20).build());

        // 完成并佩戴
        this.addRenderableWidget(Button.builder(Component.translatable("gui.denunciationdelight.cone_hat_edit.done"), b -> {
            saveAndSend(true);
            this.onClose();
        }).bounds(cx - 100, cy + 28, 95, 20).build());

        // 仅保存
        this.addRenderableWidget(Button.builder(Component.translatable("gui.denunciationdelight.cone_hat_edit.save"), b -> {
            saveAndSend(false);
            this.onClose();
        }).bounds(cx + 5, cy + 28, 95, 20).build());

        // 取消
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx - 50, cy + 54, 100, 20).build());
    }

    private static float sanitizeFontSize(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private void saveAndSend(boolean equipNow) {
        String text = this.textBox.getValue().trim();

        // 客户端本地立即写入 NBT，杜绝网络回包延迟
        CompoundTag localTag = this.hatStack.getOrCreateTag();
        localTag.putString(ConeHatItem.TAG_HAT_TEXT, text);
        localTag.putFloat(ConeHatItem.TAG_FONT_SIZE, this.fontSize);

        ModNetworking.sendToServer(new UpdateConeHatPacket(this.hand, text, equipNow, this.fontSize));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        int cy = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, cx, cy - 55, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.denunciationdelight.cone_hat_edit.hint"), cx, cy - 40, 0xAAAAAA);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.cone_hat_edit.font_size",
                        String.format(java.util.Locale.ROOT, "%.1f", this.fontSize)),
                cx - 100, cy + 7, 0xEEEEEE);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
