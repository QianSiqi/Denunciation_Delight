package net.qiansiqi.denunciationdelight.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.qiansiqi.denunciationdelight.block.entity.NationalFlagBlockEntity;
import net.qiansiqi.denunciationdelight.flag.FlagRegistry;
import net.qiansiqi.denunciationdelight.flag.FlagRegistry.FlagDef;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SelectFlagPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FlagSelectScreen extends Screen {
    private static final int COLUMNS = 2;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;

    private final NationalFlagBlockEntity flagBE;
    private final List<FlagButton> flagButtons = new ArrayList<>();

    private EditBox searchBox;
    private Button previousButton;
    private Button nextButton;
    private String query = "";
    private int page;
    private int filteredCount;

    public FlagSelectScreen(NationalFlagBlockEntity flagBE) {
        super(Component.translatable("gui.denunciationdelight.flag_select.title"));
        this.flagBE = flagBE;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;

        this.searchBox = new EditBox(this.font, cx - 100, 30, 200, 20,
                Component.translatable("gui.denunciationdelight.flag_select.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Component.translatable("gui.denunciationdelight.flag_select.search"));
        this.searchBox.setResponder(value -> {
            this.query = value == null ? "" : value;
            this.page = 0;
            this.rebuild();
        });
        this.addRenderableWidget(this.searchBox);

        this.previousButton = this.addRenderableWidget(
                Button.builder(Component.literal("<"), b -> {
                    this.page = Math.max(0, this.page - 1);
                    this.rebuild();
                }).bounds(cx - 90, this.height - 30, 30, 20).build());

        this.nextButton = this.addRenderableWidget(
                Button.builder(Component.literal(">"), b -> {
                    this.page++;
                    this.rebuild();
                }).bounds(cx - 55, this.height - 30, 30, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(cx + 40, this.height - 30, 60, 20).build());

        this.rebuild();
    }

    private void rebuild() {
        for (FlagButton button : this.flagButtons) {
            this.removeWidget(button);
        }
        this.flagButtons.clear();

        List<FlagDef> filtered = this.filteredFlags();
        this.filteredCount = filtered.size();
        int pages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.page = Mth.clamp(this.page, 0, pages - 1);

        int cx = this.width / 2;
        int startIndex = this.page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = startIndex + i;
            if (index >= filtered.size()) break;
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int x = cx - BUTTON_WIDTH - (GAP / 2) + column * (BUTTON_WIDTH + GAP);
            int y = 58 + row * (BUTTON_HEIGHT + 2);
            FlagDef flag = filtered.get(index);
            FlagButton button = new FlagButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, flag,
                    b -> {
                        ModNetworking.sendToServer(new SelectFlagPacket(this.flagBE.getBlockPos(), flag.id()));
                        this.onClose();
                    });
            this.addRenderableWidget(button);
            this.flagButtons.add(button);
        }

        if (this.previousButton != null) {
            this.previousButton.active = this.page > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.page < pages - 1;
        }
    }

    private List<FlagDef> filteredFlags() {
        String normalized = this.query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return FlagRegistry.all();
        }
        List<FlagDef> result = new ArrayList<>();
        for (FlagDef flag : FlagRegistry.all()) {
            if (flag.id().contains(normalized)
                    || flag.en().toLowerCase(Locale.ROOT).contains(normalized)
                    || flag.zh().toLowerCase(Locale.ROOT).contains(normalized)) {
                result.add(flag);
            }
        }
        return result;
    }

    private static boolean isChinese() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, cx, 14, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.denunciationdelight.flag_select.page",
                        this.page + 1,
                        Math.max(1, (this.filteredCount + PAGE_SIZE - 1) / PAGE_SIZE),
                        this.filteredCount),
                cx, this.height - 43, 0xAAAAAA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class FlagButton extends Button {
        private final FlagDef flag;

        private FlagButton(int x, int y, int width, int height, FlagDef flag, OnPress onPress) {
            super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.flag = flag;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            ResourceLocation texture = FlagRegistry.texture(this.flag.id());
            guiGraphics.blit(texture, this.getX() + 3, this.getY() + 5, 0, 0, 14, 9, 64, 40);

            String name = this.flag.displayName(isChinese());
            name = Minecraft.getInstance().font.plainSubstrByWidth(name, BUTTON_WIDTH - 23);
            guiGraphics.drawString(Minecraft.getInstance().font, name,
                    this.getX() + 21, this.getY() + 6, 0xFFFFFF, false);
        }
    }
}