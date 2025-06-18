package net.luckystudio.spelunkers_charm.block.custom.web_vein;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.Collection;

public interface WebVeinBehavior {
    WebVeinBehavior DEFAULT = new WebVeinBehavior() {
        @Override
        public boolean attemptSpreadVein(
                LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, @Nullable Collection<Direction> directions, boolean markForPostprocessing
        ) {
            if (directions == null) {
                return ((WebVeinBlock) ModBlocks.WEB_VEIN.get())
                        .getSameSpaceSpreader()
                        .spreadAll(levelAccessor.getBlockState(blockPos), levelAccessor, blockPos, markForPostprocessing)
                        > 0L;
            } else if (!directions.isEmpty()) {
                return (blockState.isAir() || blockState.getFluidState().is(Fluids.WATER)) && SculkVeinBlock.regrow(levelAccessor, blockPos, blockState, directions);
            } else {
                return WebVeinBehavior.super.attemptSpreadVein(levelAccessor, blockPos, blockState, directions, markForPostprocessing);
            }
        }

        @Override
        public int attemptUseCharge(
                WebSpreader.ChargeCursor p_222054_,
                LevelAccessor p_222055_,
                BlockPos p_222056_,
                RandomSource p_222057_,
                WebSpreader p_222058_,
                boolean p_222059_
        ) {
            return p_222054_.getDecayDelay() > 0 ? p_222054_.getCharge() : 0;
        }

        @Override
        public int updateDecayDelay(int p_222061_) {
            return Math.max(p_222061_ - 1, 0);
        }
    };

    default byte getSculkSpreadDelay() {
        return 1;
    }

    default void onDischarged(LevelAccessor level, BlockState state, BlockPos pos, RandomSource random) {
    }

    default boolean depositCharge(LevelAccessor level, BlockPos pos, RandomSource random) {
        return false;
    }

    default boolean attemptSpreadVein(
            LevelAccessor level, BlockPos pos, BlockState state, @Nullable Collection<Direction> directions, boolean markForPostprocessing
    ) {
        return ((MultifaceBlock)ModBlocks.WEB_VEIN.get()).getSpreader().spreadAll(state, level, pos, markForPostprocessing) > 0L;
    }

    default boolean canChangeBlockStateOnSpread() {
        return true;
    }

    default int updateDecayDelay(int currentDecayDelay) {
        return 1;
    }

    int attemptUseCharge(
            WebSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos pos, RandomSource random, WebSpreader spreader, boolean shouldConvertBlocks
    );
}
