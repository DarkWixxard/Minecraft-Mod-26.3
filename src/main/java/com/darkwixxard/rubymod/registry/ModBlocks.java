package com.darkwixxard.rubymod.registry;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RubyMod.MODID);

    public static final RegistryObject<Block> RUBY_BLOCK = BLOCKS.register("ruby_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("ruby_block"))
            .mapColor(MapColor.COLOR_RED)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
        )
    );

    private ModBlocks() {
    }
}
