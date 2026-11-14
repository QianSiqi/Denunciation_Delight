package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 游戏内文件浏览器，避免依赖 AWT/Swing（部分环境会 headless 或弹窗失败）。
 */
public class FileBrowserScreen extends Screen {
    private static final int PAGE_SIZE = 7;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP = 48;
    private static final int ROW_WIDTH = 280;

    private final Screen parent;
    private final Consumer<String> onSelect;
    private final List<File> entries = new ArrayList<>();
    private final List<Button> entryButtons = new ArrayList<>();

    private File currentDir;
    private int page;
    private Button upButton;
    private Button previousButton;
    private Button nextButton;

    public FileBrowserScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.translatable("gui.denunciationdelight.file_browser.title"));
        this.parent = parent;
        this.onSelect = onSelect;

        File home = new File(System.getProperty("user.home", "."));
        this.currentDir = home.isDirectory() ? home : new File(".").getAbsoluteFile();
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height - 26;

        this.upButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.denunciationdelight.file_browser.up"), b -> {
                    File parentDir = this.currentDir.getParentFile();
                    if (parentDir != null && parentDir.isDirectory()) {
                        this.currentDir = parentDir;
                        this.page = 0;
                        this.refresh();
                    }
                }).bounds(20, y, 60, 20).build());

        this.previousButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"), b -> {
                    this.page = Math.max(0, this.page - 1);
                    this.refresh();
                }).bounds(88, y, 36, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"), b -> {
                    this.page++;
                    this.refresh();
                }).bounds(130, y, 36, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(210, y, 70, 20).build());

        this.refresh();
    }

    private void refresh() {
        for (Button button : this.entryButtons) {
            this.removeWidget(button);
        }
        this.entryButtons.clear();
        this.entries.clear();

        File[] files = this.currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() || isImage(file)) {
                    this.entries.add(file);
                }
            }
        }
        this.entries.sort((left, right) -> {
            if (left.isDirectory() != right.isDirectory()) {
                return left.isDirectory() ? -1 : 1;
            }
            return left.getName().toLowerCase(Locale.ROOT)
                    .compareTo(right.getName().toLowerCase(Locale.ROOT));
        });

        int pageCount = Math.max(1, (this.entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.page = Mth.clamp(this.page, 0, pageCount - 1);

        int start = this.page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= this.entries.size()) break;
            File file = this.entries.get(index);
            String label = (file.isDirectory() ? "[D] " : "") + file.getName();
            label = this.font.plainSubstrByWidth(label, ROW_WIDTH - 12);
            int y = LIST_TOP + i * ROW_HEIGHT;
            Button button = Button.builder(Component.literal(label), b -> this.choose(file))
                    .bounds(20, y, ROW_WIDTH, 18).build();
            this.addRenderableWidget(button);
            this.entryButtons.add(button);
        }

        if (this.upButton != null) {
            this.upButton.active = this.currentDir.getParentFile() != null;
        }
        if (this.previousButton != null) {
            this.previousButton.active = this.page > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.page < pageCount - 1;
        }
    }

    private void choose(File file) {
        if (file.isDirectory()) {
            this.currentDir = file;
            this.page = 0;
            this.refresh();
            return;
        }
        this.onSelect.accept(file.getAbsolutePath());
        Minecraft.getInstance().setScreen(this.parent);
    }

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        String path = this.font.plainSubstrByWidth(this.currentDir.getAbsolutePath(), this.width - 40);
        guiGraphics.drawString(this.font, path, 20, 30, 0xAAAAAA, false);

        if (this.entries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.denunciationdelight.file_browser.empty"),
                    this.width / 2, LIST_TOP + 28, 0x888888);
        }

        int pageCount = Math.max(1, (this.entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.denunciationdelight.file_browser.page",
                        this.page + 1, pageCount),
                this.width / 2, this.height - 39, 0x888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}