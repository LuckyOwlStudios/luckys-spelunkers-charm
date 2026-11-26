// Perpendicular trench tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

import java.nio.file.*;
import java.util.*;

@GameTestHolder("spelunkers_perpendicular_trench_tests")
@PrefixGameTestTemplate(false)
public final class PerpendicularTrenchTests {
    @GameTest(template = "course", timeoutTicks = 600)
    public static void trenchRightTurns0(GameTestHelper h) {
        check(h, 0, true, false, 6);
    }

    @GameTest(template = "course", timeoutTicks = 600)
    public static void trenchRightTurns1(GameTestHelper h) {
        check(h, 1, true, false, 6);
    }

    @GameTest(template = "course", timeoutTicks = 600)
    public static void trenchRightTurns2(GameTestHelper h) {
        check(h, 2, true, false, 6);
    }

    @GameTest(template = "course", timeoutTicks = 600)
    public static void trenchRightTurns3(GameTestHelper h) {
        check(h, 3, true, false, 6);
    }

    @GameTest(template = "course", timeoutTicks = 600)
    public static void singleLocomotiveInTrench(GameTestHelper h) {
        check(h, 0, true, false, 0);
    }

    @GameTest(template = "course", timeoutTicks = 600)
    public static void reversePushingThroughTrench(GameTestHelper h) {
        check(h, 2, true, true, 6);
    }

    private static void check(
            GameTestHelper h, int rotation, boolean walls, boolean push, int wagons) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var scene =
                new Course(
                        h.getLevel(), h.absolutePos(BlockPos.ZERO), rotation, walls, push, wagons);
        var trace =
                new StringBuilder(
                        "tick,cart,x,y,z,speed,drive_dot,body_yaw,physical_yaw,reference,air,collision,brake,front_dot\n");
        int[] stage = new int[scene.carts.size()];
        boolean[] done = {false};
        h.onEachTick(
                () -> {
                    if (done[0]) return;
                    try {
                        for (int i = 0; i < scene.carts.size(); i++) {
                            var c = scene.carts.get(i);
                            var m = ((MinecartMotionVisuals) c).spelunkersCharm$motion();
                            var p = scene.local(c.position());
                            if (p.y < 5 && MinecartTrainBrakes.onRail(c))
                                stage[i] = Math.max(stage[i], 1);
                            if (p.x > 61 && p.y < 3.4 && MinecartTrainBrakes.onRail(c))
                                stage[i] = 2;
                            var track = MinecartTrack.current(c);
                            Vec3 expected = track == null ? null : scene.headings.get(track.pos());
                            double facing =
                                    c == scene.engine ? scene.engine.bodyYaw() : m.visualYaw;
                            double front =
                                    expected == null
                                            ? 1
                                            : new Vec3(
                                                                    Math.cos(
                                                                            Math.toRadians(facing)),
                                                                    0,
                                                                    Math.sin(
                                                                            Math.toRadians(facing)))
                                                            .dot(expected)
                                                    * (c == scene.engine && push ? -1 : 1);
                            double drive =
                                    c == scene.engine && expected != null
                                            ? scene.engine.driveVector().dot(expected)
                                            : 1;
                            trace.append(h.getTick())
                                    .append(',')
                                    .append(i)
                                    .append(',')
                                    .append(p.x)
                                    .append(',')
                                    .append(p.y)
                                    .append(',')
                                    .append(p.z)
                                    .append(',')
                                    .append(c.getDeltaMovement().horizontalDistance())
                                    .append(',')
                                    .append(drive)
                                    .append(',')
                                    .append(
                                            c == scene.engine
                                                    ? scene.engine.bodyYaw()
                                                    : m.visualYaw)
                                    .append(',')
                                    .append(m.physicalYaw)
                                    .append(',')
                                    .append(m.flightReferenceYaw)
                                    .append(',')
                                    .append(m.airborne)
                                    .append(',')
                                    .append(c.horizontalCollision)
                                    .append(',')
                                    .append(MinecartHandbrake.active(c))
                                    .append(',')
                                    .append(front)
                                    .append('\n');
                            if (MinecartTrainBrakes.onRail(c)
                                    && expected != null
                                    && h.getTick() > 2)
                                h.assertTrue(
                                        front > .1,
                                        "Cart front flipped at "
                                                + p
                                                + ", cart="
                                                + i
                                                + ", dot="
                                                + front);
                            if (c == scene.engine) {
                                h.assertFalse(
                                        scene.engine.emergencyBrake(),
                                        "Trench capture enabled brake");
                                h.assertTrue(
                                        scene.engine.isReversed() == push,
                                        "Trench changed drive mode");
                                if (stage[i] == 2 && MinecartTrainBrakes.onRail(c)) {
                                    h.assertTrue(
                                            drive > .1,
                                            "Trench reversed engine drive: " + drive + " at " + p);
                                    h.assertTrue(
                                            c.getDeltaMovement().horizontalDistance() > .10,
                                            "Trench capture lost momentum: "
                                                    + c.getDeltaMovement()
                                                    + " at "
                                                    + p);
                                }
                            }
                        }
                        Files.writeString(
                                Path.of("trench-" + rotation + "-" + push + "-" + wagons + ".csv"),
                                trace,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                        trace.setLength(0);
                        TrainRegressionFixture.intact(h.getLevel(), scene.carts);
                        if (scene.finished()) {
                            h.assertTrue(
                                    stage[scene.carts.indexOf(scene.engine)] == 2,
                                    "Engine never captured perpendicular rails");
                            var links = MinecartCouplings.get(h.getLevel());
                            var neighbors = links.neighbors(scene.engine.getUUID());
                            if (!neighbors.isEmpty()
                                    && h.getLevel().getEntity(neighbors.getFirst())
                                            instanceof AbstractMinecart peer) {
                                links.unlink(scene.engine.getUUID(), peer.getUUID());
                                h.assertTrue(
                                        links.link(scene.engine, peer)
                                                == MinecartCouplings.Result.LINKED,
                                        "Engine could not reconnect after right turns");
                            }
                            done[0] = true;
                            h.succeed();
                        }
                    } catch (Throwable error) {
                        try {
                            Files.writeString(
                                    Path.of(
                                            "trench-" + rotation + "-" + push + "-" + wagons
                                                    + ".csv"),
                                    trace,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND);
                        } catch (Exception ignored) {
                        }
                        done[0] = true;
                        h.fail(error.toString());
                    }
                });
    }

    /** Recreates the one-block-wide trench and the perpendicular rail above its edge. */
    static final class Course {
        final net.minecraft.server.level.ServerLevel level;
        final BlockPos origin;
        final int rotation, corner;
        final Map<BlockPos, Vec3> headings = new HashMap<>();
        final ArrayList<AbstractMinecart> carts = new ArrayList<>();
        final LocomotiveMinecart engine;

        Course(
                net.minecraft.server.level.ServerLevel level,
                BlockPos origin,
                int rotation,
                boolean walls,
                boolean push,
                int wagons) {
            this.level = level;
            this.origin = origin;
            this.rotation = rotation;
            corner = push || rotation == 2 ? 32 : 16;
            for (int x = 34; x <= 64; x++)
                for (int z = 2; z <= corner + 2; z++) {
                    level.setBlock(pos(x, 2, z), Blocks.STONE.defaultBlockState(), 2);
                    if (walls)
                        level.setBlock(pos(x, 3, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
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
                    rail(x, 4, 8, RailShape.EAST_WEST, new Vec3(1, 0, 0), false);
                for (int z = 2; z < 5; z++)
                    rail(61, 4, z, RailShape.NORTH_SOUTH, new Vec3(0, 0, 1), false);
                rail(61, 3, 5, RailShape.ASCENDING_NORTH, new Vec3(0, 0, 1), false);
                for (int z = 6; z < corner; z++)
                    rail(61, 3, z, RailShape.NORTH_SOUTH, new Vec3(0, 0, 1), false);
                rail(61, 3, corner, RailShape.NORTH_WEST, new Vec3(-1, 0, 1).normalize(), false);
                for (int x = 60; x > 56; x--)
                    rail(x, 3, corner, RailShape.EAST_WEST, new Vec3(-1, 0, 0), false);
                rail(56, 3, corner, RailShape.NORTH_EAST, new Vec3(-1, 0, -1).normalize(), false);
                for (int z = corner - 1; z >= 10; z--)
                    rail(56, 3, z, RailShape.NORTH_SOUTH, new Vec3(0, 0, -1), false);
                rail(56, 3, 10, RailShape.SOUTH_WEST, new Vec3(-1, 0, -1).normalize(), false);
                for (int x = 55; x >= 34; x--)
                    rail(x, 3, 10, RailShape.EAST_WEST, new Vec3(-1, 0, 0), false);
            }
            LocomotiveMinecart found = null;
            var links = MinecartCouplings.get(level);
            for (int i = 0; i <= wagons; i++) {
                var p = world(new Vec3(26.5 - i * 1.55, 12.0625, 8.5));
                AbstractMinecart c;
                Vec3 front = direction(new Vec3(push && i == wagons ? -1 : 1, 0, 0));
                if (i == (push ? wagons : 0)) {
                    found = new LocomotiveMinecart(level, p.x, p.y, p.z);
                    found.setDriveDirection(Direction.getNearest(front.x, 0, front.z));
                    if (push) found.selectDirection(true);
                    for (int slot = 0; slot < 3; slot++)
                        found.setItem(slot, new ItemStack(Items.COAL, 16));
                    c = found;
                } else c = new Minecart(level, p.x, p.y, p.z);
                TrainRegressionFixture.facing(c, front);
                c.setDeltaMovement(direction(new Vec3(.45, 0, 0)));
                level.addFreshEntity(c);
                if (i > 0 && links.link(carts.getLast(), c) != MinecartCouplings.Result.LINKED)
                    throw new AssertionError("Trench fixture coupling failed");
                carts.add(c);
            }
            engine = found;
        }

        Vec3 direction(Vec3 v) {
            for (int i = 0; i < rotation; i++) v = new Vec3(-v.z, v.y, v.x);
            return v;
        }

        Vec3 world(Vec3 v) {
            for (int i = 0; i < rotation; i++) v = new Vec3(128 - v.z, v.y, v.x);
            return v.add(Vec3.atLowerCornerOf(origin));
        }

        Vec3 local(Vec3 v) {
            v = v.subtract(Vec3.atLowerCornerOf(origin));
            for (int i = 0; i < rotation; i++) v = new Vec3(v.z, v.y, 128 - v.x);
            return v;
        }

        BlockPos pos(int x, int y, int z) {
            return BlockPos.containing(world(new Vec3(x + .5, y, z + .5)));
        }

        private void rail(int x, int y, int z, RailShape shape, Vec3 heading, boolean powered) {
            var p = pos(x, y, z);
            level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), 2);
            var state =
                    powered
                            ? Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, shape)
                                    .setValue(PoweredRailBlock.POWERED, true)
                            : Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
            if (powered) level.setBlock(p.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            for (int n = 0; n < rotation; n++) state = state.rotate(Rotation.CLOCKWISE_90);
            level.setBlock(p, state, 2);
            headings.put(p, direction(heading));
        }

        boolean finished() {
            var p = local(engine.position());
            return p.y < 3.4
                    && Math.abs(p.x - 56.5) < .1
                    && p.z < corner - (corner == 32 ? 11 : 1.2)
                    && p.z > 9.5;
        }
    }
}
