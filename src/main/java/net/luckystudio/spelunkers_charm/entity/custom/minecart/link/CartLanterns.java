package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.init.ModAttachmentTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.ModList;

/**
 * Lanterns hung on minecarts. Right-click any minecart with a lantern or soul lantern to hang it,
 * use shears to take it off. The light comes from LambDynamicLights, so lanterns can only be hung
 * while that mod is installed (shears still take one off without it).
 */
public final class CartLanterns {

    /** The mod that makes the lantern shine. */
    private static final String DYNAMIC_LIGHTS_MOD_ID = "lambdynlights";

    private CartLanterns() {
    }

    /** Whether LambDynamicLights is installed. Lanterns can only be hung on carts when it is. */
    public static boolean isEnabled() {
        return ModList.get().isLoaded(DYNAMIC_LIGHTS_MOD_ID);
    }

    public static CartLantern getLantern(AbstractMinecart cart) {
        if (cart.level().isClientSide()) {
            return ClientCartData.getLantern(cart.getId());
        }
        return cart.hasData(ModAttachmentTypes.CART_LANTERN) ? cart.getData(ModAttachmentTypes.CART_LANTERN) : CartLantern.NONE;
    }

    private static void setLantern(AbstractMinecart cart, CartLantern lantern) {
        if (lantern == CartLantern.NONE) {
            cart.removeData(ModAttachmentTypes.CART_LANTERN);
        } else {
            cart.setData(ModAttachmentTypes.CART_LANTERN, lantern);
        }
        CartLinking.sync(cart);
    }

    /** Server side. Returns true if the lantern was hung on the cart. */
    public static boolean hang(AbstractMinecart cart, ItemStack stack, Player player) {
        CartLantern lantern = CartLantern.fromStack(stack);
        if (!isEnabled() || lantern == CartLantern.NONE || getLantern(cart) != CartLantern.NONE) return false;
        setLantern(cart, lantern);
        stack.consume(1, player);
        cart.playSound(SoundEvents.LANTERN_PLACE, 1.0F, 1.0F);
        return true;
    }

    /** Server side. Takes the lantern off and gives it to the player. Returns true if there was one. */
    public static boolean takeOff(AbstractMinecart cart, Player player) {
        CartLantern lantern = getLantern(cart);
        if (lantern == CartLantern.NONE) return false;
        setLantern(cart, CartLantern.NONE);
        cart.playSound(SoundEvents.LANTERN_BREAK, 1.0F, 1.0F);
        ItemStack stack = lantern.toStack();
        if (!player.addItem(stack)) {
            cart.spawnAtLocation(stack, 0.5F);
        }
        return true;
    }

    /** Moves the lantern to a replacement cart, for when a minecart is turned into another kind of cart. */
    public static void transfer(AbstractMinecart from, AbstractMinecart to) {
        CartLantern lantern = getLantern(from);
        if (lantern == CartLantern.NONE) return;
        from.removeData(ModAttachmentTypes.CART_LANTERN);
        to.setData(ModAttachmentTypes.CART_LANTERN, lantern);
    }

    /** A broken cart drops its lantern. */
    public static void onCartRemoved(AbstractMinecart cart, boolean dropLantern) {
        CartLantern lantern = getLantern(cart);
        if (lantern != CartLantern.NONE && dropLantern
                && cart.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            cart.spawnAtLocation(lantern.toStack());
        }
    }
}
