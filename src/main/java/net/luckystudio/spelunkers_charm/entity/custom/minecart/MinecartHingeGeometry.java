// Minecart hinge geometry.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartHingeGeometry {
    public static final double HEIGHT = .375, LIFT = 1.0 / 16;

    private MinecartHingeGeometry() {}

    private static double exact(double value) {
        return Math.abs(value) < 1e-10 ? 0 : value;
    }

    public static Vec3 axis(double yaw, double pitch) {
        double c = Math.cos(pitch);
        return new Vec3(exact(Math.cos(yaw) * c), exact(Math.sin(pitch)), exact(Math.sin(yaw) * c));
    }

    public static Vec3 up(double yaw, double pitch) {
        return new Vec3(
                exact(-Math.cos(yaw) * Math.sin(pitch)),
                exact(Math.cos(pitch)),
                exact(-Math.sin(yaw) * Math.sin(pitch)));
    }

    public static double reach(AbstractMinecart cart) {
        return cart instanceof LocomotiveMinecart ? 11.5 / 16 : 10.5 / 16;
    }

    public static Vec3 offset(AbstractMinecart cart, double yaw, double pitch, int side) {
        return axis(yaw, pitch).scale(side * reach(cart)).add(up(yaw, pitch).scale(LIFT));
    }

    public static Vec3 pitchDerivative(AbstractMinecart cart, double yaw, double pitch, int side) {
        return up(yaw, pitch).scale(side * reach(cart)).subtract(axis(yaw, pitch).scale(LIFT));
    }

    public static Vec3 yawDerivative(AbstractMinecart cart, double yaw, double pitch, int side) {
        double along = side * reach(cart) * Math.cos(pitch) - LIFT * Math.sin(pitch);
        return new Vec3(exact(-Math.sin(yaw) * along), 0, exact(Math.cos(yaw) * along));
    }

    public static Vec3 supportedAxis(AbstractMinecart cart) {
        recoverFacing(cart);
        double facing =
                cart instanceof LocomotiveMinecart engine
                        ? engine.bodyYaw()
                        : state(cart).visualYawReady ? state(cart).visualYaw : cart.getYRot();
        var rail = MinecartTrack.current(cart);
        if (rail == null
                || rail.point(rail.offset(cart.position())).distanceToSqr(cart.position()) > .0225)
            return state(cart).flightRotationReady
                    ? axis(state(cart).physicalYaw, state(cart).physicalPitch)
                    : axis(Math.toRadians(facing), 0);
        Vec3 line = rail.point(rail.length()).subtract(rail.point(0)).normalize();
        facing = repairFacing(cart, line, (float) facing);
        if (cart instanceof LocomotiveMinecart engine)
            return line.dot(engine.bodyDirection()) < 0 ? line.scale(-1) : line;
        facing =
                MinecartBodyOrientation.stable(
                        cart, (float) Math.toDegrees(Math.atan2(line.z, line.x)));
        return line.dot(axis(Math.toRadians(facing), 0)) < 0 ? line.scale(-1) : line;
    }

    private static float repairFacing(AbstractMinecart cart, Vec3 line, float facing) {
        var motion = state(cart);
        if (!(cart.level() instanceof ServerLevel level)
                || motion.airborne
                || motion.hingeSides.isEmpty()) return facing;
        var links = MinecartCouplings.get(level);
        Vec3 front = axis(Math.toRadians(facing), 0);
        boolean suspect = false;
        for (var hook : motion.hingeSides.entrySet())
            if (links.linked(cart.getUUID(), hook.getKey())
                    && level.getEntity(hook.getKey()) instanceof AbstractMinecart peer)
                suspect |=
                        front.scale(hook.getValue()).dot(peer.position().subtract(cart.position()))
                                < -.2;
        if (!suspect) return facing;
        int required = 0;
        for (var hook : motion.hingeSides.entrySet()) {
            if (!links.linked(cart.getUUID(), hook.getKey())
                    || !(level.getEntity(hook.getKey()) instanceof AbstractMinecart peer)
                    || !MinecartTrainBrakes.onRail(peer)
                    || cart.distanceToSqr(peer) > 12.25) continue;
            var path = MinecartRailPath.between(cart, peer);
            if (path == null) continue;
            int sign = line.dot(path.firstToward()) * hook.getValue() < 0 ? -1 : 1;
            if (required != 0 && required != sign) return facing;
            required = sign;
        }
        if (required == 0 || front.dot(line.scale(required)) >= 0) return facing;
        float corrected = (float) Math.toDegrees(Math.atan2(line.z * required, line.x * required));
        motion.visualYaw = motion.visualYawOld = corrected;
        motion.visualYawReady = true;
        motion.visualYawTick = Long.MIN_VALUE;
        ((MinecartMotionVisuals) cart).spelunkersCharm$setBodyYaw(corrected);
        motion.trainSyncUntil = level.getGameTime() + 12;
        if (cart instanceof LocomotiveMinecart engine) engine.landFacing(corrected);
        return corrected;
    }

    public static int side(AbstractMinecart cart, AbstractMinecart peer) {
        if (cart.level().isClientSide()) {
            var links = (MinecartCouplingVisuals) cart;
            int bits = ((MinecartMotionVisuals) cart).spelunkersCharm$hookSides();
            int value =
                    peer.getId() == links.spelunkersCharm$firstCouplingId()
                            ? bits & 3
                            : peer.getId() == links.spelunkersCharm$secondCouplingId()
                                    ? (bits >> 2) & 3
                                    : 0;
            if (value != 0) return value == 1 ? -1 : 1;
        }
        boolean linked =
                cart.level() instanceof ServerLevel level
                        && MinecartCouplings.get(level).linked(cart.getUUID(), peer.getUUID());
        var sides = state(cart).hingeSides;
        Integer saved = sides.get(peer.getUUID());
        if (linked && saved != null) return saved;
        int side = prospectiveSide(cart, peer);
        if (linked) sides.put(peer.getUUID(), side);
        return side;
    }

    public static int prospectiveSide(AbstractMinecart cart, AbstractMinecart peer) {
        if (cart instanceof LocomotiveMinecart) return -1;
        Vec3 axis =
                state(cart).airborne && state(cart).flightRotationReady
                        ? axis(state(cart).physicalYaw, state(cart).physicalPitch)
                        : supportedAxis(cart);
        var path = MinecartRailPath.between(cart, peer);
        Vec3 toward = path == null ? peer.position().subtract(cart.position()) : path.firstToward();
        return axis.dot(toward) < 0 ? -1 : 1;
    }

    public static boolean available(AbstractMinecart cart, AbstractMinecart peer) {
        if (!(cart.level() instanceof ServerLevel level)) return false;
        int proposed = prospectiveSide(cart, peer);
        for (var id : MinecartCouplings.get(level).neighbors(cart.getUUID()))
            if (!id.equals(peer.getUUID())
                    && level.getEntity(id) instanceof AbstractMinecart other
                    && side(cart, other) == proposed) return false;
        return true;
    }

    public static void sync(AbstractMinecart cart) {
        if (!(cart.level() instanceof ServerLevel level)) return;
        recoverFacing(cart);
        ((MinecartMotionVisuals) cart)
                .spelunkersCharm$setFlightTrain(
                        state(cart).flightSpeedUntil >= level.getGameTime());
        state(cart)
                .hingeSides
                .keySet()
                .removeIf(id -> !MinecartCouplings.get(level).linked(cart.getUUID(), id));
        var links = (MinecartCouplingVisuals) cart;
        int bits = 0;
        int[] ids = {
            links.spelunkersCharm$firstCouplingId(), links.spelunkersCharm$secondCouplingId()
        };
        for (int i = 0; i < 2; i++)
            if (level.getEntity(ids[i]) instanceof AbstractMinecart peer)
                bits |= (side(cart, peer) < 0 ? 1 : 2) << (i * 2);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setHookSides(bits);
    }

    private static void recoverFacing(AbstractMinecart cart) {
        var motion = state(cart);
        if (!motion.recoverBodyFacing || !(cart.level() instanceof ServerLevel level)) return;
        if (motion.hingeSides.isEmpty()) {
            motion.recoverBodyFacing = false;
            return;
        }
        var rail = MinecartTrack.current(cart);
        if (rail == null
                || rail.point(rail.offset(cart.position())).distanceToSqr(cart.position()) > .0225)
            return;
        Vec3 line = rail.point(rail.length()).subtract(rail.point(0)).normalize();
        for (var entry : motion.hingeSides.entrySet())
            if (level.getEntity(entry.getKey()) instanceof AbstractMinecart peer) {
                var path = MinecartRailPath.between(cart, peer);
                if (path == null) continue;
                if (line.dot(path.firstToward()) * entry.getValue() < 0) line = line.scale(-1);
                motion.visualYaw =
                        motion.visualYawOld = (float) Math.toDegrees(Math.atan2(line.z, line.x));
                motion.visualYawReady = true;
                motion.recoverBodyFacing = false;
                for (var id : MinecartCouplings.get(level).neighbors(cart.getUUID()))
                    if (level.getEntity(id) instanceof AbstractMinecart other) {
                        var toward = MinecartRailPath.between(cart, other);
                        if (toward != null)
                            motion.hingeSides.put(id, line.dot(toward.firstToward()) < 0 ? -1 : 1);
                    }
                ((MinecartMotionVisuals) cart).spelunkersCharm$setBodyYaw(motion.visualYaw);
                return;
            }
    }

    private static MinecartMotionState state(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
    }
}
