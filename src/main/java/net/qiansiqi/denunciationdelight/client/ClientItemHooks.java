package net.qiansiqi.denunciationdelight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.qiansiqi.denunciationdelight.client.gui.ConeHatEditScreen;
import net.qiansiqi.denunciationdelight.client.gui.PlacardEditScreen;
import net.qiansiqi.denunciationdelight.client.gui.SelectedWorksScreen;
import net.qiansiqi.denunciationdelight.client.renderer.ConeHatArmorRenderer;
import net.qiansiqi.denunciationdelight.client.renderer.NeckPlacardArmorRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 所有物品侧的客户端细节都集中在这里，避免 common 物品类直接引用客户端类。
 */
public final class ClientItemHooks {
    private ClientItemHooks() {
    }

    public static void initializeConeHat(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                return ConeHatArmorRenderer.getModel(original, itemStack);
            }
        });
    }

    public static void initializePlacard(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                return NeckPlacardArmorRenderer.getModel(original, itemStack);
            }
        });
    }

    public static void openConeHatEditor(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new ConeHatEditScreen(stack, hand));
    }

    public static void openPlacardEditor(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new PlacardEditScreen(stack, hand));
    }

    public static void openSelectedWorks() {
        Minecraft.getInstance().setScreen(new SelectedWorksScreen());
    }
}