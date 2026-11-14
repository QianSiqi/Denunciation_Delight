package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.qiansiqi.denunciationdelight.client.SelectedWorksContent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 右键《毛泽东选集》打开的书页 GUI。
 * 正文来自外部 selected_works.json，模组本身不内置受版权保护的全文。
 */
public class SelectedWorksScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 200;
    private static final int LINES_PER_PAGE = 12;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_WIDTH = 252;

    private final SelectedWorksContent.Book book;
    private final List<Line> lines = new ArrayList<>();
    private int page;
    private Button previousButton;
    private Button nextButton;

    public SelectedWorksScreen() {
        super(Component.literal("毛泽东选集"));
        this.book = SelectedWorksContent.load();
    }

    @Override
    protected void init() {
        super.init();
        this.buildLines();
        this.page = 0;

        int px = this.panelX();
        int py = this.panelY();
        this.previousButton = this.addRenderableWidget(
                Button.builder(Component.literal("<"), b -> this.turnPage(-1))
                        .bounds(px + 16, py + PANEL_HEIGHT - 26, 44, 18).build());
        this.nextButton = this.addRenderableWidget(
                Button.builder(Component.literal(">"), b -> this.turnPage(1))
                        .bounds(px + PANEL_WIDTH - 60, py + PANEL_HEIGHT - 26, 44, 18).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(px + PANEL_WIDTH / 2 - 30, py + PANEL_HEIGHT - 26, 60, 18).build());
        this.updateButtons();
    }

    private void buildLines() {
        this.lines.clear();
        for (SelectedWorksContent.Page pageData : this.book.pages()) {
            if (!pageData.heading().isBlank()) {
                this.lines.add(new Line(Component.literal(pageData.heading())
                        .withStyle(net.minecraft.ChatFormatting.BOLD).getVisualOrderText(), 0xFF7A1F1F));
            }
            if (!pageData.body().isBlank()) {
                for (FormattedCharSequence sequence : this.font.split(
                        Component.literal(pageData.body()), TEXT_WIDTH)) {
                    this.lines.add(new Line(sequence, 0xFF2B2B2B));
                }
            }
            this.lines.add(new Line(Component.empty().getVisualOrderText(), 0));
        }
        if (this.lines.isEmpty()) {
            this.lines.add(new Line(Component.literal("（无内容）").getVisualOrderText(), 0xFF2B2B2B));
        }
    }

    private int pageCount() {
        return Math.max(1, (this.lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
    }

    private void turnPage(int delta) {
        this.page = Math.max(0, Math.min(this.pageCount() - 1, this.page + delta));
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton != null) {
            this.previousButton.active = this.page > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.page < this.pageCount() - 1;
        }
    }

    private int panelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int px = this.panelX();
        int py = this.panelY();

        // 封面
        guiGraphics.fill(px - 2, py - 2, px + PANEL_WIDTH + 2, py + PANEL_HEIGHT + 2, 0xFF3A0A0A);
        guiGraphics.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, 0xFF5A1010);

        // 内页
        guiGraphics.fill(px + 12, py + 8, px + PANEL_WIDTH - 12, py + PANEL_HEIGHT - 8, 0xFFF2E6C8);
        guiGraphics.fill(px + 12, py + 8, px + 18, py + PANEL_HEIGHT - 8, 0xFF4A0D0D);

        guiGraphics.drawCenteredString(this.font, Component.literal(this.book.title()),
                px + PANEL_WIDTH / 2 + 3, py + 18, 0xFF5A1010);

        int textX = px + 26;
        int textY = py + 36;
        int start = this.page * LINES_PER_PAGE;
        for (int i = 0; i < LINES_PER_PAGE; i++) {
            int index = start + i;
            if (index >= this.lines.size()) break;
            Line line = this.lines.get(index);
            guiGraphics.drawString(this.font, line.text(), textX, textY + i * LINE_HEIGHT,
                    line.color(), false);
        }

        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.denunciationdelight.selected_works.page",
                        this.page + 1, this.pageCount()),
                px + PANEL_WIDTH / 2 + 3, py + PANEL_HEIGHT - 40, 0xFF7A5A3A);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.turnPage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.turnPage(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Line(FormattedCharSequence text, int color) {
    }
}