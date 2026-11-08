package net.qiansiqi.denunciationdelight.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DenunciationDelight.MOD_ID);

    public static final RegistryObject<EntityType<ThrownInkBottle>> THROWN_INK_BOTTLE =
            ENTITY_TYPES.register("thrown_ink_bottle",
                    () -> EntityType.Builder.<ThrownInkBottle>of(ThrownInkBottle::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("thrown_ink_bottle"));
}
