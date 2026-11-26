// Rail stability tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class RailStabilityTests {
    @GameTest(template = "rail_large", timeoutTicks = 2050)
    public static void locomotiveKeepsDirectionAcrossRepeatedPoweredZigzags(GameTestHelper h) {
        loop(h, false, 0);
    }

    @GameTest(template = "rail_large", timeoutTicks = 2050)
    public static void locomotiveKeepsReverseDirectionAcrossRepeatedPoweredZigzags(
            GameTestHelper h) {
        loop(h, true, 0);
    }

    @GameTest(template = "rail_large", timeoutTicks = 2050)
    public static void threeCoupledWagonsNeverJamAcrossRepeatedZigzags(GameTestHelper h) {
        loop(h, false, 3);
    }

    @GameTest(template = "rail_large", timeoutTicks = 2050)
    public static void reverseTrainNeverJamsAcrossRepeatedZigzags(GameTestHelper h) {
        loop(h, true, 3);
    }

    private static void loop(GameTestHelper h, boolean reverse, int wagons) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        Route route = new Route(h);
        route.build();
        int direction = reverse ? -1 : 1;
        double start = 5.0;
        Vec3 position = route.point(start);
        var engine = new LocomotiveMinecart(h.getLevel(), position.x, position.y, position.z);
        engine.setDriveDirection(reverse ? Direction.WEST : Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 16));
        h.getLevel().addFreshEntity(engine);
        ArrayList<AbstractMinecart> carts = new ArrayList<>();
        carts.add(engine);
        var links = MinecartCouplings.get(h.getLevel());
        for (int wagon = 1; wagon <= wagons; wagon++) {
            Vec3 point = route.point(start - direction * wagon * 1.55);
            var cart = new Minecart(h.getLevel(), point.x, point.y, point.z);
            h.getLevel().addFreshEntity(cart);
            links.link(carts.getLast().getUUID(), cart.getUUID());
            carts.add(cart);
        }
        double[] previous =
                carts.stream().mapToDouble(cart -> route.progress(cart.position())).toArray();
        double[] progress = new double[carts.size()];
        double[] window = new double[carts.size()];
        float[] bodyYaw = new float[carts.size()];
        java.util.Arrays.fill(bodyYaw, Float.NaN);
        StringBuilder csv = new StringBuilder("tick,cart,progress,speed,x,z,drive_x,drive_z\n");
        String report = "stability-" + (reverse ? "reverse" : "forward") + "-" + wagons + ".csv";
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    for (int index = 0; index < carts.size(); index++) {
                        var cart = carts.get(index);
                        float yaw = MinecartBodyOrientation.yaw(cart, 1);
                        if (!Float.isNaN(bodyYaw[index]))
                            h.assertTrue(
                                    Math.abs(yaw - bodyYaw[index]) < 90.01,
                                    "Wagon swapped its front and rear on curve");
                        bodyYaw[index] = yaw;
                        for (int render = 0; render < 12; render++) {
                            MinecartBodyOrientation.yaw(cart, .25f);
                            MinecartBodyPose.axis(cart, .75f);
                            h.assertTrue(
                                    Math.abs(MinecartBodyOrientation.yaw(cart, 1) - yaw) < .001,
                                    "Repeated rendering changed physical facing within one tick");
                        }
                        if (cart instanceof LocomotiveMinecart loco) {
                            double angle = Math.toRadians(yaw);
                            h.assertTrue(
                                    new Vec3(Math.cos(angle), 0, Math.sin(angle))
                                                    .dot(loco.bodyDirection())
                                            > .7,
                                    "Rendered locomotive front disagreed with physical front");
                        }
                        h.assertTrue(
                                ((MinecartMotionVisuals) cart).spelunkersCharm$braking() == 0,
                                "Train generated brake sparks while following a curve");
                        double current = route.progress(cart.position());
                        double delta = current - previous[index];
                        if (delta < -route.length / 2) delta += route.length;
                        if (delta > route.length / 2) delta -= route.length;
                        previous[index] = current;
                        progress[index] += direction * delta;
                        csv.append(tick)
                                .append(',')
                                .append(index)
                                .append(',')
                                .append(progress[index])
                                .append(',')
                                .append(cart.getDeltaMovement().horizontalDistance())
                                .append(',')
                                .append(cart.getX())
                                .append(',')
                                .append(cart.getZ())
                                .append(',')
                                .append(engine.driveVector().x)
                                .append(',')
                                .append(engine.driveVector().z)
                                .append('\n');
                        if (tick > 35 && direction * delta < -0.035) {
                            write(report, csv);
                            h.fail(
                                    "Cart "
                                            + index
                                            + " reversed without command at tick "
                                            + tick
                                            + ", delta="
                                            + delta
                                            + ", pos="
                                            + cart.position());
                            return;
                        }
                        if (tick > 100 && tick % 80 == 0) {
                            if (progress[index] - window[index] < 1) {
                                write(report, csv);
                                h.fail(
                                        "Cart "
                                                + index
                                                + " jammed at tick "
                                                + tick
                                                + ", pos="
                                                + cart.position()
                                                + ", drive="
                                                + engine.driveVector());
                                return;
                            }
                            window[index] = progress[index];
                        }
                        if (index > 0)
                            h.assertTrue(
                                    links.linked(carts.get(index - 1).getUUID(), cart.getUUID()),
                                    "Train uncoupled on curve");
                    }
                    if (tick >= 2000) {
                        write(report, csv);
                        h.assertTrue(
                                progress[0] > route.length * 4,
                                "Locomotive did not complete four laps");
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 900)
    public static void riderCanReverseRepeatedlyOnOrdinaryRails(GameTestHelper h) {
        reversals(h, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 900)
    public static void riderCanReverseRepeatedlyOnUnpoweredRails(GameTestHelper h) {
        reversals(h, true);
    }

    @GameTest(template = "rail_large", timeoutTicks = 120)
    public static void intentionalEngineReversalStillReversesWholeTrain(GameTestHelper h) {
        for (int x = 1; x < 60; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 2, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        Vec3 position = h.absoluteVec(new Vec3(25.5, 2.0625, 8.5));
        var engine = new LocomotiveMinecart(h.getLevel(), position.x, position.y, position.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 4));
        var wagon = new Minecart(h.getLevel(), position.x - 1.55, position.y, position.z);
        h.getLevel().addFreshEntity(engine);
        h.getLevel().addFreshEntity(wagon);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(engine.getUUID(), wagon.getUUID());
        h.runAfterDelay(
                40,
                () -> {
                    double reversalPosition = engine.getX();
                    engine.switchDirection();
                    h.runAfterDelay(
                            50,
                            () -> {
                                h.assertTrue(
                                        engine.isReversed(),
                                        "Engine lost explicit reversal command");
                                h.assertTrue(
                                        engine.getX() < reversalPosition - 5
                                                && engine.getDeltaMovement().x < -0.1,
                                        "Engine did not depart in requested reverse direction");
                                h.assertTrue(
                                        wagon.getDeltaMovement().x < -0.1
                                                && links.linked(engine.getUUID(), wagon.getUUID()),
                                        "Coupling reversal protection prevented an intentional train reversal");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large")
    public static void couplersAttachToCartEndsAcrossRightAngle(GameTestHelper h) {
        for (int x = 4; x <= 8; x++)
            for (int z = 5; z <= 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(
                new BlockPos(6, 2, 8),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        h.setBlock(
                new BlockPos(7, 2, 8),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_WEST));
        h.setBlock(
                new BlockPos(7, 2, 7),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        h.getLevel()
                .setBlock(
                        h.absolutePos(new BlockPos(7, 2, 8)),
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.NORTH_WEST),
                        Block.UPDATE_CLIENTS);
        Vec3 a = h.absoluteVec(new Vec3(6.5, 2.0625, 8.5));
        Vec3 b = h.absoluteVec(new Vec3(7.5, 2.0625, 7.5));
        var first = new Minecart(h.getLevel(), a.x, a.y, a.z);
        var second = new Minecart(h.getLevel(), b.x, b.y, b.z);
        h.getLevel().addFreshEntity(first);
        h.getLevel().addFreshEntity(second);
        MinecartRailPath path = MinecartRailPath.between(first, second);
        h.assertTrue(path != null, "Right-angle fixture has no connecting rail path");
        Vec3 front = MinecartCouplerAnchors.endpoint(first, 1, a, path.firstToward());
        Vec3 rear = MinecartCouplerAnchors.endpoint(second, 1, b, path.secondToward());
        h.assertTrue(
                Math.abs(front.x - a.x - MinecartCouplerAnchors.END_OFFSET) < 0.001
                        && Math.abs(front.z - a.z) < 0.001,
                "First hook attaches diagonally or at cart center instead of front face: "
                        + front.subtract(a));
        h.assertTrue(
                Math.abs(rear.z - b.z - MinecartCouplerAnchors.END_OFFSET) < 0.001
                        && Math.abs(rear.x - b.x) < 0.001,
                "Second hook attaches diagonally or at cart center instead of rear face: "
                        + rear.subtract(b));
        h.succeed();
    }

    private static void reversals(GameTestHelper h, boolean poweredRail) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        for (int x = 1; x <= 40; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 2, 8),
                    poweredRail
                            ? Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                            : Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        Vec3 position = h.absoluteVec(new Vec3(20.5, 2.0625, 8.5));
        Minecart cart = new Minecart(h.getLevel(), position.x, position.y, position.z);
        h.getLevel().addFreshEntity(cart);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(cart, true);
        h.onEachTick(
                () -> {
                    int tick = (int) h.getTick();
                    int phase = tick % 60;
                    int direction = (tick / 60) % 2 == 0 ? 1 : -1;
                    player.setYRot(direction > 0 ? -90 : 90);
                    player.zza = 1;
                    if (phase == 40) {
                        h.assertTrue(
                                cart.getDeltaMovement().x * direction > 0.05,
                                "Cart remains stuck after reversal, tick="
                                        + tick
                                        + ", velocity="
                                        + cart.getDeltaMovement());
                        h.assertTrue(
                                ((MinecartMotionVisuals) cart).spelunkersCharm$braking() == 0,
                                "Brakes remain stuck while rider drives away");
                    }
                    if (tick >= 840) h.succeed();
                });
    }

    private static void write(String name, StringBuilder text) {
        try {
            Files.writeString(Path.of(name), text);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    /** Provides route data for this gameplay test. */
    private static final class Route {
        private final GameTestHelper helper;
        private final List<BlockPos> cells = new ArrayList<>();
        private final List<Vec3> entry = new ArrayList<>(), exit = new ArrayList<>();
        private final List<Double> distance = new ArrayList<>();
        private double length;

        private Route(GameTestHelper helper) {
            this.helper = helper;
            int[][] corners = {
                {1, 5}, {9, 5}, {9, 3}, {12, 3}, {12, 1}, {16, 1}, {16, 22}, {1, 22}, {1, 5}
            };
            for (int edge = 0; edge < corners.length - 1; edge++) {
                int[] a = corners[edge], b = corners[edge + 1];
                int dx = Integer.signum(b[0] - a[0]), dz = Integer.signum(b[1] - a[1]);
                for (int x = a[0], z = a[1]; x != b[0] || z != b[1]; x += dx, z += dz)
                    cells.add(new BlockPos(x, 2, z));
            }
            for (int i = 0; i < cells.size(); i++) {
                Vec3 center = Vec3.atCenterOf(cells.get(i)).add(0, -0.4375, 0);
                Vec3 previous =
                        Vec3.atCenterOf(cells.get(Math.floorMod(i - 1, cells.size())))
                                .add(0, -0.4375, 0);
                Vec3 next = Vec3.atCenterOf(cells.get((i + 1) % cells.size())).add(0, -0.4375, 0);
                entry.add(helper.absoluteVec(center.lerp(previous, 0.5)));
                exit.add(helper.absoluteVec(center.lerp(next, 0.5)));
                distance.add(length);
                length += entry.getLast().distanceTo(exit.getLast());
            }
        }

        private void build() {
            for (int pass = 0; pass < 2; pass++)
                for (int i = 0; i < cells.size(); i++) {
                    BlockPos pos = cells.get(i);
                    Vec3 a = entry.get(i).subtract(Vec3.atCenterOf(helper.absolutePos(pos)));
                    Vec3 b = exit.get(i).subtract(Vec3.atCenterOf(helper.absolutePos(pos)));
                    RailShape shape;
                    if (Math.abs(a.x - b.x) > 0.9) shape = RailShape.EAST_WEST;
                    else if (Math.abs(a.z - b.z) > 0.9) shape = RailShape.NORTH_SOUTH;
                    else if (a.x + b.x > 0)
                        shape = a.z + b.z > 0 ? RailShape.SOUTH_EAST : RailShape.NORTH_EAST;
                    else shape = a.z + b.z > 0 ? RailShape.SOUTH_WEST : RailShape.NORTH_WEST;
                    helper.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
                    var state =
                            shape.isAscending()
                                            || shape == RailShape.EAST_WEST
                                            || shape == RailShape.NORTH_SOUTH
                                    ? Blocks.POWERED_RAIL
                                            .defaultBlockState()
                                            .setValue(PoweredRailBlock.SHAPE, shape)
                                            .setValue(PoweredRailBlock.POWERED, true)
                                    : Blocks.RAIL
                                            .defaultBlockState()
                                            .setValue(RailBlock.SHAPE, shape);
                    helper.getLevel()
                            .setBlock(helper.absolutePos(pos), state, Block.UPDATE_CLIENTS);
                }
        }

        private Vec3 point(double progress) {
            progress = (progress % length + length) % length;
            for (int i = cells.size() - 1; i >= 0; i--)
                if (progress >= distance.get(i)) {
                    double span = entry.get(i).distanceTo(exit.get(i));
                    return entry.get(i).lerp(exit.get(i), (progress - distance.get(i)) / span);
                }
            return entry.getFirst();
        }

        private double progress(Vec3 point) {
            double best = Double.POSITIVE_INFINITY, result = 0;
            for (int i = 0; i < cells.size(); i++) {
                Vec3 segment = exit.get(i).subtract(entry.get(i));
                double fraction =
                        Math.clamp(
                                point.subtract(entry.get(i)).dot(segment) / segment.lengthSqr(),
                                0,
                                1);
                double error = entry.get(i).add(segment.scale(fraction)).distanceToSqr(point);
                if (error < best) {
                    best = error;
                    result = distance.get(i) + segment.length() * fraction;
                }
            }
            return result;
        }
    }
}
