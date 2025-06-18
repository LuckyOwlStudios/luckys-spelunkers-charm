package net.luckystudio.spelunkers_charm.worldgen.feature.custom.directional_block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class DirectionalBlockFeature extends Feature<DirectionalBlockFeatureConfiguration> {

    public DirectionalBlockFeature(Codec<DirectionalBlockFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DirectionalBlockFeatureConfiguration> context) {
        LevelAccessor world = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        DirectionalBlockFeatureConfiguration config = context.config();
        BlockState blockStateProvider = config.blockStateProvider.getState(RandomSource.create(), origin);
        BlockState supportingBlockStateProvider = config.supportingBlockProvider.getState(RandomSource.create(), origin);

        int placed = 0;

        for (int i = 0; i < 64; i++) {
            BlockPos pos = origin.offset(random.nextInt(8) - 4, random.nextInt(6) - 3, random.nextInt(8) - 4);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = pos.relative(direction);
                BlockState adjacentState = world.getBlockState(adjacent);

                if (adjacentState.isSolidRender(world, adjacent) && world.getBlockState(pos).canBeReplaced() && adjacentState == supportingBlockStateProvider) {
                    world.setBlock(pos, blockStateProvider.setValue(BlockStateProperties.FACING, direction.getOpposite()), 2);
                    placed++;
                    break; // only place once per loop
                }
            }
        }

        return placed > 0;
    }
}
