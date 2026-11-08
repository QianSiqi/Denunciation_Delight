package net.qiansiqi.denunciationdelight.client;

import net.minecraft.client.Minecraft;
import net.qiansiqi.denunciationdelight.block.entity.NationalFlagBlockEntity;
import net.qiansiqi.denunciationdelight.block.entity.SloganBannerBlockEntity;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.client.gui.FlagSelectScreen;
import net.qiansiqi.denunciationdelight.client.gui.PortraitEditScreen;
import net.qiansiqi.denunciationdelight.client.gui.SloganBannerEditScreen;

/**
 * 方块 GUI 的客户端入口，common 方块类只通过 DistExecutor 延迟调用这里。
 */
public final class BlockClientInteractions {
    private BlockClientInteractions() {
    }

    public static void openSloganBanner(SloganBannerBlockEntity bannerBE) {
        Minecraft.getInstance().setScreen(new SloganBannerEditScreen(bannerBE));
    }

    public static void openWallPortrait(WallPortraitBlockEntity portraitBE) {
        Minecraft.getInstance().setScreen(new PortraitEditScreen(portraitBE));
    }

    public static void openNationalFlag(NationalFlagBlockEntity flagBE) {
        Minecraft.getInstance().setScreen(new FlagSelectScreen(flagBE));
    }
}