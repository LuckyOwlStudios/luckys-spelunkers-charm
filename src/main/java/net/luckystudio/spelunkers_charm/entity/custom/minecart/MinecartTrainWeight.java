// Minecart train weight.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MinecartTrainWeight {
    private MinecartTrainWeight() {}

    private static boolean onRail(AbstractMinecart cart) {
        return MinecartTrainBrakes.onRail(cart);
    }

    private static boolean hanging(AbstractMinecart cart) {
        return cart.isAlive()
                && MinecartSpeedRules.managed(cart)
                && !cart.isNoGravity()
                && !cart.onGround()
                && !onRail(cart);
    }

    public static void apply(
            ServerLevel level, List<AbstractMinecart> carts, MinecartCouplings links) {
        Set<UUID> visited = new HashSet<>();
        Map<AbstractMinecart, RailGroup> rails = new IdentityHashMap<>();
        Map<AbstractMinecart, List<AbstractMinecart>> ground = new IdentityHashMap<>();
        Map<AbstractMinecart, Vec3> acceleration = new IdentityHashMap<>();
        for (var start : carts) {
            if (!hanging(start) || !visited.add(start.getUUID())) continue;
            var airborne = new ArrayList<AbstractMinecart>();
            var boundaries = new ArrayList<Boundary>();
            var anchors = new HashSet<UUID>();
            var queue = new ArrayDeque<AbstractMinecart>();
            queue.add(start);
            while (!queue.isEmpty() && airborne.size() < 64) {
                var current = queue.removeFirst();
                airborne.add(current);
                for (UUID id : links.neighbors(current.getUUID())) {
                    if (!(level.getEntity(id) instanceof AbstractMinecart peer)
                            || !peer.isAlive()
                            || !MinecartSpeedRules.managed(peer)) continue;
                    if (hanging(peer)) {
                        if (visited.add(id)) queue.addLast(peer);
                    } else if (onRail(peer)) {
                        boundaries.add(new Boundary(peer, current));
                        anchors.add(id);
                    } else if (peer.onGround()) {
                        boundaries.add(new Boundary(peer, current));
                        anchors.add(id);
                    }
                }
            }
            if (boundaries.isEmpty()) continue;
            double share = 1.0 / Math.max(1, anchors.size());
            for (var boundary : boundaries) {
                var support = boundary.support;
                var first = boundary.hanging;
                if (support.getY() - first.getY() <= .20) continue;
                double load = 0;
                for (var cart : airborne)
                    load +=
                            cart.getGravity()
                                    * Math.clamp((support.getY() - cart.getY() - .20) / .80, 0, 1);
                var motion = ((MinecartMotionVisuals) support).spelunkersCharm$motion();
                Vec3 toward =
                        motion.flightNeighborAxes.getOrDefault(
                                first.getUUID(), first.position().subtract(support.position()));
                if (!onRail(support)) {
                    var section = groundGroup(support, level, links, ground);
                    if (airborne.size() <= section.size()) continue;
                    Vec3 pull = toward.multiply(1, 0, 1).normalize();
                    if (pull.lengthSqr() < .01) continue;
                    double amount =
                            Math.min(
                                    .06, load * share / (section.size() + airborne.size() * share));
                    for (var cart : section)
                        acceleration.merge(cart, pull.scale(amount), Vec3::add);
                    continue;
                }
                RailGroup group = railGroup(support, level, links, rails);
                if (group.braked) continue;
                var track = MinecartTrack.current(support);
                if (track == null) continue;
                double sign = track.tangent().dot(toward);
                if (Math.abs(sign) < .1)
                    sign = track.tangent().dot(first.position().subtract(support.position()));
                if (Math.abs(sign) < .1) continue;
                double amount =
                        Math.min(
                                Math.max(.01, support.getGravity()),
                                load * share / (group.carts.size() + airborne.size() * share));
                if (amount < 1e-6) continue;
                Vec3 pull = track.tangent().scale(Math.signum(sign));
                if (MinecartSpeedRules.trainPowered(support)
                        && MinecartSpeedRules.trainDrive(support).dot(pull) < -.1) continue;
                distribute(support, pull, amount, group, level, links, acceleration);
            }
        }
        for (var entry : acceleration.entrySet()) {
            var cart = entry.getKey();
            if (!onRail(cart)) {
                if (cart.onGround()) {
                    Vec3 v = cart.getDeltaMovement().add(entry.getValue());
                    double speed = v.horizontalDistance();
                    if (speed > .16) v = new Vec3(v.x * .16 / speed, v.y, v.z * .16 / speed);
                    cart.setDeltaMovement(v);
                    cart.hasImpulse = true;
                }
                continue;
            }
            if (MinecartHandbrake.active(cart)) continue;
            var track = MinecartTrack.current(cart);
            if (track == null) continue;
            double limit = cart.getMaxSpeedWithRail();
            double speed =
                    Math.clamp(
                            cart.getDeltaMovement().add(entry.getValue()).dot(track.tangent()),
                            -limit,
                            limit);
            cart.setDeltaMovement(track.tangent().scale(speed));
            cart.hasImpulse = true;
        }
    }

    private static List<AbstractMinecart> groundGroup(
            AbstractMinecart start,
            ServerLevel level,
            MinecartCouplings links,
            Map<AbstractMinecart, List<AbstractMinecart>> cache) {
        var found = cache.get(start);
        if (found != null) return found;
        var section = new ArrayList<AbstractMinecart>();
        var seen = new HashSet<UUID>();
        var queue = new ArrayDeque<AbstractMinecart>();
        queue.add(start);
        seen.add(start.getUUID());
        while (!queue.isEmpty() && section.size() < 64) {
            var cart = queue.removeFirst();
            section.add(cart);
            for (var id : links.neighbors(cart.getUUID()))
                if (seen.add(id)
                        && level.getEntity(id) instanceof AbstractMinecart peer
                        && peer.isAlive()
                        && peer.onGround()
                        && !onRail(peer)) queue.addLast(peer);
        }
        for (var cart : section) cache.put(cart, section);
        return section;
    }

    private static RailGroup railGroup(
            AbstractMinecart start,
            ServerLevel level,
            MinecartCouplings links,
            Map<AbstractMinecart, RailGroup> cache) {
        RailGroup found = cache.get(start);
        if (found != null) return found;
        var carts = new ArrayList<AbstractMinecart>();
        var ids = new HashSet<UUID>();
        var queue = new ArrayDeque<AbstractMinecart>();
        queue.add(start);
        ids.add(start.getUUID());
        boolean braked = false;
        while (!queue.isEmpty() && carts.size() < 64) {
            var cart = queue.removeFirst();
            carts.add(cart);
            braked |= MinecartHandbrake.active(cart);
            for (UUID id : links.neighbors(cart.getUUID())) {
                if (ids.contains(id)
                        || !(level.getEntity(id) instanceof AbstractMinecart peer)
                        || !onRail(peer)
                        || !MinecartSpeedRules.managed(peer)) continue;
                if (MinecartRailPath.between(cart, peer) != null) {
                    ids.add(id);
                    queue.addLast(peer);
                }
            }
        }
        var group = new RailGroup(carts, ids, braked);
        for (var cart : carts) cache.put(cart, group);
        return group;
    }

    private static void distribute(
            AbstractMinecart start,
            Vec3 heading,
            double amount,
            RailGroup group,
            ServerLevel level,
            MinecartCouplings links,
            Map<AbstractMinecart, Vec3> acceleration) {
        var queue = new ArrayDeque<Heading>();
        var visited = new HashSet<UUID>();
        queue.add(new Heading(start, heading));
        visited.add(start.getUUID());
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            acceleration.merge(current.cart, current.direction.scale(amount), Vec3::add);
            for (UUID id : links.neighbors(current.cart.getUUID())) {
                if (!group.ids.contains(id)
                        || !visited.add(id)
                        || !(level.getEntity(id) instanceof AbstractMinecart peer)) continue;
                var path = MinecartRailPath.between(current.cart, peer);
                if (path == null) continue;
                double orientation = current.direction.dot(path.firstToward());
                if (Math.abs(orientation) > .1)
                    queue.addLast(
                            new Heading(peer, path.secondToward().scale(orientation > 0 ? -1 : 1)));
            }
        }
    }

    /** Connects a supported cart to a hanging section. */
    private record Boundary(AbstractMinecart support, AbstractMinecart hanging) {}

    /** Stores a supported section and its shared braking state. */
    private record RailGroup(List<AbstractMinecart> carts, Set<UUID> ids, boolean braked) {}

    /** Carries a rail direction while distributing train weight. */
    private record Heading(AbstractMinecart cart, Vec3 direction) {}
}
