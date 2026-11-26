// Minecart train sync.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public final class MinecartTrainSync {
    private static final Map<ServerLevel, List<AbstractMinecart>> PENDING = new IdentityHashMap<>();

    private MinecartTrainSync() {}

    public static void collect(ServerLevel level, List<AbstractMinecart> carts) {
        PENDING.put(level, carts);
    }

    public static void clear(ServerLevel level) {
        PENDING.remove(level);
    }

    public static void flush(ServerLevel level) {
        var carts = PENDING.remove(level);
        if (carts == null || level.players().isEmpty()) return;
        var visited = new HashSet<UUID>();
        var links = MinecartCouplings.get(level);
        long now = level.getGameTime();
        for (var start : carts) {
            if (!start.isAlive()
                    || !MinecartSpeedRules.managed(start)
                    || !visited.add(start.getUUID())) continue;
            var group = new ArrayList<AbstractMinecart>();
            var queue = new ArrayDeque<AbstractMinecart>();
            queue.add(start);
            long until = Long.MIN_VALUE;
            while (!queue.isEmpty() && group.size() < 512) {
                var cart = queue.removeFirst();
                group.add(cart);
                var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
                if (state.airborne
                        || state.flightSpeedUntil >= now
                        || cart.getDeltaMovement().lengthSqr() > .0001)
                    state.trainSyncUntil = now + 12;
                until = Math.max(until, state.trainSyncUntil);
                for (var id : links.neighbors(cart.getUUID()))
                    if (level.getEntity(id) instanceof AbstractMinecart peer
                            && peer.isAlive()
                            && MinecartSpeedRules.managed(peer)
                            && visited.add(id)) queue.addLast(peer);
            }
            if (group.size() < 2 || until < now) continue;
            var entries = new ArrayList<MinecartTrainPayload.Cart>(group.size());
            var viewers = new HashSet<ServerPlayer>();
            for (var cart : group) {
                ((MinecartMotionVisuals) cart).spelunkersCharm$motion().trainSyncUntil = until;
                entries.add(
                        new MinecartTrainPayload.Cart(
                                cart.getId(),
                                cart.position(),
                                MinecartTrainBrakes.onRail(cart)
                                        ? MinecartHingeGeometry.supportedAxis(cart)
                                        : MinecartFlightPhysics.axis(cart)));
                viewers.addAll(
                        level.getChunkSource().chunkMap.getPlayers(cart.chunkPosition(), false));
            }
            var payload = new MinecartTrainPayload(now, List.copyOf(entries));
            for (var viewer : viewers)
                if (viewer.connection.hasChannel(MinecartTrainPayload.TYPE))
                    PacketDistributor.sendToPlayer(viewer, payload);
        }
    }
}
