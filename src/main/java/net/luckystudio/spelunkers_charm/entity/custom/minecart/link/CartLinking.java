package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.luckystudio.spelunkers_charm.init.ModAttachmentTypes;
import net.luckystudio.spelunkers_charm.network.CartSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-side coupling of minecarts: creating and removing links, and keeping coupled carts together. */
public final class CartLinking {

    /** One cart in front and one behind. */
    public static final int MAX_LINKS = 2;
    /** How far apart (center to center) coupled carts like to be. */
    public static final double LINK_DISTANCE = 2.0;
    /** Carts further apart than this can't be coupled. */
    public static final double MAX_COUPLE_DISTANCE = 4.0;
    /** A coupling snaps if its carts get this far apart. */
    public static final double BREAK_DISTANCE = 8.0;

    /** The most a pair is pulled back into line in one tick, in blocks. Only matters right after coupling. */
    private static final double MAX_CORRECTION = 0.2;
    /** Distance errors smaller than this are left alone, so the carts don't jitter. */
    private static final double DEADZONE = 0.01;

    /** A coupling snaps when one of its carts has been off the rails this many ticks, instead of dragging it around. */
    private static final int OFF_RAIL_TICKS = 20;
    /** How many ticks each coupled cart has been off the rails. Server side only. */
    private static final Map<AbstractMinecart, Integer> OFF_RAIL = new WeakHashMap<>();

    /** Carts that currently have couplings and are loaded. Server side only. */
    private static final Set<AbstractMinecart> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

    public enum Result { LINKED, SAME_CART, ALREADY_LINKED, TOO_FAR, FULL }

    private CartLinking() {
    }

    public static CartLinks getLinks(AbstractMinecart cart) {
        return cart.hasData(ModAttachmentTypes.CART_LINKS) ? cart.getData(ModAttachmentTypes.CART_LINKS) : CartLinks.EMPTY;
    }

    private static void setLinks(AbstractMinecart cart, CartLinks links) {
        if (links.isEmpty()) {
            cart.removeData(ModAttachmentTypes.CART_LINKS);
            ACTIVE.remove(cart);
        } else {
            cart.setData(ModAttachmentTypes.CART_LINKS, links);
            ACTIVE.add(cart);
        }
    }

    /** The most carts counted in one train. */
    private static final int MAX_TRAIN = 32;

    /**
     * How many carts each running locomotive has to haul: the number of carts in the train
     * (everything coupled together, directly or through other carts) divided by the number of running
     * locomotives in it. 1 for a locomotive on its own. Server side.
     */
    public static int trainLoad(AbstractMinecart start) {
        if (!(start.level() instanceof ServerLevel level)) return 1;
        Set<UUID> seen = new HashSet<>();
        Deque<AbstractMinecart> queue = new ArrayDeque<>();
        seen.add(start.getUUID());
        queue.add(start);
        int carts = 0;
        int engines = 0;
        while (!queue.isEmpty() && carts < MAX_TRAIN) {
            AbstractMinecart cart = queue.poll();
            carts++;
            if (cart instanceof LocomotiveMinecart locomotive && locomotive.isBurning()) engines++;
            for (UUID id : getLinks(cart).links()) {
                if (seen.add(id) && level.getEntity(id) instanceof AbstractMinecart other && !other.isRemoved()) {
                    queue.add(other);
                }
            }
        }
        return Math.max(1, carts / Math.max(1, engines));
    }

    /** Called when a cart joins the world, so carts loaded with couplings are held together again. */
    public static void onCartJoined(AbstractMinecart cart) {
        if (!cart.level().isClientSide() && !getLinks(cart).isEmpty()) {
            ACTIVE.add(cart);
        }
    }

    /** Called when a cart leaves the world, for any reason. */
    public static void onCartLeft(AbstractMinecart cart) {
        ACTIVE.remove(cart);
    }

    public static Result link(AbstractMinecart a, AbstractMinecart b, Item item) {
        if (a == b || a.getUUID().equals(b.getUUID())) return Result.SAME_CART;
        CartLinks aLinks = getLinks(a);
        CartLinks bLinks = getLinks(b);
        if (aLinks.contains(b.getUUID())) return Result.ALREADY_LINKED;
        if (a.distanceTo(b) > MAX_COUPLE_DISTANCE) return Result.TOO_FAR;
        if (aLinks.links().size() >= MAX_LINKS || bLinks.links().size() >= MAX_LINKS) return Result.FULL;

        setLinks(a, aLinks.with(b.getUUID(), item));
        setLinks(b, bLinks.with(a.getUUID(), item));
        a.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        sync(a);
        sync(b);
        return Result.LINKED;
    }

    /** Removes every coupling on this cart. Returns how many were removed. */
    public static int unlinkAll(AbstractMinecart cart) {
        CartLinks links = getLinks(cart);
        if (links.isEmpty()) return 0;
        for (UUID id : links.links()) {
            if (findCart(cart, id) instanceof AbstractMinecart other) {
                setLinks(other, getLinks(other).without(cart.getUUID()));
                sync(other);
            }
        }
        setLinks(cart, CartLinks.EMPTY);
        cart.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
        sync(cart);
        for (UUID id : links.links()) {
            dropCouplers(cart, links.itemFor(id), 1);
        }
        return links.links().size();
    }

    public static void unlink(AbstractMinecart a, AbstractMinecart b) {
        Item item = getLinks(a).itemFor(b.getUUID());
        setLinks(a, getLinks(a).without(b.getUUID()));
        setLinks(b, getLinks(b).without(a.getUUID()));
        a.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
        sync(a);
        sync(b);
        dropCouplers(a, item, 1);
    }

    /** A coupling that comes apart gives back what it was made with, dropped at the cart. */
    private static void dropCouplers(AbstractMinecart cart, Item item, int count) {
        if (count <= 0 || cart.level().isClientSide()) return;
        if (!cart.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
        cart.spawnAtLocation(new ItemStack(item, count));
    }

    /**
     * Called when a cart is removed from the world for good (broken, killed).
     * Its partners forget about it. Carts that are only unloaded keep their links.
     */
    public static void onCartRemoved(AbstractMinecart cart) {
        CartLinks links = getLinks(cart);
        for (UUID id : links.links()) {
            dropCouplers(cart, links.itemFor(id), 1);
            if (findCart(cart, id) instanceof AbstractMinecart other) {
                setLinks(other, getLinks(other).without(cart.getUUID()));
                sync(other);
            }
        }
    }

    /**
     * Moves couplings (and the lantern) from one cart to its replacement, for when a minecart is turned into
     * another kind of cart (like putting a chest or jukebox in it).
     */
    public static void transferLinks(AbstractMinecart from, AbstractMinecart to) {
        CartLanterns.transfer(from, to);
        CartLinks links = getLinks(from);
        if (links.isEmpty()) {
            sync(to);
            return;
        }
        for (UUID id : links.links()) {
            if (findCart(from, id) instanceof AbstractMinecart other) {
                setLinks(other, getLinks(other).without(from.getUUID()).with(to.getUUID(), getLinks(other).itemFor(from.getUUID())));
                sync(other);
            }
        }
        setLinks(to, links);
        // Clear the old cart so removing it doesn't unlink the partners again.
        setLinks(from, CartLinks.EMPTY);
        sync(to);
    }

    /**
     * Holds coupled carts at exactly {@link #LINK_DISTANCE} apart, like a rigid rod.
     * Runs once per tick after every entity has moved, so it also cancels out whatever a
     * locomotive's push or a collision did to the pair that tick. Each pair is handled once,
     * by the cart with the lower UUID.
     */
    public static void solve(ServerLevel level) {
        if (ACTIVE.isEmpty()) return;
        for (AbstractMinecart cart : List.copyOf(ACTIVE)) {
            if (cart.isRemoved() || cart.level() != level) continue;
            for (UUID id : getLinks(cart).links()) {
                if (cart.getUUID().compareTo(id) > 0) continue;
                if (!(level.getEntity(id) instanceof AbstractMinecart other) || other.isRemoved()) continue;
                solvePair(cart, other);
            }
        }
    }

    private static void solvePair(AbstractMinecart a, AbstractMinecart b) {
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (a.distanceTo(b) > BREAK_DISTANCE) {
            unlink(a, b);
            return;
        }
        // A cart that has left the rails is never pulled around. If it stays off, the coupling snaps.
        boolean aOn = trackOffRail(a);
        boolean bOn = trackOffRail(b);
        if (!aOn || !bOn) {
            if (OFF_RAIL.getOrDefault(a, 0) > OFF_RAIL_TICKS || OFF_RAIL.getOrDefault(b, 0) > OFF_RAIL_TICKS) {
                unlink(a, b);
            }
            return;
        }
        if (distance < 1.0E-4) return;
        double nx = dx / distance;
        double nz = dz / distance;

        // A cart held by an unpowered directional rail stays put, so the other cart does all the moving.
        // Otherwise the coupling and the rail would push the same cart back and forth every tick, and it would shake.
        boolean aHeld = isHeld(a);
        boolean bHeld = isHeld(b);
        if (aHeld && bHeld) return;
        double wa = aHeld ? 0.0 : (bHeld ? 1.0 : 0.5);
        double wb = bHeld ? 0.0 : (aHeld ? 1.0 : 0.5);

        // Put the carts back exactly the right distance apart, half the correction each. Each cart only moves
        // along its own rail, so a cart on a slope or a different track is never pulled sideways off it.
        double error = distance - LINK_DISTANCE;
        if (Math.abs(error) > DEADZONE) {
            double move = Math.max(-MAX_CORRECTION, Math.min(MAX_CORRECTION, error));
            if (wa > 0.0) pullAlongRail(a, nx, nz, move * wa);
            if (wb > 0.0) pullAlongRail(b, nx, nz, -move * wb);
        }

        // Make them move together: remove any speed that would pull them apart or push them together.
        Vec3 va = a.getDeltaMovement();
        Vec3 vb = b.getDeltaMovement();
        double relative = (vb.x - va.x) * nx + (vb.z - va.z) * nz;
        a.setDeltaMovement(va.x + nx * relative * wa, va.y, va.z + nz * relative * wa);
        b.setDeltaMovement(vb.x - nx * relative * wb, vb.y, vb.z - nz * relative * wb);
    }

    private static boolean isHeld(AbstractMinecart cart) {
        BlockPos pos = railPos(cart);
        return pos != null && DirectionalRailBlock.isHolding(cart.level().getBlockState(pos));
    }

    /** Counts how long the cart has been off the rails. Returns whether it is on a rail now. */
    private static boolean trackOffRail(AbstractMinecart cart) {
        if (railPos(cart) != null) {
            OFF_RAIL.remove(cart);
            return true;
        }
        OFF_RAIL.merge(cart, 1, Integer::sum);
        return false;
    }

    /** The rail the cart is on, found the same way the cart finds it, or null. */
    private static BlockPos railPos(AbstractMinecart cart) {
        BlockPos pos = cart.blockPosition();
        if (BaseRailBlock.isRail(cart.level().getBlockState(pos.below()))) {
            pos = pos.below();
        }
        return BaseRailBlock.isRail(cart.level().getBlockState(pos)) ? pos : null;
    }

    /**
     * Moves a cart along its own rail, by the part of the pull that lies along the rail.
     * Only straight and sloped rails are handled. On curves the cart is left where it is.
     */
    private static void pullAlongRail(AbstractMinecart cart, double nx, double nz, double amount) {
        BlockPos pos = railPos(cart);
        if (pos == null) return;
        Level level = cart.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock rail)) return;
        RailShape shape = rail.getRailDirection(state, level, pos, cart);
        double ax;
        double az;
        double rise = 0.0;
        switch (shape) {
            case NORTH_SOUTH -> { ax = 0.0; az = 1.0; }
            case EAST_WEST -> { ax = 1.0; az = 0.0; }
            case ASCENDING_EAST -> { ax = 1.0; az = 0.0; rise = 1.0; }
            case ASCENDING_WEST -> { ax = -1.0; az = 0.0; rise = 1.0; }
            case ASCENDING_NORTH -> { ax = 0.0; az = -1.0; rise = 1.0; }
            case ASCENDING_SOUTH -> { ax = 0.0; az = 1.0; rise = 1.0; }
            default -> { return; }
        }
        double t = (nx * ax + nz * az) * amount;
        double x = cart.getX() + ax * t;
        double y = cart.getY() + rise * t;
        double z = cart.getZ() + az * t;
        // Only move if the cart stays on a rail.
        BlockPos target = BlockPos.containing(x, y, z);
        if (!BaseRailBlock.isRail(level.getBlockState(target)) && !BaseRailBlock.isRail(level.getBlockState(target.below()))) return;
        cart.setPos(x, y, z);
    }

    private static Entity findCart(AbstractMinecart cart, UUID id) {
        if (cart.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(id);
        }
        return null;
    }

    public static CartSyncPayload payloadFor(AbstractMinecart cart) {
        List<Integer> ids = new ArrayList<>();
        for (UUID id : getLinks(cart).links()) {
            Entity other = findCart(cart, id);
            if (other != null) ids.add(other.getId());
        }
        return new CartSyncPayload(cart.getId(), ids, CartLanterns.getLantern(cart).ordinal());
    }

    /** Sends this cart's links to everyone who can see it. */
    public static void sync(AbstractMinecart cart) {
        if (cart.level().isClientSide()) return;
        PacketDistributor.sendToPlayersTrackingEntity(cart, payloadFor(cart));
    }

    /** Sends this cart's links to one player, when they start seeing it. */
    public static void syncTo(AbstractMinecart cart, ServerPlayer player) {
        if (getLinks(cart).isEmpty() && CartLanterns.getLantern(cart) == CartLantern.NONE) return;
        PacketDistributor.sendToPlayer(player, payloadFor(cart));
    }
}
