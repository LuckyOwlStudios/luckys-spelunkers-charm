package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.minecraft.core.BlockPos;
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

    public EjectorRailBlock(Properties properties) {
        // Like the activator rail: it doesn't speed carts up or brake them.
        super(properties);
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
        level.playSound(null, pos, launch ? SoundEvents.SLIME_BLOCK_FALL : SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS, 1.0F, launch ? 0.8F : 1.4F);
    }
}
