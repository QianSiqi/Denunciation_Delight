package net.qiansiqi.denunciationdelight.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DenunciationDelight.MOD_ID);

    public static final RegistryObject<Item> CONE_HAT = ITEMS.register("cone_hat",
            () -> new ConeHatItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> NECK_PLACARD = ITEMS.register("neck_placard",
            () -> new NeckPlacardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> INK_BOTTLE = ITEMS.register("ink_bottle",
            () -> new InkBottleItem(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON)));
}
