package net.qiansiqi.denunciationdelight.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.qiansiqi.denunciationdelight.config.ModConfig;
import net.qiansiqi.denunciationdelight.effect.ModMobEffects;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SyncInkedPacket;

import java.util.List;

public class ThrownInkBottle extends ThrowableItemProjectile {

    public ThrownInkBottle(EntityType<? extends ThrownInkBottle> type, Level level) {
        super(type, level);
    }

    public ThrownInkBottle(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_INK_BOTTLE.get(), shooter, level);
    }

    public ThrownInkBottle(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_INK_BOTTLE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INK_BOTTLE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 统一在 onHit 中结算，避免直接命中 + 范围溅射对同一目标重复生效
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        Level level = this.level();
        if (level.isClientSide()) return;

        // 播放玻璃瓶破碎与墨汁爆溅音效
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8F, 1.2F);

        // 产生浓烈黑墨爆散粒子
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY(), this.getZ(),
                    45, 0.45D, 0.45D, 0.45D, 0.09D);
        }

        // 直接命中的目标只结算一次；范围溅射跳过该目标
        LivingEntity directTarget = result instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living ? living : null;
        if (directTarget != null) {
            applyInkEffect(directTarget, true);
        }

        // 范围飞溅（周围 2.5 格内的所有玩家和 NPC 都会被泼上一身黑墨水）
        AABB splashArea = this.getBoundingBox().inflate(2.5D, 1.5D, 2.5D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, splashArea);
        for (LivingEntity target : targets) {
            if (target != directTarget && this.distanceToSqr(target) <= 6.25D) {
                applyInkEffect(target, false);
            }
        }

        this.discard();
    }

    private void applyInkEffect(LivingEntity target, boolean directHit) {
        if (!ModConfig.ENABLE_INK.get()) return;

        Level level = this.level();
        int inkDuration = directHit ? 3600 : 2400; // 3 分钟或 2 分钟
        int blindDuration = directHit ? 200 : 120; // 10 秒或 6 秒
        int slowDuration = directHit ? 300 : 180;  // 15 秒或 9 秒

        // 1. 施加泼墨污渍效果并同步客户端
        target.addEffect(new MobEffectInstance(ModMobEffects.INKED.get(), inkDuration, directHit ? 1 : 0));
        ModNetworking.sendToTracking(new SyncInkedPacket(target.getId(), inkDuration), target);

        // 2. 墨汁糊眼：失明与减速（令被泼墨者无法看清、行动踉跄）
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, blindDuration, 0));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, slowDuration, directHit ? 1 : 0));

        // 3. 头部爆出墨滴粒子
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getEyeY(), target.getZ(),
                    18, 0.25D, 0.25D, 0.25D, 0.05D);
        }

        // 4. 针对 NPC (村民) 的专属生动恐慌与愤怒反应
        if (target instanceof net.minecraft.world.entity.npc.Villager villager) {
            // 播放村民愤怒而惊恐的哀嚎
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.2F, 0.85F);

            // 冒出愤怒云粒子
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() + 0.4D, target.getZ(),
                        5, 0.25D, 0.25D, 0.25D, 0.0D);
            }

            // 激发村民逃跑机制（恐慌狂奔）
            Entity shooter = this.getOwner();
            if (shooter instanceof LivingEntity livingShooter) {
                villager.setLastHurtByMob(livingShooter);
            }
        } else if (target instanceof net.minecraft.world.entity.Mob mob) {
            // 怪物被糊了墨水后瞬间丧失仇恨与视野
            mob.setTarget(null);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.8F, 1.6F);
        } else if (target instanceof net.minecraft.world.entity.player.Player playerTarget) {
            // 疯度 +20 (创造模式内部自动豁免；疯度系统关闭时不提示)
            if (ModConfig.ENABLE_MADNESS.get()) {
                net.qiansiqi.denunciationdelight.madness.MadnessManager.addMadness(playerTarget, 20);
                int m = net.qiansiqi.denunciationdelight.madness.MadnessManager.getMadness(playerTarget);
                playerTarget.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.denunciationdelight.ink_hit", m),
                        true
                );
            }
        }
    }
}
