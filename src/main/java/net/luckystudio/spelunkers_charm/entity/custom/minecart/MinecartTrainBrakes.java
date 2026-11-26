// Minecart train brakes.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID)
public final class MinecartTrainBrakes {
    private static final double SPACING = 1.55;
    private static final Map<ServerLevel, Eligibility> ELIGIBLE = new IdentityHashMap<>();

    private MinecartTrainBrakes() {}

    public static void invalidate(ServerLevel level) {
        ELIGIBLE.remove(level);
    }

    public static boolean onRail(AbstractMinecart cart) {
        if (!cart.isOnRails() || state(cart).airborne) return false;
        var track = MinecartTrack.current(cart);
        return track != null
                && track.point(track.offset(cart.position())).distanceToSqr(cart.position()) < .01;
    }

    public static UUID source(AbstractMinecart cart) {
        if (!(cart.level() instanceof ServerLevel level)
                || !MinecartCouplings.get(level).hasBrakedEngines()
                || !onRail(cart)) return null;
        var eligible = ELIGIBLE.get(level);
        if (eligible == null || eligible.tick != level.getGameTime()) {
            var roots = new HashMap<UUID, UUID>();
            var links = MinecartCouplings.get(level);
            for (UUID id : links.brakedEngineIds()) {
                if (!(level.getEntity(id) instanceof AbstractMinecart root)
                        || !MinecartHandbrake.local(root)
                        || !onRail(root)) continue;
                var queue = new ArrayDeque<AbstractMinecart>();
                queue.add(root);
                roots.put(id, id);
                while (!queue.isEmpty() && roots.size() <= 256) {
                    var current = queue.removeFirst();
                    for (UUID neighbor : links.neighbors(current.getUUID())) {
                        if (roots.containsKey(neighbor)
                                || !(level.getEntity(neighbor) instanceof AbstractMinecart peer)
                                || !onRail(peer)) continue;
                        var path = MinecartRailPath.between(current, peer);
                        if (path == null
                                || path.distance() > 1.85
                                || current.distanceToSqr(peer) > 2.4 * 2.4) continue;
                        roots.put(neighbor, id);
                        queue.addLast(peer);
                    }
                }
            }
            eligible = new Eligibility(level.getGameTime(), roots);
            ELIGIBLE.put(level, eligible);
        }
        UUID root = eligible.roots.get(cart.getUUID());
        return root != null
                        && !root.equals(cart.getUUID())
                        && level.getEntity(root) instanceof AbstractMinecart engine
                        && MinecartHandbrake.local(engine)
                        && MinecartCouplings.get(level).sameTrain(root, cart.getUUID())
                ? root
                : null;
    }

    @SubscribeEvent
    public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) invalidate(level);
    }

    @SubscribeEvent
    public static void before(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        invalidate(level);
        var links = MinecartCouplings.get(level);
        for (UUID id : links.brakedEngineIds()) {
            if (!(level.getEntity(id) instanceof AbstractMinecart root)
                    || !MinecartHandbrake.local(root)) continue;
            if (!state(root).flightBrake) {
                var scan = new ArrayDeque<AbstractMinecart>();
                var scanned = new HashSet<UUID>();
                scan.add(root);
                scanned.add(id);
                while (!scan.isEmpty() && scanned.size() <= 64) {
                    var cart = scan.removeFirst();
                    if (state(cart).airborne) {
                        state(root).flightBrake = true;
                        break;
                    }
                    for (UUID peer : links.neighbors(cart.getUUID()))
                        if (scanned.add(peer)
                                && level.getEntity(peer) instanceof AbstractMinecart other)
                            scan.addLast(other);
                }
            }
            double speed =
                    MinecartHandbrake.speed(root, root.getDeltaMovement().horizontalDistance());
            Vec3 heading =
                    root.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7
                            ? root.getDeltaMovement().normalize()
                            : state(root).travelHeading;
            var queue = new ArrayDeque<Direction>();
            var visited = new HashSet<UUID>();
            queue.add(new Direction(root, heading));
            visited.add(id);
            while (!queue.isEmpty() && visited.size() <= 64) {
                var node = queue.removeFirst();
                var cart = node.cart;
                var motion = state(cart);
                motion.trainBrakeGroupTick = level.getServer().getTickCount();
                if (cart != root) {
                    if (!id.equals(source(cart))) continue;
                    double incoming = cart.getDeltaMovement().horizontalDistance();
                    double synchronizedSpeed =
                            id.equals(motion.trainBrakeRoot)
                                    ? speed
                                    : Math.max(speed, Math.max(0, incoming - .28));
                    if (!id.equals(motion.trainBrakeRoot)) motion.trainBrakeAnchor = null;
                    motion.trainBrakeRoot = id;
                    motion.trainBrakeSpeed = synchronizedSpeed;
                    if (synchronizedSpeed > .003) motion.trainBrakeAnchor = null;
                    cart.setDeltaMovement(node.heading.scale(synchronizedSpeed));
                }
                for (UUID neighbor : links.neighbors(cart.getUUID())) {
                    if (!visited.add(neighbor)
                            || !(level.getEntity(neighbor) instanceof AbstractMinecart peer)
                            || !MinecartSpeedRules.managed(peer)) continue;
                    var path = MinecartRailPath.between(cart, peer);
                    if (path == null || !id.equals(source(peer))) continue;
                    double orientation = node.heading.dot(path.firstToward());
                    Vec3 nextHeading =
                            Math.abs(orientation) > .1
                                    ? path.secondToward().scale(orientation > 0 ? -1 : 1)
                                    : state(peer).travelHeading;
                    queue.addLast(new Direction(peer, nextHeading));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var links = MinecartCouplings.get(level);
        for (UUID id : links.engineIds()) {
            if (!(level.getEntity(id) instanceof AbstractMinecart root)
                    || !MinecartSpeedRules.managed(root)) continue;
            double correction = MinecartHandbrake.local(root) ? .12 : .18;
            var queue = new ArrayDeque<AbstractMinecart>();
            var visited = new HashSet<UUID>();
            queue.add(root);
            visited.add(id);
            while (!queue.isEmpty() && visited.size() <= 64) {
                var parent = queue.removeFirst();
                for (UUID neighbor : links.neighbors(parent.getUUID())) {
                    if (!visited.add(neighbor)
                            || !(level.getEntity(neighbor) instanceof AbstractMinecart cart)
                            || !MinecartSpeedRules.managed(cart)) continue;
                    var path = MinecartRailPath.between(cart, parent);
                    if (path == null) continue;
                    long landed =
                            Math.max(state(cart).railLandingTick, state(parent).railLandingTick);
                    double blend =
                            landed == Long.MIN_VALUE
                                    ? 1
                                    : Math.clamp(
                                            .35 + (level.getGameTime() - landed) / 12.0, .35, 1);
                    if (blend > 0 && (!state(root).flightBrake || path.distance() > SPACING))
                        align(cart, parent, path, correction * blend);
                    queue.addLast(cart);
                }
            }
        }
        MinecartTrainSync.flush(level);
    }

    private static void align(
            AbstractMinecart cart,
            AbstractMinecart parent,
            MinecartRailPath path,
            double correction) {
        double error = path.spatialDistance() - MinecartCouplerPhysics.spacing(cart, parent, path);
        if (Math.abs(error) < .0001 || path.distance() > 4) return;
        var track = MinecartTrack.current(cart);
        if (track == null) return;
        int sign = path.firstToward().dot(track.tangent()) > 0 ? 1 : -1;
        if (error < 0) sign = -sign;
        double metric = track.point(track.length()).distanceTo(track.point(0)) / track.length();
        double distance =
                MinecartCouplerPhysics.allowedTravel(
                        cart,
                        track.tangent().scale(sign),
                        Math.min(Math.abs(error), correction) / metric);
        if (distance < 1e-6) return;
        var next =
                MinecartTrack.advance(cart, track, track.offset(cart.position()), sign, distance);
        if (next == null
                || !cart.level()
                        .noCollision(
                                cart,
                                cart.getBoundingBox()
                                        .move(next.position().subtract(cart.position())))) return;
        if (cart.level() instanceof ServerLevel level)
            for (UUID id : MinecartCouplings.get(level).neighbors(cart.getUUID())) {
                if (!(level.getEntity(id) instanceof AbstractMinecart peer)) continue;
                double clearance = Math.min(1.12, cart.distanceTo(peer));
                if (next.position().distanceToSqr(peer.position()) < clearance * clearance - 1e-8)
                    return;
            }
        var motion = state(cart);
        Vec3 velocity = cart.getDeltaMovement();
        Vec3 heading =
                velocity.horizontalDistanceSqr() > 1.0E-7
                        ? velocity.normalize()
                        : motion.travelHeading;
        double facing = heading.dot(track.tangent().scale(sign));
        MinecartBodyOrientation.followRail(
                cart, track.tangent().scale(sign), next.track().tangent().scale(next.sign()));
        cart.setPos(next.position());
        motion.railPosition = next.track().pos();
        motion.railSign = next.sign() * (facing < 0 ? -1 : 1);
        motion.travelHeading = next.track().tangent().scale(motion.railSign);
        cart.setDeltaMovement(
                motion.travelHeading.scale(cart.getDeltaMovement().horizontalDistance()));
        if (MinecartHandbrake.active(cart) && cart.getDeltaMovement().horizontalDistance() < .003)
            motion.trainBrakeAnchor = cart.position();
        cart.hasImpulse = true;
    }

    private static MinecartMotionState state(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
    }

    /** Stores a cart and its direction during brake propagation. */
    private record Direction(AbstractMinecart cart, Vec3 heading) {}

    /** Caches whether a cart can inherit the train brake. */
    private record Eligibility(long tick, Map<UUID, UUID> roots) {}
}
