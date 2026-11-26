// Train high fall tests.
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@GameTestHolder("spelunkers_fall_tests")
@PrefixGameTestTemplate(false)
public final class TrainHighFallTests {
    @GameTest(templateNamespace = LiftTestMod.ID, template = "empty")
    public static void clientFinishesFallWhenServerHasAlreadyLanded(GameTestHelper h) {
        var p = h.absoluteVec(new Vec3(8, 20, 8));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        motion.clientVelocitySeen = true;
        motion.clientVelocity = Vec3.ZERO;
        motion.clientVerticalStep = 1;
        Vec3 target = p.add(0, -18, 0);
        for (int tick = 0; tick < 35; tick++) {
            double before = cart.getY(), previous = motion.clientVerticalStep;
            Vec3 next = MinecartInterpolation.fallback(cart, target, 1);
            motion.clientVerticalStep = Math.abs(next.y - before);
            cart.setPos(next);
            if (tick == 0)
                h.assertTrue(
                        motion.clientVerticalStep > .5,
                        "Server landing reduced visible fall to a crawl");
            h.assertTrue(
                    motion.clientVerticalStep <= previous + .081,
                    "Fall catch-up snapped rather than accelerating");
            h.assertTrue(cart.getY() >= target.y - 1e-6, "Fall catch-up overshot landing");
        }
        h.assertTrue(
                cart.position().distanceTo(target) < .01,
                "Client remained suspended after server landing");
        h.succeed();
    }

    @GameTest(
            templateNamespace = LiftTestMod.ID,
            template = "rail_high_fall",
            timeoutTicks = 650,
            attempts = 3,
            requiredSuccesses = 3)
    public static void twentyFourWagonsLeaveEightyBlockCliffAndLand(GameTestHelper h) {
        var fixture = new TrainHighFallFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        fixture.build();
        var carts = fixture.train(24);
        observe(h, carts, "cliff", 560);
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 650)
    public static void stationarySteepChainPullsLastWagonOffRail(GameTestHelper h) {
        var fixture = new TrainHighFallFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        fixture.build();
        var carts = fixture.hanging(22, true);
        observe(h, carts, "hanging", 560);
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_high_fall", timeoutTicks = 650)
    public static void midairEngineBrakeDoesNotSuspendTwentyWagons(GameTestHelper h) {
        var fixture = new TrainHighFallFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        fixture.build();
        var carts = fixture.hanging(20, false);
        MinecartHandbrake.engage(carts.getFirst());
        observe(h, carts, "braked", 560);
    }

    private static void observe(
            GameTestHelper h, ArrayList<AbstractMinecart> carts, String name, int end) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var start = h.absolutePos(BlockPos.ZERO);
        var trace = new StringBuilder("tick,cart,x,y,z,vx,vy,rail,brake,gap\n");
        var links = MinecartCouplings.get(h.getLevel());
        boolean[] flew = new boolean[carts.size()];
        double[] motion = {0};
        int[] still = {0};
        Vec3[] previous = carts.stream().map(AbstractMinecart::position).toArray(Vec3[]::new);
        int[] landedTicks = {0};
        h.onEachTick(
                () -> {
                    if (h.getTick() >= end) return;
                    double moved = 0;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        boolean air = ((MinecartMotionVisuals) cart).spelunkersCharm$airborne();
                        flew[i] |= air;
                        moved += cart.position().distanceTo(previous[i]);
                        previous[i] = cart.position();
                        double gap = i == 0 ? 0 : cart.distanceTo(carts.get(i - 1));
                        if (i > 0) {
                            if (gap <= 1 || gap >= 3)
                                try {
                                    var dump = new StringBuilder("tick=" + h.getTick() + "\n");
                                    for (int k = 0; k < carts.size(); k++) {
                                        var c = carts.get(k);
                                        var s =
                                                ((MinecartMotionVisuals) c)
                                                        .spelunkersCharm$motion();
                                        dump.append(
                                                k
                                                        + " pos="
                                                        + c.position()
                                                        + " v="
                                                        + c.getDeltaMovement()
                                                        + " rail="
                                                        + c.isOnRails()
                                                        + " air="
                                                        + s.airborne
                                                        + " launched="
                                                        + s.flightLaunched
                                                        + " pitch="
                                                        + s.physicalPitch
                                                        + "\n");
                                    }
                                    Files.writeString(
                                            Path.of("high-fall-" + name + "-failure.txt"), dump);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            h.assertTrue(
                                    gap > 1.0 && gap < 3,
                                    "High drop overlapped or stretched drawbar: "
                                            + i
                                            + ", gap="
                                            + gap);
                            var parent = carts.get(i - 1);
                            h.assertTrue(
                                    MinecartFlightPhysics.axis(parent)
                                                    .scale(MinecartHingeGeometry.side(parent, cart))
                                                    .dot(
                                                            cart.position()
                                                                    .subtract(parent.position()))
                                            > -.08,
                                    "High drop crossed a fixed hinge end: " + i);
                        }
                        trace.append(h.getTick())
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX() - start.getX())
                                .append(',')
                                .append(cart.getY() - start.getY())
                                .append(',')
                                .append(cart.getZ() - start.getZ())
                                .append(',')
                                .append(cart.getDeltaMovement().x)
                                .append(',')
                                .append(cart.getDeltaMovement().y)
                                .append(',')
                                .append(cart.isOnRails())
                                .append(',')
                                .append(MinecartHandbrake.active(cart))
                                .append(',')
                                .append(gap)
                                .append('\n');
                        h.assertTrue(cart.isAlive(), "High drop destroyed wagon " + i);
                        if (i > 0)
                            h.assertTrue(
                                    links.linked(carts.get(i - 1).getUUID(), cart.getUUID()),
                                    "High drop broke drawbar " + i);
                    }
                    if (h.getTick() % 20 == 0)
                        try {
                            Files.writeString(Path.of("high-fall-" + name + ".csv"), trace);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    if (h.getTick() > 30
                            && carts.getFirst().getY() > start.getY() + 8
                            && moved < .005) still[0]++;
                    else still[0] = 0;
                    h.assertTrue(
                            still[0] < 50,
                            "Long train froze in midair: "
                                    + name
                                    + ", head="
                                    + carts.getFirst().position()
                                    + ", tail="
                                    + carts.getLast().position());
                    boolean landed = true;
                    for (int i = 0; i < carts.size(); i++)
                        landed &=
                                flew[i]
                                        && MinecartTrainBrakes.onRail(carts.get(i))
                                        && carts.get(i).getY() < start.getY() + 4;
                    landedTicks[0] = landed ? landedTicks[0] + 1 : 0;
                    if (landedTicks[0] >= 30) h.succeed();
                });
        h.runAfterDelay(
                end,
                () -> {
                    for (int i = 0; i < carts.size(); i++)
                        h.assertTrue(
                                flew[i] && carts.get(i).getY() < start.getY() + 4,
                                "Wagon remained suspended: "
                                        + name
                                        + ", cart="
                                        + i
                                        + ", pos="
                                        + carts.get(i).position()
                                        + ", velocity="
                                        + carts.get(i).getDeltaMovement());
                    h.succeed();
                });
    }
}

/** Provides train high fall fixture data for this gameplay test. */
final class TrainHighFallFixture {
    final net.minecraft.server.level.ServerLevel level;
    final BlockPos origin;

    TrainHighFallFixture(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    void build() {
        for (int x = 0; x < 192; x++)
            for (int z = 4; z <= 12; z++)
                level.setBlock(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
        for (int x = 0; x < 192; x++)
            level.setBlock(
                    origin.offset(x, 2, 8),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.RailBlock.SHAPE,
                                    RailShape.EAST_WEST),
                    2);
        for (int x = 0; x <= 48; x++) {
            level.setBlock(origin.offset(x, 81, 8), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            level.setBlock(
                    origin.offset(x, 82, 8),
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                            .setValue(PoweredRailBlock.POWERED, true),
                    2);
        }
    }

    ArrayList<AbstractMinecart> train(int wagons) {
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i <= wagons; i++) {
            var cart = cart(i, 48.7 - i * 1.55, 82.0625);
            cart.setDeltaMovement(.4, 0, 0);
            add(carts, cart);
        }
        return carts;
    }

    ArrayList<AbstractMinecart> hanging(int wagons, boolean railTail) {
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i <= wagons; i++) {
            int distance = wagons - i;
            var cart =
                    cart(
                            i,
                            48.5 + distance * .775,
                            82.0625
                                    - distance * Math.sqrt(1.55 * 1.55 - .775 * .775)
                                    - (railTail ? 0 : 4));
            add(carts, cart);
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(i < wagons || !railTail);
        }
        return carts;
    }

    private AbstractMinecart cart(int index, double x, double y) {
        var p = new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + 8.5);
        if (index > 0) return new Minecart(level, p.x, p.y, p.z);
        var engine = new LocomotiveMinecart(level, p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 16));
        return engine;
    }

    private void add(ArrayList<AbstractMinecart> carts, AbstractMinecart cart) {
        level.addFreshEntity(cart);
        if (!carts.isEmpty()
                && MinecartCouplings.get(level).link(carts.getLast(), cart)
                        != MinecartCouplings.Result.LINKED)
            throw new AssertionError("High fall fixture did not link wagon");
        carts.add(cart);
    }
}
