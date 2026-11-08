package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLinking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Couples minecarts together. Click one cart, then another, to link them.
 * Shift-click a cart to remove its couplings. The item isn't used up.
 * The clicks are handled in MinecartEvents, since minecarts aren't living entities.
 */
public class CartCouplerItem extends Item {

    /** The first cart each player clicked, waiting for the second. Server side only. */
    private static final Map<UUID, UUID> SELECTED = new HashMap<>();

    public CartCouplerItem(Properties properties) {
        super(properties);
    }

    /** Server side. */
    public static void useOnCart(Player player, AbstractMinecart cart) {
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
            case LINKED -> message(player, "linked");
            case ALREADY_LINKED -> message(player, "already_linked");
            case TOO_FAR -> message(player, "too_far");
            case FULL -> message(player, "full");
            case SAME_CART -> message(player, "cancelled");
        }
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.spelunkers_charm.cart_coupler." + key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.cart_coupler").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.cart_coupler.unlink").withStyle(ChatFormatting.GRAY));
    }
}
