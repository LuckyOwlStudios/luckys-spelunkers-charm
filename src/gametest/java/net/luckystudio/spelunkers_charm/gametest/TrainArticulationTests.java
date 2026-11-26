// Train articulation tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainArticulationTests {
    @GameTest(template = "empty", timeoutTicks = 30)
    public static void locomotiveBodyFollowsEverySlopeWithEitherBodyFacing(GameTestHelper h) {
        int index = 0;
        for (var shape :
                new RailShape[] {
                    RailShape.ASCENDING_EAST,
                    RailShape.ASCENDING_WEST,
                    RailShape.ASCENDING_NORTH,
                    RailShape.ASCENDING_SOUTH
                }) {
            var pos = h.absolutePos(new BlockPos(4 + index * 4, 3, 8));
            h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
            var raised =
                    switch (shape) {
                        case ASCENDING_EAST -> net.minecraft.core.Direction.EAST;
                        case ASCENDING_WEST -> net.minecraft.core.Direction.WEST;
                        case ASCENDING_NORTH -> net.minecraft.core.Direction.NORTH;
                        default -> net.minecraft.core.Direction.SOUTH;
                    };
            h.getLevel().setBlock(pos.relative(raised), Blocks.STONE.defaultBlockState(), 2);
            for (int pass = 0; pass < 2; pass++)
                h.getLevel()
                        .setBlock(
                                pos,
                                Blocks.POWERED_RAIL
                                        .defaultBlockState()
                                        .setValue(PoweredRailBlock.SHAPE, shape),
                                2);
            var cart =
                    new LocomotiveMinecart(
                            h.getLevel(), pos.getX() + .5, pos.getY() + .5625, pos.getZ() + .5);
            var track = MinecartTrack.at(cart, pos);
            Vec3 line =
                    track.second().height() > track.first().height()
                            ? track.tangent().add(0, 1, 0)
                            : track.tangent().add(0, -1, 0);
            line = line.normalize();
            h.assertTrue(
                    Math.abs(MinecartBodyPose.target(cart).dot(line)) > .999,
                    "Body tangent missed native rail slope: " + shape);
            float yaw = (float) Math.toDegrees(Math.atan2(line.z, line.x));
            for (float facing : new float[] {yaw, yaw + 180}) {
                double pitch = Math.toRadians(MinecartBodyPose.pitch(line, facing)),
                        rotation = Math.toRadians(facing);
                Vec3 model =
                        new Vec3(
                                Math.cos(rotation) * Math.cos(pitch),
                                Math.sin(pitch),
                                Math.sin(rotation) * Math.cos(pitch));
                h.assertTrue(
                        Math.abs(model.dot(line)) > .99999,
                        "Locomotive tilt points away from slope: " + shape + ", yaw=" + facing);
            }
            index++;
        }
        h.succeed();
    }

    private static Minecart[] bend(GameTestHelper h) {
        var carts = new Minecart[3];
        for (int i = 0; i < 3; i++) {
            Vec3 p = h.absoluteVec(new Vec3(6 + i * 1.55, 8, 8));
            carts[i] = new Minecart(h.getLevel(), p.x, p.y, p.z);
            carts[i].moveTo(p.x, p.y, p.z, 0, 0);
            h.getLevel().addFreshEntity(carts[i]);
            ((MinecartMotionVisuals) carts[i]).spelunkersCharm$setAirborne(true);
            if (i > 0) MinecartCouplings.get(h.getLevel()).link(carts[i - 1], carts[i]);
        }
        return carts;
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void jointAngleUsesBothNeighborsInsteadOfVelocityOrLongestLink(GameTestHelper h) {
        var carts = bend(h);
        carts[0].setPos(carts[0].getX(), carts[0].getY() - 1, carts[0].getZ());
        carts[2].setPos(carts[2].getX(), carts[2].getY() + .2, carts[2].getZ());
        for (var cart : carts) cart.setDeltaMovement(1, -2, 0);
        double pitch = MinecartBodyPose.pitch(MinecartBodyPose.target(carts[1]), 0);
        h.assertTrue(
                pitch > 8 && pitch < 32,
                "Middle wagon did not blend adjacent joint angles: " + pitch);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void articulationEasesAcrossTicksAndUsesSameAxisForBothMounts(GameTestHelper h) {
        var carts = bend(h);
        var cart = carts[1];
        Vec3 before = MinecartBodyPose.axis(cart, 1);
        carts[2].setPos(carts[2].getX(), carts[2].getY() + 2, carts[2].getZ());
        cart.tickCount++;
        Vec3 after = MinecartBodyPose.axis(cart, 1), half = MinecartBodyPose.axis(cart, .5f);
        double turn = Math.toDegrees(Math.acos(Math.clamp(Math.abs(before.dot(after)), -1, 1)));
        h.assertTrue(
                turn > 1 && turn <= 14.1, "Joint rotation jumped or failed to respond: " + turn);
        h.assertTrue(
                half.distanceTo(before) < after.distanceTo(before),
                "Partial-tick body tilt was not interpolated");
        h.succeed();
    }

    @GameTest(template = "rail_flight_large", timeoutTicks = 300)
    public static void everyWagonMarksOneRailImpactAfterTallRamp(GameTestHelper h) {
        var fixture = TrainFlightOrderTests.ramp(h);
        var carts = fixture.train(24.5, 8);
        h.runAfterDelay(
                220,
                () -> {
                    for (var cart : carts)
                        h.assertTrue(
                                ((MinecartMotionVisuals) cart)
                                                .spelunkersCharm$motion()
                                                .railLandingTick
                                        != Long.MIN_VALUE,
                                "Wagon landed without its individual impact: " + cart.getId());
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void railImpactIgnoresFlatTravelAndDebouncesRepeatedContact(GameTestHelper h) {
        var carts = bend(h);
        var cart = carts[1];
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        state.airborne = false;
        state.freeTicks = 20;
        MinecartRailLanding.landed(cart, -.5);
        h.assertTrue(
                state.railLandingTick == Long.MIN_VALUE,
                "Flat/off-rail ground travel sounded like a landing");
        state.airborne = true;
        state.freeTicks = 3;
        MinecartRailLanding.landed(cart, -.5);
        long first = state.railLandingTick;
        h.assertTrue(first != Long.MIN_VALUE, "Real airborne rail contact was silent");
        h.runAfterDelay(
                3,
                () -> {
                    state.airborne = true;
                    state.freeTicks = 3;
                    MinecartRailLanding.landed(cart, -.5);
                    h.assertTrue(
                            state.railLandingTick == first,
                            "Repeated rail contact spammed impact sounds");
                    h.succeed();
                });
    }
}
