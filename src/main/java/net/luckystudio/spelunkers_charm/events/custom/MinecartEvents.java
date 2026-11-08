package net.luckystudio.spelunkers_charm.events.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.JukeboxMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLinking;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.ClientCartData;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MinecartEvents {

    @SubscribeEvent
    public static InteractionResult onMinecartInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide()) return InteractionResult.PASS;  // Early return for client side

        Entity target = event.getTarget();
        InteractionHand hand = event.getHand();
        if (!hand.equals(InteractionHand.MAIN_HAND)) return InteractionResult.PASS;
        ItemStack heldItem = player.getItemInHand(hand);
        if (!(target instanceof AbstractMinecart abstractMinecart)) return InteractionResult.PASS;
        if (abstractMinecart instanceof MinecartTNT minecartTNT) {
            minecartTNT.primeFuse();
            player.swing(hand, true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.isCrouching()) return InteractionResult.PASS;

        // Handle Minecart Interaction
        if (abstractMinecart instanceof Minecart minecart && heldItem.getItem() instanceof BlockItem blockItem) {
            if (minecartBlocks(blockItem.getBlock())) {
                AbstractMinecart newMinecart = getAbstractMinecart(minecart, blockItem.getBlock(), level);
                // Copy motion and rotation from the original minecart
                newMinecart.setPosRaw(minecart.getX(), minecart.getY(), minecart.getZ());
                newMinecart.setDeltaMovement(minecart.getDeltaMovement());
                newMinecart.setYRot(minecart.getYRot());
                newMinecart.setXRot(minecart.getXRot());

                SoundEvent placeBlockSound = blockItem.getBlock().defaultBlockState().getSoundType(level, BlockPos.containing(minecart.getX(), minecart.getY(), minecart.getZ()), minecart).getPlaceSound();
                minecart.playSound(placeBlockSound, 1.0F, 1.0F);

                // The block goes into the cart, so it's used up
                heldItem.consume(1, player);

                // Add the new minecart, keep its couplings, then remove the old one
                level.addFreshEntity(newMinecart);
                CartLinking.transferLinks(minecart, newMinecart);
                minecart.remove(Entity.RemovalReason.DISCARDED);
                player.swing(hand, true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return InteractionResult.SUCCESS;
            } else {
                BlockMinecart newMinecart = new BlockMinecart(level, minecart.getX(), minecart.getY(), minecart.getZ());
                // Copy motion and rotation from the original minecart
                newMinecart.setPosRaw(minecart.getX(), minecart.getY(), minecart.getZ());
                newMinecart.setDeltaMovement(minecart.getDeltaMovement());
                newMinecart.setYRot(minecart.getYRot());
                newMinecart.setXRot(minecart.getXRot());

                newMinecart.setDisplayBlockState(blockItem.getBlock().defaultBlockState());
                newMinecart.setBlockAmount(heldItem.getCount());
                newMinecart.updateName();
                heldItem.shrink(heldItem.getCount());

                SoundEvent placeBlockSound = blockItem.getBlock().defaultBlockState().getSoundType(level, BlockPos.containing(minecart.getX(), minecart.getY(), minecart.getZ()), minecart).getPlaceSound();
                minecart.playSound(placeBlockSound, 1.0F, 1.0F);

                // Add the new minecart, keep its couplings, then remove the old one
                level.addFreshEntity(newMinecart);
                CartLinking.transferLinks(minecart, newMinecart);
                minecart.remove(Entity.RemovalReason.DISCARDED);
                player.swing(hand, true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean minecartBlocks(Block block) {
        return block == Blocks.HOPPER || block == Blocks.CHEST || block == Blocks.FURNACE || block == Blocks.TNT
                || block == Blocks.BLAST_FURNACE || block == Blocks.JUKEBOX;
    }

    private static AbstractMinecart getAbstractMinecart(Minecart oldMinecart, Block block, Level level) {
        if (block == Blocks.HOPPER) {
            return new MinecartHopper(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.CHEST) {
            return new MinecartChest(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.FURNACE) {
            return new MinecartFurnace(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.TNT) {
            return new MinecartTNT(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.BLAST_FURNACE) {
            return new LocomotiveMinecart(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.JUKEBOX) {
            return new JukeboxMinecart(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        }
        return new Minecart(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
    }

    /** The cart coupler: click two carts to link them, shift-click to unlink. Runs before other cart clicks. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCouplerUse(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModItems.CART_COUPLER.get())) return;
        if (!(event.getTarget() instanceof AbstractMinecart cart)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (!event.getLevel().isClientSide()) {
            CartCouplerItem.useOnCart(event.getEntity(), cart);
        }
    }

    /** Hangs a lantern on any minecart, or takes it off with shears. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLanternUse(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractMinecart cart)) return;
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        boolean clientSide = event.getLevel().isClientSide();

        if (stack.is(Items.SHEARS)) {
            if (CartLanterns.getLantern(cart) == CartLantern.NONE) return;
            if (!clientSide) {
                CartLanterns.takeOff(cart, player);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            }
        } else if (CartLantern.fromStack(stack) != CartLantern.NONE && !player.isSecondaryUseActive()) {
            // Sneaking puts the lantern inside the cart as a block instead, and block carts keep adding their own block.
            if (cart instanceof BlockMinecart) return;
            if (CartLanterns.getLantern(cart) != CartLantern.NONE) return;
            if (!clientSide) {
                CartLanterns.hang(cart, stack, player);
            }
        } else {
            return;
        }
        player.swing(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(clientSide));
    }

    /** Keeps coupled carts together and moves lantern light along with carts. */
    @SubscribeEvent
    public static void onCartTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractMinecart cart && !cart.level().isClientSide()) {
            CartLinking.tick(cart);
            CartLanterns.tick(cart);
        }
    }

    /** A broken cart lets go of the carts it was coupled to. */
    @SubscribeEvent
    public static void onCartRemoved(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractMinecart cart)) return;
        if (event.getLevel().isClientSide()) {
            ClientCartData.remove(cart.getId());
            return;
        }
        Entity.RemovalReason reason = cart.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) {
            CartLinking.onCartRemoved(cart);
            CartLanterns.onCartRemoved(cart, reason == Entity.RemovalReason.KILLED);
        } else {
            CartLanterns.forget(cart);
        }
    }

    /** Players who start seeing a cart learn what it's coupled to. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AbstractMinecart cart && event.getEntity() instanceof ServerPlayer player) {
            CartLinking.syncTo(cart, player);
        }
    }
}
