package net.qiansiqi.denunciationdelight.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.client.model.ConeHatModel;
import net.qiansiqi.denunciationdelight.client.model.NeckPlacardModel;
import net.qiansiqi.denunciationdelight.client.renderer.PlacardAndHatTextLayer;
import net.qiansiqi.denunciationdelight.client.compat.curios.CuriosClientCompat;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ConeHatModel.LAYER_LOCATION, ConeHatModel::createBodyLayer);
        event.registerLayerDefinition(NeckPlacardModel.LAYER_LOCATION, NeckPlacardModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(net.qiansiqi.denunciationdelight.block.entity.ModBlockEntities.SLOGAN_BANNER.get(),
                net.qiansiqi.denunciationdelight.client.renderer.SloganBannerRenderer::new);
        event.registerBlockEntityRenderer(net.qiansiqi.denunciationdelight.block.entity.ModBlockEntities.WALL_PORTRAIT.get(),
                net.qiansiqi.denunciationdelight.client.renderer.WallPortraitRenderer::new);
        event.registerBlockEntityRenderer(net.qiansiqi.denunciationdelight.block.entity.ModBlockEntities.NATIONAL_FLAG.get(),
                net.qiansiqi.denunciationdelight.client.renderer.NationalFlagRenderer::new);

        // 注册投掷墨水瓶实体渲染器
        event.registerEntityRenderer(net.qiansiqi.denunciationdelight.entity.ModEntities.THROWN_INK_BOTTLE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.EXPERIENCE_BAR.id(),
                "madness_overlay", net.qiansiqi.denunciationdelight.client.gui.MadnessOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // 为所有玩家模型皮肤（default 和 slim）挂载挂牌与高帽文字渲染层、泼墨污渍层
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>> renderer = event.getSkin(skin);
            if (renderer != null) {
                addCustomLayers(renderer);
            }
        }

        // 为盔甲架挂载
        LivingEntityRenderer<ArmorStand, ? extends EntityModel<ArmorStand>> armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer != null) {
            addCustomLayers(armorStandRenderer);
        }

        // 为僵尸挂载
        LivingEntityRenderer<Zombie, ? extends EntityModel<Zombie>> zombieRenderer = event.getRenderer(EntityType.ZOMBIE);
        if (zombieRenderer != null) {
            addCustomLayers(zombieRenderer);
        }

        // 为村民挂载专属批斗层 (3D高帽 + 3D挂牌 + 文字 + 泼墨)
        LivingEntityRenderer<net.minecraft.world.entity.npc.Villager, ? extends EntityModel<net.minecraft.world.entity.npc.Villager>> villagerRenderer = event.getRenderer(EntityType.VILLAGER);
        if (villagerRenderer != null) {
            addVillagerLayers(villagerRenderer);
        }

        // 为流浪商人挂载专属批斗层
        LivingEntityRenderer<net.minecraft.world.entity.npc.WanderingTrader, ? extends EntityModel<net.minecraft.world.entity.npc.WanderingTrader>> traderRenderer = event.getRenderer(EntityType.WANDERING_TRADER);
        if (traderRenderer != null) {
            addVillagerLayers(traderRenderer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addVillagerLayers(LivingEntityRenderer renderer) {
        renderer.addLayer(new net.qiansiqi.denunciationdelight.client.renderer.VillagerDelightLayer(renderer));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends LivingEntity, M extends EntityModel<T>> void addCustomLayers(LivingEntityRenderer<T, M> renderer) {
        renderer.addLayer(new PlacardAndHatTextLayer(renderer));
        renderer.addLayer(new net.qiansiqi.denunciationdelight.client.renderer.InkSplatterLayer(renderer));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("curios")) {
                CuriosClientCompat.registerRenderers();
                DenunciationDelight.LOGGER.info("[Denunciation Delight] Curios renderers registered.");
            }
        });
    }
}
