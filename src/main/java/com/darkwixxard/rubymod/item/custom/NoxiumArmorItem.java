package com.darkwixxard.rubymod.item.custom;

import com.darkwixxard.rubymod.RubyMod;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Armor piece that is rendered on the body with the 3D GeckoLib model instead of the flat texture
public class NoxiumArmorItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Full set bonus: effect + level (0 = level 1, 1 = level 2, ...)
    private static final List<SetEffect> FULL_SET_EFFECTS = List.of(
            new SetEffect(MobEffects.NIGHT_VISION, 0),
            new SetEffect(MobEffects.REGENERATION, 0),
            new SetEffect(MobEffects.RESISTANCE, 0),
            new SetEffect(MobEffects.STRENGTH, 0)
    );
    private record SetEffect(Holder<MobEffect> effect, int amplifier) {

    }

    public NoxiumArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) {
                    // geckolib/models/item/armor/noxium_armor.geo.json + textures/item/armor/noxium_armor.png
                    this.renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(
                            Identifier.fromNamespaceAndPath(RubyMod.MODID, "armor/noxium_armor")));
                }
                return this.renderer;
            }
        });
    }

    // Called every tick for every worn piece; only the helmet applies the bonus so it isn't done 4 times
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if ( slot == EquipmentSlot.HEAD && entity instanceof LivingEntity living && isWearingFullSet(living)) {
            for (SetEffect setEffect : FULL_SET_EFFECTS) {
                MobEffectInstance current = living.getEffect(setEffect.effect());
                // renew shortly before it runs out (night vision starts flickering below 10 sec)
                if (current == null || current.getDuration() < 220) {
                    living.addEffect(new MobEffectInstance(setEffect.effect(), 300, setEffect.amplifier(), true, false));   // 300 means the duration in sec 300 ticks = 15sec, renewed after 220 ticks
                }
            }
        }
    }
    private static boolean isWearingFullSet(LivingEntity entity) {
        for (EquipmentSlot armorSlot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(entity.getItemBySlot(armorSlot).getItem() instanceof NoxiumArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // no animations
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}