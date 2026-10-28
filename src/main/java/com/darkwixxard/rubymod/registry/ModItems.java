package com.darkwixxard.rubymod.registry;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RubyMod.MODID);

    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby",
        () -> new Item(new Item.Properties().setId(ITEMS.key("ruby")))
    );

    public static final RegistryObject<Item> RUBY_APPLE = ITEMS.register("ruby_apple",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("ruby_apple"))
            .rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder()
                .nutrition(6)
                .saturationModifier(1.2F)
                .alwaysEdible()
                .build()
            )
        )
    );

    public static final RegistryObject<Item> RUBY_BLOCK = ITEMS.register("ruby_block",
        () -> new BlockItem(ModBlocks.RUBY_BLOCK.get(), new Item.Properties()
            .setId(ITEMS.key("ruby_block"))
            .useBlockDescriptionPrefix()
        )
    );

    private ModItems() {
    }
}
