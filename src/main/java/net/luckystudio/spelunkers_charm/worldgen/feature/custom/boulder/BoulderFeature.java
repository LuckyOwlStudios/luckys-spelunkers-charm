package net.luckystudio.spelunkers_charm.worldgen.feature.custom.boulder;

import com.mojang.serialization.Codec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderOre;
import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderType;
import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderUtils;
import net.luckystudio.spelunkers_charm.block.custom.boulder.HangingType;
import net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BoulderFeature extends Feature<NoneFeatureConfiguration> {

    public BoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!SpelunkersCharmConfig.BOULDERS.get()) return false;
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin().above();

        boolean isLushCave = world.getBiome(origin).is(ModBiomeTags.IS_JUNGLE_CAVE) || world.getBiome(origin).getKey() == Biomes.LUSH_CAVES;
        boolean isFrozenCave = world.getBiome(origin).is(ModBiomeTags.IS_COLD_CAVE) && world.getBiome(origin).getKey() != Biomes.DRIPSTONE_CAVES;
        boolean isBelowZero = origin.getY() < 0;

        BoulderOre ore = BoulderUtils.pickOre(context.random(), isBelowZero);
        BoulderType boulderType = BoulderType.of(ore, isLushCave);

        // The base under the boulder copies the natural floor it landed on, so it blends in.
        // Only falls back to a biome-based block when the floor is something unnatural (ores, wood, our own blocks...).
        BlockState floorState = world.getBlockState(origin.below());
        BlockState surroundingBlockState;
        if (isNaturalFloor(floorState)) {
            surroundingBlockState = floorState;
        } else if (isBelowZero) {
            surroundingBlockState = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        } else if (isLushCave) {
            surroundingBlockState = Blocks.MOSS_BLOCK.defaultBlockState();
        } else if (isFrozenCave) {
            surroundingBlockState = Blocks.BLUE_ICE.defaultBlockState();
        } else {
            surroundingBlockState = Blocks.COBBLESTONE.defaultBlockState();
        }

        BoulderUtils.placeBoulder(world, origin, boulderType, HangingType.NONE, Block.UPDATE_ALL);

        // Fill gaps in a 5x5 under the boulder so it never hangs over a hole. Existing solid floor is left alone.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = origin.offset(x, -1, z);
                if (world.getBlockState(pos).canBeReplaced()) {
                    this.setBlock(world, pos, surroundingBlockState);
                }
            }
        }
        return true;
    }

    private static boolean isNaturalFloor(BlockState state) {
        if (!state.isSolid() || state.hasBlockEntity()) return false;
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE);
    }
}
