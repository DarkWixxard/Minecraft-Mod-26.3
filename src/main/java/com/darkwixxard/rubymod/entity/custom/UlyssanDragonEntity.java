package com.darkwixxard.rubymod.entity.custom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class UlyssanDragonEntity extends Monster implements GeoEntity {               // The Dragon is an enemy Mob
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_calm");   // Stand
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.walk_calm");   // Walk

    // In GeckoLib 5.5.7, the method isn't called `createAnimatableInstanceCache`, but rather: createInstanceCache
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UlyssanDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    // Health, speed, damage ... (registered in RubyMod via EntityAttributeCreationEvent)
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()                                // Werte des Drachen
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 50.0)
                .add(Attributes.FOLLOW_RANGE, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.STEP_HEIGHT, 2.0);
    }

    // What the dragon does (lower number = higher priority)
    @Override
    protected void registerGoals() {                                                    // goalSelector determines what it does
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Plays the walk animation while moving, otherwise the idle animation
    @Override                                                                                   // Select the animation
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movenment", 5,            // The 5 are the ticks for a smooth transition
                test -> test.setAndContinue(test.isMoving() ? WALK : IDLE )));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;                                                                  // Save the animation state
    }
}
