package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Couples minecarts together with chains. Right-click one cart with a chain, then another, to link them;
 * that uses up a chain (not in creative). Shift-click a coupled cart with a chain to remove its couplings,
 * which gives the chains back. The clicks are handled in MinecartEvents, since minecarts aren't living entities.
 */
public final class CartCoupler {

    /** The first cart each player clicked, waiting for the second. Server side only. */
    private static final Map<UUID, UUID> SELECTED = new HashMap<>();

    private CartCoupler() {
    }

    /** Server side. */
    public static void useOnCart(Player player, AbstractMinecart cart, ItemStack chain) {
        if (player.isSecondaryUseActive()) {
            SELECTED.remove(player.getUUID());
            int removed = CartLinking.unlinkAll(cart);
            message(player, removed > 0 ? "unlinked" : "no_links");
            return;
        }

        UUID first = SELECTED.remove(player.getUUID());
        if (first == null) {
            if (CartLinking.getLinks(cart).links().size() >= CartLinking.MAX_LINKS) {
                message(player, "full");
                return;
            }
            SELECTED.put(player.getUUID(), cart.getUUID());
            cart.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
            message(player, "selected");
            return;
        }
        if (first.equals(cart.getUUID())) {
            message(player, "cancelled");
            return;
        }

        Entity firstCart = player.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(first) : null;
        if (!(firstCart instanceof AbstractMinecart other) || other.isRemoved()) {
            message(player, "missing");
            return;
        }
        switch (CartLinking.link(other, cart)) {
            case LINKED -> {
                if (!player.getAbilities().instabuild) chain.shrink(1);
                message(player, "linked");
            }
            case ALREADY_LINKED -> message(player, "already_linked");
            case TOO_FAR -> message(player, "too_far");
            case FULL -> message(player, "full");
            case SAME_CART -> message(player, "cancelled");
        }
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.spelunkers_charm.cart_coupler." + key), true);
    }
}
