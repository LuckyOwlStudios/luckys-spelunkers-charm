package net.luckystudio.spelunkers_charm.worldgen.feature.custom.geyser;

import com.mojang.serialization.Codec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Places a geyser with two blocks of fluid under it, walled in on the sides of the top fluid block.
 */
public class GeyserFeature extends Feature<GeyserConfiguration> {

    public GeyserFeature(Codec<GeyserConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GeyserConfiguration> context) {
        if (!SpelunkersCharmConfig.GEYSERS.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (level.getBiome(origin).is(Biomes.DEEP_DARK)) return false;

        GeyserConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos fluidPos = origin.below();

        place(level, random, origin, config.geyserProvider);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            place(level, random, fluidPos.relative(direction), config.blockProvider);
        }
        place(level, random, fluidPos.below(), config.liquidProvider);
        place(level, random, fluidPos, config.liquidProvider);
        return true;
    }

    private void place(WorldGenLevel level, RandomSource random, BlockPos pos, BlockStateProvider provider) {
        this.setBlock(level, pos, provider.getState(random, pos));
    }
}
