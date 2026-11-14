package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class UlyssanDragonRenderer <R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<UlyssanDragonEntity, R> {
    public UlyssanDragonRenderer(EntityRendererProvider.Context context){
        // geckolib/models/entity/ulyssan_dragon.geo.json,
        // geckolib/animations/entity/ulyssan_dragon.animation.json,
        // textures/entity/ulyssan_dragon.png
        super(context, new DefaultedEntityGeoModel<>(
                Identifier.fromNamespaceAndPath(RubyMod.MODID,"ulyssan_dragon")));
        withScale(0.5F);    // the model is huge (about 24 blocks wingspan) - shown at half size
    }
}
