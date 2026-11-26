// Train weight tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@GameTestHolder("spelunkers_weight_tests")
@PrefixGameTestTemplate(false)
public final class TrainWeightTests {
    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 40)
    public static void fiveHangingWagonsPullHarderThanOne(GameTestHelper h) {
        var fixture = fixture(h);
        var heavy = fixture.hanging(5, 3, 4, false);
        var light = fixture.hanging(1, 3, 12, false);
        double heavyStart = heavy.rails.getLast().getX(), lightStart = light.rails.getLast().getX();
        h.runAfterDelay(
                6,
                () -> {
                    double heavyDistance = heavy.rails.getLast().getX() - heavyStart,
                            lightDistance = light.rails.getLast().getX() - lightStart;
                    trace("hanging-count", heavyDistance, lightDistance);
                    h.assertTrue(
                            heavyDistance > lightDistance * 1.20 + .005,
                            "Five hanging wagons did not exert more pull: five="
                                    + heavyDistance
                                    + ", one="
                                    + lightDistance);
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 300)
    public static void fiveHangingWagonsPullThreeUnpoweredWagonsOffEdge(GameTestHelper h) {
        var train = fixture(h).hanging(5, 3, 8, false);
        var origin = h.absolutePos(BlockPos.ZERO);
        var links = MinecartCouplings.get(h.getLevel());
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 240) return;
                    for (int i = 0; i < train.carts.size(); i++) {
                        var cart = train.carts.get(i);
                        h.assertTrue(cart.isAlive(), "Weight pull destroyed a wagon");
                        if (i > 0) {
                            h.assertTrue(
                                    links.linked(cart.getUUID(), train.carts.get(i - 1).getUUID()),
                                    "Weight pull broke drawbar");
                            h.assertTrue(
                                    cart.distanceTo(train.carts.get(i - 1)) > 1.0
                                            && cart.distanceTo(train.carts.get(i - 1)) < 3,
                                    "Weight pull overlapped or stretched wagons");
                        }
                    }
                });
        h.runAfterDelay(
                240,
                () -> {
                    for (var cart : train.carts)
                        h.assertTrue(
                                cart.getY() < origin.getY() + 4,
                                "Hanging weight failed to pull stationary upper train down: "
                                        + cart.position());
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 40)
    public static void moreSupportedWagonsProvideMoreInertia(GameTestHelper h) {
        var fixture = fixture(h);
        var light = fixture.hanging(5, 3, 4, false);
        var heavy = fixture.hanging(5, 6, 12, false);
        double a = light.rails.getLast().getX(), b = heavy.rails.getLast().getX();
        h.runAfterDelay(
                6,
                () -> {
                    double fast = light.rails.getLast().getX() - a,
                            slow = heavy.rails.getLast().getX() - b;
                    trace("supported-count", fast, slow);
                    h.assertTrue(
                            fast > slow * 1.10 + .002,
                            "More rail-supported wagons did not resist acceleration: three="
                                    + fast
                                    + ", six="
                                    + slow);
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 90)
    public static void handbrakeHoldsAgainstHangingWeight(GameTestHelper h) {
        var train = fixture(h).hanging(5, 3, 8, true);
        var engine = (LocomotiveMinecart) train.rails.getLast();
        engine.toggleEmergencyBrake();
        var positions = train.rails.stream().map(AbstractMinecart::position).toList();
        h.runAfterDelay(
                60,
                () -> {
                    h.assertTrue(
                            engine.emergencyBrake(), "Hanging weight released engine handbrake");
                    for (int i = 0; i < train.rails.size(); i++)
                        h.assertTrue(
                                train.rails.get(i).position().distanceTo(positions.get(i)) < .005,
                                "Hanging weight moved braked train");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 80)
    public static void flatStationaryTrainDoesNotGainFreePropulsion(GameTestHelper h) {
        var train = fixture(h).hanging(0, 8, 8, false);
        var positions = train.rails.stream().map(AbstractMinecart::position).toList();
        h.runAfterDelay(
                50,
                () -> {
                    for (int i = 0; i < train.rails.size(); i++)
                        h.assertTrue(
                                train.rails.get(i).position().distanceTo(positions.get(i)) < .005,
                                "Flat train gained gravity propulsion");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 70)
    public static void disconnectingHangingSectionRemovesItsPull(GameTestHelper h) {
        var fixture = fixture(h);
        var train = fixture.hanging(5, 3, 8, false);
        h.runAfterDelay(
                6,
                () -> {
                    var links = MinecartCouplings.get(h.getLevel());
                    var boundary = train.rails.getFirst();
                    var hanging = train.carts.get(4);
                    h.assertTrue(
                            links.unlink(boundary.getUUID(), hanging.getUUID()),
                            "Could not detach hanging section");
                    for (int i = 0; i < train.rails.size(); i++) {
                        var cart = train.rails.get(i);
                        cart.setPos(fixture.point(48.7 - i * 1.55, 82.0625, 8));
                        cart.setDeltaMovement(Vec3.ZERO);
                    }
                    var positions = train.rails.stream().map(AbstractMinecart::position).toList();
                    h.runAfterDelay(
                            8,
                            () -> {
                                for (int i = 0; i < train.rails.size(); i++)
                                    h.assertTrue(
                                            train.rails
                                                            .get(i)
                                                            .position()
                                                            .distanceTo(positions.get(i))
                                                    < .01,
                                            "Detached weight kept accelerating upper train");
                                h.succeed();
                            });
                });
    }

    private static TrainWeightFixture fixture(GameTestHelper h) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        return new TrainWeightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
    }

    private static void trace(String name, double first, double second) {
        try {
            Files.writeString(Path.of("weight-" + name + ".txt"), first + "," + second + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

/** Provides train weight fixture data for this gameplay test. */
final class TrainWeightFixture {
    final net.minecraft.server.level.ServerLevel level;
    final BlockPos origin;

    TrainWeightFixture(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    Vec3 point(double x, double y, int z) {
        return new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + z + .5);
    }

    Train hanging(int airCount, int railCount, int z, boolean engine) {
        for (int x = 0; x < 96; x++)
            for (int offset = -1; offset <= 1; offset++)
                level.setBlock(
                        origin.offset(x, 1, z + offset), Blocks.STONE.defaultBlockState(), 2);
        for (int x = 0; x < 96; x++)
            level.setBlock(
                    origin.offset(x, 2, z),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                    2);
        for (int x = 0; x <= 48; x++) {
            level.setBlock(origin.offset(x, 81, z), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(
                    origin.offset(x, 82, z),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                    2);
        }
        var carts = new ArrayList<AbstractMinecart>();
        var rails = new ArrayList<AbstractMinecart>();
        for (int i = 0; i < airCount; i++) {
            int offset = airCount - i;
            var p =
                    point(
                            48.7 + offset * .90,
                            82.0625 - offset * Math.sqrt(1.55 * 1.55 - .90 * .90),
                            z);
            var cart = new Minecart(level, p.x, p.y, p.z);
            add(carts, cart);
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
        }
        for (int i = 0; i < railCount; i++) {
            var p = point(48.7 - i * 1.55, 82.0625, z);
            AbstractMinecart cart;
            if (engine && i == railCount - 1) {
                var locomotive = new LocomotiveMinecart(level, p.x, p.y, p.z);
                locomotive.setDriveDirection(Direction.WEST);
                cart = locomotive;
            } else cart = new Minecart(level, p.x, p.y, p.z);
            add(carts, cart);
            rails.add(cart);
            ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(true);
        }
        return new Train(carts, rails);
    }

    private void add(ArrayList<AbstractMinecart> carts, AbstractMinecart cart) {
        level.addFreshEntity(cart);
        if (!carts.isEmpty()
                && MinecartCouplings.get(level).link(carts.getLast(), cart)
                        != MinecartCouplings.Result.LINKED)
            throw new AssertionError("Weight fixture did not attach coupling");
        carts.add(cart);
    }

    /** Provides train data for this gameplay test. */
    static final class Train {
        final ArrayList<AbstractMinecart> carts, rails;

        Train(ArrayList<AbstractMinecart> carts, ArrayList<AbstractMinecart> rails) {
            this.carts = carts;
            this.rails = rails;
        }
    }
}
