// Wooden rail block.
package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
    public float getRailMaxSpeed(
            BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return MAX_SPEED;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (!level.isClientSide() && cart instanceof LocomotiveMinecart) {
            if (!level.destroyBlock(pos, false)
                    || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;
            int result = level.random.nextInt(100);
            if (result < 33) Block.popResource(level, pos, new ItemStack(this));
            else if (result < 66) {
                Block.popResource(
                        level, pos, new ItemStack(Items.STICK, 1 + level.random.nextInt(2)));
                if (level.random.nextBoolean())
                    Block.popResource(level, pos, new ItemStack(Blocks.OAK_PLANKS));
            }
        }
    }
}
