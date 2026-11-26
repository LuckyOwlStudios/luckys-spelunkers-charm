// Minecart body pose.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class MinecartBodyPose {
    private MinecartBodyPose() {}

    private static List<AbstractMinecart> peers(AbstractMinecart cart) {
        var result = new ArrayList<AbstractMinecart>(2);
        if (cart.level() instanceof ServerLevel level) {
            for (var id : MinecartCouplings.get(level).neighbors(cart.getUUID()))
                if (level.getEntity(id) instanceof AbstractMinecart peer && peer.isAlive())
                    result.add(peer);
        } else {
            var links = (MinecartCouplingVisuals) cart;
            for (int id :
                    new int[] {
                        links.spelunkersCharm$firstCouplingId(),
                        links.spelunkersCharm$secondCouplingId()
                    })
                if (cart.level().getEntity(id) instanceof AbstractMinecart peer && peer.isAlive())
                    result.add(peer);
        }
        return result;
    }

    private static Vec3 align(Vec3 axis, Vec3 reference) {
        return axis.x * reference.x + axis.z * reference.z < 0 ? axis.scale(-1) : axis;
    }

    public static Vec3 railAxis(AbstractMinecart cart, float partialTick) {
        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) return null;
        Vec3 p = cart.getPosition(partialTick);
        Vec3 center = cart.getPos(p.x, p.y, p.z);
        if (center == null || center.distanceToSqr(p) > .04) return null;
        Vec3 front = cart.getPosOffs(p.x, p.y, p.z, .3), back = cart.getPosOffs(p.x, p.y, p.z, -.3);
        return front != null && back != null && front.distanceToSqr(back) > 1e-6
                ? front.subtract(back).normalize()
                : null;
    }

    private static Vec3 reference(AbstractMinecart cart) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        double yaw =
                Math.toRadians(
                        state.visualYawReady
                                ? state.visualYaw
                                : cart instanceof LocomotiveMinecart engine
                                        ? engine.bodyYaw()
                                        : cart.getYRot());
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

    private static void rememberSides(
            AbstractMinecart cart, List<AbstractMinecart> peers, Vec3 reference, boolean rail) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        var sides = state.poseNeighborSides;
        sides.keySet()
                .removeIf(
                        id ->
                                (peers.isEmpty() || !peers.getFirst().getUUID().equals(id))
                                        && (peers.size() < 2
                                                || !peers.get(1).getUUID().equals(id)));
        for (var peer : peers) {
            Vec3 difference = peer.position().subtract(cart.position());
            double projection = difference.dot(reference);
            if (sides.containsKey(peer.getUUID()) && (!rail || Math.abs(projection) < .4)) continue;
            int side = projection < 0 ? -1 : 1;
            if (!rail && Math.abs(projection) < .1) {
                Vec3 order = state.flightNeighborAxes.get(peer.getUUID());
                if (order != null) side = order.dot(reference) < 0 ? -1 : 1;
                else if (state.clientPoseAxis != null
                        && Math.abs(difference.dot(state.clientPoseAxis)) > .1)
                    side = difference.dot(state.clientPoseAxis) < 0 ? -1 : 1;
            }
            if (peers.size() == 2)
                for (var other : peers) {
                    Integer previous = sides.get(other.getUUID());
                    if (other != peer && previous != null) side = -previous;
                }
            sides.put(peer.getUUID(), side);
        }
    }

    private static Vec3 localAxis(
            AbstractMinecart cart, Vec3 reference, List<AbstractMinecart> peers, Vec3 rail) {
        rememberSides(cart, peers, reference, rail != null);
        if (rail != null) return align(rail, reference);
        var sides = ((MinecartMotionVisuals) cart).spelunkersCharm$motion().poseNeighborSides;
        Vec3 sum = Vec3.ZERO;
        for (var peer : peers) {
            Vec3 difference = peer.position().subtract(cart.position());
            if (difference.lengthSqr() > 1e-6)
                sum = sum.add(difference.normalize().scale(sides.get(peer.getUUID())));
        }
        if (sum.lengthSqr() > 1e-6) return sum.normalize();
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (peers.isEmpty()) {
            Vec3 movement = cart.position().subtract(new Vec3(cart.xo, cart.yo, cart.zo));
            if (movement.lengthSqr() < 1e-6) movement = cart.getDeltaMovement();
            if (movement.lengthSqr() > 1e-6) return align(movement.normalize(), reference);
        }
        return state.clientPoseAxis != null ? state.clientPoseAxis : reference;
    }

    public static Vec3 target(AbstractMinecart cart) {
        var visual = (MinecartMotionVisuals) cart;
        var motion = visual.spelunkersCharm$motion();
        if (cart.level().isClientSide()
                && motion.clientTrainTick >= cart.level().getGameTime() - 1
                && motion.clientTrainAxis != null) return motion.clientTrainAxis;
        if (visual.spelunkersCharm$airborne()
                && (cart.level().isClientSide()
                        || visual.spelunkersCharm$motion().flightRotationReady)) {
            return cart.level().isClientSide()
                    ? MinecartHingeGeometry.axis(
                            Math.toRadians(visual.spelunkersCharm$flightYaw()),
                            Math.toRadians(visual.spelunkersCharm$flightPitch()))
                    : MinecartFlightPhysics.axis(cart);
        }
        Vec3 reference = reference(cart);
        if (!visual.spelunkersCharm$airborne()
                && MinecartTrack.current(cart) == null
                && (cart.onGround()
                        || cart.level().isClientSide()
                                && Math.abs(motion.clientPacketVelocity.y) < .005))
            return reference;
        var neighbors = peers(cart);
        Vec3 rail = railAxis(cart, 1);
        Vec3 local = localAxis(cart, reference, neighbors, rail);
        if (rail != null || neighbors.isEmpty()) return local;
        Vec3 axis = local.scale(2);
        for (var peer : neighbors) {
            Vec3 peerAxis = localAxis(peer, reference(peer), peers(peer), railAxis(peer, 1));
            if (peerAxis.dot(local) < 0) peerAxis = peerAxis.scale(-1);
            axis = axis.add(peerAxis);
        }
        return axis.lengthSqr() > 1e-6 ? axis.normalize() : local;
    }

    private static Vec3 smoothed(AbstractMinecart cart, float partialTick) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        long tick = cart.tickCount;
        if (state.clientPoseAxis == null
                || tick - state.clientPoseTick > 10
                || tick < state.clientPoseTick) {
            state.clientPoseAxis = target(cart);
            state.clientPoseOldAxis = state.clientPoseAxis;
            state.clientPoseTick = tick;
        } else if (tick != state.clientPoseTick) {
            Vec3 wanted = target(cart);
            double angle =
                    Math.toDegrees(Math.acos(Math.clamp(state.clientPoseAxis.dot(wanted), -1, 1)));
            double step = railAxis(cart, 1) == null ? 6 : 12;
            double fraction =
                    angle < 1e-5
                            ? 1
                            : Math.min(
                                    .35, step * Math.max(1, tick - state.clientPoseTick) / angle);
            state.clientPoseOldAxis = state.clientPoseAxis;
            state.clientPoseAxis = state.clientPoseAxis.lerp(wanted, fraction).normalize();
            state.clientPoseTick = tick;
        }
        return state.clientPoseOldAxis.lerp(state.clientPoseAxis, partialTick).normalize();
    }

    public static float pitch(Vec3 axis, float yaw) {
        double angle = Math.toRadians(yaw);
        double facing = axis.x * Math.cos(angle) + axis.z * Math.sin(angle);
        return (float)
                Math.clamp(
                        Math.toDegrees(Math.atan2(axis.y, axis.horizontalDistance()))
                                * (facing < 0 ? -1 : 1),
                        -90,
                        90);
    }

    public static float pitch(AbstractMinecart cart, float yaw, float partialTick) {
        Vec3 axis = smoothed(cart, partialTick);
        return (float)
                Math.clamp(Math.toDegrees(Math.atan2(axis.y, axis.horizontalDistance())), -90, 90);
    }

    public static float yaw(AbstractMinecart cart, float partialTick) {
        return MinecartBodyOrientation.yaw(cart, partialTick);
    }

    public static Vec3 axis(AbstractMinecart cart, float partialTick) {
        float yaw = yaw(cart, partialTick);
        double rotation = Math.toRadians(yaw),
                pitch = Math.toRadians(pitch(cart, yaw, partialTick));
        return new Vec3(
                Math.cos(rotation) * Math.cos(pitch),
                Math.sin(pitch),
                Math.sin(rotation) * Math.cos(pitch));
    }
}
