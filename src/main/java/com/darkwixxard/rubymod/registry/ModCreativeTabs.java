package com.darkwixxard.rubymod.registry;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RubyMod.MODID);

    // Our own tab lists every item of the mod, in registration order
    public static final RegistryObject<CreativeModeTab> RUBY_TAB = CREATIVE_MODE_TABS.register("ruby_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.rubymod.ruby_tab"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.RUBY.get().getDefaultInstance())
        .displayItems((_, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
        .build());

    private ModCreativeTabs() {
    }

    public static void registerListeners() {
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModCreativeTabs::addToVanillaTabs);
    }

    // Also list our items in the matching vanilla tabs
    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();
        if (tab == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RUBY);
        } else if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.RUBY_APPLE);
        } else if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.RUBY_BLOCK);
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.RUBY_ORE);
            event.accept(ModItems.DEEPSLATE_RUBY_ORE);
        } else if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.RUBY_SHOVEL);
            event.accept(ModItems.RUBY_PICKAXE);
            event.accept(ModItems.RUBY_AXE);
            event.accept(ModItems.RUBY_HOE);
        } else if (tab == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.RUBY_SWORD);
            event.accept(ModItems.RUBY_AXE);
            event.accept(ModItems.RUBY_HELMET);
            event.accept(ModItems.RUBY_CHESTPLATE);
            event.accept(ModItems.RUBY_LEGGINGS);
            event.accept(ModItems.RUBY_BOOTS);
        }
    }
}
