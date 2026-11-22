package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.projectile.IceArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

// Draws the flying ice arrow with its own texture
public class IceArrowRenderer extends ArrowRenderer<IceArrowEntity, ArrowRenderState> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(RubyMod.MODID, "textures/entity/projectiles/ice_arrow.png");

    public IceArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}