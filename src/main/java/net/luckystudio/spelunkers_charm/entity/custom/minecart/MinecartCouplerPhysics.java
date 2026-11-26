// Minecart coupling physics.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID)
public final class MinecartCouplerPhysics {
    private static final double COUPLING_DISTANCE = 1.55;
    private static final double MIN_COUPLING_DISTANCE = 1.42;
    private static final double BUFFER_DISTANCE = 1.23;
    private static final double CONTACT_LOOKAHEAD = 1.90;
    private static final double MAX_LINK_DISTANCE = 6.0;
    private static final Map<ServerLevel, TickBatch> TICKED = new IdentityHashMap<>();

    private MinecartCouplerPhysics() {}

    @SubscribeEvent
    public static void collect(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractMinecart cart
                && cart.level() instanceof ServerLevel level) {
            TICKED.computeIfAbsent(level, ignored -> new TickBatch()).add(cart);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TICKED.remove(level);
            MinecartFlightPhysics.clear(level);
            MinecartTrainSync.clear(level);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractMinecart cart)) return;
        Entity.RemovalReason reason = cart.getRemovalReason();
        if (reason != null
                && (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION)) {
            MinecartCouplings.get(level).remove(level, cart.getUUID());
        }
    }

    @SubscribeEvent
    public static void step(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        TickBatch batch = TICKED.remove(level);
        if (batch == null) return;
        List<AbstractMinecart> carts = batch.carts;
        MinecartCouplings links = MinecartCouplings.get(level);
        MinecartFlightPhysics.step(level, carts, links);
        MinecartCouplingSafety.tick(level, carts, links);
        Set<AbstractMinecart> active = new HashSet<>(carts);
        for (var cart : carts)
            if (cart.isAlive()
                    && (!cart.isOnRails()
                            || ((MinecartMotionVisuals) cart).spelunkersCharm$airborne()))
                MinecartFreeCouplings.protectTrain(cart);
        List<Contact> couplings = new ArrayList<>();
        List<Contact> buffers = new ArrayList<>();

        for (AbstractMinecart cart : carts) {
            if (cart.isRemoved()) continue;
            links.refresh(level, cart.getUUID());
            MinecartHingeGeometry.sync(cart);
            for (UUID neighbor : links.neighbors(cart.getUUID())) {
                if (!(level.getEntity(neighbor) instanceof AbstractMinecart peer)
                        || peer.isRemoved()
                        || cart.getId() >= peer.getId()
                        || !active.contains(peer)) continue;
                if (cart.distanceToSqr(peer) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) {
                    links.unlinkAndDrop(level, cart.getUUID(), neighbor);
                    links.refresh(level, cart.getUUID());
                    links.refresh(level, neighbor);
                    continue;
                }
                if (!managed(cart) || !managed(peer)) continue;
                MinecartRailPath path = MinecartRailPath.between(cart, peer);
                if (path != null) couplings.add(new Contact(cart, peer, path));
            }

            if (!bufferEnabled(cart)) continue;
            var search = cart.getBoundingBox().inflate(0.94, 0.65, 0.94);
            for (AbstractMinecart peer : level.getEntitiesOfClass(AbstractMinecart.class, search)) {
                if (cart.getId() >= peer.getId()
                        || !active.contains(peer)
                        || links.sameTrain(cart.getUUID(), peer.getUUID())
                        || !bufferEnabled(peer)) continue;
                MinecartRailPath path = MinecartRailPath.between(cart, peer);
                if (path != null && path.distance() < CONTACT_LOOKAHEAD) {
                    buffers.add(new Contact(cart, peer, path));
                }
            }
        }

        MinecartTrainWeight.apply(level, carts, links);
        Map<AbstractMinecart, Vec3> directions = trainDirections(level, carts, links);
        for (int pass = 0; pass < 3; pass++) {
            for (Contact contact : couplings) {
                double error =
                        (MinecartSpeedRules.trainBase(contact.first) >= 5
                                        ? contact.path.spatialDistance()
                                        : contact.path.distance())
                                - spacing(contact.first, contact.second, contact.path);
                double correction =
                        Mth.clamp(0.15 * error - 0.60 * contact.closingSpeed(), -0.20, 0.20);
                contact.adjust(correction, directions);
            }
            for (Contact contact : buffers) {
                double clearance = contact.path.distance() - BUFFER_DISTANCE;
                double correction =
                        contact.closingSpeed()
                                - Math.max(0, clearance) * 0.85
                                + Math.max(0, -clearance) * 0.32;
                if (correction > 0) contact.adjust(-Math.min(correction, 0.34), Map.of());
            }
        }

        MinecartGroundFriction.apply(level, carts, links);
        for (var cart : carts) if (cart.isAlive()) MinecartFreeCouplings.visual(cart);
        MinecartTrainSync.collect(level, carts);
    }

    public static boolean replacesPush(AbstractMinecart cart, AbstractMinecart other) {
        if (!(cart.level() instanceof ServerLevel level) || !managed(cart) || !managed(other))
            return false;
        if (MinecartCouplings.get(level).sameTrain(cart.getUUID(), other.getUUID())) return true;
        if (!bufferEnabled(cart) || !bufferEnabled(other)) return false;
        MinecartRailPath path = MinecartRailPath.between(cart, other);
        return path != null && path.distance() < CONTACT_LOOKAHEAD;
    }

    public static double spacing(
            AbstractMinecart first, AbstractMinecart second, MinecartRailPath path) {
        if (!MinecartTrainBrakes.onRail(first) || !MinecartTrainBrakes.onRail(second))
            return COUPLING_DISTANCE;
        double bend = path.spatialDistance() / Math.max(.5, first.distanceTo(second));
        return COUPLING_DISTANCE + .30 * Math.clamp((bend - 1.25) / .30, 0, 1);
    }

    public static double allowedTravel(AbstractMinecart cart, Vec3 heading, double requested) {
        if (!(cart.level() instanceof ServerLevel level) || requested <= 0) return requested;

        var links = MinecartCouplings.get(level);
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        boolean braked = links.brakeSource(cart.getUUID()) != null;
        if (!MinecartHandbrake.active(cart)
                && !braked
                && motion.flightSpeedUntil < level.getGameTime()) return requested;
        double allowed = requested;
        var track = MinecartTrack.current(cart);
        double metric =
                track == null
                        ? 1
                        : track.point(track.length()).distanceTo(track.point(0)) / track.length();
        for (UUID id : links.neighbors(cart.getUUID())) {
            if (!(level.getEntity(id) instanceof AbstractMinecart peer) || peer.isRemoved())
                continue;
            MinecartRailPath path = MinecartRailPath.between(cart, peer);
            if (path != null && path.firstToward().dot(heading) > .1)
                allowed =
                        Math.min(
                                allowed,
                                Math.max(0, path.spatialDistance() - MIN_COUPLING_DISTANCE)
                                        / metric);
        }
        if (braked && allowed + 1e-7 < requested) MinecartTrainBrakes.invalidate(level);
        return allowed;
    }

    public static void replacePendingCart(AbstractMinecart oldCart, AbstractMinecart replacement) {
        if (!(oldCart.level() instanceof ServerLevel level)) return;
        MinecartFlightPhysics.replace(oldCart, replacement);
        TickBatch pending = TICKED.get(level);
        if (pending != null) pending.replace(oldCart, replacement);
    }

    /** Collects carts once per server tick in entity order. */
    private static final class TickBatch {
        final List<AbstractMinecart> carts = new ArrayList<>();
        final Set<AbstractMinecart> seen =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        void add(AbstractMinecart cart) {
            if (seen.add(cart)) carts.add(cart);
        }

        void replace(AbstractMinecart oldCart, AbstractMinecart replacement) {
            int index = carts.indexOf(oldCart);
            if (index >= 0) {
                carts.set(index, replacement);
                seen.remove(oldCart);
                seen.add(replacement);
            }
        }
    }

    private static boolean managed(AbstractMinecart cart) {
        return cart.isAlive()
                && !cart.noPhysics
                && !cart.isPassenger()
                && !BetterMinecartsLinks.hasLink(cart);
    }

    private static boolean bufferEnabled(AbstractMinecart cart) {
        return cart.getCollisionHandler() == null
                && cart.isPushable()
                && cart.isOnRails()
                && cart.canUseRail()
                && managed(cart);
    }

    private static Map<AbstractMinecart, Vec3> trainDirections(
            ServerLevel level, List<AbstractMinecart> carts, MinecartCouplings links) {
        Map<AbstractMinecart, Vec3> result = new IdentityHashMap<>();
        ArrayDeque<AbstractMinecart> pending = new ArrayDeque<>();
        for (AbstractMinecart cart : carts) {
            if (!cart.isPoweredCart() || !managed(cart)) continue;
            Vec3 heading = cart.getDeltaMovement();
            if (heading.horizontalDistanceSqr() < 0.00001)
                heading = ((MinecartMotionVisuals) cart).spelunkersCharm$motion().travelHeading;
            if (heading.horizontalDistanceSqr() < 0.00001) continue;
            result.put(cart, heading.normalize());
            pending.add(cart);
        }
        while (!pending.isEmpty()) {
            AbstractMinecart cart = pending.removeFirst();
            for (UUID id : links.neighbors(cart.getUUID())) {
                if (!(level.getEntity(id) instanceof AbstractMinecart peer)
                        || result.containsKey(peer)
                        || !managed(peer)) continue;
                MinecartRailPath path = MinecartRailPath.between(cart, peer);
                if (path == null) continue;
                double toward = result.get(cart).dot(path.firstToward());
                if (Math.abs(toward) < 0.1) continue;
                result.put(peer, path.secondToward().scale(toward > 0 ? -1 : 1));
                pending.addLast(peer);
            }
        }
        return result;
    }

    /** Transfers forces between two rail contacts. */
    private record Contact(AbstractMinecart first, AbstractMinecart second, MinecartRailPath path) {
        private double closingSpeed() {
            return railSpeed(first, path.firstToward()) + railSpeed(second, path.secondToward());
        }

        private void adjust(double change, Map<AbstractMinecart, Vec3> directions) {
            if (Math.abs(change) < 1.0E-5) return;
            double firstMobility = mobility(first);
            double secondMobility = mobility(second);
            double total = firstMobility + secondMobility;
            if (total == 0) return;
            push(first, path.firstToward(), change * firstMobility / total, directions.get(first));
            push(
                    second,
                    path.secondToward(),
                    change * secondMobility / total,
                    directions.get(second));
        }

        private static void push(AbstractMinecart cart, Vec3 along, double change, Vec3 intended) {
            if (mobility(cart) == 0) return;
            Vec3 velocity = cart.getDeltaMovement();
            double limit = cart.getMaxSpeedWithRail();
            double speed =
                    Mth.clamp(railSpeed(cart, along) + change, -limit, limit)
                            / passengerFactor(cart);
            if (intended != null && speed * intended.dot(along) < 0) speed = 0;
            cart.setDeltaMovement(along.x * speed, velocity.y, along.z * speed);
            cart.hasImpulse = true;
        }

        private static double railSpeed(AbstractMinecart cart, Vec3 along) {
            double limit = cart.getMaxSpeedWithRail();
            return Mth.clamp(
                    cart.getDeltaMovement().dot(along) * passengerFactor(cart), -limit, limit);
        }

        private static double passengerFactor(AbstractMinecart cart) {
            return MinecartSpeedRules.managed(cart) ? 1.0 : cart.isVehicle() ? 0.75 : 1.0;
        }

        private static double mobility(AbstractMinecart cart) {
            if (MinecartHandbrake.active(cart)) return 0;
            var state = cart.level().getBlockState(cart.getCurrentRailPosition());
            if (net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock.isHolding(
                            state)
                    && MinecartCouplings.isEngine(cart)) return 0;
            return cart.isPoweredCart() ? 0.45 : 1;
        }
    }
}
