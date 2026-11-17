package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.init.ModAttachmentTypes;
import net.luckystudio.spelunkers_charm.network.CartSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-side coupling of minecarts: creating and removing links, and keeping coupled carts together. */
public final class CartLinking {

    /** One cart in front and one behind. */
    public static final int MAX_LINKS = 2;
    /** How far apart (center to center) coupled carts like to be. */
    public static final double LINK_DISTANCE = 1.6;
    /** Carts further apart than this can't be coupled. */
    public static final double MAX_COUPLE_DISTANCE = 4.0;
    /** A coupling snaps if its carts get this far apart. */
    public static final double BREAK_DISTANCE = 8.0;

    /** The most a pair is pulled back into line in one tick, in blocks. Only matters right after coupling. */
    private static final double MAX_CORRECTION = 0.35;
    /** Distance errors smaller than this are left alone, so the carts don't jitter. */
    private static final double DEADZONE = 0.01;

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

    public static Result link(AbstractMinecart a, AbstractMinecart b) {
        if (a == b || a.getUUID().equals(b.getUUID())) return Result.SAME_CART;
        CartLinks aLinks = getLinks(a);
        CartLinks bLinks = getLinks(b);
        if (aLinks.contains(b.getUUID())) return Result.ALREADY_LINKED;
        if (a.distanceTo(b) > MAX_COUPLE_DISTANCE) return Result.TOO_FAR;
        if (aLinks.links().size() >= MAX_LINKS || bLinks.links().size() >= MAX_LINKS) return Result.FULL;

        setLinks(a, aLinks.with(b.getUUID()));
        setLinks(b, bLinks.with(a.getUUID()));
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
        return links.links().size();
    }

    public static void unlink(AbstractMinecart a, AbstractMinecart b) {
        setLinks(a, getLinks(a).without(b.getUUID()));
        setLinks(b, getLinks(b).without(a.getUUID()));
        a.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
        sync(a);
        sync(b);
    }

    /**
     * Called when a cart is removed from the world for good (broken, killed).
     * Its partners forget about it. Carts that are only unloaded keep their links.
     */
    public static void onCartRemoved(AbstractMinecart cart) {
        CartLinks links = getLinks(cart);
        for (UUID id : links.links()) {
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
                setLinks(other, getLinks(other).without(from.getUUID()).with(to.getUUID()));
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
        if (distance < 1.0E-4) return;
        double nx = dx / distance;
        double nz = dz / distance;

        // Put the carts back exactly the right distance apart, half the correction each.
        double error = distance - LINK_DISTANCE;
        if (Math.abs(error) > DEADZONE) {
            double move = Math.max(-MAX_CORRECTION, Math.min(MAX_CORRECTION, error)) * 0.5;
            a.setPos(a.getX() + nx * move, a.getY(), a.getZ() + nz * move);
            b.setPos(b.getX() - nx * move, b.getY(), b.getZ() - nz * move);
        }

        // Make them move together: remove any speed that would pull them apart or push them together.
        Vec3 va = a.getDeltaMovement();
        Vec3 vb = b.getDeltaMovement();
        double relative = (vb.x - va.x) * nx + (vb.z - va.z) * nz;
        double half = relative * 0.5;
        a.setDeltaMovement(va.x + nx * half, va.y, va.z + nz * half);
        b.setDeltaMovement(vb.x - nx * half, vb.y, vb.z - nz * half);
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
