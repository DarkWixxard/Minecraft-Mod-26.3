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
import net.minecraftforge.entity.PartEntity;

public class UlyssanDragonEntity extends Monster implements GeoEntity {               // The Dragon is an enemy Mob
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_calm");   // Stand
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.walk_calm");   // Walk

    // In GeckoLib 5.5.7, the method isn't called `createAnimatableInstanceCache`, but rather: createInstanceCache
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Extra hitboxes (width, height, damage multiplier)
    private final UlyssanDragonPart head = new UlyssanDragonPart(this, 2.0F, 1.7F, 1.5F);
    private final UlyssanDragonPart neck = new UlyssanDragonPart(this, 1.0F, 1.2F, 1.0F);
    private final UlyssanDragonPart tail1 = new UlyssanDragonPart(this, 1.1F, 1.2F, 1.0F);
    private final UlyssanDragonPart tail2 = new UlyssanDragonPart(this, 0.9F, 0.9F, 1.0F);
    private final UlyssanDragonPart tail3 = new UlyssanDragonPart(this, 1.0F, 0.8F, 1.0F);
    private final UlyssanDragonPart wingLeft = new UlyssanDragonPart(this, 3.0F, 2.0F, 0.5F);
    private final UlyssanDragonPart wingRight = new UlyssanDragonPart(this, 3.0F, 2.0F, 0.5F);
    private final UlyssanDragonPart[] parts = {head, neck, tail1, tail2, tail3, wingLeft, wingRight};


    ///Der Konstruktor ist die Methode, die genauso heißt wie die Klasse und keinen Rückgabetyp hat,
    /// also kein void, int oder Ähnliches. Er läuft, sobald ein neuer Drache erschaffen wird,
    /// zum Beispiel beim Spawnen oder mit /summon.

    public UlyssanDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    // Each part needs its own ID immediately following the dragon's ID (just like with the Ender Dragon)
    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
        }
    }

    // Moves the hitboxes along with the dragon every tick
    @Override
    public void aiStep() {
        super.aiStep();
        //             Part        front  sideways up  (blocks, starting from the dragon's feet)
        this.placePart(this.head,       2.1,  0.0, 2.9);
        this.placePart(this.neck,       1.1,  0.0, 2.3);
        this.placePart(this.tail1,     -2.9,  0.0, 1.2);
        this.placePart(this.tail2,     -4.2,  0.0, 0.9);
        this.placePart(this.tail3,     -5.5,  0.0, 0.5);
        this.placePart(this.wingLeft,  -1.8,  3.2, 1.2);
        this.placePart(this.wingRight, -1.8, -3.2, 1.2);
    }

    private void placePart(UlyssanDragonPart part, double forward, double side, double up) {
        double yaw = Math.toRadians(this.yBodyRot);
        double forwardX = -Math.sin(yaw);   // Direction in which the body is facing
        double forwardZ = Math.cos(yaw);
        double sideX = forwardZ;            // 90° to it = to the left of the kite
        double sideZ = -forwardX;

        double oldX = part.getX(), oldY = part.getY(), oldZ = part.getZ();
        part.setPos(this.getX() + forwardX * forward + sideX * side,
                this.getY() + up,
                this.getZ() + forwardZ * forward + sideZ * side);
        // Remember the previous position so that the hitbox moves smoothly
        part.xo = oldX;  part.yo = oldY;  part.zo = oldZ;
        part.xOld = oldX; part.yOld = oldY; part.zOld = oldZ;
    }

    // Health, speed, damage ... (registered in RubyMod via EntityAttributeCreationEvent)
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()                                // Werte des Drachen
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 50.0)
                .add(Attributes.FOLLOW_RANGE, 30.0)
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
