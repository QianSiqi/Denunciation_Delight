package net.qiansiqi.denunciationdelight.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.effect.ModMobEffects;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SyncInkedPacket;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommonEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        if (target instanceof LivingEntity livingTarget) {
            boolean isHat = stack.is(ModItems.CONE_HAT.get());
            boolean isPlacard = stack.is(ModItems.NECK_PLACARD.get());

            // 1. 给村民、NPC 或其它生物强行戴高帽或挂牌
            if (isHat || isPlacard) {
                Level level = player.level();
                EquipmentSlot slot = isHat ? EquipmentSlot.HEAD : EquipmentSlot.CHEST;

                if (!level.isClientSide()) {
                    ItemStack existing = livingTarget.getItemBySlot(slot);
                    ItemStack toEquip = stack.copy();
                    toEquip.setCount(1);

                    // 弹出并保护被替换的原装备，防止吞装
                    if (!existing.isEmpty()) {
                        if (livingTarget instanceof Player targetPlayer) {
                            if (!targetPlayer.getInventory().add(existing)) {
                                targetPlayer.drop(existing, false);
                            }
                        } else {
                            livingTarget.spawnAtLocation(existing);
                        }
                    }

                    livingTarget.setItemSlot(slot, toEquip);
                    if (livingTarget instanceof Mob mob) {
                        mob.setDropChance(slot, 1.0F); // 确保掉落物保护
                    }

                    // 播放穿戴音效
                    level.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                            SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);

                    Component itemName = Component.translatable(isHat
                            ? "message.denunciationdelight.item.hat"
                            : "message.denunciationdelight.item.placard");
                    if (livingTarget instanceof Player targetPlayer) {
                        targetPlayer.displayClientMessage(Component.translatable(
                                "message.denunciationdelight.force_equip.victim", player.getName(), itemName), true);
                    } else if (livingTarget instanceof Villager) {
                        level.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.9F);
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.force_equip.sender", livingTarget.getName(), itemName), true);

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }

                // 立即拦截后续交互，阻止村民交易界面打开
                event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
                event.setCanceled(true);
                return;
            }

            // 2. 被泼墨状态下的村民拒绝交易反应
            if (livingTarget instanceof Villager villager && villager.hasEffect(ModMobEffects.INKED.get())) {
                Level level = player.level();
                if (!level.isClientSide()) {
                    level.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.2F, 0.8F);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                                villager.getX(), villager.getY() + villager.getBbHeight() + 0.4D, villager.getZ(),
                                4, 0.25D, 0.25D, 0.25D, 0.0D);
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.inked_villager"), true);
                }
                event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity livingTarget) {
            MobEffectInstance inked = livingTarget.getEffect(ModMobEffects.INKED.get());
            if (inked != null && inked.getDuration() > 0) {
                if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                    ModNetworking.sendToPlayer(new SyncInkedPacket(livingTarget.getId(), inked.getDuration()), serverPlayer);
                }
            }
            if (livingTarget instanceof Player targetPlayer && net.qiansiqi.denunciationdelight.madness.MadnessManager.isPlayerSuiciding(targetPlayer)) {
                if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                    int type = targetPlayer.getPersistentData().getInt(net.qiansiqi.denunciationdelight.madness.MadnessManager.TAG_SUICIDE_TYPE);
                    ModNetworking.sendToPlayer(new net.qiansiqi.denunciationdelight.network.SyncSuicideAnimationPacket(targetPlayer.getId(), type, 25), serverPlayer);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.INKED.get()) {
            ModNetworking.sendToTracking(new SyncInkedPacket(event.getEntity().getId(), 0), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModMobEffects.INKED.get()) {
            ModNetworking.sendToTracking(new SyncInkedPacket(event.getEntity().getId(), 0), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModMobEffects.INKED.get())) {
            entity.removeEffect(ModMobEffects.INKED.get());
            ModNetworking.sendToTracking(new SyncInkedPacket(entity.getId(), 0), entity);
        }
    }
}
