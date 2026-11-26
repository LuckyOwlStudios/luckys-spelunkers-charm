// Cart coupler.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.BetterMinecartsLinks;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CartCoupler {
    private static final Map<UUID, Selection> SELECTED = new HashMap<>();

    private CartCoupler() {}

    public static void clear(Player player) {
        SELECTED.remove(player.getUUID());
    }

    public static void useOnCart(Player player, AbstractMinecart cart, ItemStack chain) {
        if (!(cart.level() instanceof ServerLevel level) || !cart.isAlive()) return;
        MinecartCouplings links = MinecartCouplings.get(level);
        if (player.isSecondaryUseActive()) {
            SELECTED.remove(player.getUUID());
            boolean linked = !links.neighbors(cart.getUUID()).isEmpty();
            links.remove(level, cart.getUUID());
            links.refresh(level, cart.getUUID());
            message(player, linked ? "unlinked" : "no_links");
            return;
        }
        if (BetterMinecartsLinks.hasLink(cart)) {
            player.displayClientMessage(
                    Component.translatable("item.spelunkers_charm.cart_coupling_hook.foreign"),
                    true);
            return;
        }
        Selection selected = SELECTED.remove(player.getUUID());
        if (selected == null || selected.level() != level) {
            if (links.neighbors(cart.getUUID()).size() >= 2) {
                message(player, "full");
                return;
            }
            SELECTED.put(player.getUUID(), new Selection(cart.getUUID(), level));
            cart.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
            message(player, "selected");
            return;
        }
        if (selected.cart().equals(cart.getUUID())) {
            message(player, "cancelled");
            return;
        }
        if (!(level.getEntity(selected.cart()) instanceof AbstractMinecart first)
                || !first.isAlive()) {
            message(player, "missing");
            return;
        }
        if (links.linked(first.getUUID(), cart.getUUID())) {
            message(player, "already_linked");
            return;
        }
        if (BetterMinecartsLinks.hasLink(first) || first.distanceToSqr(cart) > 16.0) {
            message(player, "too_far");
            return;
        }
        switch (links.link(
                first, cart, player.getAbilities().instabuild ? null : chain.getItem())) {
            case LINKED -> {
                chain.consume(1, player);
                links.refresh(level, first.getUUID());
                links.refresh(level, cart.getUUID());
                cart.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
                message(player, "linked");
            }
            case FULL -> message(player, "full");
            case ENGINE_LIMIT -> message(player, "engine_limit");
            case END_OCCUPIED -> message(player, "end_occupied");
            case REAR_ONLY -> message(player, "rear_only");
            case CYCLE ->
                    player.displayClientMessage(
                            Component.translatable(
                                    "item.spelunkers_charm.cart_coupling_hook.cycle"),
                            true);
        }
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(
                Component.translatable("message.spelunkers_charm.cart_coupler." + key), true);
    }

    /** Stores the first cart selected for coupling. */
    private record Selection(UUID cart, ServerLevel level) {}
}
