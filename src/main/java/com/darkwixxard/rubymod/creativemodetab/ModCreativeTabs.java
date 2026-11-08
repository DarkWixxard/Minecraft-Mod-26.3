package com.darkwixxard.rubymod.creativemodetab;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Items;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RubyMod.MODID);

    // Our own tab lists every item of the mod, in registration order
    public static final RegistryObject<CreativeModeTab> RUBY_TAB = CREATIVE_MODE_TABS.register("ruby_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.rubymod.ruby_tab"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> ModItems.RUBY.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.DEEPSLATE_RUBY_ORE.get());
                output.accept(ModItems.RUBY_ORE.get());
                output.accept(ModItems.RUBY_BLOCK.get());
                output.accept(ModItems.RUBY_SWORD.get());
                output.accept(ModItems.RUBY_PICKAXE.get());
                output.accept(ModItems.RUBY_AXE.get());
                output.accept(ModItems.RUBY_SHOVEL.get());
                output.accept(ModItems.RUBY_HOE.get());
                output.accept(ModItems.RUBY_HELMET.get());
                output.accept(ModItems.RUBY_CHESTPLATE.get());
                output.accept(ModItems.RUBY_LEGGINGS.get());
                output.accept(ModItems.RUBY_BOOTS.get());
                output.accept(ModItems.RUBY.get());
            })
        .build());

    public static final RegistryObject<CreativeModeTab> ICE_TAB = CREATIVE_MODE_TABS.register("ice_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.rubymod.ice_tab"))
        .withTabsBefore(CreativeModeTabs.COMBAT)
        .icon(() -> Items.ICE.getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(Items.ICE);
                output.accept(Items.PACKED_ICE);
                output.accept(ModItems.ICE_SWORD.get());
                output.accept(ModItems.ICE_PICKAXE.get());
                output.accept(ModItems.ICE_AXE.get());
                output.accept(ModItems.ICE_SHOVEL.get());
                output.accept(ModItems.ICE_HOE.get());
            })
        .build());

    public static final RegistryObject<CreativeModeTab> BOGGED_TAB = CREATIVE_MODE_TABS.register("bogged_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rubymod.bogged_tab"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> Items.RED_MUSHROOM.getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(Items.RED_MUSHROOM);
                output.accept(ModItems.BOGGED_SWORD.get());
                output.accept(ModItems.BOGGED_PICKAXE.get());
                output.accept(ModItems.BOGGED_AXE.get());
                output.accept(ModItems.BOGGED_SHOVEL.get());
                output.accept(ModItems.BOGGED_HOE.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> NOXIUM_TAB = CREATIVE_MODE_TABS.register("noxium_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rubymod.noxium_tab"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.NOXIUM_INGOT.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.NOXIUM_INGOT.get());
                output.accept(ModItems.NOXIUM_NUGGET.get());
                output.accept(ModItems.DEEPSLATE_NOXIUM_ORE.get());
                output.accept(ModItems.NOXIUM_ORE.get());
                output.accept(ModItems.NOXIUM_BLOCK.get());
                output.accept(ModItems.NOXIUM_SWORD.get());
                output.accept(ModItems.NOXIUM_BOW.get());
                output.accept(ModItems.NOXIUM_PICKAXE.get());
                output.accept(ModItems.NOXIUM_AXE.get());
                output.accept(ModItems.NOXIUM_SHOVEL.get());
                output.accept(ModItems.NOXIUM_HOE.get());
                output.accept(ModItems.NOXIUM_HELMET.get());
                output.accept(ModItems.NOXIUM_CHESTPLATE.get());
                output.accept(ModItems.NOXIUM_LEGGINGS.get());
                output.accept(ModItems.NOXIUM_BOOTS.get());
            })
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
