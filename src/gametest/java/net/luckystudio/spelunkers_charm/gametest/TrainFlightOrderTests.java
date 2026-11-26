// Train flight order tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainFlightOrderTests {
    static TrainFlightFixture ramp(GameTestHelper h) {
        return ramp(h.getLevel(), h.absolutePos(BlockPos.ZERO));
    }

    static TrainFlightFixture ramp(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        var fixture = new TrainFlightFixture(level, origin, 37, 41, 60);
        fixture.build();
        for (int x = 60; x <= 112; x++) {
            for (int z = 7; z <= 9; z++)
                level.setBlock(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 1, 8), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            var rail =
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                            .setValue(PoweredRailBlock.POWERED, true);
            if (x == 100)
                rail =
                        net.luckystudio.spelunkers_charm.init.ModBlocks.DIRECTIONAL_RAIL
                                .get()
                                .defaultBlockState()
                                .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                                .setValue(PoweredRailBlock.POWERED, false);
            level.setBlock(origin.offset(x, 2, 8), rail, 2);
        }
        for (int x = 26; x <= 38; x++)
            for (int z = 7; z <= 9; z++)
                for (int y = 2; y <= 16; y++)
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        for (int x = 26; x <= 38; x++) {
            int y = 2 + Math.min(12, x - 26);
            for (int z = 7; z <= 9; z++)
                level.setBlock(
                        origin.offset(x, y - 1, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        }
        for (int pass = 0; pass < 2; pass++)
            for (int x = 26; x < 38; x++) {
                int y = 2 + x - 26;
                level.setBlock(
                        origin.offset(x, y, 8),
                        Blocks.POWERED_RAIL
                                .defaultBlockState()
                                .setValue(PoweredRailBlock.SHAPE, RailShape.ASCENDING_EAST)
                                .setValue(PoweredRailBlock.POWERED, true),
                        2);
            }
        for (int x = 39; x <= 40; x++)
            level.setBlock(
                    origin.offset(x, 2, 8),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.RailBlock.SHAPE,
                                    RailShape.EAST_WEST),
                    2);
        return fixture;
    }

    @GameTest(template = "rail_flight_large", timeoutTicks = 300)
    public static void fiveWagonsKeepOrderOverTallPoweredRampAndLanding(GameTestHelper h) {
        tallRamp(h, 5, false);
    }

    @GameTest(template = "rail_flight_large", timeoutTicks = 350)
    public static void twelveWagonsKeepOrderOverTallPoweredRampAndLanding(GameTestHelper h) {
        tallRamp(h, 12, false);
    }

    @GameTest(template = "rail_flight_large", timeoutTicks = 350)
    public static void pushedTrainKeepsOrderOverTallPoweredRampAndLanding(GameTestHelper h) {
        tallRamp(h, 8, true);
    }

    static void tallRamp(GameTestHelper h, int wagons, boolean pushing) {
        var fixture = ramp(h);
        var carts = fixture.train(24.5, wagons);
        if (pushing) {
            var old = carts.removeFirst();
            old.discard();
            var front = new Minecart(h.getLevel(), old.getX(), old.getY(), old.getZ());
            front.setDeltaMovement(.4, 0, 0);
            h.getLevel().addFreshEntity(front);
            carts.addFirst(front);
            var rear = carts.removeLast();
            var engine = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
            engine.setDriveDirection(net.minecraft.core.Direction.WEST);
            engine.switchDirection();
            for (int slot = 0; slot < 3; slot++)
                engine.setItem(
                        slot,
                        new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.COAL, 8));
            h.assertTrue(
                    MinecartCouplings.replaceEntity(rear, engine), "Pusher replacement failed");
            carts.add(engine);
            MinecartCouplings.get(h.getLevel()).link(front, carts.get(1));
        }
        boolean[] flew = new boolean[carts.size()], landed = new boolean[carts.size()];
        var links = MinecartCouplings.get(h.getLevel());
        var previous = new Vec3[carts.size()];
        var velocities = new Vec3[carts.size()];
        var trace = new StringBuilder("tick,cart,x,y,z,vx,vy,rail,gap\n");
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 260) return;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        double gap = i == 0 ? 0 : carts.get(i - 1).getX() - cart.getX();
                        trace.append(h.getTick())
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX())
                                .append(',')
                                .append(cart.getY())
                                .append(',')
                                .append(cart.getZ())
                                .append(',')
                                .append(cart.getDeltaMovement().x)
                                .append(',')
                                .append(cart.getDeltaMovement().y)
                                .append(',')
                                .append(cart.isOnRails())
                                .append(',')
                                .append(gap)
                                .append('\n');
                        try {
                            if (i == carts.size() - 1)
                                Files.writeString(
                                        Path.of("flight-order-" + wagons + "-" + pushing + ".csv"),
                                        trace);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        h.assertTrue(cart.isAlive(), "Ramp destroyed cart " + i);
                        if (previous[i] != null) {
                            double limit =
                                    Math.max(
                                            1.65,
                                            Math.max(
                                                            velocities[i].length(),
                                                            cart.getDeltaMovement().length())
                                                    + .75);
                            h.assertTrue(
                                    cart.position().distanceTo(previous[i]) < limit,
                                    "Ramp teleported wagon "
                                            + i
                                            + ", tick="
                                            + h.getTick()
                                            + ", from="
                                            + previous[i]
                                            + ", to="
                                            + cart.position()
                                            + ", velocity="
                                            + cart.getDeltaMovement());
                        }
                        previous[i] = cart.position();
                        velocities[i] = cart.getDeltaMovement();
                        flew[i] |= ((MinecartMotionVisuals) cart).spelunkersCharm$airborne();
                        landed[i] |=
                                flew[i]
                                        && cart.isOnRails()
                                        && cart.getX() > fixture.origin.getX() + 41;
                        if (i > 0) {
                            var parent = carts.get(i - 1);
                            h.assertTrue(
                                    cart.distanceTo(parent) >= 1.0
                                            && MinecartFlightPhysics.axis(parent)
                                                            .scale(
                                                                    MinecartHingeGeometry.side(
                                                                            parent, cart))
                                                            .dot(
                                                                    cart.position()
                                                                            .subtract(
                                                                                    parent
                                                                                            .position()))
                                                    > -.08,
                                    "Wagons overlapped/overtook: tick="
                                            + h.getTick()
                                            + ", wagon="
                                            + i
                                            + ", gap="
                                            + gap
                                            + ", distance="
                                            + cart.distanceTo(parent)
                                            + ", projection="
                                            + MinecartFlightPhysics.axis(parent)
                                                    .scale(MinecartHingeGeometry.side(parent, cart))
                                                    .dot(
                                                            cart.position()
                                                                    .subtract(parent.position()))
                                            + ", rails="
                                            + parent.isOnRails()
                                            + "/"
                                            + cart.isOnRails());
                            h.assertTrue(
                                    links.linked(carts.get(i - 1).getUUID(), cart.getUUID()),
                                    "Ramp separated train " + i);
                        }
                    }
                });
        h.runAfterDelay(
                260,
                () -> {
                    for (int i = 0; i < carts.size(); i++)
                        h.assertTrue(flew[i] && landed[i], "Cart failed flight/landing " + i);
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void fasterRearCannotPassSlowFrontInMidair(GameTestHelper h) {
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i < 6; i++) {
            Vec3 p = h.absoluteVec(new Vec3(22 - i * 1.55, 17, 8.5));
            var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            cart.setNoGravity(true);
            cart.setDeltaMovement(i == 0 ? .08 : .72, .05, 0);
            h.getLevel().addFreshEntity(cart);
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
            if (i > 0) MinecartCouplings.get(h.getLevel()).link(carts.getLast(), cart);
            carts.add(cart);
        }
        for (int x = 1; x < 63; x++) h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
        h.onEachTick(
                () -> {
                    for (int i = 1; i < carts.size(); i++) {
                        var parent = carts.get(i - 1);
                        var cart = carts.get(i);
                        h.assertTrue(
                                parent.distanceTo(cart) > .98
                                        && MinecartFlightPhysics.axis(parent)
                                                        .scale(
                                                                MinecartHingeGeometry.side(
                                                                        parent, cart))
                                                        .dot(
                                                                cart.position()
                                                                        .subtract(
                                                                                parent.position()))
                                                > -.08,
                                "Faster airborne rear crossed a fixed hinge: tick="
                                        + h.getTick()
                                        + ", wagon="
                                        + i);
                    }
                });
        h.runAfterDelay(70, h::succeed);
    }
}
