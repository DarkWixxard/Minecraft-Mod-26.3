package com.darkwixxard.rubymod.item.custom;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NoxiumBowItem extends BowItem {
    private static final double ARROW_DAMAGE = 10.0;      // normal arrows: 2.0 -> 5x damage
    private static final float SPEED_MULTIPLIER = 2.0F;   // arrows fly twice as fast
    private static final float SIDE_ARROW_ANGLE = 10.0F;  // angle of the two extra arrows (degrees)
    private static final int COOLDOWN_TICKS = 2;          // small delay between shots (20 ticks = 1 second)

    public NoxiumBowItem(Properties properties) {
        super(properties);
    }

    // Always shoots 3 arrows. The two extra ones cost no ammo and can't be picked up (like Multishot).
    @Override
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                         List<ItemStack> projectiles, float velocity, float inaccuracy, boolean crit, LivingEntity target) {
        List<ItemStack> allProjectiles = new ArrayList<>(projectiles);
        if (projectiles.size() == 1) {
            ItemStack extra = projectiles.get(0).copyWithCount(1);
            extra.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            allProjectiles.add(extra);
            allProjectiles.add(extra.copy());
        }
        super.shoot(level, shooter, hand, weapon, allProjectiles, velocity * SPEED_MULTIPLIER, inaccuracy, crit, target);

        if (shooter instanceof Player player) {
            player.getCooldowns().addCooldown(weapon, COOLDOWN_TICKS);
        }
    }

    // Called once for every arrow: this is where the damage is raised.
    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(ARROW_DAMAGE);
            // arrow.igniteForSeconds(5); // optional: burning arrows
        }
        return projectile;
    }

    // Sends arrow 0 straight ahead and arrows 1 and 2 slightly to the left and right.
    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity,
                                   float inaccuracy, float angle, LivingEntity target) {
        float spread = switch (index) {
            case 1 -> -SIDE_ARROW_ANGLE;
            case 2 -> SIDE_ARROW_ANGLE;
            default -> angle;
        };
        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, spread, target);
    }
}