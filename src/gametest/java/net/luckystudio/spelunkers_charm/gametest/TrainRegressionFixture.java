// Train regression fixture.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.*;

final class TrainRegressionFixture {
    private TrainRegressionFixture() {}

    static Vec3 rotate(Vec3 p, int turns) {
        for (int i = 0; i < turns; i++) p = new Vec3(32 - p.z, p.y, p.x);
        return p;
    }

    static ArrayList<AbstractMinecart> curve(
            ServerLevel level, BlockPos origin, int rotation, ServerPlayer player) {
        for (int pass = 0; pass < 2; pass++)
            for (int i = 8; i <= 24; i++) {
                for (int leg = 0; leg < 2; leg++) {
                    if (leg == 1 && i < 17 || leg == 0 && i > 16) continue;
                    int x = leg == 0 ? i : 16, z = leg == 0 ? 16 : i;
                    var pos =
                            origin.offset(
                                    BlockPos.containing(
                                            rotate(new Vec3(x + .5, 2, z + .5), rotation)));
                    var state =
                            Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(
                                            RailBlock.SHAPE,
                                            i == 16
                                                    ? RailShape.SOUTH_WEST
                                                    : leg == 0
                                                            ? RailShape.EAST_WEST
                                                            : RailShape.NORTH_SOUTH);
                    for (int k = 0; k < rotation; k++) state = state.rotate(Rotation.CLOCKWISE_90);
                    level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(pos, state, 2);
                }
            }
        var carts = new ArrayList<AbstractMinecart>();
        double corner = Math.sqrt(.5);
        for (int i = 0; i < 3; i++) {
            double s = corner * .5 + (i - 1) * 1.55;
            Vec3 point =
                    s < 0
                            ? new Vec3(16 + s, 2.0625, 16.5)
                            : s <= corner
                                    ? new Vec3(
                                            16 + s / Math.sqrt(2), 2.0625, 16.5 + s / Math.sqrt(2))
                                    : new Vec3(16.5, 2.0625, 17 + s - corner);
            Vec3 next =
                    s < 0
                            ? new Vec3(1, 0, 0)
                            : s <= corner ? new Vec3(1, 0, 1).normalize() : new Vec3(0, 0, 1);
            Vec3 p = Vec3.atLowerCornerOf(origin).add(rotate(point, rotation));
            Vec3 axis =
                    rotate(point.add(next), rotation).subtract(rotate(point, rotation)).normalize();
            if (i == 1) axis = axis.scale(-1);
            var cart = new Minecart(level, p.x, p.y, p.z);
            cart.setNoGravity(true);
            facing(cart, axis);
            level.addFreshEntity(cart);
            carts.add(cart);
        }
        var hookStack = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) hookStack.getItem();
        player.getInventory().setItem(10, new ItemStack(Items.CHAIN, 8));
        for (int i = 1; i < carts.size(); i++) {
            hook.useOnCart(hookStack, player, carts.get(i - 1));
            hook.useOnCart(hookStack, player, carts.get(i));
        }
        return carts;
    }

    static void facing(AbstractMinecart cart, Vec3 axis) {
        float yaw = (float) Math.toDegrees(Math.atan2(axis.z, axis.x));
        cart.setYRot(yaw);
        cart.yRotO = yaw;
        var visual = (MinecartMotionVisuals) cart;
        var s = visual.spelunkersCharm$motion();
        s.visualYaw = s.visualYawOld = yaw;
        s.visualYawReady = true;
        visual.spelunkersCharm$setBodyYaw(yaw);
    }

    static ArrayList<AbstractMinecart> jump(
            ServerLevel level, BlockPos origin, int height, boolean reverse, int wagons) {
        for (int x = 36; x < 184; x++) {
            var p = origin.offset(x, 1, 8);
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(
                    p.above(),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                    2);
        }
        for (int pass = 0; pass < 2; pass++)
            for (int x = 0; x <= 35; x++) {
                int y = 2 + height + Math.max(0, x - 30);
                var p = origin.offset(x, y, 8);
                level.setBlock(p.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                if (x >= 30) level.setBlock(p.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                level.setBlock(
                        p,
                        Blocks.POWERED_RAIL
                                .defaultBlockState()
                                .setValue(
                                        PoweredRailBlock.SHAPE,
                                        x >= 30 ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST)
                                .setValue(PoweredRailBlock.POWERED, true),
                        2);
            }
        var carts = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(level);
        for (int i = 0; i <= wagons; i++) {
            var p = Vec3.atLowerCornerOf(origin).add(27.5 - i * 1.55, 2 + height + .0625, 8.5);
            AbstractMinecart cart;
            if (i == (reverse ? wagons : 0)) {
                var engine = new LocomotiveMinecart(level, p.x, p.y, p.z);
                engine.setDriveDirection(reverse ? Direction.WEST : Direction.EAST);
                if (reverse) engine.switchDirection();
                for (int slot = 0; slot < 3; slot++)
                    engine.setItem(slot, new ItemStack(Items.COAL, 16));
                cart = engine;
            } else cart = new Minecart(level, p.x, p.y, p.z);
            facing(cart, new Vec3(reverse && i == wagons ? -1 : 1, 0, 0));
            cart.setDeltaMovement(.4, 0, 0);
            level.addFreshEntity(cart);
            if (i > 0 && links.link(carts.getLast(), cart) != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Jump fixture could not link " + i);
            carts.add(cart);
        }
        return carts;
    }

    static ArrayList<AbstractMinecart> downhill(
            ServerLevel level, BlockPos origin, int rotation, boolean powered, int wagons) {
        for (int pass = 0; pass < 2; pass++)
            for (int x = 3; x <= 100; x++) {
                int y = 2 + Math.clamp(x - 30, 0, 16);
                var cell = BlockPos.containing(rotate(new Vec3(x + .5, y, 16.5), rotation));
                var pos = origin.offset(cell);
                level.setBlock(
                        pos.below(),
                        (powered ? Blocks.REDSTONE_BLOCK : Blocks.STONE).defaultBlockState(),
                        2);
                if (x >= 30 && x < 46)
                    level.setBlock(
                            origin.offset(
                                    BlockPos.containing(
                                            rotate(new Vec3(x + 1.5, y, 16.5), rotation))),
                            Blocks.STONE.defaultBlockState(),
                            2);
                var shape = x >= 30 && x < 46 ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST;
                var state =
                        powered
                                ? Blocks.POWERED_RAIL
                                        .defaultBlockState()
                                        .setValue(PoweredRailBlock.SHAPE, shape)
                                        .setValue(PoweredRailBlock.POWERED, true)
                                : Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
                for (int i = 0; i < rotation; i++) state = state.rotate(Rotation.CLOCKWISE_90);
                level.setBlock(pos, state, 2);
            }
        var carts = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(level);
        Vec3 forward =
                rotate(new Vec3(17, 0, 16), rotation)
                        .subtract(rotate(new Vec3(16, 0, 16), rotation));
        for (int i = 0; i <= wagons; i++) {
            var p =
                    Vec3.atLowerCornerOf(origin)
                            .add(rotate(new Vec3(62.5 - i * 1.55, 18.0625, 16.5), rotation));
            AbstractMinecart cart;
            if (i == 0) {
                var engine = new LocomotiveMinecart(level, p.x, p.y, p.z);
                engine.setDriveDirection(Direction.getNearest(forward.x, 0, forward.z));
                engine.switchDirection();
                for (int slot = 0; slot < 3; slot++)
                    engine.setItem(slot, new ItemStack(Items.COAL, 16));
                cart = engine;
            } else cart = new Minecart(level, p.x, p.y, p.z);
            facing(cart, forward);
            level.addFreshEntity(cart);
            if (i > 0 && links.link(carts.getLast(), cart) != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Downhill fixture could not link");
            carts.add(cart);
        }
        return carts;
    }

    static Loop loop(ServerLevel level, BlockPos origin, int wagons) {
        var route = new PassengerTrainRoute(level, origin);
        route.build();
        double start = 80;
        Vec3 direction = route.point(start + .1).subtract(route.point(start - .1)).normalize();
        while (Math.abs(direction.x) > .01 && Math.abs(direction.z) > .01) {
            start += 1;
            direction = route.point(start + .1).subtract(route.point(start - .1)).normalize();
        }
        var carts = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(level);
        LocomotiveMinecart engine = null;
        for (int i = 0; i <= wagons; i++) {
            double at = start - i * 1.55;
            var p = route.point(at);
            AbstractMinecart cart;
            Vec3 axis = route.point(at + .03).subtract(route.point(at - .03)).normalize();
            if (i == 0) {
                engine = new LocomotiveMinecart(level, p.x, p.y, p.z);
                engine.setDriveDirection(Direction.getNearest(axis.x, 0, axis.z));
                for (int slot = 0; slot < 3; slot++)
                    engine.setItem(slot, new ItemStack(Items.COAL, 16));
                cart = engine;
            } else cart = new Minecart(level, p.x, p.y, p.z);
            facing(cart, axis);
            level.addFreshEntity(cart);
            if (i > 0 && links.link(carts.getLast(), cart) != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Hairpin train did not link at " + i);
            carts.add(cart);
        }
        return new Loop(route, carts, engine);
    }

    /** Holds a route and its reversible train. */
    record Loop(
            PassengerTrainRoute route,
            ArrayList<AbstractMinecart> carts,
            LocomotiveMinecart engine) {}

    static void intact(ServerLevel level, List<AbstractMinecart> carts) {
        var links = MinecartCouplings.get(level);
        for (int i = 0; i < carts.size(); i++) {
            var cart = carts.get(i);
            if (!cart.isAlive())
                throw new AssertionError(
                        "Cart destroyed: "
                                + i
                                + " at "
                                + cart.position()
                                + ", removal="
                                + cart.getRemovalReason()
                                + ", tick="
                                + cart.tickCount);
            if (i > 0 && !links.linked(carts.get(i - 1).getUUID(), cart.getUUID()))
                throw new AssertionError(
                        "Normal train uncoupled: "
                                + i
                                + " at "
                                + cart.position()
                                + ", gap="
                                + carts.get(i - 1).distanceTo(cart));
        }
    }

    static void ends(ServerLevel level, List<AbstractMinecart> carts) {
        for (int i = 1; i < carts.size(); i++) {
            var a = carts.get(i - 1);
            var b = carts.get(i);
            var path = MinecartRailPath.between(a, b);
            if (path != null) {
                if (MinecartHingeGeometry.supportedAxis(a)
                                        .scale(MinecartHingeGeometry.side(a, b))
                                        .dot(path.firstToward())
                                < .1
                        || MinecartHingeGeometry.supportedAxis(b)
                                        .scale(MinecartHingeGeometry.side(b, a))
                                        .dot(path.secondToward())
                                < .1)
                    throw new AssertionError("Hook crossed a fixed cart end: " + i);
            }
        }
    }
}
