package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.qiansiqi.denunciationdelight.block.entity.NationalFlagBlockEntity;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SelectFlagPacket;

public class FlagSelectScreen extends Screen {
    private final NationalFlagBlockEntity flagBE;

    public static final String[][] FLAGS = {
            {"china", "flag.denunciationdelight.china"},
            {"ussr", "flag.denunciationdelight.ussr"},
            {"russia", "flag.denunciationdelight.russia"},
            {"usa", "flag.denunciationdelight.usa"},
            {"uk", "flag.denunciationdelight.uk"},
            {"france", "flag.denunciationdelight.france"},
            {"germany", "flag.denunciationdelight.germany"},
            {"un", "flag.denunciationdelight.un"}
    };

    public FlagSelectScreen(NationalFlagBlockEntity flagBE) {
        super(Component.translatable("gui.denunciationdelight.flag_select.title"));
        this.flagBE = flagBE;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        int buttonWidth = 160;
        int buttonHeight = 20;

        for (int i = 0; i < FLAGS.length; i++) {
            String countryId = FLAGS[i][0];
            String countryNameKey = FLAGS[i][1];

            int col = i % 2;
            int row = i / 2;
            int x = cx - buttonWidth - 5 + col * (buttonWidth + 10);
            int y = cy - 50 + row * (buttonHeight + 6);

            this.addRenderableWidget(Button.builder(Component.translatable(countryNameKey), b -> {
                ModNetworking.sendToServer(new SelectFlagPacket(this.flagBE.getBlockPos(), countryId));
                this.onClose();
            }).bounds(x, y, buttonWidth, buttonHeight).build());
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx - 50, cy + 65, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
