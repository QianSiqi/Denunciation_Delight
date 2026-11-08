package net.qiansiqi.denunciationdelight.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.RequestPortraitPacket;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientPortraitCache {
    private static final Map<String, ResourceLocation> TEXTURE_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    public static final ResourceLocation DEFAULT_PORTRAIT = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/block/default_portrait.png");

    public static ResourceLocation getOrRequestTexture(String portraitId) {
        if (portraitId == null || portraitId.isEmpty()) {
            return DEFAULT_PORTRAIT;
        }

        ResourceLocation loc = TEXTURE_MAP.get(portraitId);
        if (loc != null) {
            return loc;
        }

        if (REQUESTED.add(portraitId)) {
            // 向服务端发起请求获取图片数据
            ModNetworking.sendToServer(new RequestPortraitPacket(portraitId));
        }

        return DEFAULT_PORTRAIT;
    }

    /**
     * 退出服务器时释放动态纹理并清空请求缓存，避免换服后继续用旧图/旧纹理。
     */
    public static void clear() {
        for (ResourceLocation loc : TEXTURE_MAP.values()) {
            Minecraft.getInstance().getTextureManager().release(loc);
        }
        TEXTURE_MAP.clear();
        REQUESTED.clear();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(ClientPortraitCache::clear);
    }

    public static void registerImage(String portraitId, byte[] data) {
        if (data == null || data.length == 0) return;
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(data));
                DynamicTexture texture = new DynamicTexture(nativeImage);
                ResourceLocation loc = new ResourceLocation(DenunciationDelight.MOD_ID, "portrait_" + portraitId.toLowerCase());
                Minecraft.getInstance().getTextureManager().register(loc, texture);
                TEXTURE_MAP.put(portraitId, loc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
