// Train flight fixture.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

final class TrainFlightFixture {
    final ServerLevel level;
    final BlockPos origin;
    final int ramp, landing, stop;

    TrainFlightFixture(ServerLevel level, BlockPos origin, int ramp, int landing, int stop) {
        this.level = level;
        this.origin = origin;
        this.ramp = ramp;
        this.landing = landing;
        this.stop = stop;
    }

    void build() {
        for (int x = 0; x <= 62; x++)
            for (int z = 7; z <= 9; z++)
                level.setBlock(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
        for (int pass = 0; pass < 2; pass++)
            for (int x = 0; x <= 62; x++) {
                var pos = origin.offset(x, 2, 8);
                if (x > ramp && x < landing) {
                    level.setBlock(
                            pos,
                            x == ramp + 1
                                    ? Blocks.STONE.defaultBlockState()
                                    : Blocks.AIR.defaultBlockState(),
                            2);
                    continue;
                }
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                var rail =
                        Blocks.POWERED_RAIL
                                .defaultBlockState()
                                .setValue(
                                        PoweredRailBlock.SHAPE,
                                        x == ramp ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST)
                                .setValue(PoweredRailBlock.POWERED, true);
                if (x == stop)
                    rail =
                            ModBlocks.DIRECTIONAL_RAIL
                                    .get()
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                                    .setValue(PoweredRailBlock.POWERED, false);
                level.setBlock(pos, rail, 2);
            }
    }

    Vec3 point(double x) {
        return new Vec3(
                origin.getX() + x,
                origin.getY() + 2.0625 + (x >= ramp && x < ramp + 1 ? x - ramp : 0),
                origin.getZ() + 8.5);
    }

    LocomotiveMinecart engine(double x, double speed) {
        Vec3 p = point(x);
        var cart = new LocomotiveMinecart(level, p.x, p.y, p.z);
        cart.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) cart.setItem(slot, new ItemStack(Items.COAL, 8));
        cart.setDeltaMovement(speed, 0, 0);
        level.addFreshEntity(cart);
        return cart;
    }

    ArrayList<AbstractMinecart> train(double x, int wagons) {
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(engine(x, .40));
        var links = MinecartCouplings.get(level);
        for (int i = 1; i <= wagons; i++) {
            Vec3 p = point(x - i * 1.55);
            var cart = new Minecart(level, p.x, p.y, p.z);
            cart.setDeltaMovement(.40, 0, 0);
            level.addFreshEntity(cart);
            links.link(carts.getLast(), cart);
            carts.add(cart);
        }
        return carts;
    }
}
