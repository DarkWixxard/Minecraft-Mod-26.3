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

    public static final RegistryObject<CreativeModeTab> RUBY_TAB = CREATIVE_MODE_TABS.register("ruby_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.rubymod.ruby_tab"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.RUBY.get().getDefaultInstance())
        .displayItems((_, output) -> {
            output.accept(ModItems.RUBY.get());
            output.accept(ModItems.RUBY_APPLE.get());
            output.accept(ModItems.RUBY_BLOCK.get());
        })
        .build());

    private ModCreativeTabs() {
    }

    public static void registerListeners() {
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModCreativeTabs::addToVanillaTabs);
    }

    // Also list our items in the matching vanilla tabs
    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.accept(ModItems.RUBY);
        else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS)
            event.accept(ModItems.RUBY_APPLE);
        else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(ModItems.RUBY_BLOCK);
    }
}
