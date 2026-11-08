package net.qiansiqi.denunciationdelight.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DenunciationDelight.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.denunciationdelight.tab"))
                    .icon(() -> new ItemStack(ModItems.CONE_HAT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CONE_HAT.get());
                        output.accept(ModItems.NECK_PLACARD.get());
                        output.accept(ModItems.INK_BOTTLE.get());
                        output.accept(net.qiansiqi.denunciationdelight.block.ModBlocks.SLOGAN_BANNER.get());
                        output.accept(net.qiansiqi.denunciationdelight.block.ModBlocks.WALL_PORTRAIT.get());
                        output.accept(net.qiansiqi.denunciationdelight.block.ModBlocks.NATIONAL_FLAG.get());
                    })
                    .build());
}
