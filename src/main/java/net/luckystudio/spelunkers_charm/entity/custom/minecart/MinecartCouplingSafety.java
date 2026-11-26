// Minecart coupling safety.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class MinecartCouplingSafety {
    public static final int GROUND_IDLE_TICKS = 1200;
    public static final double MAX_HOOK_LENGTH = 1.4;
    public static final int TENSION_TICKS = 8;

    private MinecartCouplingSafety() {}

    public static double length(AbstractMinecart a, AbstractMinecart b) {
        return end(a, b).distanceTo(end(b, a));
    }

    private static Vec3 end(AbstractMinecart cart, AbstractMinecart peer) {
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        Vec3 axis =
                state.airborne && state.flightRotationReady
                        ? MinecartFlightPhysics.axis(cart)
                        : MinecartHingeGeometry.supportedAxis(cart);
        double yaw = Math.atan2(axis.z, axis.x), pitch = Math.asin(Math.clamp(axis.y, -1, 1));
        return cart.position()
                .add(0, MinecartHingeGeometry.HEIGHT, 0)
                .add(
                        MinecartHingeGeometry.offset(
                                cart, yaw, pitch, MinecartHingeGeometry.side(cart, peer)));
    }

    private static void settleLanding(AbstractMinecart a, AbstractMinecart b) {
        boolean first = MinecartTrainBrakes.onRail(a), second = MinecartTrainBrakes.onRail(b);
        if (first == second) return;
        var fixed = first ? a : b;
        var free = first ? b : a;
        if (!((MinecartMotionVisuals) free).spelunkersCharm$airborne()) return;
        Vec3 delta = end(free, fixed).subtract(end(fixed, free));
        double span = delta.length();
        if (span <= .85 || span > 1.5) return;
        Vec3 correction = delta.scale(-Math.min(.3, span - .85) / span);
        double fraction = 1;
        while (fraction > .01
                && free.position().add(correction.scale(fraction)).distanceToSqr(fixed.position())
                        < 1.12 * 1.12) fraction *= .5;
        free.move(net.minecraft.world.entity.MoverType.SELF, correction.scale(fraction));
    }

    public static void tick(
            ServerLevel level, List<AbstractMinecart> carts, MinecartCouplings links) {
        long now = level.getGameTime();
        for (var cart : carts) {
            if (!cart.isAlive() || !MinecartSpeedRules.managed(cart)) continue;
            var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
            if (state.hookStressSample < now - 1)
                state.hookStressSince.entrySet().removeIf(e -> e.getValue() <= now);
            state.hookStressSample = now;
            state.hookStressSince.keySet().retainAll(links.neighbors(cart.getUUID()));
            boolean ground = cart.onGround() && !MinecartTrainBrakes.onRail(cart);
            if (ground) {
                state.physicalPitch = Mth.approach((float) state.physicalPitch, 0, .12f);
                state.pitchRate = state.yawRate = 0;
                state.flightLaunched = false;
                state.flightFollower = false;
                state.flightSpeedUntil = Long.MIN_VALUE;
                state.flightArcSpeed = Double.POSITIVE_INFINITY;
                ((MinecartMotionVisuals) cart)
                        .spelunkersCharm$setFlightPose(
                                (float) Math.toDegrees(state.physicalYaw),
                                (float) Math.toDegrees(state.physicalPitch));
                cart.setXRot((float) Math.toDegrees(state.physicalPitch));
            }
            boolean idle =
                    ground
                            && cart.getDeltaMovement().horizontalDistanceSqr() < .0001
                            && cart.position().distanceToSqr(state.beforePosition) < .0004;
            state.groundIdleTicks =
                    idle ? Math.min(GROUND_IDLE_TICKS, state.groundIdleTicks + 1) : 0;
            if (state.groundIdleTicks >= GROUND_IDLE_TICKS)
                for (var id : links.neighbors(cart.getUUID()))
                    links.unlinkAndDrop(level, cart.getUUID(), id);
        }
        for (var cart : carts)
            if (cart.isAlive())
                for (var id : links.neighbors(cart.getUUID()))
                    if (level.getEntity(id) instanceof AbstractMinecart peer
                            && cart.getId() < peer.getId()) {
                        for (int pass = 0; pass < 3; pass++) settleLanding(cart, peer);
                        var stress =
                                ((MinecartMotionVisuals) cart)
                                        .spelunkersCharm$motion()
                                        .hookStressSince;
                        double span = length(cart, peer);
                        if (span <= MAX_HOOK_LENGTH) {
                            stress.remove(id);
                            continue;
                        }
                        long since = stress.computeIfAbsent(id, ignored -> now);
                        if ((span > MAX_HOOK_LENGTH * 2 || now - since >= TENSION_TICKS)
                                && links.unlinkAndDrop(level, cart.getUUID(), id)) {
                            stress.remove(id);
                            cart.playSound(SoundEvents.CHAIN_BREAK, .8f, .9f);
                        }
                    }
    }
}
