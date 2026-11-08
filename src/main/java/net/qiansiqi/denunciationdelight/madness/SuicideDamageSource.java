package net.qiansiqi.denunciationdelight.madness;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class SuicideDamageSource extends DamageSource {
    private final int suicideType;

    public SuicideDamageSource(Holder<DamageType> typeHolder, int suicideType) {
        super(typeHolder);
        this.suicideType = suicideType;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        if (suicideType == MadnessManager.TYPE_GUN) {
            return Component.translatable("death.attack.denunciationdelight.suicide_gun", entity.getDisplayName());
        } else if (suicideType == MadnessManager.TYPE_BLADE) {
            return Component.translatable("death.attack.denunciationdelight.suicide_blade", entity.getDisplayName());
        } else {
            return Component.translatable("death.attack.denunciationdelight.suicide_wall", entity.getDisplayName());
        }
    }
}
