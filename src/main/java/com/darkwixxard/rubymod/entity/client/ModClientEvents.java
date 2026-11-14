package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.ModEntities;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonEntity;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RubyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ULYSSAN_DRAGON.get(),
                UlyssanDragonRenderer::new);
    }
}
