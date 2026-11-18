package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * When powered, throws passengers out of carts that pass over it.
 * With a slime block under it, the passengers are launched into the air instead.
 */
public class EjectorRailBlock extends PoweredRailBlock {

    private static final double LAUNCH_SPEED = 1.3;
    private static final double LAUNCH_FORWARD = 1.5;

    /** On for a few ticks after it throws someone out, giving a short redstone pulse. */
    public static final BooleanProperty EJECTING = BooleanProperty.create("ejecting");
    private static final int PULSE_TICKS = 4;

    public EjectorRailBlock(Properties properties) {
        // Like the activator rail: it doesn't speed carts up or brake them.
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(EJECTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EJECTING);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** The pulse goes to every side except the block below, like the cargo rail, so hoppers under it aren't locked. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(EJECTING) && direction != Direction.UP ? 15 : 0;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(EJECTING)) {
            level.setBlock(pos, state.setValue(EJECTING, false), 3);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (level.isClientSide() || !state.getValue(POWERED) || !cart.isVehicle()) return;

        boolean launch = level.getBlockState(pos.below()).is(Blocks.SLIME_BLOCK);
        Vec3 motion = cart.getDeltaMovement();
        for (Entity passenger : List.copyOf(cart.getPassengers())) {
            passenger.stopRiding();
            if (launch) {
                passenger.setDeltaMovement(motion.x * LAUNCH_FORWARD, LAUNCH_SPEED, motion.z * LAUNCH_FORWARD);
                passenger.resetFallDistance();
                // Players move themselves, so they need to be told about the new speed.
                passenger.hurtMarked = true;
            }
        }
        // A piston sound whenever someone is thrown out, with a slime sound on top when launching.
        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (launch) {
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        if (!state.getValue(EJECTING)) {
            level.setBlock(pos, state.setValue(EJECTING, true), 3);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        level.scheduleTick(pos, this, PULSE_TICKS);
    }
}
