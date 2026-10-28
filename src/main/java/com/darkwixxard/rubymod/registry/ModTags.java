package com.darkwixxard.rubymod.registry;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    // Items that can be used to craft and repair ruby tools (data/rubymod/tags/item/ruby_tool_materials.json)
    public static final TagKey<Item> RUBY_TOOL_MATERIALS = itemTag("ruby_tool_materials");
    // Items that can repair ruby armor in an anvil (data/rubymod/tags/item/repairs_ruby_armor.json)
    public static final TagKey<Item> REPAIRS_RUBY_ARMOR = itemTag("repairs_ruby_armor");

    private ModTags() {
    }

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RubyMod.MODID, name));
    }
}
