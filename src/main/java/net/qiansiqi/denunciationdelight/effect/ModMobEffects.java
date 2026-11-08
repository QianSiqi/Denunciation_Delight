package net.qiansiqi.denunciationdelight.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DenunciationDelight.MOD_ID);

    public static final RegistryObject<MobEffect> INKED = MOB_EFFECTS.register("inked", InkedMobEffect::new);
}
