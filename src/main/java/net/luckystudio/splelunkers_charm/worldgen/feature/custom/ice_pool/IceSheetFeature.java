package net.luckystudio.splelunkers_charm.worldgen.feature.custom.ice_pool;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class IceSheetFeature extends Feature<IceSheetConfiguration> {

    public IceSheetFeature(Codec<IceSheetConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<IceSheetConfiguration> context) {
        WorldGenLevel worldgenlevel = context.level();
        IceSheetConfiguration config = context.config();
        RandomSource randomsource = context.random();
        BlockPos blockpos = context.origin();
        Predicate<BlockState> predicate = p_204782_ -> p_204782_.is(config.replaceable);
        int i = config.xzRadius.sample(randomsource) + 1;
        int j = config.xzRadius.sample(randomsource) + 1;
        Set<BlockPos> set = this.placeGroundPatch(worldgenlevel);
        return !set.isEmpty();
    }

    protected Set<BlockPos> placeGroundPatch(
            WorldGenLevel level
    ) {
        Set<BlockPos> set1 = new HashSet<>();

        for (BlockPos blockpos1 : set1) {
            level.setBlock(blockpos1, Blocks.ICE.defaultBlockState(), 2);
        }

        return set1;
    }
}
