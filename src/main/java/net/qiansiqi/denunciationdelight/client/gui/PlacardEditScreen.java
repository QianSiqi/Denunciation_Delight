package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.UpdatePlacardPacket;

public class PlacardEditScreen extends Screen {
    private final ItemStack placardStack;
    private final InteractionHand hand;

    private EditBox line1Box;
    private EditBox line2Box;
    private EditBox line3Box;
    private Checkbox redCrossCheckbox;
    private float fontSize = 1.0F;

    public PlacardEditScreen(ItemStack placardStack, InteractionHand hand) {
        super(Component.translatable("gui.denunciationdelight.placard_edit.title"));
        this.placardStack = placardStack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxWidth = 200;
        int boxHeight = 20;

        CompoundTag tag = this.placardStack.getTag();
        String initialLine1 = tag != null ? tag.getString(NeckPlacardItem.TAG_LINE1) : "";
        String initialLine2 = tag != null ? tag.getString(NeckPlacardItem.TAG_LINE2) : "";
        String initialLine3 = tag != null ? tag.getString(NeckPlacardItem.TAG_LINE3) : "";
        boolean initialRedCross = tag != null && tag.getBoolean(NeckPlacardItem.TAG_RED_CROSS);
        this.fontSize = (tag != null && tag.contains(NeckPlacardItem.TAG_FONT_SIZE)) ? tag.getFloat(NeckPlacardItem.TAG_FONT_SIZE) : 1.0F;
        this.fontSize = sanitizeFontSize(this.fontSize);

        // 如果第2行是空的，可以默认填玩家名字
        if (initialLine2.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
            initialLine2 = this.minecraft.player.getGameProfile().getName();
        }

        // 输入框 1
        this.line1Box = new EditBox(this.font, centerX - 100, centerY - 65, boxWidth, boxHeight,
                Component.translatable("gui.denunciationdelight.placard_edit.line1"));
        this.line1Box.setMaxLength(32);
        this.line1Box.setValue(initialLine1);
        this.addRenderableWidget(this.line1Box);

        // 输入框 2
        this.line2Box = new EditBox(this.font, centerX - 100, centerY - 35, boxWidth, boxHeight,
                Component.translatable("gui.denunciationdelight.placard_edit.line2"));
        this.line2Box.setMaxLength(32);
        this.line2Box.setValue(initialLine2);
        this.addRenderableWidget(this.line2Box);

        // 输入框 3
        this.line3Box = new EditBox(this.font, centerX - 100, centerY - 5, boxWidth, boxHeight,
                Component.translatable("gui.denunciationdelight.placard_edit.line3"));
        this.line3Box.setMaxLength(32);
        this.line3Box.setValue(initialLine3);
        this.addRenderableWidget(this.line3Box);

        // 红叉复选框
        this.redCrossCheckbox = new Checkbox(centerX - 100, centerY + 22, 75, 20,
                Component.translatable("gui.denunciationdelight.placard_edit.red_cross"), initialRedCross);
        this.addRenderableWidget(this.redCrossCheckbox);

        // 字号调节按钮
        this.addRenderableWidget(Button.builder(Component.literal("A-"), b -> {
            this.fontSize = Math.max(0.5F, Math.round((this.fontSize - 0.1F) * 10.0F) / 10.0F);
        }).bounds(centerX + 20, centerY + 22, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("A+"), b -> {
            this.fontSize = Math.min(2.5F, Math.round((this.fontSize + 0.1F) * 10.0F) / 10.0F);
        }).bounds(centerX + 60, centerY + 22, 35, 20).build());

        // 完成并佩戴按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.denunciationdelight.placard_edit.done"), button -> {
            saveAndSend(true);
            this.onClose();
        }).bounds(centerX - 100, centerY + 50, 95, 20).build());

        // 仅保存按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.denunciationdelight.placard_edit.save"), button -> {
            saveAndSend(false);
            this.onClose();
        }).bounds(centerX + 5, centerY + 50, 95, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(centerX - 50, centerY + 76, 100, 20).build());
    }

    private static float sanitizeFontSize(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private void saveAndSend(boolean equipNow) {
        String l1 = this.line1Box.getValue();
        String l2 = this.line2Box.getValue();
        String l3 = this.line3Box.getValue();
        boolean redCross = this.redCrossCheckbox.selected();

        // 客户端本地立即写入 NBT，杜绝延迟
        CompoundTag localTag = this.placardStack.getOrCreateTag();
        localTag.putString(NeckPlacardItem.TAG_LINE1, l1.trim());
        localTag.putString(NeckPlacardItem.TAG_LINE2, l2.trim());
        localTag.putString(NeckPlacardItem.TAG_LINE3, l3.trim());
        localTag.putBoolean(NeckPlacardItem.TAG_RED_CROSS, redCross);
        localTag.putFloat(NeckPlacardItem.TAG_FONT_SIZE, this.fontSize);

        ModNetworking.sendToServer(new UpdatePlacardPacket(this.hand, l1, l2, l3, redCross, equipNow, this.fontSize));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 绘制标题
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 88, 0xFFFFFF);

        // 绘制输入框左上角的小标签
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.placard_edit.label1"),
                centerX - 100, centerY - 76, 0xAAAAAA);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.placard_edit.label2"),
                centerX - 100, centerY - 46, 0xAAAAAA);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.placard_edit.label3"),
                centerX - 100, centerY - 16, 0xAAAAAA);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.placard_edit.font_size",
                        String.format(java.util.Locale.ROOT, "%.1f", this.fontSize)),
                centerX - 20, centerY + 27, 0xCCCCCC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
