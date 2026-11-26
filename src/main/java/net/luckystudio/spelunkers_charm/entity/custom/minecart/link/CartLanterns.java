// Cart lanterns.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModAttachmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Lanterns hung on minecarts. Right-click any minecart with a lantern or soul lantern to hang it,
 * use shears to take it off. The cart lights its surroundings with an invisible light block that
 * follows it, and with LambDynamicLights installed it also glows smoothly as a dynamic light.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID)
public final class CartLanterns {

    private CartLanterns() {}

    public static CartLantern getLantern(AbstractMinecart cart) {
        if (cart.level().isClientSide()) {
            return ((MinecartLanternVisuals) cart).spelunkersCharm$getLantern();
        }
        return cart.hasData(ModAttachmentTypes.CART_LANTERN)
                ? cart.getData(ModAttachmentTypes.CART_LANTERN)
                : CartLantern.NONE;
    }

    private static void setLantern(AbstractMinecart cart, CartLantern lantern) {
        if (lantern == CartLantern.NONE) {
            cart.removeData(ModAttachmentTypes.CART_LANTERN);
        } else {
            cart.setData(ModAttachmentTypes.CART_LANTERN, lantern);
        }
        ((MinecartLanternVisuals) cart).spelunkersCharm$setLantern(lantern);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof AbstractMinecart cart) {
            ((MinecartLanternVisuals) cart).spelunkersCharm$setLantern(getLantern(cart));
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractMinecart cart
                && !cart.level().isClientSide()
                && !cart.isRemoved()) {
            tick(cart);
        }
    }

    @SubscribeEvent
    public static void onRemoved(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof AbstractMinecart cart) {
            var reason = cart.getRemovalReason();
            if (reason != null
                    && (reason.shouldDestroy()
                            || reason == Entity.RemovalReason.CHANGED_DIMENSION)) {
                onCartRemoved(cart, reason == Entity.RemovalReason.KILLED);
            }
        }
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

    /**
     * Server side. Takes the lantern off and gives it to the player. Returns true if there was one.
     */
    public static boolean takeOff(AbstractMinecart cart, Player player) {
        CartLantern lantern = getLantern(cart);
        if (lantern == CartLantern.NONE) return false;
        removeLight(cart);
        setLantern(cart, CartLantern.NONE);
        cart.playSound(SoundEvents.LANTERN_BREAK, 1.0F, 1.0F);
        ItemStack stack = lantern.toStack();
        if (!player.addItem(stack)) {
            cart.spawnAtLocation(stack, 0.5F);
        }
        return true;
    }

    /**
     * Moves the lantern to a replacement cart, for when a minecart is turned into another kind of
     * cart.
     */
    public static void transfer(AbstractMinecart from, AbstractMinecart to) {
        CartLantern lantern = getLantern(from);
        if (lantern == CartLantern.NONE) return;
        removeLight(from);
        setLantern(from, CartLantern.NONE);
        setLantern(to, lantern);
    }

    /** A broken cart drops its lantern and puts out its light. */
    private static void onCartRemoved(AbstractMinecart cart, boolean dropLantern) {
        removeLight(cart);
        CartLantern lantern = getLantern(cart);
        if (lantern != CartLantern.NONE
                && dropLantern
                && cart.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            cart.spawnAtLocation(lantern.toStack());
        }
    }

    /** Moves the cart's light block along with it. Server side, every tick. */
    private static void tick(AbstractMinecart cart) {
        CartLantern lantern = getLantern(cart);
        if (lantern == CartLantern.NONE) {
            removeLight(cart);
            return;
        }

        Level level = cart.level();
        // The cart's own block is the rail, so the light goes in the block above it.
        BlockPos target = cart.blockPosition().above();
        GlobalPos location = GlobalPos.of(level.dimension(), target);
        BlockState light =
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lantern.lightLevel());
        if (cart.hasData(ModAttachmentTypes.CART_LANTERN_LIGHT)
                && location.equals(cart.getData(ModAttachmentTypes.CART_LANTERN_LIGHT))
                && level.getBlockState(target) == light) return;

        removeLight(cart);
        if (level.isLoaded(target)
                && level.getBlockState(target).isAir()
                && level.setBlock(target, light, Block.UPDATE_ALL)) {
            cart.setData(ModAttachmentTypes.CART_LANTERN_LIGHT, location);
        }
    }

    private static void removeLight(AbstractMinecart cart) {
        if (!cart.hasData(ModAttachmentTypes.CART_LANTERN_LIGHT)) return;
        GlobalPos previous = cart.getData(ModAttachmentTypes.CART_LANTERN_LIGHT);
        cart.removeData(ModAttachmentTypes.CART_LANTERN_LIGHT);
        Level level = cart.level();
        if (!previous.dimension().equals(level.dimension()) || !level.isLoaded(previous.pos()))
            return;
        BlockState state = level.getBlockState(previous.pos());
        if (state.is(Blocks.LIGHT)
                && state.getValue(LightBlock.LEVEL) == getLantern(cart).lightLevel()) {
            level.setBlock(previous.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
