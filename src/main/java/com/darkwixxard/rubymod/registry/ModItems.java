package com.darkwixxard.rubymod.registry;

import com.darkwixxard.rubymod.RubyMod;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RubyMod.MODID);

    // Materials & food
    public static final RegistryObject<Item> RUBY = item("ruby", p -> p);

    public static final RegistryObject<Item> RUBY_APPLE = item("ruby_apple", p -> p
            .rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2F).build()
        )
    );

    // Block items
    public static final RegistryObject<Item> RUBY_BLOCK = blockItem("ruby_block", ModBlocks.RUBY_BLOCK);
    public static final RegistryObject<Item> RUBY_ORE = blockItem("ruby_ore", ModBlocks.RUBY_ORE);
    public static final RegistryObject<Item> DEEPSLATE_RUBY_ORE = blockItem("deepslate_ruby_ore", ModBlocks.DEEPSLATE_RUBY_ORE);

    // Tools: the two numbers are attack damage and attack speed (same values as the diamond tools)
    // Ruby-Tools
    public static final RegistryObject<Item> RUBY_SWORD = item("ruby_sword", p -> p.sword(ModMaterials.RUBY_TOOL, 3.0F, -2.4F));
    public static final RegistryObject<Item> RUBY_PICKAXE = item("ruby_pickaxe", p -> p.pickaxe(ModMaterials.RUBY_TOOL, 1.0F, -2.8F));
    public static final RegistryObject<Item> RUBY_AXE = item("ruby_axe", p -> p.axe(ModMaterials.RUBY_TOOL, 5.0F, -3.0F));
    public static final RegistryObject<Item> RUBY_SHOVEL = item("ruby_shovel", p -> p.shovel(ModMaterials.RUBY_TOOL, 1.5F, -3.0F));
    public static final RegistryObject<Item> RUBY_HOE = item("ruby_hoe", p -> p.hoe(ModMaterials.RUBY_TOOL, -2.5F, 0.0F));
    //Ice-Tools
    public static final RegistryObject<Item> ICE_SWORD = item("ice_sword", p -> p.sword(ModMaterials.ICE_TOOL, 3.0F, -2.4F));
    public static final RegistryObject<Item> ICE_PICKAXE = item("ice_pickaxe", p -> p.pickaxe(ModMaterials.ICE_TOOL, 1.0F, -2.8F));
    public static final RegistryObject<Item> ICE_AXE = item("ice_axe", p -> p.axe(ModMaterials.ICE_TOOL, 5.0F, -3.0F));
    public static final RegistryObject<Item> ICE_SHOVEL = item("ice_shovel", p -> p.shovel(ModMaterials.ICE_TOOL, 1.5F, -3.0F));
    public static final RegistryObject<Item> ICE_HOE = item("ice_hoe", p -> p.hoe(ModMaterials.ICE_TOOL, -2.5F, 0.0F));
    // Armor
    public static final RegistryObject<Item> RUBY_HELMET = armor("ruby_helmet", ArmorType.HELMET);
    public static final RegistryObject<Item> RUBY_CHESTPLATE = armor("ruby_chestplate", ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> RUBY_LEGGINGS = armor("ruby_leggings", ArmorType.LEGGINGS);
    public static final RegistryObject<Item> RUBY_BOOTS = armor("ruby_boots", ArmorType.BOOTS);

    private ModItems() {
    }

    private static RegistryObject<Item> item(String name, UnaryOperator<Item.Properties> properties) {
        return ITEMS.register(name, () -> new Item(properties.apply(new Item.Properties().setId(ITEMS.key(name)))));
    }

    private static RegistryObject<Item> armor(String name, ArmorType type) {
        return item(name, p -> p.humanoidArmor(ModMaterials.RUBY_ARMOR, type));
    }

    private static RegistryObject<Item> blockItem(String name, Supplier<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()
            .setId(ITEMS.key(name))
            .useBlockDescriptionPrefix()
        ));
    }
}
