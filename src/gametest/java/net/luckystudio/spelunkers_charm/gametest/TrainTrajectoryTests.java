// Train trajectory tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("spelunkers_trajectory_tests")
@PrefixGameTestTemplate(false)
public final class TrainTrajectoryTests {
    @GameTest(templateNamespace = LiftTestMod.ID, template = "empty")
    public static void airbornePredictionDoesNotPlaceRearCartThroughTheLandedFront(
            GameTestHelper h) {
        Vec3 target = h.absoluteVec(new Vec3(8, 10, 8));
        var rear = new Minecart(h.getLevel(), target.x, target.y, target.z);
        var visual = (MinecartMotionVisuals) rear;
        visual.spelunkersCharm$setAirborne(true);
        var motion = visual.spelunkersCharm$motion();
        motion.clientVelocitySeen = true;
        motion.clientVelocity = new Vec3(0, -1.5, 0);
        motion.clientPacketVelocity = motion.clientVelocity;
        Vec3 front = target.add(0, -1.55, 0),
                predicted = MinecartInterpolation.predicted(rear, target);
        h.assertTrue(
                predicted.y - front.y > 1,
                "Air prediction moved the rear cart through its landed predecessor");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large")
    public static void regularPositionPacketsAdvanceOnEveryClientTick(GameTestHelper h) {
        for (int x = 2; x < 62; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(x, 2, 8)),
                            Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                            2);
        }
        Vec3 start = h.absoluteVec(new Vec3(8.5, 2.0625, 8.5));
        var cart = new Minecart(h.getLevel(), start.x, start.y, start.z);
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        state.clientVelocitySeen = true;
        state.clientVelocity = new Vec3(.45, 0, 0);
        state.clientPacketVelocity = state.clientVelocity;
        Vec3 target = start;
        int remaining = 0;
        for (int tick = 0; tick < 80; tick++) {
            double server = start.x + tick * .45;
            if (tick % 2 == 0) {
                target = MinecartInterpolation.predicted(cart, new Vec3(server, start.y, start.z));
                remaining = 3;
            }
            double before = cart.getX();
            Vec3 next = MinecartInterpolation.next(cart, target, remaining);
            h.assertTrue(next != null, "Regular interpolation lost a straight rail");
            cart.setPos(next);
            remaining--;
            if (tick > 35) {
                h.assertTrue(
                        Math.abs(cart.getX() - before - .45) < .005,
                        "Packet interpolation created a pause or speed pulse: "
                                + (cart.getX() - before));
                h.assertTrue(
                        Math.abs(server - cart.getX()) < .30,
                        "Smooth interpolation lagged behind the server: " + (server - cart.getX()));
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large")
    public static void predictedRailPositionFollowsTheBendWithoutCuttingAcrossBlocks(
            GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(8, 2, 8));
        for (int x = 7; x <= 10; x++)
            for (int z = 7; z <= 10; z++)
                h.getLevel()
                        .setBlock(
                                h.absolutePos(new BlockPos(x, 1, z)),
                                Blocks.STONE.defaultBlockState(),
                                2);
        h.getLevel()
                .setBlock(
                        origin,
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                        2);
        h.getLevel()
                .setBlock(
                        origin.east(),
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST),
                        2);
        h.getLevel()
                .setBlock(
                        origin.east().south(),
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH),
                        2);
        var cart =
                new Minecart(
                        h.getLevel(),
                        origin.getX() + 1.25,
                        origin.getY() + .0625,
                        origin.getZ() + .75);
        var track = MinecartTrack.at(cart, origin.east());
        Vec3 target = track.point(track.length() * .5);
        cart.setPos(target);
        ((MinecartMotionVisuals) cart).spelunkersCharm$motion().clientPacketVelocity =
                new Vec3(.4, 0, .4);
        Vec3 predicted = MinecartInterpolation.predicted(cart, target);
        var end = MinecartTrack.at(cart, BlockPos.containing(predicted));
        h.assertTrue(
                end != null && end.point(end.offset(predicted)).distanceTo(predicted) < 1e-6,
                "Prediction left the connected rail path");
        h.assertTrue(
                predicted.distanceTo(target) > .25,
                "Prediction did not compensate interpolation delay");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "empty")
    public static void brakePacketsDoNotPredictPastTheStoppedCart(GameTestHelper h) {
        var cart = new Minecart(h.getLevel(), 8, 10, 8);
        var visual = (MinecartMotionVisuals) cart;
        visual.spelunkersCharm$motion().clientPacketVelocity = new Vec3(.45, 0, 0);
        visual.spelunkersCharm$setBraking(.8F);
        Vec3 target = new Vec3(8.2, 10, 8);
        h.assertTrue(
                MinecartInterpolation.predicted(cart, target).equals(target),
                "Brake prediction overshot the server stop");
        visual.spelunkersCharm$setBraking(0);
        visual.spelunkersCharm$motion().clientVelocitySeen = true;
        visual.spelunkersCharm$motion().clientVelocity = Vec3.ZERO;
        h.assertTrue(
                MinecartInterpolation.predicted(cart, target).equals(target),
                "A stopped velocity packet kept extrapolating old movement");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "empty")
    public static void fixedHingeDerivativesMatchTheirPitchAndYawAxes(GameTestHelper h) {
        var cart = new Minecart(h.getLevel(), 10, 20, 8);
        double epsilon = 1e-6;
        for (int side : new int[] {-1, 1})
            for (int i = 0; i < 20; i++) {
                double yaw = i * .31, pitch = -1.4 + i * .14;
                Vec3 p = MinecartHingeGeometry.offset(cart, yaw, pitch, side);
                Vec3 dp =
                        MinecartHingeGeometry.offset(cart, yaw, pitch + epsilon, side)
                                .subtract(p)
                                .scale(1 / epsilon);
                Vec3 dy =
                        MinecartHingeGeometry.offset(cart, yaw + epsilon, pitch, side)
                                .subtract(p)
                                .scale(1 / epsilon);
                h.assertTrue(
                        dp.distanceTo(MinecartHingeGeometry.pitchDerivative(cart, yaw, pitch, side))
                                < 1e-5,
                        "Pitch hinge changed its rotation axis");
                h.assertTrue(
                        dy.distanceTo(MinecartHingeGeometry.yawDerivative(cart, yaw, pitch, side))
                                < 1e-5,
                        "Yaw hinge changed its rotation axis");
            }
        h.succeed();
    }
}
