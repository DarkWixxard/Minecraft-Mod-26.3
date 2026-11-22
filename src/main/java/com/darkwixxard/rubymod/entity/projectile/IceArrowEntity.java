package com.darkwixxard.rubymod.entity.projectile;

import com.darkwixxard.rubymod.entity.ModEntities;
import com.darkwixxard.rubymod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// The flying ARROW in the world: slows and freezes what it hits, leaves a trail of snowflakes.
public class IceArrowEntity extends AbstractArrow {
    // used by EntityType.Builder.of(IceArrowEntity::new, ...) and when loading the world
    public IceArrowEntity(EntityType<? extends IceArrowEntity> type, Level level) {
        super(type, level);
    }

    // used by IceArrowItem when a bow shoots it
    public IceArrowEntity(Level level, LivingEntity owner, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.ICE_ARROW.get(), owner, level, pickupItemStack, firedFromWeapon);
    }

    @Override
    public void tick() {
        super.tick();
        // particles are only drawn on the client
        if (this.level().isClientSide() && !this.isInGround()) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    // what the arrow does to a living entity it hits (server side)
    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this.getEffectSource());   // 5 s Slowness II
        target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);                                // frozen look for 3 s
    }

    // what you get back when you pick the arrow up
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.ICEARROW.get());
    }
}