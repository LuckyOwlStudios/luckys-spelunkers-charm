// Minecart rail capture.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class MinecartRailCapture {
    private MinecartRailCapture() {}

    public static boolean nearLanding(AbstractMinecart cart) {
        var track = MinecartTrack.at(cart, cart.getCurrentRailPosition());
        if (track == null) track = MinecartTrack.at(cart, cart.getCurrentRailPosition().below());
        if (track == null) return false;
        Vec3 point = track.point(track.offset(cart.position()));
        double height = cart.getY() - point.y;
        return height >= -.05
                && height <= 1.1
                && point.subtract(cart.position()).horizontalDistanceSqr() < .25
                && !cart.level()
                        .getBlockCollisions(
                                cart,
                                new net.minecraft.world.phys.AABB(
                                        point.x - .16,
                                        point.y,
                                        point.z - .16,
                                        point.x + .16,
                                        point.y + .68,
                                        point.z + .16))
                        .iterator()
                        .hasNext();
    }

    public static Vec3 approach(AbstractMinecart cart, Vec3 displacement) {
        if (displacement.y >= 0) return displacement;
        var track = MinecartTrack.at(cart, cart.getCurrentRailPosition());
        if (track == null) track = MinecartTrack.at(cart, cart.getCurrentRailPosition().below());
        if (track == null) return displacement;
        Vec3 landing = track.point(track.offset(cart.position()));
        double height = cart.getY() - landing.y;
        Vec3 horizontal = displacement.multiply(1, 0, 1);
        if (height < .15 || height > 1.1) return displacement;
        Vec3 lateral = cart.position().subtract(landing).multiply(1, 0, 1);
        if (lateral.horizontalDistanceSqr() > .2025) return displacement;
        Vec3 along = track.tangent().scale(horizontal.dot(track.tangent()));
        double distance = lateral.horizontalDistance();
        Vec3 centring = distance > .05 ? lateral.scale(-.05 / distance) : lateral.scale(-1);
        return along.add(centring).add(0, displacement.y, 0);
    }

    public static void land(AbstractMinecart cart, MinecartTrack track, Vec3 incoming) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        int sign = direction(cart, track, incoming);
        Vec3 heading = track.tangent().scale(sign);
        int front = motion.flightForwardSign;
        if (!motion.flightLaunched) {
            double yaw =
                    cart instanceof LocomotiveMinecart engine
                            ? Math.toRadians(engine.bodyYaw())
                            : motion.flightRotationReady
                                    ? motion.physicalYaw
                                    : Math.toRadians(
                                            motion.visualYawReady
                                                    ? motion.visualYaw
                                                    : cart.getYRot());
            Vec3 previous =
                    incoming.horizontalDistanceSqr() > 1e-6 ? incoming : motion.travelHeading;
            front = previous.dot(MinecartHingeGeometry.axis(yaw, 0)) < 0 ? -1 : 1;
        }
        double reference =
                Double.isFinite(motion.flightReferenceYaw)
                        ? motion.flightReferenceYaw
                        : motion.physicalYaw;
        double alignment = track.tangent().dot(MinecartHingeGeometry.axis(reference, 0));
        Vec3 facing =
                Math.abs(alignment) > .5
                        ? track.tangent().scale(alignment < 0 ? -1 : 1)
                        : heading.scale(front);
        MinecartBodyOrientation.landFacing(cart, facing);
        double speed = Math.max(incoming.horizontalDistance(), motion.flightRailSpeed);
        cart.setDeltaMovement(heading.scale(speed));
        motion.railPosition = track.pos();
        motion.railSign = sign;
        motion.travelHeading = heading;
        motion.flightRailSpeed = 0;
        motion.flightCaptured = true;
        motion.reversalRequested = false;
        motion.freeTicks = 0;
        if (motion.trainPowered) motion.trainDrive = heading;
        if (cart instanceof MinecartFurnace furnace && furnace.fuel > 0) {
            furnace.xPush = heading.x;
            furnace.zPush = heading.z;
        }
        cart.horizontalCollision = false;
    }

    private static int direction(AbstractMinecart cart, MinecartTrack track, Vec3 incoming) {
        double projection = track.tangent().dot(incoming);
        if (cart.level() instanceof ServerLevel level) {
            var links = MinecartCouplings.get(level);
            var visited = new HashSet<UUID>();
            var queue = new ArrayDeque<UUID>();
            queue.add(cart.getUUID());
            visited.add(cart.getUUID());
            while (!queue.isEmpty() && visited.size() <= 64)
                for (UUID id : links.neighbors(queue.removeFirst()))
                    if (visited.add(id)) {
                        if (level.getEntity(id) instanceof AbstractMinecart peer) {
                            var state = ((MinecartMotionVisuals) peer).spelunkersCharm$motion();
                            if (state.flightCaptured && MinecartTrainBrakes.onRail(peer)) {
                                int resolved = transport(cart, track, peer);
                                if (resolved != 0) return resolved;
                            }
                            queue.addLast(id);
                        }
                    }
        }
        if (Math.abs(projection) > .025) return projection < 0 ? -1 : 1;
        projection =
                track.tangent()
                        .dot(((MinecartMotionVisuals) cart).spelunkersCharm$motion().travelHeading);
        if (Math.abs(projection) > .1) return projection < 0 ? -1 : 1;
        // At a right-angle landing gravity favours the flat/downhill branch over a nearby climb.
        double positive = branch(cart, track, 1), negative = branch(cart, track, -1);
        return negative > positive + 1e-6 ? -1 : 1;
    }

    private static int transport(
            AbstractMinecart cart, MinecartTrack start, AbstractMinecart peer) {
        var end = MinecartTrack.current(peer);
        if (end == null) return 0;
        Vec3 travel =
                peer.getDeltaMovement().horizontalDistanceSqr() > 1e-6
                        ? peer.getDeltaMovement()
                        : ((MinecartMotionVisuals) peer).spelunkersCharm$motion().travelHeading;
        for (int initial : new int[] {1, -1}) {
            var track = start;
            int sign = initial;
            for (int step = 0; step < 32; step++) {
                if (track.pos().equals(end.pos()))
                    return travel.dot(track.tangent().scale(sign)) < 0 ? -initial : initial;
                Vec3 edge = track.point(sign > 0 ? track.length() : 0);
                track = track.next(cart, sign);
                if (track == null) break;
                sign = track.offset(edge) < track.length() * .5 ? 1 : -1;
                if (track.pos().equals(start.pos())) break;
            }
        }
        return 0;
    }

    private static double branch(AbstractMinecart cart, MinecartTrack start, int sign) {
        var track = start;
        double score = 0;
        for (int step = 0; step < 12; step++) {
            int slope = track.slope(track.tangent().scale(sign));
            if (slope != 0) return score - slope * 4;
            score += .01;
            Vec3 edge = track.point(sign > 0 ? track.length() : 0);
            track = track.next(cart, sign);
            if (track == null) break;
            sign = track.offset(edge) < track.length() * .5 ? 1 : -1;
        }
        return score;
    }
}
