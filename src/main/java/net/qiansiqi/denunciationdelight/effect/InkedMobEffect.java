package net.qiansiqi.denunciationdelight.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class InkedMobEffect extends MobEffect {

    public InkedMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x1A1A22);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 如果泡在水里或雨中，加速洗刷褪色并产生水中墨水粒子
        if (entity.isInWaterOrRain()) {
            if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
                if (entity.getRandom().nextFloat() < 0.25F) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                            entity.getX(), entity.getY(0.5D), entity.getZ(),
                            2, 0.2D, 0.2D, 0.2D, 0.02D);
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
