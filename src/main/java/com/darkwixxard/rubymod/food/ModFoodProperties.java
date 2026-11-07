package com.darkwixxard.rubymod.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class ModFoodProperties {
    public static final FoodProperties RUBY_APPLE = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(1.2F)
            .build();
    private ModFoodProperties() {

    }
}