package com.darkwixxard.rubymod.block;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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

    // Ores drop 3-7 experience when mined without Silk Touch, like diamond ore
    public static final RegistryObject<Block> RUBY_ORE = BLOCKS.register("ruby_ore",
        () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("ruby_ore"))
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F)
        )
    );

    public static final RegistryObject<Block> DEEPSLATE_RUBY_ORE = BLOCKS.register("deepslate_ruby_ore",
        () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("deepslate_ruby_ore"))
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(4.5F, 3.0F)
            .sound(SoundType.DEEPSLATE)
        )
    );

    public static final RegistryObject<Block> NOXIUM_BLOCK = BLOCKS.register("noxium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("noxium_block"))
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(10.0F, 15.0F)
                    .sound(SoundType.METAL)
            )
    );

    // Ores drop 3-7 experience when mined without Silk Touch, like diamond ore
    public static final RegistryObject<Block> NOXIUM_ORE = BLOCKS.register("noxium_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("noxium_ore"))
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(7.0F, 10.0F)
            )
    );

    public static final RegistryObject<Block> DEEPSLATE_NOXIUM_ORE = BLOCKS.register("deepslate_noxium_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("deepslate_noxium_ore"))
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(8.5F, 10.0F)
                    .sound(SoundType.DEEPSLATE)
            )
    );

    private ModBlocks() {
    }
}
