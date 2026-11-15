package com.darkwixxard.rubymod.entity.custom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.EnumSet;

public class UlyssanDragonEntity extends Monster implements GeoEntity {               // The Dragon is an enemy Mob
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_calm");   // Stand
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.walk_calm");   // Walk
    private static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_aggro");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.run_charge");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.ulyssan_dragon.death_ground");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_bite");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_claw_left");
    private static final RawAnimation TAIL = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_tail_sweep");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.hurt_light");

    // Flying
    private static final RawAnimation FLY_HOVER = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_hover");
    private static final RawAnimation FLY_CRUISE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_cruise");
    private static final RawAnimation FLY_DIVE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_dive");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.fly_takeoff");
    private static final RawAnimation LAND = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.fly_land");

    // Is the dragon flying? "Synched" = the server sends the value to every player, so the animations know it
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(UlyssanDragonEntity.class, EntityDataSerializers.BOOLEAN);

    // Movement "brains": one for walking, one for flying. We swap them on takeoff / landing
    private final MoveControl groundMoveControl;
    private final PathNavigation groundNavigation;
    private final FlyingMoveControl<UlyssanDragonEntity> flyingMoveControl;
    private final FlyingPathNavigation flyingNavigation;
    private int flightTicks;     // how long the dragon has been in the air

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
        this.groundMoveControl = this.moveControl;
        this.groundNavigation = this.navigation;
        this.flyingMoveControl = new FlyingMoveControl<>(this, 10, true);
        this.flyingNavigation = new FlyingPathNavigation(this, level);
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

    // Flies around without a target; after ~20 seconds it flies down to land
    private class DragonFlyAroundGoal extends Goal {
        private double wantedX, wantedY, wantedZ;

        DragonFlyAroundGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isFlying() && getTarget() == null;
        }

        @Override
        public void start() {
            this.pickNewPoint();
        }

        @Override
        public void tick() {
            boolean reached = distanceToSqr(wantedX, wantedY, wantedZ) < 3 * 3;
            if (reached || random.nextInt(200) == 0) {
                this.pickNewPoint();
            }
            // FlyingMoveControl forgets the target after each tick, so we set it every tick
            getMoveControl().setWantedPosition(wantedX, wantedY, wantedZ, 1.0);
        }

        private void pickNewPoint() {
            wantedX = getX() + (random.nextDouble() * 2 - 1) * 20;
            wantedZ = getZ() + (random.nextDouble() * 2 - 1) * 20;
            double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) wantedX, (int) wantedZ);
            boolean wantsToLand = flightTicks > 400 && random.nextInt(3) == 0;
            wantedY = wantsToLand ? ground : ground + 8 + random.nextInt(10);   // land or stay 8-17 blocks high
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

    // Melee hits always arrive at the dragon itself (see ModClientEvents).
    // Here we check which part the player was looking at and apply its multiplier.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Player player) {
            UlyssanDragonPart part = this.getPartLookedAt(player);
            if (part != null) {
                amount *= part.getDamageMultiplier();
            }
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !this.isDeadOrDying()) {
            this.triggerAnim("action", "hurt");
        }
        return hurt;
    }

    // Shoots an invisible ray from the player's eyes and returns the closest part it hits
    // (null = the body itself is closer, or no part was hit)
    private UlyssanDragonPart getPartLookedAt(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(8.0));
        double bestDistance = this.getBoundingBox().clip(eye, end)
                .map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
        UlyssanDragonPart best = null;
        for (UlyssanDragonPart part : this.parts) {
            var hit = part.getBoundingBox().clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestDistance) {
                bestDistance = eye.distanceToSqr(hit.get());
                best = part;
            }
        }
        return best;
    }

    // Health, speed, damage ... (registered in RubyMod via EntityAttributeCreationEvent)
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()                                // Werte des Drachen
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 50.0)
                .add(Attributes.FOLLOW_RANGE, 30.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.STEP_HEIGHT, 2.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0);   // no fall damage

        /// Pay attention to the semicolon: It must now come after the last line.
    }

    // What the dragon does (lower number = higher priority)
    @Override
    protected void registerGoals() {                                                    // goalSelector determines what it does
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(4, new DragonFlyAroundGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {   // ← hier { statt );
            @Override
            public boolean canUse() {
                return !isFlying() && super.canUse();       // only walk around when on the ground
            }
        });                                                                             // ← hier erst });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Plays the walk animation while moving, otherwise the idle animation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1) Movement: decides every frame which LOOP animation plays
        controllers.add(new AnimationController<UlyssanDragonEntity>("movement", 5, test -> {
            UlyssanDragonEntity dragon = test.animatable();
            if (dragon.isDeadOrDying()) {
                return test.setAndContinue(DEATH);
            }
            if (dragon.isAggressive()) {                       // has a target (set by MeleeAttackGoal)
                return test.setAndContinue(test.isMoving() ? RUN : IDLE_AGGRO);
            }
            if (dragon.isFlying()) {
                double fallSpeed = dragon.yo - dragon.getY();         // how far it sank since the last tick
                if (fallSpeed > 0.3) {
                    return test.setAndContinue(FLY_DIVE);
                }
                return test.setAndContinue(test.isMoving() ? FLY_CRUISE : FLY_HOVER);
            }
            return test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));

        // 2) Actions: plays nothing by itself, only when the server triggers an animation
        controllers.add(new AnimationController<UlyssanDragonEntity>("action", 2, test -> PlayState.STOP)
                .triggerableAnim("bite", BITE)
                .triggerableAnim("claw", CLAW)
                .triggerableAnim("tail", TAIL)
                .triggerableAnim("hurt", HURT)
                .triggerableAnim("takeoff", TAKEOFF)
                .triggerableAnim("land", LAND));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        String[] attacks = {"bite", "claw", "tail"};
        this.triggerAnim("action", attacks[this.random.nextInt(attacks.length)]); // random attack
        return super.doHurtTarget(level, target);
    }

    // Wait until the death animation (3.4 s = 68 ticks) has played before the dragon disappears
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= 68 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);   // smoke particles
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    private void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
        this.setNoGravity(flying);
        this.navigation.stop();
        this.moveControl = flying ? this.flyingMoveControl : this.groundMoveControl;
        this.navigation = flying ? this.flyingNavigation : this.groundNavigation;
        this.flightTicks = 0;
        this.resetFallDistance();
    }

    // Runs every tick on the server: decides when to take off and when to land
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (!this.isFlying()) {
            boolean targetFarAway = target != null && this.distanceToSqr(target) > 16 * 16;
            boolean targetHigh = target != null && target.getY() > this.getY() + 4;
            if (this.onGround() && (targetFarAway || targetHigh || this.random.nextInt(1200) == 0)) {
                this.setFlying(true);
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.6, 0));   // jump into the air
                this.triggerAnim("action", "takeoff");
            }
        } else {
            this.flightTicks++;
            // touched the ground again after a while -> land
            if (this.onGround() && this.flightTicks > 40) {
                this.setFlying(false);
                this.triggerAnim("action", "land");
            }
        }
    }

    // How strongly the dragon accelerates in the air (Minecraft's default for mobs is only 0.02)
    @Override
    protected float getFlyingSpeed() {
        return this.isFlying() ? this.getSpeed() * 0.1F : super.getFlyingSpeed();
    }

    // Remember in the save file if the dragon was flying
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Flying", this.isFlying());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setFlying(input.getBooleanOr("Flying", false));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;                                                                  // Save the animation state
    }
}
