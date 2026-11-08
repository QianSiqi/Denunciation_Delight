package net.qiansiqi.denunciationdelight;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.qiansiqi.denunciationdelight.item.ModCreativeTabs;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DenunciationDelight.MOD_ID)
public class DenunciationDelight {
    public static final String MOD_ID = "denunciationdelight";
    public static final Logger LOGGER = LogManager.getLogger();

    public DenunciationDelight() {
        // 注册通用配置（客户端/服务端共享同一份玩法开关）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                net.qiansiqi.denunciationdelight.config.ModConfig.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册方块、方块实体、物品、实体类型、药水效果和创造模式标签页
        net.qiansiqi.denunciationdelight.block.ModBlocks.BLOCKS.register(modEventBus);
        net.qiansiqi.denunciationdelight.block.entity.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        net.qiansiqi.denunciationdelight.entity.ModEntities.ENTITY_TYPES.register(modEventBus);
        net.qiansiqi.denunciationdelight.effect.ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);

        // 注册到 MinecraftForge 主事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // 如果安装了 Curios 模组，注册 Curios Capability 支持
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            MinecraftForge.EVENT_BUS.register(net.qiansiqi.denunciationdelight.compat.curios.CuriosCommonCompat.class);
            LOGGER.info("[Denunciation Delight] Curios capability listener registered.");
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册网络通信包
            ModNetworking.register();
            LOGGER.info("[Denunciation Delight] Network channel initialized.");
        });
    }
}
