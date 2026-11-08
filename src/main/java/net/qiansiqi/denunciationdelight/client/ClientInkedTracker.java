package net.qiansiqi.denunciationdelight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.effect.ModMobEffects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientInkedTracker {

    public static class InkedInfo {
        public final long expireGameTime;
        public final int maxDuration;

        public InkedInfo(long expireGameTime, int maxDuration) {
            this.expireGameTime = expireGameTime;
            this.maxDuration = maxDuration;
        }
    }

    private static final Map<Integer, InkedInfo> INKED_MAP = new ConcurrentHashMap<>();

    public static void setInked(int entityId, int durationTicks) {
        if (durationTicks <= 0) {
            INKED_MAP.remove(entityId);
            return;
        }
        long currentTick = 0;
        if (Minecraft.getInstance().level != null) {
            currentTick = Minecraft.getInstance().level.getGameTime();
        }
        INKED_MAP.put(entityId, new InkedInfo(currentTick + durationTicks, durationTicks));
    }

    public static void clear() {
        INKED_MAP.clear();
    }

    public static boolean isInked(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            if (entity != null) {
                INKED_MAP.remove(entity.getId());
            }
            return false;
        }

        // 1. 如果实体是本地玩家自身：本地玩家的原版药水状态由客户端网络强保证同步
        // 若本地玩家身上没有 INKED 效果（例如死亡重生后、喝牛奶后），必须判定为未被泼墨，并清除残留缓存
        if (Minecraft.getInstance().player != null && entity.getId() == Minecraft.getInstance().player.getId()) {
            if (!entity.hasEffect(ModMobEffects.INKED.get())) {
                INKED_MAP.remove(entity.getId());
                return false;
            }
            return true;
        }

        // 2. 如果实体自带原版效果 (例如本地玩家自身)
        if (entity.hasEffect(ModMobEffects.INKED.get())) {
            return true;
        }

        // 3. 检查追踪映射 (NPC、其他玩家、生物等)
        if (Minecraft.getInstance().level != null) {
            long currentTick = Minecraft.getInstance().level.getGameTime();
            InkedInfo info = INKED_MAP.get(entity.getId());
            if (info != null) {
                if (currentTick <= info.expireGameTime) {
                    return true;
                } else {
                    INKED_MAP.remove(entity.getId());
                }
            }
        }
        return false;
    }

    public static float getAlpha(LivingEntity entity) {
        if (!isInked(entity)) return 0.0F;

        // 原版效果
        if (entity.hasEffect(ModMobEffects.INKED.get())) {
            var effect = entity.getEffect(ModMobEffects.INKED.get());
            if (effect != null && effect.getDuration() < 40) {
                return Math.max(0.0F, effect.getDuration() / 40.0F);
            }
            return 1.0F;
        }

        // 同步字典
        if (Minecraft.getInstance().level != null) {
            long currentTick = Minecraft.getInstance().level.getGameTime();
            InkedInfo info = INKED_MAP.get(entity.getId());
            if (info != null) {
                long remaining = info.expireGameTime - currentTick;
                if (remaining <= 0) {
                    INKED_MAP.remove(entity.getId());
                    return 0.0F;
                }
                if (remaining < 40) {
                    return Math.max(0.0F, remaining / 40.0F);
                }
                return 1.0F;
            }
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        if (event.getOldPlayer() != null) {
            setInked(event.getOldPlayer().getId(), 0);
        }
        if (event.getNewPlayer() != null) {
            setInked(event.getNewPlayer().getId(), 0);
        }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
