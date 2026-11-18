package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A cheap early rail. Carts go much slower on it, and a locomotive is too heavy and breaks it. */
public class WoodenRailBlock extends RailBlock {

    /** Normal rails let carts go 0.4 blocks per tick. */
    public static final float MAX_SPEED = 0.15F;

    public WoodenRailBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return MAX_SPEED;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (!level.isClientSide() && cart instanceof LocomotiveMinecart) {
            level.destroyBlock(pos, true);
        }
    }
}
