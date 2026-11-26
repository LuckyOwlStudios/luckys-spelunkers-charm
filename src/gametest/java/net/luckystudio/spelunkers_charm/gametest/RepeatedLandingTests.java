// Repeated landing tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

import java.nio.file.*;
import java.util.*;

@GameTestHolder("spelunkers_repeated_landing_tests")
@PrefixGameTestTemplate(false)
public final class RepeatedLandingTests {
    @GameTest(template = "course", timeoutTicks = 430)
    public static void pullThroughUTurn(GameTestHelper h) {
        course(h, false, false, 65, 6);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void pullThroughSBend(GameTestHelper h) {
        course(h, false, true, 65, 6);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void pushThroughUTurn(GameTestHelper h) {
        course(h, true, false, 67, 6);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void pullLongTrainThroughUTurn(GameTestHelper h) {
        course(h, false, false, 67, 12);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void singleEngineThroughUTurn(GameTestHelper h) {
        course(h, false, false, 65, 0);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void landDirectlyBeforeBend(GameTestHelper h) {
        course(h, false, false, 64, 6);
    }

    @GameTest(template = "course", timeoutTicks = 430)
    public static void landBeforeAdjacentCorners(GameTestHelper h) {
        course(h, false, false, 65, 6, 1);
    }

    private static void course(
            GameTestHelper h, boolean push, boolean sBend, int corner, int wagons) {
        course(h, push, sBend, corner, wagons, 2);
    }

    private static void course(
            GameTestHelper h, boolean push, boolean sBend, int corner, int wagons, int span) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var scene =
                new Course(
                        h.getLevel(),
                        h.absolutePos(BlockPos.ZERO),
                        corner,
                        sBend,
                        push,
                        wagons,
                        span);
        boolean[] wasAir = {false}, done = {false};
        int[] launches = {0}, captures = {0};
        double[] lowest = {10}, previous = {scene.engine.getX()};
        String name =
                (push ? "push" : "pull")
                        + (sBend ? "-s" : "-u")
                        + "-"
                        + wagons
                        + "-"
                        + corner
                        + "-"
                        + span;
        var trace =
                new StringBuilder(
                        "tick,x,y,z,speed,drive_dot,body_yaw,physical_yaw,reference_yaw,air,brake,held,reversing,rail_speed\n");
        h.onEachTick(
                () -> {
                    if (done[0]) return;
                    try {
                        var e = scene.engine;
                        var m = ((MinecartMotionVisuals) e).spelunkersCharm$motion();
                        boolean air = ((MinecartMotionVisuals) e).spelunkersCharm$airborne();
                        if (air && !wasAir[0]) launches[0]++;
                        if (!air && wasAir[0]) captures[0]++;
                        wasAir[0] = air;
                        var track = MinecartTrack.current(e);
                        Vec3 heading = track == null ? null : scene.headings.get(track.pos());
                        double dot = heading == null ? 1 : e.driveVector().dot(heading);
                        trace.append(h.getTick())
                                .append(',')
                                .append(e.getX())
                                .append(',')
                                .append(e.getY())
                                .append(',')
                                .append(e.getZ())
                                .append(',')
                                .append(e.getDeltaMovement().horizontalDistance())
                                .append(',')
                                .append(dot)
                                .append(',')
                                .append(e.bodyYaw())
                                .append(',')
                                .append(m.physicalYaw)
                                .append(',')
                                .append(m.flightReferenceYaw)
                                .append(',')
                                .append(air)
                                .append(',')
                                .append(e.emergencyBrake())
                                .append(',')
                                .append(m.held)
                                .append(',')
                                .append(m.reversalRequested)
                                .append(',')
                                .append(MinecartSpeedRules.flightLimit(e))
                                .append('\n');
                        Files.writeString(Path.of("repeated-landing-" + name + ".csv"), trace);
                        TrainRegressionFixture.intact(h.getLevel(), scene.carts);
                        h.assertFalse(e.emergencyBrake(), "Capture engaged engine brake");
                        h.assertTrue(e.isReversed() == push, "Capture changed selected drive mode");
                        if (MinecartTrainBrakes.onRail(e) && heading != null) {
                            h.assertTrue(
                                    dot > .1,
                                    "Rail capture flipped drive: " + dot + " at " + track.pos());
                            if (captures[0] > 0) {
                                lowest[0] =
                                        Math.min(
                                                lowest[0],
                                                e.getDeltaMovement().horizontalDistance());
                                h.assertTrue(
                                        e.getDeltaMovement().horizontalDistance() > .10,
                                        "Landing stalled locomotive: " + e.getDeltaMovement());
                            }
                        }
                        previous[0] = e.getX();
                        boolean finished = scene.finished();
                        if (finished) {
                            h.assertTrue(
                                    launches[0] >= 2 && captures[0] >= 2,
                                    "Course missed consecutive jumps: "
                                            + launches[0]
                                            + ", "
                                            + captures[0]);
                            done[0] = true;
                            h.succeed();
                        }
                    } catch (Throwable error) {
                        done[0] = true;
                        h.fail(error.toString());
                    }
                });
    }

    @GameTest(template = "course")
    public static void landingAfterTwoTurnsKeepsPhysicalFront(GameTestHelper h) {
        var o = h.absolutePos(BlockPos.ZERO);
        var p = o.offset(20, 4, 8);
        h.setBlock(new BlockPos(20, 3, 8), Blocks.STONE);
        h.setBlock(
                new BlockPos(20, 4, 8),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        var e =
                new LocomotiveMinecart(
                        h.getLevel(), p.getX() + .5, p.getY() + .0625, p.getZ() + .5);
        e.setDriveDirection(Direction.EAST);
        h.getLevel().addFreshEntity(e);
        var c = new Minecart(h.getLevel(), e.getX() - 1.55, e.getY(), e.getZ());
        h.getLevel().addFreshEntity(c);
        MinecartCouplings.get(h.getLevel()).link(e, c);
        var m = ((MinecartMotionVisuals) e).spelunkersCharm$motion();
        m.flightRotationReady = true;
        m.flightReferenceYaw = 0;
        m.physicalYaw = 0;
        TrainRegressionFixture.facing(e, new Vec3(1, 0, 0));
        MinecartBodyOrientation.followRail(e, new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        MinecartBodyOrientation.followRail(e, new Vec3(0, 0, 1), new Vec3(-1, 0, 0));
        c.setPos(e.getX() + 1.55, e.getY(), e.getZ());
        MinecartBodyOrientation.land(e, MinecartTrack.current(e));
        h.assertTrue(
                e.bodyDirection().x < -.9 && e.driveVector().x < -.9,
                "Second capture reused the first launch facing");
        h.succeed();
    }

    /** Builds two drops and a short return bend with a fixed direction per rail cell. */
    static final class Course {
        final net.minecraft.server.level.ServerLevel level;
        final BlockPos origin;
        final Map<BlockPos, Vec3> headings = new HashMap<>();
        final ArrayList<AbstractMinecart> carts = new ArrayList<>();
        final LocomotiveMinecart engine;
        final int corner, span;
        final boolean sBend;

        Course(
                net.minecraft.server.level.ServerLevel level,
                BlockPos origin,
                int corner,
                boolean sBend,
                boolean push,
                int wagons) {
            this(level, origin, corner, sBend, push, wagons, 2);
        }

        Course(
                net.minecraft.server.level.ServerLevel level,
                BlockPos origin,
                int corner,
                boolean sBend,
                boolean push,
                int wagons,
                int span) {
            this.level = level;
            this.origin = origin;
            this.corner = corner;
            this.span = span;
            this.sBend = sBend;
            for (int pass = 0; pass < 2; pass++) {
                for (int x = 0; x <= 35; x++)
                    rail(
                            x,
                            12 + Math.max(0, x - 30),
                            8,
                            x >= 30 ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST,
                            new Vec3(1, 0, 0),
                            true);
                for (int x = 36; x <= 60; x++)
                    rail(x, 4, 8, RailShape.EAST_WEST, new Vec3(1, 0, 0), true);
                for (int x = 61; x < corner; x++)
                    rail(x, 3, 8, RailShape.EAST_WEST, new Vec3(1, 0, 0), true);
                rail(corner, 3, 8, RailShape.SOUTH_WEST, new Vec3(1, 0, 1).normalize(), false);
                for (int z = 9; z < 8 + span; z++)
                    rail(corner, 3, z, RailShape.NORTH_SOUTH, new Vec3(0, 0, 1), true);
                rail(
                        corner,
                        3,
                        8 + span,
                        sBend ? RailShape.NORTH_EAST : RailShape.NORTH_WEST,
                        new Vec3(sBend ? 1 : -1, 0, 1).normalize(),
                        false);
                for (int n = 1; n <= 34; n++)
                    rail(
                            corner + (sBend ? n : -n),
                            3,
                            8 + span,
                            RailShape.EAST_WEST,
                            new Vec3(sBend ? 1 : -1, 0, 0),
                            true);
            }
            LocomotiveMinecart found = null;
            for (int i = 0; i <= wagons; i++) {
                var p = Vec3.atLowerCornerOf(origin).add(26.5 - i * 1.55, 12.0625, 8.5);
                AbstractMinecart c;
                if (i == (push ? wagons : 0)) {
                    found = new LocomotiveMinecart(level, p.x, p.y, p.z);
                    found.setDriveDirection(push ? Direction.WEST : Direction.EAST);
                    if (push) found.selectDirection(true);
                    for (int slot = 0; slot < 3; slot++)
                        found.setItem(slot, new ItemStack(Items.COAL, 16));
                    c = found;
                } else c = new Minecart(level, p.x, p.y, p.z);
                TrainRegressionFixture.facing(
                        c, new Vec3(c instanceof LocomotiveMinecart && push ? -1 : 1, 0, 0));
                c.setDeltaMovement(.45, 0, 0);
                level.addFreshEntity(c);
                if (i > 0
                        && MinecartCouplings.get(level).link(carts.getLast(), c)
                                != MinecartCouplings.Result.LINKED)
                    throw new AssertionError("Course link failed");
                carts.add(c);
            }
            engine = found;
        }

        boolean finished() {
            return carts.stream()
                    .allMatch(
                            c ->
                                    sBend
                                            ? c.getX() > origin.getX() + corner + 9
                                            : c.getX() < origin.getX() + corner - 9
                                                    && c.getZ() > origin.getZ() + 8 + span - .2);
        }

        private void rail(int x, int y, int z, RailShape shape, Vec3 heading, boolean powered) {
            var p = origin.offset(x, y, z);
            level.setBlock(p.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            var state =
                    powered
                            ? Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, shape)
                                    .setValue(PoweredRailBlock.POWERED, true)
                            : Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
            level.setBlock(p, state, 2);
            headings.put(p, heading);
        }
    }
}
