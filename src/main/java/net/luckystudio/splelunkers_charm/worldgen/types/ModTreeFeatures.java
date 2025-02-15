package net.luckystudio.splelunkers_charm.worldgen.types;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.worldgen.feature.ModFeature;
import net.luckystudio.splelunkers_charm.worldgen.util.ModFeatureUtils;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class ModTreeFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_CAVE_MUSHROOM = ModFeatureUtils.createKey("huge_cave_mushroom");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(
                context,
                HUGE_CAVE_MUSHROOM,
                ModFeature.HUGE_CAVE_MUSHROOM.get(),
                new HugeMushroomFeatureConfiguration(
                        BlockStateProvider.simple(
                                ModBlocks.CAVE_MUSHROOM_BLOCK.get()
                                        .defaultBlockState()
                                        .setValue(HugeMushroomBlock.UP, Boolean.valueOf(true))
                                        .setValue(HugeMushroomBlock.DOWN, Boolean.valueOf(false))
                        ),
                        BlockStateProvider.simple(
                                Blocks.MUSHROOM_STEM
                                        .defaultBlockState()
                                        .setValue(HugeMushroomBlock.UP, Boolean.valueOf(false))
                                        .setValue(HugeMushroomBlock.DOWN, Boolean.valueOf(false))
                        ),
                        3
                )
        );
    }
}
