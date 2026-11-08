package net.qiansiqi.denunciationdelight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DenunciationDelight.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientSuicideTracker {

    public static final int TYPE_GUN = 0;
    public static final int TYPE_BLADE = 1;
    public static final int TYPE_WALL_SLAM = 2;

    public static class AnimData {
        public final int type;
        public final int totalTicks;
        public int elapsedTicks;

        public AnimData(int type, int totalTicks) {
            this.type = type;
            this.totalTicks = totalTicks;
            this.elapsedTicks = 0;
        }
    }

    private static final Map<Integer, AnimData> ACTIVE_ANIMS = new ConcurrentHashMap<>();

    public static void startAnimation(int entityId, int type, int totalTicks) {
        ACTIVE_ANIMS.put(entityId, new AnimData(type, totalTicks));
    }

    public static boolean isSuiciding(int entityId) {
        return ACTIVE_ANIMS.containsKey(entityId);
    }

    public static AnimData getAnimData(int entityId) {
        return ACTIVE_ANIMS.get(entityId);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().isPaused()) return;

        ACTIVE_ANIMS.entrySet().removeIf(entry -> {
            AnimData data = entry.getValue();
            data.elapsedTicks++;
            return data.elapsedTicks > data.totalTicks + 10;
        });
    }

    /**
     * 拦截并修改玩家模型的肢体骨骼旋转，实现逼真的自裁动作
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        AnimData anim = ACTIVE_ANIMS.get(player.getId());
        if (anim == null) return;

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        float partialTick = event.getPartialTick();
        float progress = (anim.elapsedTicks + partialTick);

        // 强行保持双手可见并摆出特殊绝望动作
        model.rightArm.visible = true;
        model.leftArm.visible = true;

        if (anim.type == TYPE_GUN) {
            applyGunSuicidePose(model, progress);
        } else if (anim.type == TYPE_BLADE) {
            applyBladeSuicidePose(model, progress);
        } else if (anim.type == TYPE_WALL_SLAM) {
            applyWallSlamPose(model, progress);
        }
    }

    /**
     * 动作一：持枪对准太阳穴，扣动扳机饮弹自尽
     */
    private static void applyGunSuicidePose(PlayerModel<?> model, float progress) {
        // 前 20 刻：缓缓抬起手枪贴在右侧太阳穴上，全身绝望颤抖
        float t = Mth.clamp(progress / 20.0F, 0.0F, 1.0F);
        float ease = t * t * (3.0F - 2.0F * t); // 平滑过渡

        // 细微肌肉颤栗抖动
        float tremble = (float) Math.sin(progress * 1.6F) * 0.035F;

        if (progress < 24.0F) {
            // 右手臂抬起并向内弯折，枪口贴近头部太阳穴
            model.rightArm.xRot = -1.45F * ease + tremble;
            model.rightArm.yRot = -1.15F * ease;
            model.rightArm.zRot = 0.55F * ease + tremble;

            // 左臂无力垂在身体一侧微微颤抖
            model.leftArm.xRot = 0.1F + tremble;
            model.leftArm.zRot = -0.15F;

            // 头部微微右偏偏向枪口，仿佛在感知冰冷枪管
            model.head.zRot = 0.22F * ease;
            model.head.yRot = 0.18F * ease + tremble;
            model.hat.copyFrom(model.head);
        } else {
            // 开枪击发瞬间（第 24 刻之后）：巨大后坐力将头部和手臂剧烈震飞
            float shotRecoil = (progress - 24.0F) / 4.0F; // 0.0 ~ 1.0
            model.head.zRot = -0.75F + shotRecoil * 0.2F;
            model.head.xRot = -0.65F;
            model.head.yRot = -0.35F;
            model.hat.copyFrom(model.head);

            // 手臂被后坐力向后上推开
            model.rightArm.xRot = -1.85F;
            model.rightArm.yRot = -0.6F;
            model.rightArm.zRot = 0.9F;
        }
    }

    /**
     * 动作二：横剑架在咽喉处，决绝挥刀自刎
     */
    private static void applyBladeSuicidePose(PlayerModel<?> model, float progress) {
        float t = Mth.clamp(progress / 18.0F, 0.0F, 1.0F);
        float ease = t * t * (3.0F - 2.0F * t);
        float tremble = (float) Math.sin(progress * 2.0F) * 0.04F;

        if (progress < 24.0F) {
            // 前 20 刻：右手将宝剑横架在咽喉前方
            model.rightArm.xRot = -1.15F * ease + tremble;
            model.rightArm.yRot = -0.95F * ease;
            model.rightArm.zRot = 0.75F * ease;

            // 左手抬起紧紧抓在剑柄处颤抖辅助
            model.leftArm.xRot = -1.05F * ease;
            model.leftArm.yRot = 0.85F * ease;
            model.leftArm.zRot = -0.65F * ease + tremble;

            // 头部后仰露出脖子，身体微微颤抖
            model.head.xRot = -0.35F * ease;
            model.head.yRot = tremble;
            model.hat.copyFrom(model.head);
        } else {
            // 第 24 刻割喉刹那：右手猛然向右后方强力挥拉划开
            float slashProgress = Mth.clamp((progress - 24.0F) / 4.0F, 0.0F, 1.0F);
            model.rightArm.xRot = -0.6F;
            model.rightArm.yRot = 1.25F * slashProgress;
            model.rightArm.zRot = 0.3F;

            // 头颅无力向前下方耷拉垂落
            model.head.xRot = 0.85F;
            model.head.zRot = 0.35F;
            model.hat.copyFrom(model.head);
        }
    }

    /**
     * 动作三：发狂俯冲撞墙，头部反弹瘫软
     */
    private static void applyWallSlamPose(PlayerModel<?> model, float progress) {
        if (progress < 16.0F) {
            // 疯狂俯冲冲锋：身体前倾，双臂向后甩开
            model.body.xRot = 0.75F;
            model.rightArm.xRot = 1.4F;
            model.leftArm.xRot = 1.4F;
            model.rightArm.zRot = 0.35F;
            model.leftArm.zRot = -0.35F;

            // 头部发狂般前伸摇摆
            float sway = (float) Math.sin(progress * 2.5F) * 0.15F;
            model.head.xRot = -0.55F;
            model.head.yRot = sway;
            model.hat.copyFrom(model.head);
        } else {
            // 猛撞方块瞬间：头部剧烈后仰反弹折断，四肢瘫软
            model.body.xRot = -0.35F;
            model.head.xRot = -1.15F;
            model.head.zRot = 0.45F;
            model.hat.copyFrom(model.head);

            model.rightArm.xRot = 0.2F;
            model.leftArm.xRot = 0.2F;
        }
    }

    /**
     * 本地玩家第一人称视角剧烈震颤与后坐力摇晃
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AnimData anim = ACTIVE_ANIMS.get(mc.player.getId());
        if (anim == null) return;

        float progress = anim.elapsedTicks + (float) event.getPartialTick();

        if (anim.type == TYPE_GUN) {
            if (progress < 24.0F) {
                // 瞄准期间的心跳与手抖震荡
                float shake = (float) Math.sin(progress * 2.0F) * 0.8F;
                event.setPitch(event.getPitch() + shake);
                event.setRoll(event.getRoll() + shake * 0.5F);
            } else {
                // 开枪瞬间：镜头猛烈上仰并倾斜
                float kick = Math.max(0.0F, 1.0F - (progress - 24.0F) / 6.0F);
                event.setPitch(event.getPitch() - kick * 32.0F);
                event.setRoll(event.getRoll() + kick * 20.0F);
            }
        } else if (anim.type == TYPE_BLADE) {
            if (progress >= 24.0F) {
                // 挥刀割喉瞬间：天旋地转并前栽
                float kick = Math.max(0.0F, 1.0F - (progress - 24.0F) / 6.0F);
                event.setPitch(event.getPitch() + kick * 25.0F);
                event.setRoll(event.getRoll() - kick * 25.0F);
            }
        } else if (anim.type == TYPE_WALL_SLAM) {
            if (progress < 16.0F) {
                // 冲锋颠簸
                float bob = (float) Math.sin(progress * 3.0F) * 2.0F;
                event.setPitch(event.getPitch() + bob);
            } else {
                // 撞击瞬间震颤巨响
                float rebound = Math.max(0.0F, 1.0F - (progress - 16.0F) / 8.0F);
                event.setPitch(event.getPitch() - rebound * 28.0F);
            }
        }
    }
}
