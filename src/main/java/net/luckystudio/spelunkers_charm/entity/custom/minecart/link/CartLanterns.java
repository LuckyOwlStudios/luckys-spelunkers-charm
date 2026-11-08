package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.init.ModAttachmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lanterns hung on minecarts. Right-click any minecart with a lantern or soul lantern to hang it,
 * use shears to take it off. The cart lights its surroundings with an invisible light block that
 * follows it, and with LambDynamicLights installed it also glows smoothly as a dynamic light.
 */
public final class CartLanterns {

    /** Where each cart last put its light block. Server side only. */
    private static final Map<AbstractMinecart, BlockPos> LIGHTS = new WeakHashMap<>();

    private CartLanterns() {
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
        if (lantern == CartLantern.NONE || getLantern(cart) != CartLantern.NONE) return false;
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
        removeLight(cart);
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
        removeLight(from);
        from.removeData(ModAttachmentTypes.CART_LANTERN);
        to.setData(ModAttachmentTypes.CART_LANTERN, lantern);
    }

    /** A broken cart drops its lantern and puts out its light. */
    public static void onCartRemoved(AbstractMinecart cart, boolean dropLantern) {
        removeLight(cart);
        CartLantern lantern = getLantern(cart);
        if (lantern != CartLantern.NONE && dropLantern
                && cart.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            cart.spawnAtLocation(lantern.toStack());
        }
    }

    /** Moves the cart's light block along with it. Server side, every tick. */
    public static void tick(AbstractMinecart cart) {
        CartLantern lantern = getLantern(cart);
        if (lantern == CartLantern.NONE) {
            if (LIGHTS.containsKey(cart)) removeLight(cart);
            return;
        }

        Level level = cart.level();
        // The cart's own block is the rail, so the light goes in the block above it.
        BlockPos target = cart.blockPosition().above();
        BlockPos current = LIGHTS.get(cart);
        if (target.equals(current)) return;

        removeLight(cart);
        BlockState state = level.getBlockState(target);
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lantern.lightLevel());
        if (state.isAir()) {
            level.setBlock(target, light, Block.UPDATE_ALL);
            LIGHTS.put(cart, target);
        } else if (state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == lantern.lightLevel()) {
            // Left over from before the chunk was saved, adopt it.
            LIGHTS.put(cart, target);
        }
    }

    /** The cart was unloaded with its chunk: keep the light block (it's saved too), just stop tracking it. */
    public static void forget(AbstractMinecart cart) {
        LIGHTS.remove(cart);
    }

    private static void removeLight(AbstractMinecart cart) {
        BlockPos pos = LIGHTS.remove(cart);
        if (pos == null) return;
        Level level = cart.level();
        if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
