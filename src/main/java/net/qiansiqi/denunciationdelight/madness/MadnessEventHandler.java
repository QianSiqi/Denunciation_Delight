package net.qiansiqi.denunciationdelight.madness;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.config.ModConfig;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SyncMadnessPacket;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MadnessEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!ModConfig.ENABLE_MADNESS.get()) return;
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            if (event.player instanceof ServerPlayer serverPlayer) {
                if (MadnessManager.isPlayerSuiciding(serverPlayer)) {
                    MadnessManager.tickSuicide(serverPlayer);
                } else {
                    if (serverPlayer.tickCount % 20 == 0) {
                        MadnessManager.tickWearing(serverPlayer);
                    }

                    // 疯度 > 80 时的狂乱惩罚机制 (非创造/旁观模式)
                    if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                        int madness = MadnessManager.getMadness(serverPlayer);
                        if (madness > 80) {
                            // 1. 持续施加恶心 Buff (Confusion / Nausea)
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));

                            // 2. 持续快速消耗饱食度 (每 2 秒 / 40 ticks 扣除 1 点饱食度)
                            if (serverPlayer.tickCount % 40 == 0) {
                                FoodData foodData = serverPlayer.getFoodData();
                                if (foodData.getSaturationLevel() > 0.0F) {
                                    foodData.setSaturation(Math.max(0.0F, foodData.getSaturationLevel() - 1.5F));
                                } else if (foodData.getFoodLevel() > 0) {
                                    foodData.setFoodLevel(foodData.getFoodLevel() - 1);
                                } else {
                                    foodData.addExhaustion(2.0F);
                                }

                                // 伴随反胃胃痛的干呕不适声
                                if (serverPlayer.tickCount % 80 == 0) {
                                    serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                            SoundEvents.FOX_SPIT, SoundSource.PLAYERS, 0.7F, 0.8F);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!ModConfig.ENABLE_MADNESS.get()) return;
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            // 吃东西减少 5 点疯度 (非创造模式)
            if (!player.isCreative() && !player.isSpectator() && event.getItem().isEdible()) {
                int old = MadnessManager.getMadness(player);
                if (old > 80 && ModConfig.ENABLE_SUICIDE.get()) {
                    // 疯度 > 80 时重度狂乱恶心，进食无法降低疯度；
                    // 若服务器关闭了自裁，则允许进食恢复，避免疯度卡在 100
                    player.displayClientMessage(
                            Component.translatable("message.denunciationdelight.madness_eat_blocked", old),
                            true
                    );
                } else if (old > 0) {
                    MadnessManager.addMadness(player, -5);
                    int now = MadnessManager.getMadness(player);
                    player.displayClientMessage(
                            Component.translatable("message.denunciationdelight.madness_eat_reduced", now),
                            true
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int madness = MadnessManager.getMadness(player);
            ModNetworking.sendToPlayer(new SyncMadnessPacket(madness), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int madness = MadnessManager.getMadness(player);
            ModNetworking.sendToPlayer(new SyncMadnessPacket(madness), player);

            // 重生清除泼墨状态并通知全客户端
            player.removeEffect(net.qiansiqi.denunciationdelight.effect.ModMobEffects.INKED.get());
            ModNetworking.sendToTracking(new net.qiansiqi.denunciationdelight.network.SyncInkedPacket(player.getId(), 0), player);
            ModNetworking.sendToPlayer(new net.qiansiqi.denunciationdelight.network.SyncInkedPacket(player.getId(), 0), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 死亡重生重置为 0 并清理自杀标记
            MadnessManager.setMadness(event.getEntity(), 0);
            if (event.getEntity() instanceof ServerPlayer sp) {
                MadnessManager.cleanupSuicide(sp);
                sp.removeEffect(net.qiansiqi.denunciationdelight.effect.ModMobEffects.INKED.get());
                ModNetworking.sendToTracking(new net.qiansiqi.denunciationdelight.network.SyncInkedPacket(sp.getId(), 0), sp);
                ModNetworking.sendToPlayer(new net.qiansiqi.denunciationdelight.network.SyncInkedPacket(sp.getId(), 0), sp);
            }
            if (event.getOriginal() instanceof ServerPlayer oldSp) {
                oldSp.removeEffect(net.qiansiqi.denunciationdelight.effect.ModMobEffects.INKED.get());
                ModNetworking.sendToTracking(new net.qiansiqi.denunciationdelight.network.SyncInkedPacket(oldSp.getId(), 0), oldSp);
            }
        } else {
            // 跨维度等保留疯度
            int old = MadnessManager.getMadness(event.getOriginal());
            MadnessManager.setMadness(event.getEntity(), old);
        }
    }
}
