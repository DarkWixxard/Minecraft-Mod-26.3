package com.darkwixxard.rubymod.entity;

import com.darkwixxard.rubymod.RubyMod;
import com.darkwixxard.rubymod.entity.ModEntities;
import com.darkwixxard.rubymod.entity.custom.UlyssanDragonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RubyMod.MODID);


    public static final RegistryObject<EntityType<UlyssanDragonEntity>> ULYSSAN_DRAGON =
            ENTITY_TYPES.register("ulyssan_dragon", () ->
                    EntityType.Builder.of(
                            UlyssanDragonEntity::new,
                            MobCategory.MONSTER)
                            .sized(5.0F, 4.0F)                     // hitbox: 5 blocks wide, 4 blocks high
                            .clientTrackingRange(10)            // big mob: stays visible from further away
                            .build(ENTITY_TYPES.key("ulyssan_dragon")));

    private ModEntities() {

    }
}
