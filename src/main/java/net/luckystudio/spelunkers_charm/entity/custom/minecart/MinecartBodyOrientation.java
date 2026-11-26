// Minecart front direction.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartBodyOrientation {
    private MinecartBodyOrientation() {}

    public static void land(AbstractMinecart cart, MinecartTrack track) {
        var visual = (MinecartMotionVisuals) cart;
        var state = visual.spelunkersCharm$motion();
        float reference =
                Double.isFinite(state.flightReferenceYaw)
                                && cart.level()
                                        instanceof net.minecraft.server.level.ServerLevel level
                                && !MinecartCouplings.get(level).neighbors(cart.getUUID()).isEmpty()
                        ? (float) Math.toDegrees(state.flightReferenceYaw)
                        : state.flightRotationReady
                                ? (float) Math.toDegrees(state.physicalYaw)
                                : state.visualYawReady
                                        ? state.visualYaw
                                        : cart instanceof LocomotiveMinecart engine
                                                ? engine.bodyYaw()
                                                : cart.getYRot();
        Vec3 line = track.tangent();
        float yaw = (float) Math.toDegrees(Math.atan2(line.z, line.x));
        float delta = Mth.wrapDegrees(yaw - reference);
        if (delta > 90) delta -= 180;
        else if (delta < -90) delta += 180;
        float facing = reference + delta;
        landFacing(
                cart,
                new Vec3(Math.cos(Math.toRadians(facing)), 0, Math.sin(Math.toRadians(facing))));
    }

    public static void landFacing(AbstractMinecart cart, Vec3 front) {
        var visual = (MinecartMotionVisuals) cart;
        var state = visual.spelunkersCharm$motion();
        float facing = (float) Math.toDegrees(Math.atan2(front.z, front.x));
        state.visualYaw = state.visualYawOld = facing;
        state.visualYawReady = true;
        state.visualYawTick = Long.MIN_VALUE;
        state.physicalYaw = Math.toRadians(facing);
        state.recoverBodyFacing = false;
        state.physicalPitch = Math.atan2(front.y, front.horizontalDistance());
        state.yawRate = state.pitchRate = 0;
        state.flightReferenceYaw = state.physicalYaw;
        visual.spelunkersCharm$setBodyYaw(facing);
        if (cart instanceof LocomotiveMinecart engine) engine.landFacing(facing);
    }

    public static void acceptServerFacing(AbstractMinecart cart, float facing) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        var end = MinecartTrack.current(cart);
        var target = motion.clientPacketTarget;
        var start =
                target == null
                        ? null
                        : MinecartTrack.at(cart, net.minecraft.core.BlockPos.containing(target));
        if (start == null && target != null)
            start = MinecartTrack.at(cart, net.minecraft.core.BlockPos.containing(target).below());
        if (start != null && end != null && !start.pos().equals(end.pos())) {
            boolean found = false;
            for (int initial : new int[] {1, -1}) {
                var track = start;
                int sign = initial;
                Vec3 first = track.tangent().scale(sign);
                for (int step = 0; step < 12; step++) {
                    Vec3 edge = track.point(sign > 0 ? track.length() : 0);
                    var next = track.next(cart, sign);
                    if (next == null) break;
                    track = next;
                    sign = track.offset(edge) < track.length() * .5 ? 1 : -1;
                    if (track.pos().equals(end.pos())) {
                        Vec3 last = track.tangent().scale(sign);
                        facing +=
                                Mth.wrapDegrees(
                                        (float)
                                                Math.toDegrees(
                                                        Math.atan2(last.z, last.x)
                                                                - Math.atan2(first.z, first.x)));
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        }
        motion.visualYaw = motion.visualYawOld = facing;
        motion.visualYawReady = true;
        motion.clientBodyFacingSeen = true;
        motion.visualYawTick = Long.MIN_VALUE;
    }

    public static void followRail(AbstractMinecart cart, Vec3 previous, Vec3 next) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        float before = (float) Math.toDegrees(Math.atan2(previous.z, previous.x));
        float after = (float) Math.toDegrees(Math.atan2(next.z, next.x));
        if (!state.visualYawReady) {
            float facing =
                    cart instanceof LocomotiveMinecart engine ? engine.bodyYaw() : cart.getYRot();
            float authoritative = ((MinecartMotionVisuals) cart).spelunkersCharm$bodyYaw();
            if (cart.level().isClientSide() && Float.isFinite(authoritative))
                facing = authoritative;
            state.visualYaw = before + (Math.abs(Mth.wrapDegrees(facing - before)) > 90 ? 180 : 0);
            state.visualYawOld = state.visualYaw;
            state.visualYawReady = true;
        }
        if (cart.level().isClientSide() && state.visualYawTick != cart.level().getGameTime()) {
            state.visualYawOld = state.visualYaw;
            state.visualYawTick = cart.level().getGameTime();
        }
        state.visualYaw += Mth.wrapDegrees(after - before);
        state.facingVelocity = next;
        if (!cart.level().isClientSide()) {
            ((MinecartMotionVisuals) cart).spelunkersCharm$setBodyYaw(state.visualYaw);
            state.physicalYaw = Math.toRadians(state.visualYaw);
            if (Double.isFinite(state.flightReferenceYaw))
                state.flightReferenceYaw = state.physicalYaw;
            if (state.trainPowered)
                state.trainDrive = next.scale(state.trainDrive.dot(previous) < 0 ? -1 : 1);
            if (cart instanceof LocomotiveMinecart engine) engine.followCurve(previous, next);
            if (cart instanceof net.minecraft.world.entity.vehicle.MinecartFurnace furnace) {
                double sign = Math.signum(new Vec3(furnace.xPush, 0, furnace.zPush).dot(previous));
                furnace.xPush = next.x * sign;
                furnace.zPush = next.z * sign;
            }
        }
    }

    public static float yaw(AbstractMinecart cart, float partialTick) {
        float facing = frameYaw(cart, partialTick);
        if (cart.level().isClientSide()
                && !((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) {
            Vec3 front = MinecartHingeGeometry.axis(Math.toRadians(frameYaw(cart, 1)), 0);
            Vec3 axis = MinecartInterpolation.renderAxis(cart, partialTick, front);
            if (axis != null && axis.horizontalDistanceSqr() > .0004) {
                float delta =
                        Mth.wrapDegrees(
                                (float) Math.toDegrees(Math.atan2(axis.z, axis.x)) - facing);
                return facing + delta;
            }
        }
        return facing;
    }

    private static float frameYaw(AbstractMinecart cart, float partialTick) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (cart.level().isClientSide()
                && state.clientTrainAxis != null
                && state.clientTrainTick >= cart.level().getGameTime() - 1
                && state.clientTrainAxis.horizontalDistanceSqr() > .0004) {
            float facing =
                    (float)
                            Math.toDegrees(
                                    Math.atan2(state.clientTrainAxis.z, state.clientTrainAxis.x));
            if (state.visualYawTick != cart.level().getGameTime()) {
                state.visualYawOld = state.visualYawReady ? state.visualYaw : facing;
                state.visualYaw = state.visualYawOld + Mth.wrapDegrees(facing - state.visualYawOld);
                state.visualYawReady = true;
                state.visualYawTick = cart.level().getGameTime();
            }
            return Mth.lerp(partialTick, state.visualYawOld, state.visualYaw);
        }
        float authoritative = ((MinecartMotionVisuals) cart).spelunkersCharm$bodyYaw();
        if (cart.level().isClientSide()
                && !state.visualYawReady
                && !state.airborne
                && Float.isFinite(authoritative)) {
            state.visualYaw = state.visualYawOld = authoritative;
            state.visualYawReady = true;
        }
        if ((((MinecartMotionVisuals) cart).spelunkersCharm$airborne()
                        || MinecartTrack.current(cart) == null && state.flightRotationReady)
                && (cart.level().isClientSide() || state.flightRotationReady)) {
            float target =
                    cart.level().isClientSide()
                            ? ((MinecartMotionVisuals) cart).spelunkersCharm$flightYaw()
                            : (float) Math.toDegrees(state.physicalYaw);
            if (state.visualYawTick != cart.level().getGameTime()) {
                state.visualYawOld = state.visualYawReady ? state.visualYaw : target;
                state.visualYaw = state.visualYawOld + Mth.wrapDegrees(target - state.visualYawOld);
                state.visualYawReady = true;
                state.visualYawTick = cart.level().getGameTime();
            }
            return Mth.lerp(partialTick, state.visualYawOld, state.visualYaw);
        }
        long tick = cart.level().getGameTime();
        if (state.visualYawTick != tick || !state.visualYawReady) {
            Vec3 movement =
                    state.facingPosition == null
                            ? Vec3.ZERO
                            : cart.position().subtract(state.facingPosition).multiply(1, 0, 1);
            Vec3 axis = MinecartBodyPose.railAxis(cart, 1);
            if (axis == null) {
                Vec3 tangent = MinecartBodyPose.target(cart);
                if (tangent.horizontalDistanceSqr() > .04)
                    axis = tangent.multiply(1, 0, 1).normalize();
                else if (((MinecartCouplingVisuals) cart).spelunkersCharm$firstCouplingId() < 0
                        && movement.horizontalDistanceSqr() > .0004) axis = movement.normalize();
            }
            float initial =
                    cart instanceof LocomotiveMinecart engine ? engine.bodyYaw() : cart.getYRot();
            if (!state.visualYawReady) {
                state.visualYaw = initial;
                state.visualYawReady = true;
                if (axis != null) stable(cart, (float) Math.toDegrees(Math.atan2(axis.z, axis.x)));
                state.visualYawOld = state.visualYaw;
            } else {
                state.visualYawOld = state.visualYaw;
                if (axis != null) {
                    float target = (float) Math.toDegrees(Math.atan2(axis.z, axis.x));
                    float difference = Mth.wrapDegrees(target - state.visualYaw);
                    if (Math.abs(Math.abs(difference) - 90) < .001
                            && movement.horizontalDistanceSqr() > 1e-6) {
                        double angle = Math.toRadians(state.visualYaw);
                        Vec3 previous =
                                state.facingVelocity.horizontalDistanceSqr() > 1e-6
                                        ? state.facingVelocity
                                        : movement;
                        double sign =
                                previous.dot(new Vec3(Math.cos(angle), 0, Math.sin(angle))) < 0
                                        ? -1
                                        : 1;
                        if (axis.dot(movement) * sign < 0) target += 180;
                    }
                    stable(cart, target);
                }
            }
            state.facingPosition = cart.position();
            if (movement.horizontalDistanceSqr() > 1e-6)
                state.facingVelocity = movement.normalize();
            state.visualYawTick = tick;
        }
        return Mth.lerp(partialTick, state.visualYawOld, state.visualYaw);
    }

    public static float stable(AbstractMinecart cart, float railYaw) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (!state.visualYawReady) {
            float reference =
                    cart instanceof LocomotiveMinecart engine ? engine.bodyYaw() : cart.getYRot();
            float difference = Mth.wrapDegrees(railYaw - reference);
            if (difference > 90) railYaw -= 180;
            if (difference < -90) railYaw += 180;
        }
        if (!state.visualYawReady) {
            state.visualYawReady = true;
            state.visualYaw = railYaw;
        } else {
            float delta = Mth.wrapDegrees(railYaw - state.visualYaw);
            if (delta > 90) delta -= 180;
            if (delta < -90) delta += 180;
            state.visualYaw += delta;
        }
        return state.visualYaw;
    }
}
