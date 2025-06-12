package net.luckystudio.splelunkers_charm.worldgen.feature.custom.icicle;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.Optional;

public class PointedBlockFeature extends Feature<IcicleConfiguration> {

    public PointedBlockFeature(Codec<IcicleConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<IcicleConfiguration> context) {
        LevelAccessor levelaccessor = context.level();
        BlockPos blockpos = context.origin();
        RandomSource randomsource = context.random();
        IcicleConfiguration IcicleConfiguration = context.config();
        Optional<Direction> optional = getTipDirection(levelaccessor, blockpos, randomsource);
        if (optional.isEmpty()) {
            return false;
        } else {
            BlockPos blockpos1 = blockpos.relative(optional.get().getOpposite());
            createPatchOfDripstoneBlocks(levelaccessor, randomsource, blockpos1, IcicleConfiguration);
            int i = randomsource.nextFloat() < IcicleConfiguration.chanceOfTallerDripstone
                    && IcicleBlockUtil.isEmptyOrWater(levelaccessor.getBlockState(blockpos.relative(optional.get())))
                    ? 2
                    : 1;
            IcicleBlockUtil.growPointedIcicle(levelaccessor, blockpos, optional.get(), i, false);
            return true;
        }
    }

    private static Optional<Direction> getTipDirection(LevelAccessor level, BlockPos pos, RandomSource random) {
        boolean flag = IcicleBlockUtil.isIcicleBase(level.getBlockState(pos.above()));
        boolean flag1 = IcicleBlockUtil.isIcicleBase(level.getBlockState(pos.below()));
        if (flag && flag1) {
            return Optional.of(random.nextBoolean() ? Direction.DOWN : Direction.UP);
        } else if (flag) {
            return Optional.of(Direction.DOWN);
        } else {
            return flag1 ? Optional.of(Direction.UP) : Optional.empty();
        }
    }

    private static void createPatchOfDripstoneBlocks(
            LevelAccessor level, RandomSource random, BlockPos pos, IcicleConfiguration config
    ) {
        IcicleBlockUtil.placeIcicleBlockIfPossible(level, pos);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(random.nextFloat() > config.chanceOfDirectionalSpread)) {
                BlockPos blockpos = pos.relative(direction);
                IcicleBlockUtil.placeIcicleBlockIfPossible(level, blockpos);
                if (!(random.nextFloat() > config.chanceOfSpreadRadius2)) {
                    BlockPos blockpos1 = blockpos.relative(Direction.getRandom(random));
                    IcicleBlockUtil.placeIcicleBlockIfPossible(level, blockpos1);
                    if (!(random.nextFloat() > config.chanceOfSpreadRadius3)) {
                        BlockPos blockpos2 = blockpos1.relative(Direction.getRandom(random));
                        IcicleBlockUtil.placeIcicleBlockIfPossible(level, blockpos2);
                    }
                }
            }
        }
    }
}
