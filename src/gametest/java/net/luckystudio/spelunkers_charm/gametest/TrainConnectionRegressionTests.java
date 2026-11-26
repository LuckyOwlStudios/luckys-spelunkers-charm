// Train connection regression tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder("spelunkers_train_regression_tests")
@PrefixGameTestTemplate(false)
public final class TrainConnectionRegressionTests {
    @GameTest(template = "matrix", timeoutTicks = 80)
    public static void curveCoupling0(GameTestHelper h) {
        curve(h, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 80)
    public static void curveCoupling1(GameTestHelper h) {
        curve(h, 1);
    }

    @GameTest(template = "matrix", timeoutTicks = 80)
    public static void curveCoupling2(GameTestHelper h) {
        curve(h, 2);
    }

    @GameTest(template = "matrix", timeoutTicks = 80)
    public static void curveCoupling3(GameTestHelper h) {
        curve(h, 3);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight0Forward(GameTestHelper h) {
        jump(h, 0, false);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight0Reverse(GameTestHelper h) {
        jump(h, 0, true);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight8Forward(GameTestHelper h) {
        jump(h, 8, false);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight8Reverse(GameTestHelper h) {
        jump(h, 8, true);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight24Forward(GameTestHelper h) {
        jump(h, 24, false);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight24Reverse(GameTestHelper h) {
        jump(h, 24, true);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight80Forward(GameTestHelper h) {
        jump(h, 80, false);
    }

    @GameTest(template = "high", timeoutTicks = 480)
    public static void jumpHeight80Reverse(GameTestHelper h) {
        jump(h, 80, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 320)
    public static void downhill0Plain(GameTestHelper h) {
        downhill(h, 0, false);
    }

    @GameTest(template = "matrix", timeoutTicks = 320)
    public static void downhill0Powered(GameTestHelper h) {
        downhill(h, 0, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 320)
    public static void downhill2Plain(GameTestHelper h) {
        downhill(h, 2, false);
    }

    @GameTest(template = "matrix", timeoutTicks = 320)
    public static void downhill2Powered(GameTestHelper h) {
        downhill(h, 2, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 820)
    public static void reverseThroughHairpinsTwelveWagons(GameTestHelper h) {
        reverseLoop(h, 12);
    }

    @GameTest(template = "matrix", timeoutTicks = 820)
    public static void reverseThroughHairpinsTwentyWagons(GameTestHelper h) {
        reverseLoop(h, 20);
    }

    static void reverseLoop(GameTestHelper h, int wagons) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var scene = TrainRegressionFixture.loop(h.getLevel(), h.absolutePos(BlockPos.ZERO), wagons);
        var previous =
                scene.carts().stream()
                        .mapToDouble(c -> scene.route().progress(c.position()))
                        .toArray();
        double[] reverseDistance = new double[previous.length];
        boolean[] done = {false};
        watch(
                h,
                () -> {
                    if (done[0]) return;
                    long tick = h.getTick();
                    if (tick == 120) scene.engine().toggleEmergencyBrake();
                    if (tick == 160) scene.engine().selectDirection(true);
                    TrainRegressionFixture.intact(h.getLevel(), scene.carts());
                    TrainRegressionFixture.ends(h.getLevel(), scene.carts());
                    for (int i = 0; i < scene.carts().size(); i++) {
                        var c = scene.carts().get(i);
                        double progress = scene.route().progress(c.position()),
                                step = scene.route().delta(previous[i], progress);
                        previous[i] = progress;
                        if (tick > 200) reverseDistance[i] -= step;
                        if (tick > 40 && i > 0)
                            h.assertTrue(
                                    c.distanceTo(scene.carts().get(i - 1)) > .97,
                                    "Server reverse spacing collapsed: " + i);
                        Vec3 expected =
                                scene.route()
                                        .point(progress + .03)
                                        .subtract(scene.route().point(progress - .03))
                                        .normalize();
                        h.assertTrue(
                                MinecartHingeGeometry.supportedAxis(c).dot(expected) > .1,
                                "Hairpin reversed a physical front: cart=" + i + ", tick=" + tick);
                    }
                    if (tick > 200)
                        h.assertTrue(
                                scene.engine().isReversed(),
                                "Hairpin changed selected reverse mode");
                    if (tick >= 720) {
                        for (double distance : reverseDistance)
                            h.assertTrue(
                                    distance > 90,
                                    "Reverse train stalled in hairpins: " + distance);
                        done[0] = true;
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void occupiedHookCannotBeUsedForASecondLink(GameTestHelper h) {
        var level = h.getLevel();
        var p = h.absoluteVec(new Vec3(8, 4, 8.5));
        var a = new net.minecraft.world.entity.vehicle.Minecart(level, p.x, p.y, p.z);
        var b = new net.minecraft.world.entity.vehicle.Minecart(level, p.x + 1.55, p.y, p.z);
        var c = new net.minecraft.world.entity.vehicle.Minecart(level, p.x + 1.8, p.y, p.z);
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        level.addFreshEntity(c);
        var links = MinecartCouplings.get(level);
        links.link(a, b);
        h.assertTrue(
                links.link(a, c) == MinecartCouplings.Result.END_OCCUPIED,
                "Two wagons used the same physical hook");
        h.succeed();
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void briefExtensionDoesNotSnapAHealthyCoupling(GameTestHelper h) {
        var level = h.getLevel();
        var p = h.absoluteVec(new Vec3(8, 4, 8.5));
        var a = new net.minecraft.world.entity.vehicle.Minecart(level, p.x, p.y, p.z);
        var b = new net.minecraft.world.entity.vehicle.Minecart(level, p.x + 1.55, p.y, p.z);
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        var links = MinecartCouplings.get(level);
        links.link(a, b, net.minecraft.world.item.Items.CHAIN);
        b.setPos(p.add(3, 0, 0));
        MinecartCouplingSafety.tick(level, java.util.List.of(a, b), links);
        h.assertTrue(
                links.linked(a.getUUID(), b.getUUID()),
                "One transient sample snapped a healthy coupling");
        b.setPos(p.add(1.55, 0, 0));
        MinecartCouplingSafety.tick(level, java.util.List.of(a, b), links);
        h.succeed();
    }

    @GameTest(template = "matrix", timeoutTicks = 55)
    public static void blockedPersistentTensionBreaksAndRefundsOneChain(GameTestHelper h) {
        var level = h.getLevel();
        var p = h.absoluteVec(new Vec3(8, 4, 8.5));
        var a = new net.minecraft.world.entity.vehicle.Minecart(level, p.x, p.y, p.z);
        var b = new net.minecraft.world.entity.vehicle.Minecart(level, p.x + 3.6, p.y, p.z);
        for (int x = 9; x <= 10; x++)
            for (int y = 3; y <= 6; y++)
                h.setBlock(new BlockPos(x, y, 8), net.minecraft.world.level.block.Blocks.STONE);
        a.setNoGravity(true);
        b.setNoGravity(true);
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        var links = MinecartCouplings.get(level);
        links.link(a, b, net.minecraft.world.item.Items.CHAIN);
        h.runAfterDelay(
                4,
                () ->
                        h.assertTrue(
                                links.linked(a.getUUID(), b.getUUID()),
                                "Brief tension broke too early"));
        h.runAfterDelay(
                22,
                () -> {
                    h.assertFalse(
                            links.linked(a.getUUID(), b.getUUID()),
                            "Persistent blocked tension never broke the chain: a="
                                    + a.position()
                                    + ", b="
                                    + b.position()
                                    + ", span="
                                    + MinecartCouplingSafety.length(a, b)
                                    + ", stress="
                                    + ((MinecartMotionVisuals) a)
                                            .spelunkersCharm$motion()
                                            .hookStressSince);
                    int count =
                            level
                                    .getEntitiesOfClass(
                                            net.minecraft.world.entity.item.ItemEntity.class,
                                            a.getBoundingBox().inflate(8))
                                    .stream()
                                    .filter(
                                            e ->
                                                    e.getItem()
                                                            .is(
                                                                    net.minecraft.world.item.Items
                                                                            .CHAIN))
                                    .mapToInt(e -> e.getItem().getCount())
                                    .sum();
                    h.assertTrue(count == 1, "Tension did not return exactly one chain");
                    h.succeed();
                });
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void fixedHooksRepairAFlippedMiddleCartAndItsSavedPose(GameTestHelper h) {
        for (int x = 6; x < 15; x++) {
            h.setBlock(new BlockPos(x, 3, 8), net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 4, 8),
                    net.minecraft.world.level.block.Blocks.RAIL
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.RailBlock.SHAPE,
                                    net.minecraft.world.level.block.state.properties.RailShape
                                            .EAST_WEST));
        }
        var level = h.getLevel();
        var carts = new java.util.ArrayList<net.minecraft.world.entity.vehicle.AbstractMinecart>();
        for (int i = 0; i < 3; i++) {
            var p = h.absoluteVec(new Vec3(8 + i * 1.55, 4.0625, 8.5));
            var c = new net.minecraft.world.entity.vehicle.Minecart(level, p.x, p.y, p.z);
            TrainRegressionFixture.facing(c, new Vec3(1, 0, 0));
            level.addFreshEntity(c);
            ((MinecartMotionVisuals) c).spelunkersCharm$setOnRails(true);
            carts.add(c);
        }
        var links = MinecartCouplings.get(level);
        links.link(carts.get(0), carts.get(1));
        links.link(carts.get(1), carts.get(2));
        var middle = carts.get(1);
        var visual = (MinecartMotionVisuals) middle;
        visual.spelunkersCharm$motion().visualYaw =
                visual.spelunkersCharm$motion().visualYawOld = 180;
        visual.spelunkersCharm$setBodyYaw(180);
        var tag = new net.minecraft.nbt.CompoundTag();
        middle.saveWithoutId(tag);
        var loaded = new net.minecraft.world.entity.vehicle.Minecart(level, 0, 0, 0);
        loaded.load(tag);
        h.assertTrue(
                MinecartHingeGeometry.supportedAxis(middle).x > .99,
                "Fixed hooks did not repair a live 180-degree flip");
        h.assertTrue(
                MinecartHingeGeometry.supportedAxis(loaded).x > .99,
                "Saved flipped pose ignored its fixed hooks");
        TrainRegressionFixture.intact(level, carts);
        h.succeed();
    }

    static void watch(GameTestHelper h, Runnable check) {
        h.onEachTick(
                () -> {
                    try {
                        check.run();
                    } catch (AssertionError failure) {
                        h.fail(failure.getMessage());
                    }
                });
    }

    static void curve(GameTestHelper h, int rotation) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var carts =
                TrainRegressionFixture.curve(
                        h.getLevel(), h.absolutePos(BlockPos.ZERO), rotation, player);
        TrainRegressionFixture.intact(h.getLevel(), carts);
        TrainRegressionFixture.ends(h.getLevel(), carts);
        h.assertTrue(
                player.getInventory().getItem(10).getCount() == 6,
                "Curve linking charged the wrong chain count");
        boolean[] done = {false};
        watch(
                h,
                () -> {
                    if (done[0]) return;
                    TrainRegressionFixture.intact(h.getLevel(), carts);
                    TrainRegressionFixture.ends(h.getLevel(), carts);
                });
        h.runAfterDelay(
                40,
                () -> {
                    done[0] = true;
                    h.succeed();
                });
    }

    static void jump(GameTestHelper h, int height, boolean reverse) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var carts =
                TrainRegressionFixture.jump(
                        h.getLevel(), h.absolutePos(BlockPos.ZERO), height, reverse, 8);
        boolean[] flew = new boolean[carts.size()];
        int[] landed = {0};
        Vec3[] previous = carts.stream().map(c -> c.position()).toArray(Vec3[]::new);
        Vec3[] velocities = carts.stream().map(c -> c.getDeltaMovement()).toArray(Vec3[]::new);
        watch(
                h,
                () -> {
                    TrainRegressionFixture.intact(h.getLevel(), carts);
                    TrainRegressionFixture.ends(h.getLevel(), carts);
                    boolean all = true;
                    for (int i = 0; i < carts.size(); i++) {
                        var c = carts.get(i);
                        var s = ((MinecartMotionVisuals) c).spelunkersCharm$motion();
                        flew[i] |= s.airborne;
                        double expected = reverse && i == carts.size() - 1 ? -1 : 1;
                        Vec3 axis =
                                s.airborne
                                        ? MinecartFlightPhysics.axis(c)
                                        : MinecartHingeGeometry.supportedAxis(c);
                        h.assertTrue(
                                axis.x * expected >= -.01,
                                "Cart flipped 180 during jump/capture: "
                                        + i
                                        + ", axis="
                                        + axis
                                        + ", height="
                                        + height);
                        double limit =
                                Math.max(
                                        1.65,
                                        Math.max(
                                                        velocities[i].length(),
                                                        c.getDeltaMovement().length())
                                                + .8);
                        h.assertTrue(
                                c.position().distanceTo(previous[i]) < limit,
                                "Landing position jumped: " + i + ", height=" + height);
                        previous[i] = c.position();
                        velocities[i] = c.getDeltaMovement();
                        all &=
                                flew[i]
                                        && MinecartTrainBrakes.onRail(c)
                                        && c.getY() < h.absolutePos(BlockPos.ZERO).getY() + 3;
                    }
                    landed[0] = all ? landed[0] + 1 : 0;
                    if (landed[0] >= 20) h.succeed();
                });
    }

    static void downhill(GameTestHelper h, int rotation, boolean powered) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var origin = h.absolutePos(new BlockPos(rotation == 2 ? 76 : 0, 0, rotation == 2 ? 64 : 0));
        var carts = TrainRegressionFixture.downhill(h.getLevel(), origin, rotation, powered, 8);
        Vec3 front =
                TrainRegressionFixture.rotate(new Vec3(17, 0, 16), rotation)
                        .subtract(TrainRegressionFixture.rotate(new Vec3(16, 0, 16), rotation));
        Vec3 start = carts.getFirst().position();
        boolean[] done = {false};
        watch(
                h,
                () -> {
                    if (done[0]) return;
                    TrainRegressionFixture.intact(h.getLevel(), carts);
                    TrainRegressionFixture.ends(h.getLevel(), carts);
                    h.assertTrue(
                            ((LocomotiveMinecart) carts.getFirst()).isReversed(),
                            "Reverse drive mode changed by itself");
                    for (var c : carts) {
                        h.assertTrue(
                                MinecartHingeGeometry.supportedAxis(c).dot(front) > .4,
                                "Downhill reverse changed physical front");
                        if (h.getTick() > 20)
                            h.assertTrue(
                                    c.getDeltaMovement().dot(front) < .03,
                                    "Train changed downhill travel direction at tick="
                                            + h.getTick()
                                            + ", pos="
                                            + c.position()
                                            + ", velocity="
                                            + c.getDeltaMovement());
                    }
                    if (carts.getFirst().position().subtract(start).dot(front) < -34
                            && carts.getFirst().getY() < origin.getY() + 3) {
                        done[0] = true;
                        h.succeed();
                    }
                });
    }
}
