// Minecart free couplings.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartFreeCouplings {
    private MinecartFreeCouplings() {}

    private static Vec3 order(AbstractMinecart cart, AbstractMinecart peer) {
        var axes = ((MinecartMotionVisuals) cart).spelunkersCharm$motion().flightNeighborAxes;
        Vec3 direction = axes.get(peer.getUUID());
        if (direction == null) {
            direction = peer.position().subtract(cart.position()).multiply(1, 0, 1);
            if (direction.lengthSqr() < 1e-8) {
                direction =
                        ((MinecartMotionVisuals) cart)
                                .spelunkersCharm$motion()
                                .travelHeading
                                .multiply(1, 0, 1);
                if (direction.lengthSqr() < 1e-8) direction = new Vec3(1, 0, 0);
                if (peer.getY() > cart.getY()) direction = direction.scale(-1);
            }
            direction = direction.normalize();
            axes.put(peer.getUUID(), direction);
            ((MinecartMotionVisuals) peer)
                    .spelunkersCharm$motion()
                    .flightNeighborAxes
                    .put(cart.getUUID(), direction.scale(-1));
        }
        return direction;
    }

    public static void protectTrain(AbstractMinecart cart) {
        if (!(cart.level() instanceof ServerLevel level)) return;
        long tick = level.getGameTime();
        if (((MinecartMotionVisuals) cart).spelunkersCharm$motion().flightSpacingSample == tick)
            return;
        var pending = new java.util.ArrayDeque<AbstractMinecart>();
        pending.add(cart);
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            var motion = ((MinecartMotionVisuals) current).spelunkersCharm$motion();
            if (motion.flightSpacingSample == tick) continue;
            motion.flightSpacingSample = tick;
            motion.flightSpacingUntil = tick + 8;
            var neighbors = MinecartCouplings.get(level).neighbors(current.getUUID());
            motion.flightNeighborAxes.keySet().removeIf(id -> !neighbors.contains(id));
            for (var id : neighbors)
                if (level.getEntity(id) instanceof AbstractMinecart peer && peer.isAlive()) {
                    order(current, peer);
                    pending.addLast(peer);
                }
        }
    }

    public static void visual(AbstractMinecart cart) {
        if (!MinecartSpeedRules.managed(cart)) return;
        var visuals = (MinecartMotionVisuals) cart;
        var motion = visuals.spelunkersCharm$motion();
        visuals.spelunkersCharm$setAirborne(!cart.isOnRails() && !cart.onGround());
        if (cart.isOnRails()) {
            motion.flightPitch = 0;
            return;
        }
        Vec3 line = MinecartBodyPose.target(cart);
        float target =
                (float)
                        Math.clamp(
                                Math.toDegrees(Math.atan2(line.y, line.horizontalDistance())),
                                -90,
                                90);
        motion.flightPitch = Mth.approachDegrees(motion.flightPitch, target, 6);
        cart.setXRot(motion.flightPitch);
    }
}
