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

public class NeckPlacardItem extends ArmorItem {
    public static final String TAG_LINE1 = "Line1";
    public static final String TAG_LINE2 = "Line2";
    public static final String TAG_LINE3 = "Line3";
    public static final String TAG_RED_CROSS = "RedCross";
    public static final String TAG_FONT_SIZE = "FontSize";

    public NeckPlacardItem(Properties properties) {
        super(ModArmorMaterials.PAPER, Type.CHESTPLATE, properties);
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (target instanceof Player || target instanceof net.minecraft.world.entity.npc.Villager
                || target instanceof net.minecraft.world.entity.decoration.ArmorStand
                || target instanceof net.minecraft.world.entity.monster.Zombie) {

            if (!level.isClientSide()) {
                EquipmentSlot slot = EquipmentSlot.CHEST;
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
                    targetPlayer.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.placard_victim", player.getName()), true);
                    player.displayClientMessage(Component.translatable(
                            "message.denunciationdelight.placard_sender", targetPlayer.getName()), true);
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

        // 如果按住潜行键，则执行原版的穿戴动作
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        // 普通右键打开编辑界面
        if (level.isClientSide()) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> ClientItemHooks.openPlacardEditor(stack, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.denunciationdelight.neck_placard.desc").withStyle(net.minecraft.ChatFormatting.GRAY));

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String line1 = tag.getString(TAG_LINE1);
            String line2 = tag.getString(TAG_LINE2);
            String line3 = tag.getString(TAG_LINE3);
            boolean redCross = tag.getBoolean(TAG_RED_CROSS);

            if (!line1.isEmpty()) {
                tooltipComponents.add(Component.literal("§7[1] §f" + line1));
            }
            if (!line2.isEmpty()) {
                tooltipComponents.add(Component.literal("§7[2] §e" + line2 + (redCross ? " §c(❌)" : "")));
            }
            if (!line3.isEmpty()) {
                tooltipComponents.add(Component.literal("§7[3] §7" + line3));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.denunciationdelight.neck_placard.usage"));
        }

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> ClientItemHooks.initializePlacard(consumer));
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, EquipmentSlot slot, String type) {
        return "denunciationdelight:textures/models/armor/neck_placard.png";
    }
}
