package net.qiansiqi.denunciationdelight.client.compat.curios;

import net.qiansiqi.denunciationdelight.item.ModItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class CuriosClientCompat {
    public static void registerRenderers() {
        CuriosRendererRegistry.register(ModItems.CONE_HAT.get(), ConeHatCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.NECK_PLACARD.get(), NeckPlacardCurioRenderer::new);
    }
}
