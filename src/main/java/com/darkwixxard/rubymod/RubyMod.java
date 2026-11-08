package com.darkwixxard.rubymod;

import com.darkwixxard.rubymod.block.ModBlocks;
import com.darkwixxard.rubymod.creativemodetab.ModCreativeTabs;
import com.darkwixxard.rubymod.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here must match the modId in META-INF/mods.toml
@Mod(RubyMod.MODID)
public final class RubyMod {
    public static final String MODID = "rubymod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RubyMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBusGroup);
        ModCreativeTabs.registerListeners();

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Ruby Mod loaded!");
    }
}
