package net.qiansiqi.denunciationdelight.madness;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.qiansiqi.denunciationdelight.compat.epicfight.EpicFightCompat;
import net.qiansiqi.denunciationdelight.config.ModConfig;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.network.ModNetworking;
import net.qiansiqi.denunciationdelight.network.SyncMadnessPacket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MadnessManager {
    private static boolean taczReflectionInitialized;
    private static Method taczGetGunOrNull;
    private static Method taczHasBulletInBarrel;
    private static Method taczGetCurrentAmmoCount;
    private static Method taczReduceCurrentAmmoCount;
    private static Method taczUseDummyAmmo;
    private static Method taczGetDummyAmmoAmount;
    private static Method taczSetDummyAmmoAmount;
    private static Method taczSetBulletInBarrel;

    public static final String TAG_MADNESS = "DenunciationMadness";
    public static final String TAG_HAT_TIMER = "DenunciationHatTimer";
    public static final String TAG_PLACARD_TIMER = "DenunciationPlacardTimer";
    public static final int MAX_MADNESS = 100;

    public static final String TAG_IS_SUICIDING = "DenunciationIsSuiciding";
    public static final String TAG_SUICIDE_TYPE = "DenunciationSuicideType";
    public static final String TAG_SUICIDE_TICK = "DenunciationSuicideTick";
    public static final String TAG_SUICIDE_TARGET_X = "DenunciationSuicideTargetX";
    public static final String TAG_SUICIDE_TARGET_Y = "DenunciationSuicideTargetY";
    public static final String TAG_SUICIDE_TARGET_Z = "DenunciationSuicideTargetZ";

    public static final int TYPE_GUN = 0;
    public static final int TYPE_BLADE = 1;
    public static final int TYPE_WALL_SLAM = 2;

    public static boolean isPlayerSuiciding(Player player) {
        return player.getPersistentData().getBoolean(TAG_IS_SUICIDING);
    }

    // 客户端渲染缓存
    private static int clientMadness = 0;

    public static int getClientMadness() {
        return clientMadness;
    }

    public static void setClientMadness(int value) {
        clientMadness = Mth.clamp(value, 0, MAX_MADNESS);
    }

    public static int getMadness(Player player) {
        CompoundTag tag = player.getPersistentData();
        return tag.getInt(TAG_MADNESS);
    }

    public static void setMadness(Player player, int value) {
        int clamped = Mth.clamp(value, 0, MAX_MADNESS);
        player.getPersistentData().putInt(TAG_MADNESS, clamped);
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new SyncMadnessPacket(clamped), serverPlayer);
        }
    }

    public static void addMadness(Player player, int delta) {
        if (!ModConfig.ENABLE_MADNESS.get()) {
            return;
        }
        // 创造模式和旁观模式豁免疯度增减与自杀
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        int current = getMadness(player);
        int target = Mth.clamp(current + delta, 0, MAX_MADNESS);
        setMadness(player, target);

        if (delta > 0) {
            if (target >= MAX_MADNESS) {
                if (ModConfig.ENABLE_SUICIDE.get() && player instanceof ServerPlayer serverPlayer) {
                    triggerSuicide(serverPlayer);
                }
            } else if (target >= 80) {
                // 高疯度危险心跳与低语警告
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.85F);
            }
        }
    }

    /**
     * 每秒检查佩戴高帽和挂牌状态 (20 ticks 调用一次)
     */
    public static void tickWearing(ServerPlayer player) {
        if (!ModConfig.ENABLE_MADNESS.get()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            player.getPersistentData().remove(TAG_HAT_TIMER);
            player.getPersistentData().remove(TAG_PLACARD_TIMER);
            return;
        }

        CompoundTag tag = player.getPersistentData();

        // 1. 高帽检测：每佩戴满 30 秒 (600 ticks) 增加 10 疯度
        boolean wearingHat = isWearingConeHat(player);
        if (wearingHat) {
            int timer = tag.getInt(TAG_HAT_TIMER) + 20;
            if (timer >= 600) {
                timer = 0;
                addMadness(player, 10);
                int now = getMadness(player);
                player.displayClientMessage(
                        Component.translatable("message.denunciationdelight.madness_hat", now),
                        true
                );
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 0.9F);
            }
            tag.putInt(TAG_HAT_TIMER, timer);
        } else {
            tag.remove(TAG_HAT_TIMER);
        }

        // 2. 挂牌检测：每佩戴满 30 秒 (600 ticks) 增加 10 疯度
        boolean wearingPlacard = isWearingPlacard(player);
        if (wearingPlacard) {
            int timer = tag.getInt(TAG_PLACARD_TIMER) + 20;
            if (timer >= 600) {
                timer = 0;
                addMadness(player, 10);
                int now = getMadness(player);
                player.displayClientMessage(
                        Component.translatable("message.denunciationdelight.madness_placard", now),
                        true
                );
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 0.9F);
            }
            tag.putInt(TAG_PLACARD_TIMER, timer);
        } else {
            tag.remove(TAG_PLACARD_TIMER);
        }
    }

    /**
     * 当疯度达到 100 时触发绝望自杀流程 (启动自杀动作状态机)
     */
    public static void triggerSuicide(ServerPlayer player) {
        if (!ModConfig.ENABLE_SUICIDE.get()) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (isPlayerSuiciding(player)) return;

        ServerLevel level = player.serverLevel();
        CompoundTag tag = player.getPersistentData();

        // 1. 重置疯度与计时器，防止重复触发
        setMadness(player, 0);
        tag.remove(TAG_HAT_TIMER);
        tag.remove(TAG_PLACARD_TIMER);

        // 2. 检索物品栏中的所有武器位置 (优先有子弹的枪械，其次冷兵器，最后撞墙)
        // 槽位说明：-1: 主手, -2: 副手, 0~35: 物品栏槽位, -3: 未找到
        int gunSlot = -3;
        int meleeSlot = -3;

        // 检查主手 (-1)
        ItemStack mainItem = player.getMainHandItem();
        if (isGun(mainItem) && (!isTaczGun(mainItem) || hasGunAmmo(mainItem))) {
            gunSlot = -1;
        } else if (isMelee(mainItem)) {
            meleeSlot = -1;
        }

        // 检查副手 (-2)
        ItemStack offItem = player.getOffhandItem();
        if (gunSlot == -3 && isGun(offItem) && (!isTaczGun(offItem) || hasGunAmmo(offItem))) {
            gunSlot = -2;
        }
        if (meleeSlot == -3 && isMelee(offItem)) {
            meleeSlot = -2;
        }

        // 检查快捷栏与背包 (0 ~ 35)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (gunSlot == -3 && isGun(s) && (!isTaczGun(s) || hasGunAmmo(s))) {
                gunSlot = i;
            }
            if (meleeSlot == -3 && isMelee(s)) {
                meleeSlot = i;
            }
        }

        int suicideType = TYPE_WALL_SLAM;
        if (gunSlot != -3) {
            suicideType = TYPE_GUN;
            switchToMainHand(player, gunSlot);
        } else if (meleeSlot != -3) {
            suicideType = TYPE_BLADE;
            switchToMainHand(player, meleeSlot);
            EpicFightCompat.onSuicideStart(player);
        } else {
            suicideType = TYPE_WALL_SLAM;
            BlockPos targetPos = findNearestSolidBlock(level, player);
            tag.putInt(TAG_SUICIDE_TARGET_X, targetPos.getX());
            tag.putInt(TAG_SUICIDE_TARGET_Y, targetPos.getY());
            tag.putInt(TAG_SUICIDE_TARGET_Z, targetPos.getZ());
        }

        tag.putBoolean(TAG_IS_SUICIDING, true);
        tag.putInt(TAG_SUICIDE_TYPE, suicideType);
        tag.putInt(TAG_SUICIDE_TICK, 0);

        // 广播自杀动画数据包给周围所有客户端
        ModNetworking.sendToTracking(
                new net.qiansiqi.denunciationdelight.network.SyncSuicideAnimationPacket(player.getId(), suicideType, 30),
                player
        );
    }

    /**
     * 每 tick 处理玩家正在进行的自杀动作与致命终结
     */
    public static void tickSuicide(ServerPlayer player) {
        if (!isPlayerSuiciding(player)) return;
        if (!ModConfig.ENABLE_SUICIDE.get()) {
            cleanupSuicide(player);
            return;
        }

        CompoundTag tag = player.getPersistentData();
        int tick = tag.getInt(TAG_SUICIDE_TICK) + 1;
        tag.putInt(TAG_SUICIDE_TICK, tick);
        int type = tag.getInt(TAG_SUICIDE_TYPE);
        ServerLevel level = player.serverLevel();

        // 方式一：举枪自尽 (持枪贴近太阳穴，扳机咬合，轰然自裁)
        if (type == TYPE_GUN) {
            if (tick == 4) {
                // 金属机械咬合声
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.2F, 0.75F);
            } else if (tick == 12) {
                // 剧烈急促心跳
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.3F, 1.1F);
            } else if (tick == 24) {
                // 饮弹自尽消耗弹匣/枪膛 1 发子弹
                consumeGunAmmo(player.getMainHandItem());

                // 扣动扳机轰鸣爆炸
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.8F, 1.15F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.8F, 1.8F);

                // 头部爆出大量火焰与火药浓烟
                level.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getEyeY(), player.getZ(),
                        35, 0.15D, 0.15D, 0.15D, 0.1D);
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        player.getX(), player.getEyeY(), player.getZ(),
                        45, 0.25D, 0.25D, 0.25D, 0.06D);
                level.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getEyeY(), player.getZ(),
                        25, 0.2D, 0.2D, 0.2D, 0.12D);

                // 致命伤害 (自定义死亡类型: *** 饮弹自尽)
                DamageSource damageSource = new SuicideDamageSource(player.damageSources().genericKill().typeHolder(), TYPE_GUN);
                player.hurt(damageSource, Float.MAX_VALUE);
                if (player.isAlive()) {
                    player.setHealth(0.0F);
                    player.die(damageSource);
                }
            } else if (tick >= 28) {
                cleanupSuicide(player);
            }
            return;
        }

        // 方式二：横剑自刎 (架剑于颈，利刃破喉，横扫自裁)
        if (type == TYPE_BLADE) {
            if (tick == 4) {
                // 利刃拔出与破空
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.2F, 1.3F);
            } else if (tick == 14) {
                // 绝望喘息与心跳
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.1F, 1.0F);
            } else if (tick == 24) {
                // 利刃破肉与划过咽喉
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 2.2F, 0.85F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.8F, 1.4F);

                // 颈部横扫斩击刀光与大量鲜红暴击指示
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX(), player.getY() + 1.3D, player.getZ(),
                        3, 0.1D, 0.1D, 0.1D, 0.0D);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.3D, player.getZ(),
                        30, 0.25D, 0.25D, 0.25D, 0.12D);

                // 致命伤害 (自定义死亡类型: *** 挥刀自刎)
                DamageSource damageSource = new SuicideDamageSource(player.damageSources().genericKill().typeHolder(), TYPE_BLADE);
                player.hurt(damageSource, Float.MAX_VALUE);
                if (player.isAlive()) {
                    player.setHealth(0.0F);
                    player.die(damageSource);
                }
            } else if (tick >= 28) {
                cleanupSuicide(player);
            }
            return;
        }

        // 方式三：身无寸铁撞墙 (发狂俯冲冲锋，狠狠撞碎在硬方块上)
        if (type == TYPE_WALL_SLAM) {
            BlockPos targetPos = new BlockPos(
                    tag.getInt(TAG_SUICIDE_TARGET_X),
                    tag.getInt(TAG_SUICIDE_TARGET_Y),
                    tag.getInt(TAG_SUICIDE_TARGET_Z)
            );
            Vec3 targetCenter = Vec3.atCenterOf(targetPos);

            if (tick < 16) {
                // 持续以极高冲锋速度撞向方块
                Vec3 dir = targetCenter.subtract(player.position()).normalize();
                player.setDeltaMovement(dir.x * 1.6D, Math.max(0.2D, dir.y * 1.3D), dir.z * 1.6D);
                player.hurtMarked = true;
            } else if (tick == 16) {
                // 撞击接触瞬间：剧烈反弹、沉重钝击与骨骼碎裂声
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 2.8F, 0.65F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 2.2F, 0.9F);

                // 方块大片碎屑飞溅
                BlockState targetState = level.getBlockState(targetPos);
                if (!targetState.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, targetState),
                            targetCenter.x, targetCenter.y, targetCenter.z,
                            70, 0.4D, 0.4D, 0.4D, 0.15D);
                }

                // 致命伤害 (自定义死亡类型: *** 撞死了)
                DamageSource damageSource = new SuicideDamageSource(player.damageSources().flyIntoWall().typeHolder(), TYPE_WALL_SLAM);
                player.hurt(damageSource, Float.MAX_VALUE);
                if (player.isAlive()) {
                    player.setHealth(0.0F);
                    player.die(damageSource);
                }
            } else if (tick >= 25) {
                cleanupSuicide(player);
            }
        }
    }

    public static void cleanupSuicide(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(TAG_IS_SUICIDING);
        tag.remove(TAG_SUICIDE_TYPE);
        tag.remove(TAG_SUICIDE_TICK);
        tag.remove(TAG_SUICIDE_TARGET_X);
        tag.remove(TAG_SUICIDE_TARGET_Y);
        tag.remove(TAG_SUICIDE_TARGET_Z);
    }

    /**
     * 将物品栏指定位置的武器真实转移至主手，杜绝 copy 导致多掉落/刷物品
     */
    private static void switchToMainHand(ServerPlayer player, int slot) {
        if (slot == -1) {
            // 原本就在主手，无需操作
            return;
        }
        if (slot == -2) {
            // 在副手，与主手互换
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, off);
            player.setItemInHand(InteractionHand.OFF_HAND, main);
            return;
        }
        if (slot >= 0 && slot < 9) {
            // 在快捷栏 0~8，直接切换快捷栏选中格
            player.getInventory().selected = slot;
            return;
        }
        if (slot >= 9 && slot < player.getInventory().getContainerSize()) {
            // 在背包内部，将武器与主手当前槽位的物品进行槽位互换
            int selected = player.getInventory().selected;
            ItemStack currentMain = player.getInventory().getItem(selected);
            ItemStack weapon = player.getInventory().getItem(slot);

            player.getInventory().setItem(selected, weapon);
            player.getInventory().setItem(slot, currentMain);
        }
    }

    /**
     * 寻找玩家身前或附近最近的实心硬方块
     */
    private static BlockPos findNearestSolidBlock(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockPos eyeBlock = BlockPos.containing(eyePos);

        // 优先检查视线正前方的方块 (1 ~ 3 格)
        for (double d = 1.0D; d <= 3.0D; d += 0.8D) {
            BlockPos forward = BlockPos.containing(eyePos.add(look.scale(d)));
            BlockState state = level.getBlockState(forward);
            if (!state.isAir() && state.isSolid()) {
                return forward;
            }
        }

        // 其次在周围 3x3 范围内搜寻最近的实心方块
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = eyeBlock.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.isSolid()) {
                        double d = p.distToCenterSqr(eyePos);
                        if (d < bestDist) {
                            bestDist = d;
                            bestPos = p;
                        }
                    }
                }
            }
        }

        if (bestPos != null) {
            return bestPos;
        }

        // 兜底：直接返回正下方地面
        return player.blockPosition().below();
    }

    /**
     * 判断是否为枪械 (完美支持 TACZ 现代枪械及其扩展、或含 gun 关键字的枪械物品)
     */
    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            String ns = id.getNamespace().toLowerCase();
            String path = id.getPath().toLowerCase();
            // 排除 gunpowder 这类只含 "gun" 子串但不是枪械的物品
            if (path.contains("gunpowder")) {
                return false;
            }
            if (ns.equals("tacz") && path.contains("gun")) {
                return true;
            }
            if (path.contains("gun") || path.contains("pistol") || path.contains("rifle") || path.contains("shotgun") || path.contains("sniper")) {
                return true;
            }
        }
        String className = item.getClass().getName().toLowerCase();
        return className.contains("tacz") && className.contains("gun");
    }

    /**
     * 判断是否为 TACZ 枪械
     */
    public static boolean isTaczGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null && id.getNamespace().equalsIgnoreCase("tacz")) {
            return true;
        }
        return item.getClass().getName().toLowerCase().contains("tacz");
    }

    /**
     * 判断枪械内是否有子弹 (弹匣或枪膛内)
     */
    public static boolean hasGunAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Object gun = getTaczGun(stack);
        if (gun != null) {
            if (invokeBoolean(taczHasBulletInBarrel, gun, stack)) return true;
            if (invokeInt(taczGetCurrentAmmoCount, gun, stack) > 0) return true;
            if (invokeBoolean(taczUseDummyAmmo, gun, stack)
                    && invokeInt(taczGetDummyAmmoAmount, gun, stack) > 0) return true;
            return false;
        }

        // NBT 备用校验
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.getBoolean("HasBulletInBarrel")) return true;
            if (tag.getInt("GunCurrentAmmoCount") > 0) return true;
            if (tag.getInt("DummyAmmo") > 0) return true;
            if (tag.getInt("AmmoCount") > 0) return true;
        }
        return false;
    }

    /**
     * 饮弹自尽消耗 1 发子弹
     */
    public static boolean consumeGunAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Object gun = getTaczGun(stack);
        if (gun != null) {
            // 1. 优先扣除弹匣内子弹
            if (invokeInt(taczGetCurrentAmmoCount, gun, stack) > 0) {
                invokeVoid(taczReduceCurrentAmmoCount, gun, stack);
                return true;
            }
            // 2. 若弹匣空但枪膛有上膛子弹，击发并清空枪膛子弹
            if (invokeBoolean(taczHasBulletInBarrel, gun, stack)) {
                invokeVoid(taczSetBulletInBarrel, gun, stack, false);
                return true;
            }
            // 3. 虚拟子弹
            if (invokeBoolean(taczUseDummyAmmo, gun, stack)) {
                int dummy = invokeInt(taczGetDummyAmmoAmount, gun, stack);
                if (dummy > 0) {
                    invokeVoid(taczSetDummyAmmoAmount, gun, stack, dummy - 1);
                    return true;
                }
            }
        }

        // NBT 兜底更新
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            int ammo = tag.getInt("GunCurrentAmmoCount");
            if (ammo > 0) {
                tag.putInt("GunCurrentAmmoCount", ammo - 1);
                return true;
            }
            if (tag.getBoolean("HasBulletInBarrel")) {
                tag.putBoolean("HasBulletInBarrel", false);
                return true;
            }
            int dummy = tag.getInt("DummyAmmo");
            if (dummy > 0) {
                tag.putInt("DummyAmmo", dummy - 1);
                return true;
            }
            int genericAmmo = tag.getInt("AmmoCount");
            if (genericAmmo > 0) {
                tag.putInt("AmmoCount", genericAmmo - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * TACZ 是可选兼容：用反射读取 IGun，避免构建时硬依赖 69MB 的枪械模组 jar。
     */
    private static void initTaczReflection() {
        if (taczReflectionInitialized) return;
        taczReflectionInitialized = true;
        try {
            Class<?> igun = Class.forName("com.tacz.guns.api.item.IGun");
            taczGetGunOrNull = igun.getMethod("getIGunOrNull", ItemStack.class);
            taczHasBulletInBarrel = igun.getMethod("hasBulletInBarrel", ItemStack.class);
            taczGetCurrentAmmoCount = igun.getMethod("getCurrentAmmoCount", ItemStack.class);
            taczReduceCurrentAmmoCount = igun.getMethod("reduceCurrentAmmoCount", ItemStack.class);
            taczUseDummyAmmo = igun.getMethod("useDummyAmmo", ItemStack.class);
            taczGetDummyAmmoAmount = igun.getMethod("getDummyAmmoAmount", ItemStack.class);
            taczSetDummyAmmoAmount = igun.getMethod("setDummyAmmoAmount", ItemStack.class, int.class);
            taczSetBulletInBarrel = igun.getMethod("setBulletInBarrel", ItemStack.class, boolean.class);
        } catch (Throwable ignored) {
            taczGetGunOrNull = null;
        }
    }

    private static Object getTaczGun(ItemStack stack) {
        if (!ModList.get().isLoaded("tacz")) return null;
        initTaczReflection();
        if (taczGetGunOrNull == null) return null;
        try {
            return taczGetGunOrNull.invoke(null, stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Method method, Object target, Object... args) {
        if (method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(target, args));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int invokeInt(Method method, Object target, Object... args) {
        if (method == null) return 0;
        try {
            Object value = method.invoke(target, args);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void invokeVoid(Method method, Object target, Object... args) {
        if (method == null) return;
        try {
            method.invoke(target, args);
        } catch (Throwable ignored) {
        }
    }

    private static final List<TagKey<Item>> WEAPON_TAGS = List.of(
            ItemTags.SWORDS,
            ItemTags.AXES,
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/swords")),
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/weapons")),
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "weapons")),
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "weapons/melee")),
            TagKey.create(Registries.ITEM, new ResourceLocation("epicfight", "weapons")),
            TagKey.create(Registries.ITEM, new ResourceLocation("epicfight", "weapon"))
    );

    /**
     * 判断是否为冷兵器类 (原版剑/斧/三叉戟、史诗战斗 Epic Fight 武器、通用武器 Tag 及额外近战伤害武器)
     */
    public static boolean isMelee(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        // 1. 原版近战冷兵器
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem) {
            return true;
        }

        // 2. 史诗战斗 (Epic Fight) 专属能力与武器类型
        if (EpicFightCompat.isEpicFightWeapon(stack)) {
            return true;
        }

        // 3. 通用武器 Tag 标签判定
        for (TagKey<Item> tag : WEAPON_TAGS) {
            if (stack.is(tag)) {
                return true;
            }
        }

        // 4. 属性修饰符判定 (排除挖掘工具/射击武器，主手具备攻击伤害加成)
        if (hasWeaponAttackDamage(stack)) {
            return true;
        }

        return false;
    }

    private static boolean hasWeaponAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof DiggerItem && !(item instanceof AxeItem)) return false;
        if (item instanceof ProjectileWeaponItem) return false;
        if (isGun(stack)) return false;

        try {
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            var damageModifiers = modifiers.get(Attributes.ATTACK_DAMAGE);
            if (damageModifiers != null && !damageModifiers.isEmpty()) {
                double total = 0.0;
                for (var mod : damageModifiers) {
                    total += mod.getAmount();
                }
                return total > 2.0;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isWearingConeHat(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CONE_HAT.get())) {
            return true;
        }
        if (ModList.get().isLoaded("curios")) {
            return hasCuriosItem(player, ModItems.CONE_HAT.get());
        }
        return false;
    }

    public static boolean isWearingPlacard(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NECK_PLACARD.get())) {
            return true;
        }
        if (ModList.get().isLoaded("curios")) {
            return hasCuriosItem(player, ModItems.NECK_PLACARD.get());
        }
        return false;
    }

    private static boolean hasCuriosItem(Player player, Item item) {
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .map(inv -> inv.findFirstCurio(item).isPresent())
                    .orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void broadcastMessage(ServerLevel level, Component message) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.displayClientMessage(message, false);
        }
    }
}
