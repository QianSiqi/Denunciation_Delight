package net.qiansiqi.denunciationdelight.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DenunciationDelight.MOD_ID);

    public static final RegistryObject<Block> SLOGAN_BANNER = registerBlock("slogan_banner",
            () -> new SloganBannerBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).noCollission().strength(0.5F)));

    public static final RegistryObject<Block> WALL_PORTRAIT = registerBlock("wall_portrait",
            () -> new WallPortraitBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.0F)));

    public static final RegistryObject<Block> NATIONAL_FLAG = registerBlock("national_flag",
            () -> new NationalFlagBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).strength(0.8F).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> blockSupplier) {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
