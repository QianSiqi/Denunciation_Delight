package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.UploadPortraitPacket;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

public class PortraitEditScreen extends Screen {
    private final WallPortraitBlockEntity portraitBE;
    private EditBox pathBox;
    private Component statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.initial");
    private int statusColor = 0xAAAAAA;

    public PortraitEditScreen(WallPortraitBlockEntity portraitBE) {
        super(Component.translatable("gui.denunciationdelight.portrait_edit.title"));
        this.portraitBE = portraitBE;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.pathBox = new EditBox(this.font, cx - 140, cy - 25, 280, 20,
                Component.translatable("gui.denunciationdelight.portrait_edit.path"));
        this.pathBox.setMaxLength(512);
        this.addRenderableWidget(this.pathBox);

        // 上传按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.denunciationdelight.portrait_edit.upload"), b -> {
            processAndUpload();
        }).bounds(cx - 100, cy + 18, 95, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx + 5, cy + 18, 95, 20).build());
    }

    private void processAndUpload() {
        String input = this.pathBox.getValue().trim();
        if (input.isEmpty()) {
            this.statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.empty");
            return;
        }

        this.statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.reading");
        new Thread(() -> {
            try {
                BufferedImage original;
                if (input.startsWith("http://") || input.startsWith("https://")) {
                    original = ImageIO.read(new URL(input));
                } else {
                    File file = new File(input.replace("\"", ""));
                    if (!file.exists()) {
                        this.statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.not_found");
                        return;
                    }
                    original = ImageIO.read(file);
                }

                if (original == null) {
                    this.statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.invalid");
                    return;
                }

                // 等比缩放，限制最大宽高度为 256
                int maxDim = 256;
                int w = original.getWidth();
                int h = original.getHeight();
                if (w > maxDim || h > maxDim) {
                    if (w > h) {
                        h = (int) ((double) h / w * maxDim);
                        w = maxDim;
                    } else {
                        w = (int) ((double) w / h * maxDim);
                        h = maxDim;
                    }
                }

                BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.drawImage(original.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaled, "PNG", baos);
                byte[] bytes = baos.toByteArray();

                // 上传到服务端
                ModNetworking.sendToServer(new UploadPortraitPacket(this.portraitBE.getBlockPos(), bytes));

                if (this.minecraft != null) {
                    this.minecraft.execute(this::onClose);
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.statusMessage = Component.translatable("gui.denunciationdelight.portrait_edit.failed", e.getMessage());
            }
        }).start();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        int cy = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, cx, cy - 65, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.statusMessage, cx, cy - 45, this.statusColor);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
