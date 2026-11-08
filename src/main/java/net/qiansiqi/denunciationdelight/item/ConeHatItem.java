package net.qiansiqi.denunciationdelight.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import net.qiansiqi.denunciationdelight.client.ClientItemHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ConeHatItem extends ArmorItem {
    public static final String TAG_HAT_TEXT = "HatText";
    public static final String TAG_FONT_SIZE = "FontSize";

    public ConeHatItem(Properties properties) {
        super(ModArmorMaterials.PAPER, Type.HELMET, properties);
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (target instanceof Player || target instanceof net.minecraft.world.entity.npc.Villager
                || target instanceof net.minecraft.world.entity.decoration.ArmorStand
                || target instanceof net.minecraft.world.entity.monster.Zombie) {

            if (!level.isClientSide()) {
                EquipmentSlot slot = EquipmentSlot.HEAD;
                ItemStack existing = target.getItemBySlot(slot);

                ItemStack toEquip = stack.copy();
                toEquip.setCount(1);

                if (target instanceof Player targetPlayer) {
                    if (!existing.isEmpty()) {
                        if (!targetPlayer.getInventory().add(existing)) {
                            targetPlayer.drop(existing, false);
                        }
                    }
                    targetPlayer.setItemSlot(slot, toEquip);
                    Component hatName = Component.translatable("message.denunciationdelight.item.hat");
                    targetPlayer.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.force_equip.victim", player.getName(), hatName), true);
                    player.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.force_equip.sender", targetPlayer.getName(), hatName), true);
                } else {
                    target.setItemSlot(slot, toEquip);
                }

                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        if (level.isClientSide()) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> ClientItemHooks.openConeHatEditor(stack, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.denunciationdelight.cone_hat.desc").withStyle(net.minecraft.ChatFormatting.GRAY));

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_HAT_TEXT)) {
            String text = tag.getString(TAG_HAT_TEXT);
            if (!text.isEmpty()) {
                tooltipComponents.add(Component.translatable(
                        "tooltip.denunciationdelight.cone_hat.text", text));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.denunciationdelight.cone_hat.usage"));
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // 护甲模型必须可靠注册，文本直接由模型渲染，避免只依赖实体层
        ClientItemHooks.initializeConeHat(consumer);
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, EquipmentSlot slot, String type) {
        return "denunciationdelight:textures/models/armor/cone_hat.png";
    }
}
