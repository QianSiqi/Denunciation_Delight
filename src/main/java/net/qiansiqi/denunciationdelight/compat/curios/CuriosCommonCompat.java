package net.qiansiqi.denunciationdelight.compat.curios;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.item.ConeHatItem;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CuriosCommonCompat {
    private static final ResourceLocation CURIOS_CAP = new ResourceLocation(DenunciationDelight.MOD_ID, "curio_item");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.getItem() instanceof ConeHatItem || stack.getItem() instanceof NeckPlacardItem) {
            ICurio curio = new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }
            };

            LazyOptional<ICurio> optional = LazyOptional.of(() -> curio);
            ICapabilityProvider provider = new ICapabilityProvider() {
                @Override
                public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return CuriosCapability.ITEM.orEmpty(cap, optional);
                }
            };
            event.addCapability(CURIOS_CAP, provider);
        }
    }
}
