package com.darkwixxard.rubymod.item.custom;

import com.darkwixxard.rubymod.entity.projectile.IceArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// The ITEM in the inventory. Its only job: tell the bow which arrow entity to create.
public class IceArrowItem extends ArrowItem {
    public IceArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
        return new IceArrowEntity(level, owner, itemStack.copyWithCount(1), firedFromWeapon);
    }
}