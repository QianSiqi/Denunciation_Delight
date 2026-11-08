package net.qiansiqi.denunciationdelight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.qiansiqi.denunciationdelight.entity.ThrownInkBottle;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InkBottleItem extends Item {

    public InkBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            ThrownInkBottle inkBottle = new ThrownInkBottle(level, player);
            inkBottle.setItem(stack);
            inkBottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.85F, 1.0F);
            level.addFreshEntity(inkBottle);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.denunciationdelight.ink_bottle.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.denunciationdelight.ink_bottle.usage"));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
