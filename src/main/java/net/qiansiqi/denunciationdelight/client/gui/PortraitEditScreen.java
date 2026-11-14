package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.UploadPortraitPacket;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.Locale;

public class PortraitEditScreen extends Screen {
    private final WallPortraitBlockEntity portraitBE;
    private EditBox pathBox;
    private String prefillPath = "";
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
        if (!this.prefillPath.isEmpty()) {
            this.pathBox.setValue(this.prefillPath);
        }
        this.addRenderableWidget(this.pathBox);

        // 选择文件按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.denunciationdelight.portrait_edit.choose"),
                b -> openFileChooser()).bounds(cx - 100, cy - 2, 95, 20).build());

        // 上传按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.denunciationdelight.portrait_edit.upload"), b -> {
            processAndUpload();
        }).bounds(cx - 100, cy + 18, 95, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx + 5, cy + 18, 95, 20).build());
    }

    private void openFileChooser() {
        if (this.minecraft == null) return;
        if (this.pathBox != null) {
            this.prefillPath = this.pathBox.getValue();
        }
        String initialPath = this.prefillPath;
        if (initialPath.startsWith("http://") || initialPath.startsWith("https://")) {
            initialPath = "";
        }

        // Windows 下优先用 LWJGL tinyfd 调 Win32 原生文件对话框；
        // Linux/macOS 直接用跨平台的 AWT FileDialog，失败再落到游戏内浏览器。
        final String initial = initialPath;
        if (!isWindows()) {
            this.openAwtFileChooser();
            return;
        }

        new Thread(() -> {
            String selected = null;
            boolean nativeFailed = false;
            try {
                selected = openTinyFileDialog(initial);
            } catch (Throwable t) {
                nativeFailed = true;
                DenunciationDelight.LOGGER.warn("tinyfiledialogs failed, falling back to AWT", t);
            }

            if (selected != null && !selected.isBlank()) {
                this.applySelectedPath(selected);
                return;
            }
            if (!nativeFailed) {
                // null 且没有异常：用户主动取消了对话框
                return;
            }
            if (this.minecraft != null) {
                this.minecraft.execute(this::openAwtFileChooser);
            }
        }, "Denunciation Portrait File Chooser").start();
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private String openTinyFileDialog(String initialPath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(3);
            filters.put(stack.UTF8("*.png"));
            filters.put(stack.UTF8("*.jpg"));
            filters.put(stack.UTF8("*.jpeg"));
            filters.flip();

            File initial = initialPath == null || initialPath.isBlank() ? null : new File(initialPath);
            String defaultPath = "";
            if (initial != null) {
                defaultPath = initial.isDirectory()
                        ? initial.getAbsolutePath()
                        : (initial.getParent() != null ? initial.getParent() : "");
            }

            return TinyFileDialogs.tinyfd_openFileDialog(
                    Component.translatable(
                            "gui.denunciationdelight.portrait_edit.choose_title").getString(),
                    defaultPath,
                    filters,
                    Component.translatable(
                            "gui.denunciationdelight.portrait_edit.choose_filter").getString(),
                    false);
        }
    }

    private void applySelectedPath(String path) {
        if (this.minecraft == null) return;
        this.minecraft.execute(() -> {
            this.prefillPath = path;
            if (this.pathBox != null) {
                this.pathBox.setValue(path);
            }
        });
    }

    private void openAwtFileChooser() {
        if (this.minecraft == null) return;
        try {
            EventQueue.invokeLater(() -> {
                FileDialog dialog = null;
                try {
                    dialog = new FileDialog((Frame) null,
                            Component.translatable(
                                    "gui.denunciationdelight.portrait_edit.choose_title").getString(),
                            FileDialog.LOAD);
                    dialog.setMultipleMode(false);
                    dialog.setAlwaysOnTop(true);
                    dialog.setVisible(true);

                    String fileName = dialog.getFile();
                    if (fileName != null) {
                        this.applySelectedPath(new File(dialog.getDirectory(), fileName).getAbsolutePath());
                    }
                } catch (Throwable nativeFailure) {
                    DenunciationDelight.LOGGER.warn("AWT FileDialog failed, using in-game browser", nativeFailure);
                    if (this.minecraft != null) {
                        this.minecraft.execute(() ->
                                this.minecraft.setScreen(new FileBrowserScreen(
                                        PortraitEditScreen.this, this::setSelectedPath)));
                    }
                } finally {
                    if (dialog != null) {
                        dialog.dispose();
                    }
                }
            });
        } catch (Throwable schedulingFailure) {
            this.minecraft.setScreen(new FileBrowserScreen(this, this::setSelectedPath));
        }
    }

    public void setSelectedPath(String path) {
        this.prefillPath = path;
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

                // 等比缩放；多方块画像也能保持更高分辨率
                int maxDim = 512;
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
