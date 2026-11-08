package net.qiansiqi.denunciationdelight.compat.epicfight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

public class EpicFightCompat {
    public static final String MOD_ID = "epicfight";

    private static boolean initialized = false;
    private static Method getItemStackCapabilityMethod = null;
    private static Class<?> weaponCapabilityClass = null;
    private static Class<?> weaponItemClass = null;
    private static Method getEntityPatchMethod = null;
    private static Class<?> playerPatchClass = null;

    private static void init() {
        if (initialized) return;
        initialized = true;

        if (!ModList.get().isLoaded(MOD_ID)) return;

        try {
            Class<?> capsClass = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            getItemStackCapabilityMethod = capsClass.getMethod("getItemStackCapability", ItemStack.class);
        } catch (Throwable ignored) {
        }

        try {
            weaponCapabilityClass = Class.forName("yesman.epicfight.world.capabilities.item.WeaponCapability");
        } catch (Throwable ignored) {
        }

        try {
            weaponItemClass = Class.forName("yesman.epicfight.world.item.WeaponItem");
        } catch (Throwable ignored) {
        }

        try {
            Class<?> capsClass = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            playerPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            getEntityPatchMethod = capsClass.getMethod("getEntityPatch", net.minecraft.world.entity.Entity.class, Class.class);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 判断是否为史诗战斗武器 (完美兼容 Epic Fight 原生武器及其 Datapack/Addon 扩展武器)
     */
    public static boolean isEpicFightWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return isEpicFightNamespaceItem(stack);
        }

        init();

        // 1. 检查是否为 Epic Fight 的 WeaponItem 类实例
        if (weaponItemClass != null && weaponItemClass.isInstance(stack.getItem())) {
            return true;
        }

        // 2. 检查 Epic Fight 的 CapabilityItem 是否为 WeaponCapability
        if (getItemStackCapabilityMethod != null) {
            try {
                Object cap = getItemStackCapabilityMethod.invoke(null, stack);
                if (cap != null) {
                    if (weaponCapabilityClass != null && weaponCapabilityClass.isInstance(cap)) {
                        return true;
                    }
                    // 检查 getWeaponCategory()
                    try {
                        Method getCatMethod = cap.getClass().getMethod("getWeaponCategory");
                        Object cat = getCatMethod.invoke(cap);
                        if (cat != null) {
                            String catName = cat.toString().toLowerCase();
                            if (!catName.contains("not_weapon") && !catName.contains("none")) {
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 3. 命名空间与注册名备用判断
        return isEpicFightNamespaceItem(stack);
    }

    private static boolean isEpicFightNamespaceItem(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && id.getNamespace().equalsIgnoreCase(MOD_ID)) {
            String path = id.getPath().toLowerCase();
            return path.contains("sword") || path.contains("greatsword") || path.contains("spear")
                    || path.contains("dagger") || path.contains("tachi") || path.contains("katana")
                    || path.contains("longsword") || path.contains("uchigatana") || path.contains("knuckle")
                    || path.contains("glove") || path.contains("scythe") || path.contains("rapier")
                    || path.contains("blade") || path.contains("axe") || path.contains("mace");
        }
        return false;
    }

    /**
     * 当自裁开始时，重置史诗战斗战斗模式，确保姿态动画与自刎流畅衔接
     */
    public static void onSuicideStart(Player player) {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        init();

        if (getEntityPatchMethod != null && playerPatchClass != null) {
            try {
                Object patch = getEntityPatchMethod.invoke(null, player, playerPatchClass);
                if (patch != null) {
                    for (Method m : patch.getClass().getMethods()) {
                        String name = m.getName().toLowerCase();
                        if (name.equals("tovanillamode") || name.equals("tominingmode") || name.equals("setbattlemode")) {
                            if (m.getParameterCount() == 0) {
                                m.invoke(patch);
                                break;
                            } else if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                                m.invoke(patch, false);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
