package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Sends attachment transitions; normal movement continues through vanilla sync. */
@EventBusSubscriber(modid = "spelunkers_charm")
public final class LiftSupportSync {
    private static final Map<Level, State> STATES = new WeakHashMap<>();

    private LiftSupportSync() {}

    private static synchronized State state(Level level) {
        return STATES.computeIfAbsent(level, ignored -> new State());
    }

    public static long begin(AbstractLift lift) {
        return ++state(lift.level()).serial;
    }

    public static void support(AbstractLift lift, Entity entity, long serial) {
        State state = state(lift.level());
        float relativeY = (float) (entity.getY() - lift.getY());
        Contact contact = new Contact(lift.getId(), relativeY, serial, lift.level().getGameTime());
        Contact previous = state.contacts.put(entity.getId(), contact);
        if (previous == null
                || previous.liftId != contact.liftId
                || Math.abs(previous.relativeY - relativeY) > 0.001F) {
            sendTracking(entity, payload(entity.getId(), contact));
        }
    }

    public static void finish(AbstractLift lift, long serial) {
        State state = state(lift.level());
        Iterator<Map.Entry<Integer, Contact>> iterator = state.contacts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Contact> entry = iterator.next();
            Contact contact = entry.getValue();
            if (contact.liftId == lift.getId() && contact.serial != serial) {
                detach(lift.level(), entry.getKey());
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.connection.hasChannel(LiftSupportPayload.TYPE)) return;
        Entity target = event.getTarget();
        State state = state(target.level());
        Entity root = target.getRootVehicle();
        Contact contact = state.contacts.get(root.getId());
        if (contact != null) PacketDistributor.sendToPlayer(player, payload(root.getId(), contact));
        // The rider can enter tracking range before its supporting platform.
        if (target instanceof AbstractLift lift) {
            for (Map.Entry<Integer, Contact> entry : state.contacts.entrySet()) {
                if (entry.getValue().liftId == lift.getId()) {
                    PacketDistributor.sendToPlayer(
                            player, payload(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        State state = state(level);
        Iterator<Map.Entry<Integer, Contact>> iterator = state.contacts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Contact> entry = iterator.next();
            Contact contact = entry.getValue();
            if (level.getGameTime() - contact.time > 2
                    || level.getEntity(entry.getKey()) == null
                    || !(level.getEntity(contact.liftId) instanceof AbstractLift)) {
                detach(level, entry.getKey());
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static synchronized void levelUnload(LevelEvent.Unload event) {
        STATES.remove(event.getLevel());
    }

    private static LiftSupportPayload payload(int entityId, Contact contact) {
        return new LiftSupportPayload(entityId, contact.liftId, contact.relativeY);
    }

    private static void detach(Level level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity != null) {
            sendTracking(entity, new LiftSupportPayload(entityId, -1, 0));
        }
    }

    private static void sendTracking(Entity entity, LiftSupportPayload payload) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        for (ServerPlayer viewer :
                level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false)) {
            if (viewer != entity && viewer.connection.hasChannel(LiftSupportPayload.TYPE))
                PacketDistributor.sendToPlayer(viewer, payload);
        }
        if (entity instanceof ServerPlayer player
                && player.connection.hasChannel(LiftSupportPayload.TYPE))
            PacketDistributor.sendToPlayer(player, payload);
    }

    private static final class State {
        long serial;
        final Map<Integer, Contact> contacts = new HashMap<>();
    }

    private record Contact(int liftId, float relativeY, long serial, long time) {}
}
