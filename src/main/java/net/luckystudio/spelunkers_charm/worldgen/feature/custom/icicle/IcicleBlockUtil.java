package net.luckystudio.spelunkers_charm.worldgen.feature.custom.icicle;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

import java.util.function.Consumer;

public class IcicleBlockUtil {

    protected static void buildBaseToTipColumn(Direction direction, int height, boolean mergeTip, Consumer<BlockState> blockSetter) {
        if (height >= 3) {
            blockSetter.accept(createIcicle(direction, DripstoneThickness.BASE));

            for (int i = 0; i < height - 3; i++) {
                blockSetter.accept(createIcicle(direction, DripstoneThickness.MIDDLE));
            }
        }

        if (height >= 2) {
            blockSetter.accept(createIcicle(direction, DripstoneThickness.FRUSTUM));
        }

        if (height >= 1) {
            blockSetter.accept(createIcicle(direction, mergeTip ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP));
        }
    }

    public static void growPointedIcicle(LevelAccessor level, BlockPos pos, Direction direction, int height, boolean mergeTip) {
        if (isIcicleBase(level.getBlockState(pos.relative(direction.getOpposite())))) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();
            buildBaseToTipColumn(direction, height, mergeTip, blockState -> {
                if (blockState.is(ModBlocks.ICICLE.get())) {
                    blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(level.isWaterAt(blockpos$mutableblockpos)));
                }

                level.setBlock(blockpos$mutableblockpos, blockState, 2);
                blockpos$mutableblockpos.move(direction);
            });
        }
    }

    public static boolean placeIcicleBlockIfPossible(LevelAccessor level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.is(ModBlocks.PERMAFROST)) {
            level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 2);
            return true;
        } else {
            return false;
        }
    }

    public static BlockState createIcicle(Direction direction, DripstoneThickness dripstoneThickness) {
        return ModBlocks.ICICLE.get()
                .defaultBlockState()
                .setValue(BlockStateProperties.VERTICAL_DIRECTION, direction)
                .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, dripstoneThickness);
    }

    public static boolean isIcicleBase(BlockState state) {
        return state.is(Blocks.PACKED_ICE) || state.is(ModBlocks.PERMAFROST);
    }

    public static boolean isEmptyOrWater(LevelAccessor level, BlockPos pos) {
        return level.isStateAtPosition(pos, IcicleBlockUtil::isEmptyOrWater);
    }

    public static boolean isEmptyOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    public static boolean isNeitherEmptyNorWater(BlockState state) {
        return !state.isAir() && !state.is(Blocks.WATER);
    }

    public static boolean isIcicleBaseOrLava(BlockState state) {
        return isIcicleBase(state) || state.is(Blocks.LAVA);
    }

    public static boolean isCircleMostlyEmbeddedInStone(WorldGenLevel level, BlockPos pos, int radius) {
        if (isEmptyOrWaterOrLava(level, pos)) {
            return false;
        } else {
            float f = 6.0F;
            float f1 = 6.0F / (float)radius;

            for (float f2 = 0.0F; f2 < (float) (Math.PI * 2); f2 += f1) {
                int i = (int)(Mth.cos(f2) * (float)radius);
                int j = (int)(Mth.sin(f2) * (float)radius);
                if (isEmptyOrWaterOrLava(level, pos.offset(i, 0, j))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean isEmptyOrWaterOrLava(LevelAccessor level, BlockPos pos) {
        return level.isStateAtPosition(pos, IcicleBlockUtil::isEmptyOrWaterOrLava);
    }

    public static boolean isEmptyOrWaterOrLava(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    public static double getIcicleHeight(double radius, double maxRadius, double scale, double minRadius) {
        if (radius < minRadius) {
            radius = minRadius;
        }

        double d0 = 0.384;
        double d1 = radius / maxRadius * 0.384;
        double d2 = 0.75 * Math.pow(d1, 1.3333333333333333);
        double d3 = Math.pow(d1, 0.6666666666666666);
        double d4 = 0.3333333333333333 * Math.log(d1);
        double d5 = scale * (d2 - d3 - d4);
        d5 = Math.max(d5, 0.0);
        return d5 / 0.384 * maxRadius;
    }
}
