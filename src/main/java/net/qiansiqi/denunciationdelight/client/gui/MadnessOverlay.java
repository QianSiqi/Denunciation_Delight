package net.qiansiqi.denunciationdelight.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.qiansiqi.denunciationdelight.config.ModConfig;
import net.qiansiqi.denunciationdelight.madness.MadnessManager;

public class MadnessOverlay implements IGuiOverlay {
    public static final MadnessOverlay INSTANCE = new MadnessOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ModConfig.ENABLE_MADNESS.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        int madness = MadnessManager.getClientMadness();
        boolean hasWear = MadnessManager.isWearingConeHat(player) || MadnessManager.isWearingPlacard(player);

        // 如果疯度为 0 且未佩戴高帽/挂牌，不遮挡屏幕
        if (madness <= 0 && !hasWear) {
            return;
        }

        Font font = mc.font;

        // 渲染位置：屏幕中央正下方，在经验条上方
        int barWidth = 84;
        int barHeight = 7;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight - 48; // 位于快捷栏和经验条上方

        // 1. 底框背景
        guiGraphics.fill(x - 2, y - 11, x + barWidth + 2, y + barHeight + 2, 0xAA111115);
        guiGraphics.renderOutline(x - 2, y - 11, barWidth + 4, barHeight + 13, 0xFF444450);

        // 2. 标题文字
        String title = Component.translatable("hud.denunciationdelight.madness", madness).getString();
        if (madness >= 80) {
            long time = System.currentTimeMillis();
            if ((time / 250) % 2 == 0) {
                title = Component.translatable("hud.denunciationdelight.madness_critical", madness).getString();
            }
        }
        int textX = (screenWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, textX, y - 9, 0xFFFFFFFF, false);

        // 3. 槽底背景
        guiGraphics.fill(x, y + 1, x + barWidth, y + barHeight, 0xFF222226);

        // 4. 进度条填充颜色
        int fillWidth = (int) ((madness / 100.0F) * barWidth);
        if (fillWidth > 0) {
            int color;
            if (madness < 40) {
                color = 0xFF55FF55; // 绿
            } else if (madness < 70) {
                color = 0xFFFFBB00; // 橙黄
            } else if (madness < 85) {
                color = 0xFFFF4422; // 橙红
            } else {
                color = 0xFFFF0000; // 猩红
            }
            guiGraphics.fill(x, y + 1, x + fillWidth, y + barHeight, color);
        }

        // 5. 高疯度屏幕边缘猩红暗角
        if (madness >= 75) {
            float alphaFactor = (madness - 70) / 30.0F; // 0.16 ~ 1.0
            float pulse = (float) (0.5F + 0.5F * Math.sin(System.currentTimeMillis() / 200.0D));
            int alpha = (int) (40 * alphaFactor * pulse);
            if (alpha > 0) {
                int vignetteColor = (alpha << 24) | 0xAA0000;
                guiGraphics.fill(0, 0, screenWidth, 6, vignetteColor);
                guiGraphics.fill(0, screenHeight - 6, screenWidth, screenHeight, vignetteColor);
                guiGraphics.fill(0, 6, 6, screenHeight - 6, vignetteColor);
                guiGraphics.fill(screenWidth - 6, 6, screenWidth, screenHeight - 6, vignetteColor);
            }
        }

        // 6. 自杀致命瞬间全屏血幕剧烈闪现
        if (net.qiansiqi.denunciationdelight.client.ClientSuicideTracker.isSuiciding(player.getId())) {
            var anim = net.qiansiqi.denunciationdelight.client.ClientSuicideTracker.getAnimData(player.getId());
            if (anim != null && anim.elapsedTicks >= 22) {
                float flash = Math.max(0.0F, 1.0F - (anim.elapsedTicks - 22) / 8.0F);
                int flashAlpha = (int) (180 * flash);
                if (flashAlpha > 0) {
                    guiGraphics.fill(0, 0, screenWidth, screenHeight, (flashAlpha << 24) | 0x880000);
                }
            }
        }
    }
}
