package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

// Draws the custom ice texture over the normal boss bar of the Ulyssan dragon
@Mod.EventBusSubscriber(modid = RubyMod.MODID, value = Dist.CLIENT)
public final class UlyssanBossBar {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(RubyMod.MODID, "textures/gui/ulyssan_boss_bar.png");
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(RubyMod.MODID, "ulyssan_boss_bar");

    // Texture layout (the png is 256 x 32 pixels)
    private static final int TEX_WIDTH = 256, TEX_HEIGHT = 32;
    private static final int FRAME_WIDTH = 206, FRAME_HEIGHT = 15;   // frame at (0, 0)
    private static final int FILL_V = 16;                              // health fill at (0, 16)
    private static final int BAR_WIDTH = 182, BAR_HEIGHT = 5;         // same size as the vanilla bar

    // Where our bars are on the screen this frame
    private record Bar(int x, int y, float progress) {}
    private static final List<Bar> BARS = new ArrayList<>();

    private UlyssanBossBar() {
    }

    // 1) Minecraft is about to draw a boss bar: if it is ours, remember where
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (boss.getName().getContents() instanceof TranslatableContents name
                && name.getKey().equals(UlyssanDragonEntity.BOSS_BAR_NAME)) {
            BARS.add(new Bar(event.getX(), event.getY(), boss.getProgress()));
            event.setIncrement(event.getIncrement() + 6);   // a bit more space for the icicles
        }
    }

    // 2) Register our own HUD layer directly ABOVE the vanilla boss bars, so it is drawn on top of them
    @SubscribeEvent
    public static void addLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.BOSS_OVERLAY,
                (graphics, deltaTracker) -> drawBars(graphics));
    }

    // 3) Draw frame + health fill over every remembered bar
    private static void drawBars(GuiGraphicsExtractor graphics) {
        for (Bar bar : BARS) {
            // frame: the slot for the bar is 12 px from the left and 5 px from the top of the frame
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bar.x() - 12, bar.y() - 5,
                    0, 0, FRAME_WIDTH, FRAME_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            int width = Mth.lerpDiscrete(bar.progress(), 0, BAR_WIDTH);
            if (width > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bar.x(), bar.y(),
                        0, FILL_V, width, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            }
        }
        BARS.clear();
    }
}