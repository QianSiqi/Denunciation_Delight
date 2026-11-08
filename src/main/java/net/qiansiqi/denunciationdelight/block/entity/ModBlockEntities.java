package net.qiansiqi.denunciationdelight.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DenunciationDelight.MOD_ID);

    public static final RegistryObject<BlockEntityType<SloganBannerBlockEntity>> SLOGAN_BANNER =
            BLOCK_ENTITIES.register("slogan_banner", () ->
                    BlockEntityType.Builder.of(SloganBannerBlockEntity::new, ModBlocks.SLOGAN_BANNER.get()).build(null));

    public static final RegistryObject<BlockEntityType<WallPortraitBlockEntity>> WALL_PORTRAIT =
            BLOCK_ENTITIES.register("wall_portrait", () ->
                    BlockEntityType.Builder.of(WallPortraitBlockEntity::new, ModBlocks.WALL_PORTRAIT.get()).build(null));

    public static final RegistryObject<BlockEntityType<NationalFlagBlockEntity>> NATIONAL_FLAG =
            BLOCK_ENTITIES.register("national_flag", () ->
                    BlockEntityType.Builder.of(NationalFlagBlockEntity::new, ModBlocks.NATIONAL_FLAG.get()).build(null));
}
