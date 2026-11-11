package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Passive guide, using the wooden rack appearance from the original release.
 *
 * <p>Former tooltip: "Passive rack. Descending does not require power."
 */
public class BasicLiftTrackBlock extends LiftTrackBlock {
    public static final MapCodec<BasicLiftTrackBlock> CODEC = simpleCodec(BasicLiftTrackBlock::new);

    public BasicLiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BasicLiftTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasSignal(Level level, BlockPos pos, BlockState state) {
        return false;
    }
}
