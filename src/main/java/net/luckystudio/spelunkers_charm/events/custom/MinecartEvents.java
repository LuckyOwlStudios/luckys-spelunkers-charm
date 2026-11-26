// Minecart events.
package net.luckystudio.spelunkers_charm.events.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartBlockLoading;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MinecartEvents {

    @SubscribeEvent
    public static void cancelCoupling(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        var player = event.getEntity();
        if (!player.isSecondaryUseActive()
                || !(stack.getItem() instanceof CartCouplingHook)
                        && !stack.is(
                                net.luckystudio
                                        .spelunkers_charm
                                        .datagen
                                        .itemTags
                                        .ModItemTags
                                        .CART_COUPLERS)) return;
        if (!player.level().isClientSide()) {
            CartCouplingHook.clearSelection(stack);
            net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartCoupler.clear(player);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "item.spelunkers_charm.cart_coupling_hook.cleared"),
                    true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void resetHookOnGrindstone(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isSecondaryUseActive()
                || !(event.getItemStack().getItem() instanceof CartCouplingHook)
                || !event.getLevel()
                        .getBlockState(event.getPos())
                        .is(net.minecraft.world.level.block.Blocks.GRINDSTONE)) return;
        var context =
                new net.minecraft.world.item.context.UseOnContext(
                        event.getEntity(), event.getHand(), event.getHitVec());
        event.setCancellationResult(event.getItemStack().getItem().useOn(context));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clearCouplingOnLogout(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartCoupler.clear(
                event.getEntity());
        for (var stack : event.getEntity().getInventory().items)
            if (stack.getItem() instanceof CartCouplingHook) CartCouplingHook.clearSelection(stack);
        for (var stack : event.getEntity().getInventory().offhand)
            if (stack.getItem() instanceof CartCouplingHook) CartCouplingHook.clearSelection(stack);
    }

    @SubscribeEvent
    public static void onMinecartInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractMinecart cart)) return;
        Player player = event.getEntity();
        Level level = player.level();
        InteractionHand hand = event.getHand();
        ItemStack heldItem = event.getItemStack();
        if (useAccessory(cart, player, hand, heldItem)) {
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            event.setCanceled(true);
            return;
        }
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return;

        if (cart instanceof MinecartTNT minecartTNT) {
            minecartTNT.primeFuse();
            player.swing(hand, true);
        }
        if (player.isCrouching()
                && cart instanceof Minecart
                && MinecartBlockLoading.load(cart, heldItem, player)) {
            player.swing(hand, true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static boolean useAccessory(
            AbstractMinecart cart, Player player, InteractionHand hand, ItemStack stack) {
        boolean clientSide = cart.level().isClientSide();
        if (stack.getItem() instanceof CartCouplingHook hook) {
            if (!clientSide) hook.useOnCart(stack, player, cart);
            return true;
        }
        CartLantern lantern = CartLanterns.getLantern(cart);
        if (stack.is(Items.SHEARS) && lantern != CartLantern.NONE) {
            if (!clientSide && CartLanterns.takeOff(cart, player)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return true;
        }
        if (lantern == CartLantern.NONE
                && CartLantern.fromStack(stack) != CartLantern.NONE
                && !player.isSecondaryUseActive()
                && !(cart instanceof BlockMinecart)) {
            if (!clientSide) CartLanterns.hang(cart, stack, player);
            return true;
        }
        return false;
    }

    /** An unpowered directional rail stops a cart even if the cart moved on past it during its own tick. */
    @SubscribeEvent
    public static void onCartTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractMinecart cart) || cart.level().isClientSide())
            return;
        if (!net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings.isEngine(
                cart)) return;
        if (net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartSpeedRules.managed(
                cart)) return;
        BlockPos pos = cart.blockPosition();
        BlockState state = cart.level().getBlockState(pos);
        if (!(state.getBlock() instanceof DirectionalRailBlock)) {
            pos = pos.below();
            state = cart.level().getBlockState(pos);
        }
        if (state.getBlock() instanceof DirectionalRailBlock
                && !state.getValue(DirectionalRailBlock.POWERED)) {
            DirectionalRailBlock.holdCart(state, pos, cart);
        }
    }
}
