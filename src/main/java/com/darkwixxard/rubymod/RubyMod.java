package com.darkwixxard.rubymod;

import com.darkwixxard.rubymod.block.ModBlocks;
import com.darkwixxard.rubymod.creativemodetab.ModCreativeTabs;
import com.darkwixxard.rubymod.entity.ModEntities;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonEntity;
import com.darkwixxard.rubymod.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
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

        ModEntities.ENTITY_TYPES.register(modBusGroup);
        EntityAttributeCreationEvent.BUS.addListener(RubyMod::registerAttributes);


        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }

    // Tells Minecraft which attributes (health, speed, ...) our mobs have
    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ULYSSAN_DRAGON.get(), UlyssanDragonEntity.createAttributes().build());
    }

@SubscribeEvent
public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(
                ModEntities.ULYSSAN_DRAGON.get(),
                UlyssanDragonEntity.createAttributes().build()
        );
}

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Ruby Mod loaded!");
    }
}
