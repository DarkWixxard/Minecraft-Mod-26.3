package com.darkwixxard.rubymod.materials;

import com.darkwixxard.rubymod.RubyMod;
import java.util.Map;

import com.darkwixxard.rubymod.tags.ModTags;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

// Ruby sits between iron and diamond: mines like diamond, but wears out a bit faster
public final class ModMaterials {
    public static final ToolMaterial RUBY_TOOL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // which blocks this tool can NOT harvest
        1200,  // durability (iron 250, diamond 1561)
        8.5F,  // mining speed (iron 6, diamond 8)
        2.5F,  // bonus attack damage (iron 2, diamond 3)
        15,    // enchantability (iron 14, diamond 10)
        ModTags.RUBY_TOOL_MATERIALS
    );
    public static final ToolMaterial ICE_TOOL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1500,
        9.5F,
        3.5F,
        17,
        ModTags.ICE_TOOL_MATERIALS
    );
    public static final ToolMaterial BOGGED_TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            2000,
            10.5F,
            4.5F,
            20,
            ModTags.BOGGED_TOOL_MATERIALS
    );
    public static final ToolMaterial NOXIUM_TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            2000,
            10.5F,
            4.5F,
            20,
            ModTags.NOXIUM_TOOL_MATERIALS
    );

    // Points to assets/rubymod/equipment/ruby.json, which defines how the armor looks when worn
    public static final ResourceKey<EquipmentAsset> RUBY_EQUIPMENT_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(RubyMod.MODID, "ruby"));

    public static final ArmorMaterial RUBY_ARMOR = new ArmorMaterial(
        28, // durability multiplier (iron 15, diamond 33)
        Map.of(
            ArmorType.HELMET, 3,
            ArmorType.CHESTPLATE, 7,
            ArmorType.LEGGINGS, 6,
            ArmorType.BOOTS, 3,
            ArmorType.BODY, 9
        ),
        15,   // enchantability
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        1.0F, // toughness
        0.0F, // knockback resistance
        ModTags.REPAIRS_RUBY_ARMOR,
        RUBY_EQUIPMENT_ASSET
    );

    // Points to assets/rubymod/equipment/noxium.json, which defines how the armor looks when worn
    public static final ResourceKey<EquipmentAsset> NOXIUM_EQUIPMENT_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(RubyMod.MODID, "noxium"));

    public static final ArmorMaterial NOXIUM_ARMOR = new ArmorMaterial(
            28, // durability multiplier (iron 15, diamond 33)
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3,
                    ArmorType.BODY, 9
            ),
            15,   // enchantability
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            1.0F, // toughness
            0.0F, // knockback resistance
            ModTags.REPAIRS_NOXIUM_ARMOR,
            NOXIUM_EQUIPMENT_ASSET
    );
    private ModMaterials() {
    }
}
