// Train push tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
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
public final class TrainPushTests {
    @GameTest(template = "rail_large", timeoutTicks = 1900)
    public static void twentyPushedWagonsKeepSpeedAcrossHairpins(GameTestHelper h) {
        loop(h, 20, false, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 1900)
    public static void twentyPushedWagonsKeepSpeedInReverse(GameTestHelper h) {
        loop(h, 20, true, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 1900)
    public static void thirtyTwoPushedWagonsKeepSpeedAcrossHairpins(GameTestHelper h) {
        loop(h, 32, false, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 1900)
    public static void furnacePushesTwentyWagonsAcrossHairpins(GameTestHelper h) {
        loop(h, 20, false, true);
    }

    private static void loop(GameTestHelper h, int wagons, boolean reverse, boolean furnace) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        Route route = new Route(h);
        route.build();
        int direction = reverse ? -1 : 1;
        Vec3 p = route.point(10);
        AbstractMinecart engine;
        if (furnace) {
            var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
            cart.fuel = 20000;
            cart.xPush = direction;
            engine = cart;
        } else {
            var cart = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
            cart.setDriveDirection(reverse ? Direction.EAST : Direction.WEST);
            cart.switchDirection();
            for (int slot = 0; slot < 3; slot++) cart.setItem(slot, new ItemStack(Items.COAL, 16));
            engine = cart;
        }
        h.getLevel().addFreshEntity(engine);
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(engine);
        var links = MinecartCouplings.get(h.getLevel());
        for (int index = 1; index <= wagons; index++) {
            Vec3 point = route.point(10 + direction * index * 1.55);
            var cart = new Minecart(h.getLevel(), point.x, point.y, point.z);
            h.getLevel().addFreshEntity(cart);
            links.link(carts.getLast(), cart);
            carts.add(cart);
        }
        double[] previous =
                carts.stream().mapToDouble(cart -> route.progress(cart.position())).toArray();
        double[] total = new double[carts.size()], window = new double[carts.size()];
        String name = "push-" + wagons + "-" + reverse + "-" + furnace + ".csv";
        StringBuilder report = new StringBuilder("tick,cart,progress,speed,x,z\n");
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    for (int index = 0; index < carts.size(); index++) {
                        var cart = carts.get(index);
                        double now = route.progress(cart.position()), delta = now - previous[index];
                        if (delta < -route.length / 2) delta += route.length;
                        if (delta > route.length / 2) delta -= route.length;
                        previous[index] = now;
                        total[index] += direction * delta;
                        report.append(tick)
                                .append(',')
                                .append(index)
                                .append(',')
                                .append(total[index])
                                .append(',')
                                .append(cart.getDeltaMovement().horizontalDistance())
                                .append(',')
                                .append(cart.getX())
                                .append(',')
                                .append(cart.getZ())
                                .append('\n');
                        if (tick > 70 && direction * delta < -.04) {
                            write(name, report);
                            h.fail("Train reversed at fold: cart=" + index + ", tick=" + tick);
                            return;
                        }
                        if (tick > 100 && tick % 160 == 0) {
                            if (total[index] - window[index] < 4) {
                                write(name, report);
                                h.fail(
                                        "Long train jammed at fold: cart="
                                                + index
                                                + ", tick="
                                                + tick
                                                + ", position="
                                                + cart.position());
                                return;
                            }
                            window[index] = total[index];
                        }
                        h.assertTrue(
                                ((MinecartMotionVisuals) cart).spelunkersCharm$braking() == 0,
                                "Fold generated false brake effects");
                        if (index > 0)
                            h.assertTrue(
                                    links.linked(carts.get(index - 1).getUUID(), cart.getUUID()),
                                    "Long train broke a coupling on fold");
                    }
                    if (tick >= 1800) {
                        write(name, report);
                        for (double progress : total)
                            h.assertTrue(
                                    progress > (furnace ? 600 : 750),
                                    "Pushed train lost speed across hairpins: " + progress);
                        h.succeed();
                    }
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
    static final class Route {
        private final GameTestHelper h;
        private final List<BlockPos> cells = new ArrayList<>();
        private final List<Vec3> entry = new ArrayList<>(), exit = new ArrayList<>();
        private final List<Double> offset = new ArrayList<>();
        private double length;

        Route(GameTestHelper h) {
            this.h = h;
            int[][] corners = {
                {2, 12}, {18, 12}, {18, 3}, {21, 3}, {21, 12}, {22, 12}, {22, 3}, {55, 3}, {55, 27},
                {2, 27}, {2, 12}
            };
            for (int edge = 0; edge < corners.length - 1; edge++) {
                int[] a = corners[edge], b = corners[edge + 1];
                int dx = Integer.signum(b[0] - a[0]), dz = Integer.signum(b[1] - a[1]);
                for (int x = a[0], z = a[1]; x != b[0] || z != b[1]; x += dx, z += dz)
                    cells.add(new BlockPos(x, 2, z));
            }
            for (int i = 0; i < cells.size(); i++) {
                Vec3 center = Vec3.atCenterOf(cells.get(i)).add(0, -.4375, 0);
                Vec3 previous =
                        Vec3.atCenterOf(cells.get(Math.floorMod(i - 1, cells.size())))
                                .add(0, -.4375, 0);
                Vec3 next = Vec3.atCenterOf(cells.get((i + 1) % cells.size())).add(0, -.4375, 0);
                entry.add(h.absoluteVec(center.lerp(previous, .5)));
                exit.add(h.absoluteVec(center.lerp(next, .5)));
                offset.add(length);
                length += entry.getLast().distanceTo(exit.getLast());
            }
        }

        void build() {
            for (int pass = 0; pass < 2; pass++)
                for (int i = 0; i < cells.size(); i++) {
                    BlockPos pos = cells.get(i);
                    Vec3 a = entry.get(i).subtract(Vec3.atCenterOf(h.absolutePos(pos))),
                            b = exit.get(i).subtract(Vec3.atCenterOf(h.absolutePos(pos)));
                    RailShape shape;
                    if (Math.abs(a.x - b.x) > .9) shape = RailShape.EAST_WEST;
                    else if (Math.abs(a.z - b.z) > .9) shape = RailShape.NORTH_SOUTH;
                    else if (a.x + b.x > 0)
                        shape = a.z + b.z > 0 ? RailShape.SOUTH_EAST : RailShape.NORTH_EAST;
                    else shape = a.z + b.z > 0 ? RailShape.SOUTH_WEST : RailShape.NORTH_WEST;
                    h.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
                    var state =
                            shape == RailShape.EAST_WEST || shape == RailShape.NORTH_SOUTH
                                    ? Blocks.POWERED_RAIL
                                            .defaultBlockState()
                                            .setValue(PoweredRailBlock.SHAPE, shape)
                                            .setValue(PoweredRailBlock.POWERED, true)
                                    : Blocks.RAIL
                                            .defaultBlockState()
                                            .setValue(RailBlock.SHAPE, shape);
                    h.getLevel().setBlock(h.absolutePos(pos), state, Block.UPDATE_CLIENTS);
                }
        }

        Vec3 point(double value) {
            value = (value % length + length) % length;
            for (int i = cells.size() - 1; i >= 0; i--)
                if (value >= offset.get(i))
                    return entry.get(i)
                            .lerp(
                                    exit.get(i),
                                    (value - offset.get(i)) / entry.get(i).distanceTo(exit.get(i)));
            return entry.getFirst();
        }

        private double progress(Vec3 point) {
            double error = Double.POSITIVE_INFINITY, result = 0;
            for (int i = 0; i < cells.size(); i++) {
                Vec3 segment = exit.get(i).subtract(entry.get(i));
                double t =
                        Math.clamp(
                                point.subtract(entry.get(i)).dot(segment) / segment.lengthSqr(),
                                0,
                                1);
                double distance = entry.get(i).add(segment.scale(t)).distanceToSqr(point);
                if (distance < error) {
                    error = distance;
                    result = offset.get(i) + segment.length() * t;
                }
            }
            return result;
        }
    }
}
