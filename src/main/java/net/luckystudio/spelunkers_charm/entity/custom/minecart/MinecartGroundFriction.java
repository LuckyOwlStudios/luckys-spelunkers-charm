// Minecart ground braking.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

final class MinecartGroundFriction {
    private static final double DECELERATION = 0.12;
    private static final double CONTACT_MARGIN = 0.10;

    private MinecartGroundFriction() {}

    static void apply(ServerLevel level, List<AbstractMinecart> carts, MinecartCouplings links) {
        for (var cart : carts) {
            var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
            motion.groundContactTicks =
                    grounded(cart) ? Math.min(3, motion.groundContactTicks + 1) : 0;
        }
        var visited = new HashSet<UUID>();
        for (var start : carts) {
            if (!brakingContact(start) || !visited.add(start.getUUID())) continue;
            var train = new ArrayList<AbstractMinecart>();
            var pending = new ArrayDeque<AbstractMinecart>();
            pending.add(start);
            boolean supported = true;
            boolean engine = false, groundedEngine = false;
            while (!pending.isEmpty()) {
                var cart = pending.removeFirst();
                train.add(cart);
                supported &= supported(cart);
                if (MinecartCouplings.isEngine(cart)) {
                    engine = true;
                    groundedEngine |= brakingContact(cart);
                }
                for (UUID id : links.neighbors(cart.getUUID())) {
                    if (level.getEntity(id) instanceof AbstractMinecart peer && peer.isAlive()) {
                        if (visited.add(id)) pending.addLast(peer);
                    } else supported = false;
                }
            }
            if (supported && (!engine || groundedEngine)) for (var cart : train) slow(cart);
        }
    }

    static boolean brakingContact(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion().groundContactTicks >= 3
                && grounded(cart);
    }

    private static boolean supported(AbstractMinecart cart) {
        return cart.isAlive()
                && !cart.isInWaterOrBubble()
                && MinecartSpeedRules.managed(cart)
                && (MinecartTrainBrakes.onRail(cart) || contact(cart));
    }

    private static boolean grounded(AbstractMinecart cart) {
        return cart.isAlive()
                && !cart.isInWaterOrBubble()
                && !MinecartTrainBrakes.onRail(cart)
                && !MinecartRailCapture.nearLanding(cart)
                && MinecartSpeedRules.managed(cart)
                && contact(cart);
    }

    private static boolean contact(AbstractMinecart cart) {
        if (cart.onGround()) return true;
        var box = cart.getBoundingBox();
        var feet =
                new AABB(
                        box.minX + .01,
                        box.minY - CONTACT_MARGIN,
                        box.minZ + .01,
                        box.maxX - .01,
                        box.minY + .000001,
                        box.maxZ - .01);
        return cart.level().getBlockCollisions(cart, feet).iterator().hasNext();
    }

    private static void slow(AbstractMinecart cart) {
        Vec3 velocity = cart.getDeltaMovement();
        double speed = velocity.horizontalDistance();
        if (speed == 0) return;
        double remaining = Math.max(0, speed - DECELERATION);
        if (remaining == 0) cart.setDeltaMovement(0, velocity.y, 0);
        else {
            double scale = remaining / speed;
            cart.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
        }
        cart.hasImpulse = true;
    }
}
