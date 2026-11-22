package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.ModEntities;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonPart;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import com.darkwixxard.rubymod.network.MistStepPacket;
import com.darkwixxard.rubymod.network.ModNetworking;
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
        event.registerEntityRenderer(ModEntities.ICE_ARROW.get(),
                IceArrowRenderer::new);
    }

    @SubscribeEvent
    public static boolean onAttack(InputEvent.InteractionKeyMappingTriggered.Attack event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof UlyssanDragonPart part
        && mc.player != null && mc.gameMode != null) {
            mc.gameMode.attack(mc.player, part.getParent());
            return true;    // true = cancel the normal attack (that one would hit the part)
        }
        return false;
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.MIST_STEP);
    }

    // Every client tick: was the Mist Step key pressed? -> ask the server to perform it
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeyBindings.MIST_STEP.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) {
                ModNetworking.sendToServer(MistStepPacket.INSTANCE);
            }
        }
    }
}
