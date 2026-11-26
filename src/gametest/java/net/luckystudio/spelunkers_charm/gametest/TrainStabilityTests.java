// Train stability tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

@GameTestHolder("spelunkers_stability_tests")
@PrefixGameTestTemplate(false)
public final class TrainStabilityTests {
    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_flight_large")
    public static void elasticSolverStaysFiniteForTwentyFourConnectedCarts(GameTestHelper h)
            throws Exception {
        var carts = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 0; i < 24; i++) {
            Vec3 p = h.absoluteVec(new Vec3(50 - i * 1.55, 20, 8));
            var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            cart.setNoGravity(true);
            cart.setDeltaMovement(.2, 0, 0);
            h.getLevel().addFreshEntity(cart);
            if (i > 0) links.link(carts.getLast(), cart);
            carts.add(cart);
        }
        long start = System.nanoTime();
        for (int tick = 0; tick < 100; tick++) {
            for (var cart : carts) MinecartFlightPhysics.defer(cart, 1);
            MinecartFlightPhysics.step(h.getLevel(), carts, links);
        }
        for (var cart : carts)
            h.assertTrue(
                    Double.isFinite(cart.getX()) && cart.getDeltaMovement().length() < 1,
                    "Elastic solver produced invalid motion");
        Files.writeString(
                Path.of("train-constraint-benchmark.txt"),
                "nanos_per_tick=" + ((System.nanoTime() - start) / 100));
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_flight_large")
    public static void unrelatedGroundedCartsAreNotMovedByAirbornePhysics(GameTestHelper h) {
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i < 40; i++) {
            Vec3 p = h.absoluteVec(new Vec3(8 + i * 2, 3, 14));
            var c = new Minecart(h.getLevel(), p.x, p.y, p.z);
            c.setOnGround(true);
            c.setNoGravity(true);
            h.getLevel().addFreshEntity(c);
            carts.add(c);
        }
        Vec3 start = h.absoluteVec(new Vec3(8, 20, 8));
        var free = new Minecart(h.getLevel(), start.x, start.y, start.z);
        free.setDeltaMovement(.2, 0, 0);
        h.getLevel().addFreshEntity(free);
        carts.add(free);
        var positions = carts.stream().map(AbstractMinecart::position).toList();
        MinecartFlightPhysics.defer(free, 1);
        MinecartFlightPhysics.step(h.getLevel(), carts, MinecartCouplings.get(h.getLevel()));
        for (int i = 0; i < 40; i++)
            h.assertTrue(
                    carts.get(i).position().equals(positions.get(i)),
                    "Unrelated grounded cart entered the airborne solver");
        h.assertTrue(
                free.getY() < start.y && free.getX() > start.x,
                "Airborne cart did not receive independent gravity");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "empty")
    public static void verticalDrawbarsKeepTheirFrontAndTiltThroughHorizontalNoise(
            GameTestHelper h) {
        for (int facing = 0; facing < 2; facing++) {
            var carts = new ArrayList<AbstractMinecart>();
            for (int i = 0; i < 3; i++) {
                Vec3 p = h.absoluteVec(new Vec3(6 + i * 1.55, 15, 6 + facing * 5));
                var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
                cart.setYRot(facing * 180);
                h.getLevel().addFreshEntity(cart);
                carts.add(cart);
                ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
                if (i > 0) MinecartCouplings.get(h.getLevel()).link(carts.get(i - 1), cart);
            }
            var middle = carts.get(1);
            MinecartBodyPose.axis(middle, 1);
            for (int step = 0; step < 80; step++) {
                double noise = step % 2 == 0 ? .002 : -.002;
                carts.get(0).setPos(middle.getX() - noise, middle.getY() - 1.55, middle.getZ());
                carts.get(2).setPos(middle.getX() + noise, middle.getY() + 1.55, middle.getZ());
                middle.tickCount++;
                float pitch = MinecartBodyPose.pitch(middle, facing * 180, 1);
                if (step > 20)
                    h.assertTrue(
                            (facing == 0 ? pitch : -pitch) > 65,
                            "Stationary vertical wagon inverted its front: "
                                    + pitch
                                    + ", facing="
                                    + facing);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 480)
    public static void brakedFoldedTrainKeepsStableJointsAndDoesNotOverlap(GameTestHelper h) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var fixture = new TrainStabilityFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        fixture.build();
        var carts = fixture.train();
        var start = carts.getFirst().position();
        var previous = new Vec3[carts.size()];
        var settled = new Vec3[carts.size()];
        double[] maxTurn = {0};
        var trace = new StringBuilder("tick,cart,x,y,yaw,pitch,gap\n");
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 420) return;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        Vec3 axis = MinecartBodyPose.axis(cart, 1);
                        if (h.getTick() == 250) settled[i] = cart.position();
                        if (h.getTick() > 250)
                            h.assertTrue(
                                    cart.position().distanceTo(settled[i]) < .2,
                                    "A braked hanging train kept oscillating: " + i);
                        if (previous[i] != null && h.getTick() > 20) {
                            double turn =
                                    Math.toDegrees(
                                            Math.acos(Math.clamp(previous[i].dot(axis), -1, 1)));
                            maxTurn[0] = Math.max(maxTurn[0], turn);
                            h.assertTrue(
                                    turn < 24,
                                    "Suspended wagon snapped its body: " + i + ", turn=" + turn);
                        }
                        previous[i] = axis;
                        double gap = i == 0 ? 0 : cart.distanceTo(carts.get(i - 1));
                        h.assertTrue(cart.isAlive(), "Stopped train lost wagon " + i);
                        if (i > 0)
                            h.assertTrue(
                                    gap > 1.0 && gap < 2.85,
                                    "Stopped train stretched or overlapped joint "
                                            + i
                                            + ": "
                                            + gap);
                        trace.append(h.getTick())
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX())
                                .append(',')
                                .append(cart.getY())
                                .append(',')
                                .append(MinecartBodyPose.yaw(cart, 1))
                                .append(',')
                                .append(
                                        MinecartBodyPose.pitch(
                                                cart, MinecartBodyPose.yaw(cart, 1), 1))
                                .append(',')
                                .append(gap)
                                .append('\n');
                    }
                    h.assertTrue(
                            carts.getFirst().position().distanceTo(start) < .08,
                            "Suspended wagons moved the braked engine");
                });
        h.runAfterDelay(
                420,
                () -> {
                    try {
                        Files.writeString(Path.of("stopped-ramp-trace.csv"), trace);
                        Files.writeString(
                                Path.of("stopped-ramp-checks.txt"), "PASS max_turn=" + maxTurn[0]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    h.succeed();
                });
    }
}

/** Provides train stability fixture data for this gameplay test. */
final class TrainStabilityFixture {
    final net.minecraft.server.level.ServerLevel level;
    final BlockPos origin;

    TrainStabilityFixture(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    void build() {
        for (int x = 2; x < 28; x++)
            for (int z = 5; z <= 11; z++)
                level.setBlock(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
        for (int x = 2; x <= 10; x++)
            level.setBlock(
                    origin.offset(x, 2, 8),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.RailBlock.SHAPE,
                                    RailShape.EAST_WEST),
                    2);
        for (int x = 13; x <= 19; x++) {
            int y = 21 - x;
            level.setBlock(
                    origin.offset(x, y - 1, 8), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
            level.setBlock(
                    origin.offset(x, y, 8),
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.ASCENDING_WEST),
                    2);
        }
    }

    ArrayList<AbstractMinecart> train() {
        double[][] points = {
            {6, 2.0625},
            {7.55, 2.0625},
            {9.10, 2.0625},
            {10.35, 3},
            {10.90, 4.45},
            {11.1, 6},
            {11.35, 7.5},
            {12.5, 8.55},
            {13.6, 8.4625},
            {14.7, 7.3625},
            {15.8, 6.2625},
            {16.9, 5.1625},
            {18, 4.0625}
        };
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i < points.length; i++) {
            Vec3 p =
                    new Vec3(
                            origin.getX() + points[i][0],
                            origin.getY() + points[i][1],
                            origin.getZ() + 8.5);
            AbstractMinecart cart =
                    i == 0
                            ? new LocomotiveMinecart(level, p.x, p.y, p.z)
                            : new Minecart(level, p.x, p.y, p.z);
            if (cart instanceof LocomotiveMinecart engine) engine.setDriveDirection(Direction.WEST);
            cart.setYRot(0);
            level.addFreshEntity(cart);
            if (i > 0
                    && MinecartCouplings.get(level).link(carts.getLast(), cart)
                            != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Folded fixture link failed " + i);
            carts.add(cart);
        }
        MinecartHandbrake.engage(carts.getFirst());
        return carts;
    }
}
