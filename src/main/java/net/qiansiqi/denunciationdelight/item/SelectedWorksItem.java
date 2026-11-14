package net.qiansiqi.denunciationdelight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.qiansiqi.denunciationdelight.client.ClientItemHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SelectedWorksItem extends Item {

    public SelectedWorksItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientItemHooks.openSelectedWorks());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(
                "item.denunciationdelight.selected_works.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.denunciationdelight.selected_works.usage")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}