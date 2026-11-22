package com.darkwixxard.rubymod.ability;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Mist Step": the player dissolves into mist and dashes a few blocks forward,
 * leaving a trail of mist and ghostly after-images behind.
 * <p>
 * All of this runs on the SERVER, so every player nearby sees the particles.
 */
public final class MistStep {
    /** How far the dash goes (in blocks). */
    public static final double MAX_DISTANCE = 8.0;
    /** Cooldown between two dashes (20 ticks = 1 second). */
    public static final int COOLDOWN_TICKS = 30;
    /** How fine the collision check walks along the path. */
    private static final double STEP = 0.25;
    /** Every how many blocks an after-image appears. */
    private static final double AFTERIMAGE_SPACING = 1.6;

    // Last game tick in which each player used Mist Step
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    private MistStep() {
    }

    public static void perform(ServerPlayer player) {
        if (player.isSpectator() || player.isPassenger() || player.isSleeping() || !player.isAlive()) {
            return;
        }

        ServerLevel level = player.level();
        long now = level.getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return;
        }

        // Dash horizontally in the direction the player is looking (like in the video: along the ground)
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) {
            return; // looking straight up/down -> no direction
        }
        dir = dir.normalize();

        Vec3 start = player.position();
        Vec3 end = findDestination(player, start, dir);
        double distance = horizontalDistance(start, end);
        if (distance < 0.5) {
            return; // wall right in front of us
        }

        LAST_USE.put(player.getUUID(), now);

        spawnMistTrail(level, player, start, end);

        // Move the player. teleportTo keeps rotation and current momentum.
        player.teleportTo(end.x, end.y, end.z);
        player.resetFallDistance();
        player.causeFoodExhaustion(0.3F);

        // A small push forward so the dash "flows out" instead of stopping dead
        player.setDeltaMovement(dir.x * 0.35, Math.max(player.getDeltaMovement().y, 0.0), dir.z * 0.35);
        player.needsSync = true; // sync the new velocity to the client

        level.playSound(null, start.x, start.y, start.z, SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.playSound(null, end.x, end.y, end.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    /**
     * Walks along the path in small steps and stops at the last position where the
     * player's hitbox fits. Small steps up (max. 1 block, e.g. sand ledges) are allowed.
     */
    private static Vec3 findDestination(ServerPlayer player, Vec3 start, Vec3 dir) {
        ServerLevel level = player.level();
        AABB box = player.getBoundingBox();
        Vec3 current = start;

        for (double travelled = STEP; travelled <= MAX_DISTANCE; travelled += STEP) {
            Vec3 next = new Vec3(start.x + dir.x * travelled, current.y, start.z + dir.z * travelled);
            Vec3 offset = next.subtract(start);

            if (level.noCollision(player, box.move(offset))) {
                current = next;
            } else if (level.noCollision(player, box.move(offset.add(0, 1.0, 0)))) {
                current = next.add(0, 1.0, 0); // step up one block
            } else {
                break; // wall
            }
        }
        return current;
    }

    private static void spawnMistTrail(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        double height = player.getBbHeight();
        double width = player.getBbWidth();
        double distance = start.distanceTo(end);
        Vec3 path = end.subtract(start);

        // Burst where the player disappears
        level.sendParticles(ParticleTypes.POOF, start.x, start.y + height * 0.5, start.z,
                12, width * 0.5, height * 0.4, width * 0.5, 0.02);
        level.sendParticles(ParticleTypes.SMALL_GUST, start.x, start.y + 0.3, start.z,
                1, 0, 0, 0, 0);

        // Kick up some dust from the block the player was standing on
        BlockState ground = player.getBlockStateOn();
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), start.x, start.y + 0.1, start.z,
                    20, width * 0.6, 0.05, width * 0.6, 0.15);
        }

        // Thin mist line along the ground
        for (double d = 0; d <= distance; d += 0.35) {
            Vec3 p = start.add(path.scale(d / distance));
            level.sendParticles(ParticleTypes.WHITE_SMOKE, p.x, p.y + 0.15, p.z,
                    2, 0.15, 0.05, 0.15, 0.005);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + height * 0.45, p.z,
                    1, 0.2, height * 0.3, 0.2, 0.01);
        }

        // Ghostly after-images: player-shaped pillars of mist
        for (double d = 0; d < distance; d += AFTERIMAGE_SPACING) {
            Vec3 p = start.add(path.scale(d / distance));
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + height * 0.5, p.z,
                    8, width * 0.25, height * 0.3, width * 0.25, 0.0);
            level.sendParticles(ParticleTypes.WHITE_SMOKE, p.x, p.y + height * 0.85, p.z,
                    3, width * 0.15, 0.1, width * 0.15, 0.0);
        }

        // Puff where the player reappears
        level.sendParticles(ParticleTypes.CLOUD, end.x, end.y + height * 0.5, end.z,
                10, width * 0.4, height * 0.3, width * 0.4, 0.03);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Forget players who logged out so the map doesn't grow forever. */
    public static void forget(UUID player) {
        LAST_USE.remove(player);
    }
}
