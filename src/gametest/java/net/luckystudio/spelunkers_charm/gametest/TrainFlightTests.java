// Train flight tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainFlightTests {
    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void offRailCartPreservesMomentumWithSmoothFriction(GameTestHelper h) {
        for (int x = 1; x <= 60; x++) h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
        Vec3 p = h.absoluteVec(new Vec3(8.5, 2, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        cart.setOnGround(true);
        cart.setDeltaMovement(.45, 0, 0);
        h.getLevel().addFreshEntity(cart);
        var previous = new Vec3[] {cart.position()};
        double[] last = {.45};
        h.onEachTick(
                () -> {
                    if (h.getTick() == 0 || h.getTick() >= 20) return;
                    double step = cart.position().distanceTo(previous[0]);
                    h.assertTrue(
                            step > .14 && step < .5, "Off-rail motion stalled or jumped: " + step);
                    h.assertTrue(
                            step / last[0] > .90 && step / last[0] < 1.04,
                            "Off-rail drag changed speed abruptly: " + step + " after " + last[0]);
                    previous[0] = cart.position();
                    last[0] = step;
                });
        h.runAfterDelay(
                20,
                () -> {
                    h.assertTrue(cart.getX() > p.x + 5, "Cart lost carried momentum off rails");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void risingRampLaunchesCartWithBallisticApexAndRailLanding(GameTestHelper h) {
        var fixture =
                new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 10, 16, 52);
        fixture.build();
        var cart = fixture.engine(10.75, .45);
        double[] apex = {cart.getY()};
        boolean[] up = {false}, down = {false}, landed = {false};
        int[] air = {0};
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 70) return;
                    apex[0] = Math.max(apex[0], cart.getY());
                    if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) {
                        air[0]++;
                        up[0] |= cart.getDeltaMovement().y > .1;
                        down[0] |= cart.getDeltaMovement().y < -.1;
                    }
                    if (air[0] > 4 && cart.isOnRails() && cart.getX() > fixture.origin.getX() + 16)
                        landed[0] = true;
                });
        h.runAfterDelay(
                70,
                () -> {
                    h.assertTrue(
                            air[0] >= 12 && up[0] && down[0],
                            "Ramp failed to preserve vertical momentum");
                    h.assertTrue(
                            apex[0] > fixture.origin.getY() + 4,
                            "Ramp did not create a visible jump: " + apex[0]);
                    h.assertTrue(landed[0] && cart.isAlive(), "Jump did not land back on rails");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void fasterCartJumpsHigherThanSlowCart(GameTestHelper h) {
        var slow = new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 10, 16, 52);
        slow.build();
        var fast =
                new TrainFlightFixture(
                        h.getLevel(), h.absolutePos(new BlockPos(0, 0, 12)), 10, 16, 52);
        fast.build();
        var a = slow.engine(10.80, .16);
        var b = fast.engine(10.80, .45);
        double[] highest = {a.getY(), b.getY()};
        h.onEachTick(
                () -> {
                    if (h.getTick() < 50) {
                        highest[0] = Math.max(highest[0], a.getY());
                        highest[1] = Math.max(highest[1], b.getY());
                    }
                });
        h.runAfterDelay(
                50,
                () -> {
                    h.assertTrue(
                            highest[1] > highest[0] + .65,
                            "Jump height does not follow speed: slow="
                                    + highest[0]
                                    + ", fast="
                                    + highest[1]);
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 250)
    public static void linkedTrainLaunchesInSequenceAndKeepsHitchesThroughLanding(
            GameTestHelper h) {
        var fixture =
                new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 36, 42, 58);
        fixture.build();
        var carts = fixture.train(20.5, 8);
        var links = MinecartCouplings.get(h.getLevel());
        int[] launch = new int[carts.size()];
        Arrays.fill(launch, -1);
        boolean[] landed = new boolean[carts.size()];
        double[] peaks = new double[carts.size()];
        double[] maxGap = {0};
        int[] wave = {0};
        var trace = new StringBuilder("tick,cart,x,y,z,vy,air,pitch\n");
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    if (tick >= 200) return;
                    int air = 0;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        boolean flying = ((MinecartMotionVisuals) cart).spelunkersCharm$airborne();
                        h.assertTrue(cart.isAlive(), "Flight destroyed a wagon");
                        trace.append(tick)
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX())
                                .append(',')
                                .append(cart.getY())
                                .append(',')
                                .append(cart.getZ())
                                .append(',')
                                .append(cart.getDeltaMovement().y)
                                .append(',')
                                .append(flying)
                                .append(',')
                                .append(cart.getXRot())
                                .append('\n');
                        peaks[i] = Math.max(peaks[i], cart.getY() - fixture.origin.getY());
                        if (flying) {
                            air++;
                            if (launch[i] < 0) launch[i] = (int) tick;
                        }
                        if (launch[i] >= 0
                                && tick > launch[i] + 4
                                && cart.isOnRails()
                                && cart.getX() > fixture.origin.getX() + 42) landed[i] = true;
                        if (i > 0) {
                            h.assertTrue(
                                    links.linked(carts.get(i - 1).getUUID(), cart.getUUID()),
                                    "Flight broke coupling: tick=" + tick + ", wagon=" + i);
                            double gap = cart.position().distanceTo(carts.get(i - 1).position());
                            maxGap[0] = Math.max(maxGap[0], gap);
                            h.assertTrue(gap < 3, "Flying drawbar stretched too far: " + gap);
                        }
                    }
                    if (air > 0 && air < carts.size()) wave[0]++;
                });
        h.runAfterDelay(
                200,
                () -> {
                    try {
                        Files.writeString(Path.of("train-flight-trace.csv"), trace);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    for (int i = 0; i < carts.size(); i++) {
                        h.assertTrue(
                                launch[i] >= 0 && landed[i],
                                "Wagon did not complete launch and rail landing: "
                                        + i
                                        + ", launch="
                                        + launch[i]
                                        + ", landed="
                                        + landed[i]);
                        h.assertTrue(
                                peaks[i] > 3.075,
                                "Wagon did not leave ramp: " + i + ", height=" + peaks[i]);
                        if (i > 0)
                            h.assertTrue(
                                    launch[i] >= launch[i - 1] - 1,
                                    "Train jumped as one rigid block");
                    }
                    h.assertTrue(wave[0] > 10, "Train did not form a staggered flight wave");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void offRailCouplingsSurviveMoreThanTwentyTicks(GameTestHelper h) {
        for (int x = 1; x <= 60; x++) h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
        Vec3 p = h.absoluteVec(new Vec3(8.5, 2, 8.5));
        var a = new Minecart(h.getLevel(), p.x, p.y, p.z);
        var b = new Minecart(h.getLevel(), p.x - 1.55, p.y, p.z);
        a.setDeltaMovement(.3, 0, 0);
        b.setDeltaMovement(.3, 0, 0);
        h.getLevel().addFreshEntity(a);
        h.getLevel().addFreshEntity(b);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, b);
        h.runAfterDelay(
                60,
                () -> {
                    h.assertTrue(
                            links.linked(a.getUUID(), b.getUUID()),
                            "Off-rail timer still removes valid coupling");
                    h.assertTrue(a.distanceTo(b) < 2.2, "Ground drawbar stretched");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void savingAirborneCartPreservesMomentumAndFlightState(GameTestHelper h) {
        var fixture =
                new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 10, 16, 52);
        fixture.build();
        var original = fixture.engine(10.8, .45);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            ((MinecartMotionVisuals) original).spelunkersCharm$airborne(),
                            "Save test did not reach flight");
                    var saved = original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                    double height = original.getY();
                    original.discard();
                    var restored = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
                    restored.load(saved);
                    h.assertTrue(
                            ((MinecartMotionVisuals) restored).spelunkersCharm$airborne(),
                            "Save lost flight metadata");
                    h.getLevel().addFreshEntity(restored);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        restored.getY() > height + .2
                                                && restored.getDeltaMovement().horizontalDistance()
                                                        > .3,
                                        "Reload discarded airborne momentum");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void airborneEmergencyBrakeStillAllowsGravity(GameTestHelper h) {
        var fixture =
                new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 10, 16, 52);
        fixture.build();
        var cart = fixture.engine(10.8, .45);
        h.runAfterDelay(
                5,
                () -> {
                    double height = cart.getY();
                    h.assertTrue(
                            ((MinecartMotionVisuals) cart).spelunkersCharm$airborne(),
                            "Brake test did not reach air");
                    MinecartHandbrake.engage(cart);
                    h.runAfterDelay(
                            35,
                            () -> {
                                h.assertTrue(
                                        cart.getY() < height - .3 && cart.isAlive(),
                                        "Emergency brake suspended cart in midair: start="
                                                + height
                                                + ", now="
                                                + cart.position()
                                                + ", velocity="
                                                + cart.getDeltaMovement()
                                                + ", ground="
                                                + cart.onGround()
                                                + ", rails="
                                                + cart.isOnRails()
                                                + ", anchored="
                                                + cart.brakeAnchored()
                                                + ", alive="
                                                + cart.isAlive());
                                h.succeed();
                            });
                });
    }
}
