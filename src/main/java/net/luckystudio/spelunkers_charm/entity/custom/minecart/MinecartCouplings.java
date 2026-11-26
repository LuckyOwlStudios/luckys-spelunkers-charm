// Minecart links and train rules.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MinecartCouplings extends SavedData {
    private static final Factory<MinecartCouplings> FACTORY =
            new Factory<>(MinecartCouplings::new, MinecartCouplings::load);

    private final Map<UUID, Set<UUID>> links = new HashMap<>();
    private final Map<UUID, List<UUID>> neighborSnapshots = new HashMap<>();
    private final Map<Pair, Item> materials = new HashMap<>();
    private final Set<UUID> engines = new HashSet<>();
    private final Set<UUID> brakedEngines = new HashSet<>();
    private final Map<UUID, UUID> components = new HashMap<>();

    /** Lists the possible results of connecting carts. */
    public enum Result {
        LINKED,
        FULL,
        CYCLE,
        REAR_ONLY,
        END_OCCUPIED,
        ENGINE_LIMIT
    }

    public static MinecartCouplings get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(FACTORY, "spelunkers_charm_minecart_couplings");
    }

    public static MinecartCouplings load(CompoundTag tag, HolderLookup.Provider registries) {
        MinecartCouplings result = new MinecartCouplings();
        for (Tag entry : tag.getList("Couplings", Tag.TAG_COMPOUND)) {
            if (entry instanceof CompoundTag pair
                    && pair.hasUUID("First")
                    && pair.hasUUID("Second")) {
                UUID first = pair.getUUID("First");
                UUID second = pair.getUUID("Second");
                if (result.link(first, second) == Result.LINKED && pair.contains("Material")) {
                    ResourceLocation id = ResourceLocation.tryParse(pair.getString("Material"));
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                        result.materials.put(
                                Pair.of(first, second), BuiltInRegistries.ITEM.get(id));
                    }
                }
            }
        }
        for (Tag entry : tag.getList("Engines", Tag.TAG_COMPOUND)) {
            if (entry instanceof CompoundTag engine && engine.hasUUID("Cart")) {
                result.engines.add(engine.getUUID("Cart"));
                if (engine.getBoolean("Braked")) result.brakedEngines.add(engine.getUUID("Cart"));
            }
        }
        result.setDirty(false);
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pairs = new ListTag();
        links.keySet().stream()
                .sorted()
                .forEach(
                        first -> {
                            for (UUID second : neighbors(first).stream().sorted().toList()) {
                                if (first.compareTo(second) >= 0) continue;
                                CompoundTag pair = new CompoundTag();
                                pair.putUUID("First", first);
                                pair.putUUID("Second", second);
                                Item material = materials.get(Pair.of(first, second));
                                if (material != null)
                                    pair.putString(
                                            "Material",
                                            BuiltInRegistries.ITEM.getKey(material).toString());
                                pairs.add(pair);
                            }
                        });
        tag.put("Couplings", pairs);
        ListTag powered = new ListTag();
        engines.stream()
                .sorted()
                .forEach(
                        id -> {
                            CompoundTag engine = new CompoundTag();
                            engine.putUUID("Cart", id);
                            if (brakedEngines.contains(id)) engine.putBoolean("Braked", true);
                            powered.add(engine);
                        });
        tag.put("Engines", powered);
        return tag;
    }

    public List<UUID> neighbors(UUID cart) {
        return neighborSnapshots.getOrDefault(cart, List.of());
    }

    private void updateNeighbors(UUID cart) {
        Set<UUID> adjacent = links.get(cart);
        if (adjacent == null || adjacent.isEmpty()) neighborSnapshots.remove(cart);
        else neighborSnapshots.put(cart, List.copyOf(adjacent));
    }

    public boolean linked(UUID first, UUID second) {
        return links.getOrDefault(first, Set.of()).contains(second);
    }

    public boolean sameTrain(UUID first, UUID second) {
        return first.equals(second) || connected(first, second);
    }

    public void setEngineBrake(UUID engine, boolean braked) {
        if (braked) engines.add(engine);
        boolean changed = braked ? brakedEngines.add(engine) : brakedEngines.remove(engine);
        if (changed) setDirty();
    }

    public UUID brakeSource(UUID cart) {
        for (UUID engine : brakedEngines) if (sameTrain(cart, engine)) return engine;
        return null;
    }

    public Set<UUID> brakedEngineIds() {
        return Set.copyOf(brakedEngines);
    }

    public boolean hasBrakedEngines() {
        return !brakedEngines.isEmpty();
    }

    public Set<UUID> engineIds() {
        return Set.copyOf(engines);
    }

    public Result link(UUID first, UUID second) {
        if (linked(first, second)) return Result.LINKED;
        if (first.equals(second) || connected(first, second)) return Result.CYCLE;
        if (links.getOrDefault(first, Set.of()).size() >= 2
                || links.getOrDefault(second, Set.of()).size() >= 2) {
            return Result.FULL;
        }
        links.computeIfAbsent(first, ignored -> new LinkedHashSet<>()).add(second);
        links.computeIfAbsent(second, ignored -> new LinkedHashSet<>()).add(first);
        updateNeighbors(first);
        updateNeighbors(second);
        components.clear();
        setDirty();
        return Result.LINKED;
    }

    public Result link(UUID first, UUID second, Item material) {
        if (linked(first, second)) return Result.LINKED;
        Result result = link(first, second);
        if (result == Result.LINKED && material != null)
            materials.put(Pair.of(first, second), material);
        return result;
    }

    public Result link(AbstractMinecart first, AbstractMinecart second) {
        return link(first, second, null);
    }

    public Result link(AbstractMinecart first, AbstractMinecart second, Item material) {
        if (!(first.level() instanceof ServerLevel level) || second.level() != level)
            return Result.FULL;
        if (linked(first.getUUID(), second.getUUID())) return Result.LINKED;
        if (!sameTrain(first.getUUID(), second.getUUID())
                && engineCount(level, first.getUUID()) + engineCount(level, second.getUUID()) > 1)
            return Result.ENGINE_LIMIT;
        if (!MinecartCouplerSides.rear(first, second) || !MinecartCouplerSides.rear(second, first))
            return Result.REAR_ONLY;
        if (first instanceof LocomotiveMinecart && !neighbors(first.getUUID()).isEmpty()
                || second instanceof LocomotiveMinecart && !neighbors(second.getUUID()).isEmpty())
            return Result.FULL;
        if (!MinecartHingeGeometry.available(first, second)
                || !MinecartHingeGeometry.available(second, first)) return Result.END_OCCUPIED;
        Result result = link(first.getUUID(), second.getUUID(), material);
        if (result == Result.LINKED) {
            refresh(level, first.getUUID());
            refresh(level, second.getUUID());
            MinecartHingeGeometry.sync(first);
            MinecartHingeGeometry.sync(second);
            MinecartTrainBrakes.invalidate(level);
            if (MinecartCouplingSafety.length(first, second)
                    > MinecartCouplingSafety.MAX_HOOK_LENGTH) {
                var lower = first.getId() < second.getId() ? first : second;
                var upper = lower == first ? second : first;
                ((MinecartMotionVisuals) lower)
                        .spelunkersCharm$motion()
                        .hookStressSince
                        .put(upper.getUUID(), level.getGameTime() + 12);
            }
        }
        return result;
    }

    public static boolean isEngine(AbstractMinecart cart) {
        return cart instanceof LocomotiveMinecart
                || cart instanceof net.minecraft.world.entity.vehicle.MinecartFurnace;
    }

    public static boolean hasEngine(AbstractMinecart cart) {
        return cart.level() instanceof ServerLevel level
                && get(level).engineCount(level, cart.getUUID()) > 0;
    }

    private int engineCount(ServerLevel level, UUID start) {
        Set<UUID> seen = new HashSet<>();
        ArrayDeque<UUID> pending = new ArrayDeque<>();
        seen.add(start);
        pending.add(start);
        int count = 0;
        while (!pending.isEmpty()) {
            UUID id = pending.removeFirst();
            if (level.getEntity(id) instanceof AbstractMinecart cart) rememberEngine(cart);
            if (engines.contains(id)) count++;
            for (UUID peer : neighbors(id)) if (seen.add(peer)) pending.addLast(peer);
        }
        return count;
    }

    private void rememberEngine(AbstractMinecart cart) {
        boolean changed =
                isEngine(cart) ? engines.add(cart.getUUID()) : engines.remove(cart.getUUID());
        if (changed) setDirty();
    }

    public static int trainLoad(AbstractMinecart start) {
        if (!(start.level() instanceof ServerLevel level)) return 1;
        MinecartCouplings data = get(level);
        Set<UUID> seen = new HashSet<>();
        ArrayDeque<AbstractMinecart> pending = new ArrayDeque<>();
        seen.add(start.getUUID());
        pending.add(start);
        int carts = 0;
        int engines = 0;
        while (!pending.isEmpty() && carts < 64) {
            AbstractMinecart cart = pending.removeFirst();
            carts++;
            if (cart instanceof LocomotiveMinecart locomotive && locomotive.isBurning()) engines++;
            for (UUID id : data.neighbors(cart.getUUID())) {
                if (seen.add(id)
                        && level.getEntity(id) instanceof AbstractMinecart peer
                        && peer.isAlive()) pending.addLast(peer);
            }
        }
        return Math.max(1, carts / Math.max(1, engines));
    }

    public boolean unlinkAndDrop(ServerLevel level, UUID first, UUID second) {
        Item material =
                materials.getOrDefault(
                        Pair.of(first, second), net.minecraft.world.item.Items.CHAIN);
        if (!unlink(first, second)) return false;
        Entity cart = level.getEntity(first);
        if (cart == null) cart = level.getEntity(second);
        if (material != null
                && cart != null
                && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            cart.spawnAtLocation(new ItemStack(material));
        }
        refresh(level, first);
        refresh(level, second);
        return true;
    }

    private boolean connected(UUID first, UUID second) {
        if (!links.containsKey(first) || !links.containsKey(second)) return false;
        return component(first).equals(component(second));
    }

    private UUID component(UUID first) {
        UUID cached = components.get(first);
        if (cached != null) return cached;
        Set<UUID> visited = new HashSet<>();
        ArrayDeque<UUID> pending = new ArrayDeque<>();
        visited.add(first);
        pending.add(first);
        while (!pending.isEmpty()) {
            for (UUID next : links.getOrDefault(pending.removeFirst(), Set.of())) {
                if (visited.add(next)) pending.addLast(next);
            }
        }
        for (UUID id : visited) components.put(id, first);
        return first;
    }

    public boolean unlink(UUID first, UUID second) {
        if (!linked(first, second)) return false;
        removeEnd(first, second);
        removeEnd(second, first);
        materials.remove(Pair.of(first, second));
        components.clear();
        setDirty();
        return true;
    }

    public static boolean replaceEntity(AbstractMinecart oldCart, AbstractMinecart replacement) {
        if (!(oldCart.level() instanceof ServerLevel level)) return false;
        MinecartCouplings data = get(level);
        if (replacement instanceof LocomotiveMinecart engine) {
            var peers = data.neighbors(oldCart.getUUID());
            if (peers.size() > 1) return false;
            boolean automatic = engine.driveVector().horizontalDistanceSqr() < 1e-6;
            Vec3 facing =
                    oldCart.getDeltaMovement().horizontalDistanceSqr() > 1e-6
                            ? oldCart.getDeltaMovement()
                            : ((MinecartMotionVisuals) oldCart)
                                    .spelunkersCharm$motion()
                                    .travelHeading;
            if (facing.horizontalDistanceSqr() < 1e-6) {
                var track = MinecartTrack.current(oldCart);
                if (track != null) facing = track.tangent();
            }
            if (automatic) engine.initializeBodyDirection(facing);
            if (automatic)
                for (var peer : peers)
                    if (level.getEntity(peer) instanceof AbstractMinecart neighbor)
                        engine.alignRearToward(neighbor.position().subtract(oldCart.position()));
            for (var peer : peers)
                if (level.getEntity(peer) instanceof AbstractMinecart neighbor
                        && engine.bodyDirection()
                                        .dot(neighbor.position().subtract(oldCart.position()))
                                >= 0) return false;
        }
        if (isEngine(replacement)
                && data.engineCount(level, oldCart.getUUID()) - (isEngine(oldCart) ? 1 : 0) >= 1)
            return false;
        replacement.moveTo(
                oldCart.getX(),
                oldCart.getY(),
                oldCart.getZ(),
                oldCart.getYRot(),
                oldCart.getXRot());
        replacement.setDeltaMovement(oldCart.getDeltaMovement());
        replacement.xOld = oldCart.xOld;
        replacement.yOld = oldCart.yOld;
        replacement.zOld = oldCart.zOld;
        replacement.xo = oldCart.xo;
        replacement.yo = oldCart.yo;
        replacement.zo = oldCart.zo;
        replacement.yRotO = oldCart.yRotO;
        replacement.xRotO = oldCart.xRotO;
        replacement.tickCount = oldCart.tickCount;
        replacement.fallDistance = oldCart.fallDistance;
        var oldVisuals = (MinecartMotionVisuals) oldCart;
        var newVisuals = (MinecartMotionVisuals) replacement;
        var oldMotion = oldVisuals.spelunkersCharm$motion();
        var newMotion = newVisuals.spelunkersCharm$motion();
        oldMotion.copyMotionTo(newMotion);
        newVisuals.spelunkersCharm$setBodyYaw(oldVisuals.spelunkersCharm$bodyYaw());
        if (replacement instanceof LocomotiveMinecart) newMotion.visualYawReady = false;
        newVisuals.spelunkersCharm$setOnRails(oldCart.isOnRails());
        newVisuals.spelunkersCharm$setAirborne(oldVisuals.spelunkersCharm$airborne());
        newVisuals.spelunkersCharm$setBraking(oldVisuals.spelunkersCharm$braking());
        int source =
                oldMotion.replacementGameTime == level.getGameTime()
                                && oldVisuals.spelunkersCharm$replacedCartId() >= 0
                        ? oldVisuals.spelunkersCharm$replacedCartId()
                        : oldCart.getId();
        newVisuals.spelunkersCharm$setReplacedCartId(source);
        newMotion.replacementGameTime = level.getGameTime();
        int firstPeer = -1, secondPeer = -1;
        for (UUID neighbor : data.neighbors(oldCart.getUUID())) {
            if (level.getEntity(neighbor) instanceof AbstractMinecart peer && peer.isAlive()) {
                if (firstPeer < 0) firstPeer = peer.getId();
                else secondPeer = peer.getId();
            }
        }
        ((MinecartCouplingVisuals) replacement)
                .spelunkersCharm$setCouplingIds(firstPeer, secondPeer);
        if (!level.addFreshEntity(replacement)) return false;
        data.engines.remove(oldCart.getUUID());
        if (data.brakedEngines.remove(oldCart.getUUID())) data.setDirty();
        data.rememberEngine(replacement);
        CartLanterns.transfer(oldCart, replacement);
        get(level).replace(level, oldCart.getUUID(), replacement.getUUID());
        MinecartTrainBrakes.invalidate(level);
        MinecartCouplerPhysics.replacePendingCart(oldCart, replacement);
        oldCart.discard();
        return true;
    }

    private void removeEnd(UUID cart, UUID neighbor) {
        Set<UUID> neighbors = links.get(cart);
        if (neighbors == null) return;
        neighbors.remove(neighbor);
        if (neighbors.isEmpty()) links.remove(cart);
        updateNeighbors(cart);
    }

    public void remove(ServerLevel level, UUID cart) {
        if (brakedEngines.remove(cart)) setDirty();
        if (engines.remove(cart)) setDirty();
        for (UUID neighbor : neighbors(cart)) {
            unlinkAndDrop(level, cart, neighbor);
            refresh(level, neighbor);
        }
    }

    private void replace(ServerLevel level, UUID oldCart, UUID replacement) {
        components.clear();
        Set<UUID> neighbors = links.remove(oldCart);
        neighborSnapshots.remove(oldCart);
        if (neighbors == null) return;
        links.put(replacement, neighbors);
        updateNeighbors(replacement);
        for (UUID neighbor : neighbors) {
            Item material = materials.remove(Pair.of(oldCart, neighbor));
            if (material != null) materials.put(Pair.of(replacement, neighbor), material);
            Set<UUID> adjacent = links.get(neighbor);
            adjacent.remove(oldCart);
            adjacent.add(replacement);
            updateNeighbors(neighbor);
            refresh(level, neighbor);
        }
        refresh(level, replacement);
        setDirty();
    }

    public void refresh(ServerLevel level, UUID cart) {
        if (!(level.getEntity(cart) instanceof AbstractMinecart minecart)) return;
        if (minecart instanceof LocomotiveMinecart engine
                && neighbors(cart).size() == 1
                && level.getEntity(neighbors(cart).getFirst()) instanceof AbstractMinecart peer)
            engine.resolveLegacyFacing(peer.position().subtract(engine.position()));
        rememberEngine(minecart);
        if (isEngine(minecart)) setEngineBrake(cart, MinecartHandbrake.local(minecart));
        int first = -1;
        int second = -1;
        for (UUID neighbor : links.getOrDefault(cart, Set.of())) {
            if (level.getEntity(neighbor) instanceof AbstractMinecart peer && !peer.isRemoved()) {
                if (first < 0) first = peer.getId();
                else second = peer.getId();
            }
        }
        ((MinecartCouplingVisuals) minecart).spelunkersCharm$setCouplingIds(first, second);
    }

    /** Provides a stable key for two linked cart IDs. */
    private record Pair(UUID first, UUID second) {
        private static Pair of(UUID first, UUID second) {
            return first.compareTo(second) < 0 ? new Pair(first, second) : new Pair(second, first);
        }
    }
}
