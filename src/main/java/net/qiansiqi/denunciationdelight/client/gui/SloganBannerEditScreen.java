package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.qiansiqi.denunciationdelight.block.entity.SloganBannerBlockEntity;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.UpdateSloganBannerPacket;

public class SloganBannerEditScreen extends Screen {
    private final SloganBannerBlockEntity bannerBE;
    private EditBox textBox;
    private int selectedBgColor;
    private int selectedTextColor;

    private static final int[] BG_COLORS = {0xCC1111, 0xF0F0F0, 0x1A4F9C, 0x222222};
    private static final String[] BG_KEYS = {
            "color.denunciationdelight.bg.red",
            "color.denunciationdelight.bg.white",
            "color.denunciationdelight.bg.blue",
            "color.denunciationdelight.bg.black"
    };

    private static final int[] TEXT_COLORS = {0xFFEE00, 0xCC1111, 0xFFFFFF, 0x222222};
    private static final String[] TEXT_KEYS = {
            "color.denunciationdelight.text.yellow",
            "color.denunciationdelight.text.red",
            "color.denunciationdelight.text.white",
            "color.denunciationdelight.text.black"
    };

    private int bgIndex = 0;
    private int textIndex = 0;

    private float fontSize = 1.0F;

    public SloganBannerEditScreen(SloganBannerBlockEntity bannerBE) {
        super(Component.translatable("gui.denunciationdelight.slogan_banner_edit.title"));
        this.bannerBE = bannerBE;
        this.selectedBgColor = bannerBE.getBackgroundColor();
        this.selectedTextColor = bannerBE.getTextColor();
        this.fontSize = bannerBE.getFontSize();

        for (int i = 0; i < BG_COLORS.length; i++) {
            if (BG_COLORS[i] == this.selectedBgColor) bgIndex = i;
        }
        for (int i = 0; i < TEXT_COLORS.length; i++) {
            if (TEXT_COLORS[i] == this.selectedTextColor) textIndex = i;
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.textBox = new EditBox(this.font, cx - 120, cy - 42, 240, 20,
                Component.translatable("gui.denunciationdelight.slogan_banner_edit.text"));
        this.textBox.setMaxLength(64);
        this.textBox.setValue(this.bannerBE.getText());
        this.addRenderableWidget(this.textBox);

        // 切换背景颜色按钮
        Button bgBtn = Button.builder(
                Component.translatable("gui.denunciationdelight.slogan_banner_edit.bg",
                        Component.translatable(BG_KEYS[bgIndex])), b -> {
                    bgIndex = (bgIndex + 1) % BG_COLORS.length;
                    this.selectedBgColor = BG_COLORS[bgIndex];
                    b.setMessage(Component.translatable("gui.denunciationdelight.slogan_banner_edit.bg",
                            Component.translatable(BG_KEYS[bgIndex])));
                }).bounds(cx - 120, cy - 16, 75, 20).build();
        this.addRenderableWidget(bgBtn);

        // 切换文字颜色按钮
        Button textBtn = Button.builder(
                Component.translatable("gui.denunciationdelight.slogan_banner_edit.text_color",
                        Component.translatable(TEXT_KEYS[textIndex])), b -> {
                    textIndex = (textIndex + 1) % TEXT_COLORS.length;
                    this.selectedTextColor = TEXT_COLORS[textIndex];
                    b.setMessage(Component.translatable("gui.denunciationdelight.slogan_banner_edit.text_color",
                            Component.translatable(TEXT_KEYS[textIndex])));
                }).bounds(cx - 40, cy - 16, 75, 20).build();
        this.addRenderableWidget(textBtn);

        // 字号调节按钮
        Button sizeMinusBtn = Button.builder(Component.literal("A-"), b -> {
            this.fontSize = Math.max(0.5F, Math.round((this.fontSize - 0.1F) * 10.0F) / 10.0F);
        }).bounds(cx + 40, cy - 16, 35, 20).build();
        this.addRenderableWidget(sizeMinusBtn);

        Button sizePlusBtn = Button.builder(Component.literal("A+"), b -> {
            this.fontSize = Math.min(2.5F, Math.round((this.fontSize + 0.1F) * 10.0F) / 10.0F);
        }).bounds(cx + 80, cy - 16, 35, 20).build();
        this.addRenderableWidget(sizePlusBtn);

        // 保存完成按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            saveAndClose();
        }).bounds(cx - 100, cy + 22, 95, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx + 5, cy + 22, 95, 20).build());
    }

    private void saveAndClose() {
        String newText = this.textBox.getValue().trim();
        ModNetworking.sendToServer(new UpdateSloganBannerPacket(
                this.bannerBE.getBlockPos(), newText, this.selectedBgColor, this.selectedTextColor, this.fontSize));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        int cy = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, cx, cy - 70, 0xFFFFFF);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.slogan_banner_edit.hint"),
                cx - 120, cy - 54, 0xCCCCCC);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.denunciationdelight.slogan_banner_edit.font_size",
                        String.format(java.util.Locale.ROOT, "%.1f", this.fontSize)),
                cx + 45, cy + 8, 0xEEEEEE);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
