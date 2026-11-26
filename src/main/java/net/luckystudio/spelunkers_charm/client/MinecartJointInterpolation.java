// Minecart joint smoothing.
package net.luckystudio.spelunkers_charm.client;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, value = Dist.CLIENT)
public final class MinecartJointInterpolation {
    private static final double CLEARANCE = 1.05;

    private MinecartJointInterpolation() {}

    private static boolean settling(AbstractMinecart cart) {
        var visual = (MinecartMotionVisuals) cart;
        var motion = visual.spelunkersCharm$motion();
        return visual.spelunkersCharm$airborne()
                || motion.clientPacketTarget != null
                        && Math.abs(motion.clientPacketTarget.y - cart.getY()) > .08;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        var carts = new ArrayList<AbstractMinecart>();
        for (var entity : mc.level.entitiesForRendering())
            if (entity instanceof AbstractMinecart cart && MinecartSpeedRules.managed(cart))
                carts.add(cart);
        MinecartTrainInterpolation.apply(carts);
        var pairs = new ArrayList<Pair>();
        var original = new IdentityHashMap<AbstractMinecart, Vec3>();
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractMinecart cart) || !MinecartSpeedRules.managed(cart))
                continue;
            var links = (MinecartCouplingVisuals) cart;
            for (int id :
                    new int[] {
                        links.spelunkersCharm$firstCouplingId(),
                        links.spelunkersCharm$secondCouplingId()
                    }) {
                if (id <= cart.getId()
                        || !(mc.level.getEntity(id) instanceof AbstractMinecart peer)
                        || !MinecartSpeedRules.managed(peer)
                        || cart.distanceToSqr(peer) > 1.5625) continue;
                if (!settling(cart) && !settling(peer) && !connectedRails(cart, peer)) continue;
                pairs.add(new Pair(cart, peer));
                original.put(cart, cart.position());
                original.put(peer, peer.position());
            }
        }
        if (pairs.isEmpty()) return;
        pairs.sort(
                Comparator.comparingInt((Pair p) -> p.first.getId())
                        .thenComparingInt(p -> p.second.getId()));
        var positions = new IdentityHashMap<>(original);
        for (int pass = 0; pass < 12; pass++) {
            boolean changed = false;
            for (int i = 0; i < pairs.size(); i++) {
                var pair = pairs.get(pass % 2 == 0 ? i : pairs.size() - 1 - i);
                Vec3 delta = positions.get(pair.second).subtract(positions.get(pair.first));
                double distance = delta.length();
                if (distance < 1e-6 || distance >= CLEARANCE) continue;
                Vec3 change = delta.scale((CLEARANCE - distance) / distance);
                double a = settling(pair.first) ? 1 : .5,
                        b = settling(pair.second) ? 1 : .5,
                        total = a + b;
                changed |= adjust(pair.first, change.scale(-a / total), positions, original);
                changed |= adjust(pair.second, change.scale(b / total), positions, original);
            }
            if (!changed) break;
        }
        for (var entry : positions.entrySet()) {
            var cart = entry.getKey();
            Vec3 next = entry.getValue();
            if (next.distanceToSqr(cart.position()) < 1e-10) continue;
            cart.setPos(next);
            var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
            motion.clientVerticalStep = Math.abs(next.y - cart.yo);
            if (motion.clientTrainTick == cart.level().getGameTime())
                motion.clientTrainPosition = next;
            for (var rider : cart.getPassengers()) cart.positionRider(rider);
        }
    }

    private static boolean connectedRails(AbstractMinecart a, AbstractMinecart b) {
        return MinecartTrack.current(a) != null
                && MinecartTrack.current(b) != null
                && MinecartRailPath.between(a, b) != null;
    }

    private static boolean adjust(
            AbstractMinecart cart,
            Vec3 change,
            Map<AbstractMinecart, Vec3> positions,
            Map<AbstractMinecart, Vec3> original) {
        Vec3 before = positions.get(cart), offset = before.add(change).subtract(original.get(cart));
        double horizontal = offset.horizontalDistance();
        if (horizontal > .20)
            offset = new Vec3(offset.x * .20 / horizontal, offset.y, offset.z * .20 / horizontal);
        offset = new Vec3(offset.x, Math.clamp(offset.y, -.25, .25), offset.z);
        Vec3 next = original.get(cart).add(offset);
        if (!settling(cart)) {
            var track = MinecartTrack.current(cart);
            if (track != null
                    && track.point(track.offset(cart.position())).distanceToSqr(cart.position())
                            < .01) {
                double along = next.subtract(cart.position()).dot(track.tangent());
                var step =
                        MinecartTrack.advance(
                                cart,
                                track,
                                track.offset(cart.position()),
                                along < 0 ? -1 : 1,
                                Math.abs(along));
                next = step == null ? cart.position() : step.position();
            }
        }
        if (next.distanceToSqr(before) < 1e-12) return false;
        positions.put(cart, next);
        return true;
    }

    /** Identifies a visible coupled pair that may need a small clearance correction. */
    private record Pair(AbstractMinecart first, AbstractMinecart second) {}
}
