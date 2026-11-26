// Hinged flight tests.
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

@GameTestHolder("spelunkers_hinged_tests")
@PrefixGameTestTemplate(false)
public final class HingedFlightTests {
    @GameTest(template = "high_fall", timeoutTicks = 650)
    public static void aSteepTwentyTwoCartChainPullsOffTheLedgeAndLands(GameTestHelper h) {
        TrainHighFallTests.stationarySteepChainPullsLastWagonOffRail(h);
    }

    @GameTest(template = "flight", timeoutTicks = 400)
    public static void landingBrakeDoesNotTearOffTheAirborneTail(GameTestHelper h) {
        TrainParabolaTests.longTrainFinishesFallingAfterLeadEngineBrakesOnLanding(h);
    }

    @GameTest(template = "flight", timeoutTicks = 480)
    public static void foldedTrainRespectsSolidCrestAndSettles(GameTestHelper h) {
        TrainStabilityTests.brakedFoldedTrainKeepsStableJointsAndDoesNotOverlap(h);
    }

    @GameTest(template = "flight", timeoutTicks = 310)
    public static void elasticTrainCompletesPoweredRampAndRailLanding(GameTestHelper h) {
        TrainFlightOrderTests.tallRamp(h, 5, false);
    }

    @GameTest(template = "flight", timeoutTicks = 310)
    public static void elasticPushedTrainCompletesPoweredRampAndRailLanding(GameTestHelper h) {
        TrainFlightOrderTests.tallRamp(h, 8, true);
    }

    private static Minecart cart(GameTestHelper h, Vec3 point, Vec3 velocity, double pitch) {
        Vec3 p = h.absoluteVec(point);
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        cart.setDeltaMovement(velocity);
        h.getLevel().addFreshEntity(cart);
        var s = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        s.flightRotationReady = true;
        s.physicalYaw = 0;
        s.physicalPitch = pitch;
        s.flightForwardSign = velocity.x < 0 ? -1 : 1;
        ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
        return cart;
    }

    @GameTest(template = "medium", timeoutTicks = 40)
    public static void unlinkedCartFollowsGravityAndItsOwnMomentum(GameTestHelper h) {
        var c = cart(h, new Vec3(10, 80, 8), new Vec3(.4, .4, 0), Math.PI / 4);
        Vec3 start = c.position();
        h.runAfterDelay(
                15,
                () -> {
                    int ticks = c.tickCount;
                    double vx = .4,
                            x = 0,
                            vy = .4,
                            y = 0,
                            drag = Math.pow(MinecartFlightPhysics.AIR_DRAG, .1);
                    for (int i = 0; i < ticks * 10; i++) {
                        double next = vx * drag;
                        x += (vx + next) * .05;
                        vx = next;
                        double vertical = vy - .004;
                        y += (vy + vertical) * .05;
                        vy = vertical;
                    }
                    h.assertTrue(
                            Math.abs(c.getX() - start.x - x) < .03
                                    && Math.abs(c.getY() - start.y - y) < .03,
                            "Free cart lost its parabola: "
                                    + c.position()
                                    + ", expected delta="
                                    + x
                                    + ","
                                    + y);
                    h.assertTrue(
                            Math.abs(c.getDeltaMovement().y - vy) < .01,
                            "Gravity was changed by flight guidance");
                    h.succeed();
                });
    }

    @GameTest(template = "medium", timeoutTicks = 30)
    public static void slackLinkedCartsUseTheSameBallisticRuleAsUnlinkedCarts(GameTestHelper h) {
        Vec3 v = new Vec3(.4, .4, 0),
                offset = new Vec3(1.55 / Math.sqrt(2), 1.55 / Math.sqrt(2), 0);
        var a = cart(h, new Vec3(20, 80, 8), v, Math.PI / 4);
        var b = cart(h, new Vec3(20, 80, 8).subtract(offset), v, Math.PI / 4);
        var solo = cart(h, new Vec3(20, 80, 16), v, Math.PI / 4);
        MinecartCouplings.get(h.getLevel()).link(a, b);
        h.runAfterDelay(
                5,
                () -> {
                    h.assertTrue(
                            Math.abs(a.getX() - solo.getX()) < .025
                                    && Math.abs(a.getY() - solo.getY()) < .025,
                            "Coupling selected a different base flight rule");
                    h.assertTrue(
                            Math.abs(a.getDeltaMovement().y - solo.getDeltaMovement().y) < .025,
                            "Linked gravity differs from solo gravity");
                    h.succeed();
                });
    }

    @GameTest(template = "medium", timeoutTicks = 40)
    public static void greaterLaunchSpeedProducesMoreRangeAndHeight(GameTestHelper h) {
        var slow = cart(h, new Vec3(10, 80, 8), new Vec3(.2, .2, 0), Math.PI / 4);
        var fast = cart(h, new Vec3(10, 80, 16), new Vec3(.45, .45, 0), Math.PI / 4);
        h.runAfterDelay(
                20,
                () -> {
                    h.assertTrue(
                            fast.getX() > slow.getX() + 4 && fast.getY() > slow.getY() + 4,
                            "Launch speed did not control the ballistic jump");
                    h.succeed();
                });
    }

    @GameTest(template = "tall", timeoutTicks = 135)
    public static void aLongFallBecomesNearlyVertical(GameTestHelper h) {
        var c = cart(h, new Vec3(10, 290, 8), new Vec3(.3, 0, 0), 0);
        h.runAfterDelay(
                110,
                () -> {
                    double angle =
                            Math.toDegrees(
                                    Math.atan2(
                                            -c.getDeltaMovement().y,
                                            c.getDeltaMovement().horizontalDistance()));
                    h.assertTrue(
                            c.isAlive() && angle > 85,
                            "Long fall did not approach vertical: " + angle);
                    h.assertTrue(
                            Math.abs(
                                            ((MinecartMotionVisuals) c)
                                                    .spelunkersCharm$motion()
                                                    .physicalPitch)
                                    > Math.toRadians(75),
                            "Body did not turn with a long fall");
                    h.succeed();
                });
    }

    @GameTest(template = "high", timeoutTicks = 100)
    public static void externalKnockDoesNotSwapFixedHooksOrOverbendTheTrain(GameTestHelper h) {
        var carts = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 0; i < 8; i++) {
            var c =
                    cart(
                            h,
                            new Vec3(
                                    30 - i * 1.55 / Math.sqrt(2), 150 - i * 1.55 / Math.sqrt(2), 8),
                            new Vec3(.35, .35, 0),
                            Math.PI / 4);
            if (i > 0) links.link(carts.getLast(), c);
            carts.add(c);
        }
        int[] sides = new int[14];
        for (int i = 1; i < 8; i++) {
            sides[(i - 1) * 2] = MinecartHingeGeometry.side(carts.get(i - 1), carts.get(i));
            sides[(i - 1) * 2 + 1] = MinecartHingeGeometry.side(carts.get(i), carts.get(i - 1));
        }
        h.runAfterDelay(10, () -> carts.get(4).push(.12, .05, .16));
        h.runAfterDelay(
                60,
                () -> {
                    for (int i = 1; i < 8; i++) {
                        var a = carts.get(i - 1);
                        var b = carts.get(i);
                        int sa = MinecartHingeGeometry.side(a, b),
                                sb = MinecartHingeGeometry.side(b, a);
                        h.assertTrue(
                                sa == sides[(i - 1) * 2] && sb == sides[(i - 1) * 2 + 1],
                                "A hinge swapped its attachment end");
                        Vec3 aa = MinecartFlightPhysics.axis(a), bb = MinecartFlightPhysics.axis(b);
                        double bend =
                                Math.toDegrees(
                                        Math.acos(
                                                Math.clamp(
                                                        aa.scale(sa).dot(bb.scale(-sb)), -1, 1)));
                        h.assertTrue(bend < 65, "Hinge overbent after external force: " + bend);
                        h.assertTrue(
                                a.distanceTo(b) < 2.5, "Elastic hook stretched without a limit");
                    }
                    h.succeed();
                });
    }

    private static void hanging(GameTestHelper h, boolean reversed, boolean mirror) {
        for (int x = mirror ? 12 : 20; x <= (mirror ? 20 : 28); x++) {
            h.setBlock(new BlockPos(x, 19, 8), Blocks.STONE);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(x, 20, 8)),
                            Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                            2);
        }
        Vec3 p = h.absoluteVec(new Vec3(20.5, 20.0625, 8.5));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(mirror ? Direction.WEST : Direction.EAST);
        h.getLevel().addFreshEntity(engine);
        MinecartHandbrake.engage(engine);
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(engine);
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 1; i <= 5; i++) {
            var c =
                    cart(
                            h,
                            new Vec3(20.5 + (mirror ? 1 : -1) * i * 1.55, 20.0625, 8.5),
                            Vec3.ZERO,
                            0);
            h.assertTrue(
                    links.link(carts.getLast(), c) == MinecartCouplings.Result.LINKED,
                    "Hanging fixture did not connect");
            carts.add(c);
        }
        if (reversed) {
            for (int i = 1; i < carts.size(); i++) {
                var c = carts.get(i);
                c.setPos(p.x + (mirror ? 1 : -1) * i * .1, p.y - i * 1.55, p.z);
                var s = ((MinecartMotionVisuals) c).spelunkersCharm$motion();
                s.physicalPitch = mirror ? -Math.PI / 2 : Math.PI / 2;
            }
        }
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        h.runAfterDelay(
                220,
                () -> {
                    var trace = new StringBuilder();
                    for (var c : carts)
                        trace.append(c.position())
                                .append(" pitch=")
                                .append(
                                        Math.toDegrees(
                                                ((MinecartMotionVisuals) c)
                                                        .spelunkersCharm$motion()
                                                        .physicalPitch))
                                .append('\n');
                    try {
                        Files.writeString(
                                Path.of("hinge-hanging-" + reversed + "-" + mirror + ".txt"),
                                trace);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    h.assertTrue(
                            engine.position().distanceTo(p) < .03,
                            "Airborne weight moved a braked anchor");
                    for (int i = 2; i < carts.size(); i++) {
                        var a = carts.get(i - 1);
                        var b = carts.get(i);
                        h.assertTrue(
                                a.getY() > b.getY() + 1,
                                "Hanging tail did not fall below its predecessor");
                        h.assertTrue(
                                Math.abs(a.getX() - b.getX()) < .55,
                                "Hanging tail did not straighten vertically");
                        h.assertTrue(
                                Math.abs(
                                                ((MinecartMotionVisuals) b)
                                                        .spelunkersCharm$motion()
                                                        .physicalPitch)
                                        > Math.toRadians(75),
                                "Hanging body did not become vertical");
                    }
                    h.succeed();
                });
    }

    @GameTest(template = "small", timeoutTicks = 250)
    public static void brakedFrontLetsTheRearHangVertically(GameTestHelper h) {
        hanging(h, false, false);
    }

    @GameTest(template = "small", timeoutTicks = 250)
    public static void anInitiallyVerticalTailStaysAttachedWithoutTwisting(GameTestHelper h) {
        hanging(h, true, false);
    }

    @GameTest(template = "small", timeoutTicks = 250)
    public static void brakedRearLetsTheFrontHangOnTheOppositeSide(GameTestHelper h) {
        hanging(h, false, true);
    }

    @GameTest(template = "small", timeoutTicks = 100)
    public static void deepWaterLandingProtectsCartAndPassenger(GameTestHelper h) {
        for (int x = 4; x <= 24; x++)
            for (int z = 4; z <= 12; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            }
        var c = cart(h, new Vec3(10, 35, 8), new Vec3(.02, 0, 0), 0);
        var pig = net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
        pig.setNoAi(true);
        pig.setPos(c.position());
        h.getLevel().addFreshEntity(pig);
        pig.startRiding(c, true);
        float health = pig.getHealth();
        h.runAfterDelay(
                70,
                () -> {
                    h.assertTrue(
                            c.isAlive() && pig.isAlive() && pig.getHealth() == health,
                            "Water landing applied dry ground crash damage");
                    h.assertTrue(c.getY() < 10, "Water fixture did not reach the landing");
                    h.succeed();
                });
    }
}
