// Minecart coupling tool.
package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.BetterMinecartsLinks;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

public final class CartCouplingHook extends Item {
    private static final String CART = "SelectedCart";
    private static final String DIMENSION = "SelectedDimension";
    private static final String SESSION = "SelectedSession";
    private static final java.util.Map<net.minecraft.server.MinecraftServer, UUID> SESSIONS =
            new java.util.WeakHashMap<>();
    private static final double MAX_REACH_SQUARED = 16;
    private static final String MESSAGE = "item.spelunkers_charm.cart_coupling_hook.";

    public CartCouplingHook(Properties properties) {
        super(properties);
    }

    public void useOnCart(ItemStack stack, Player player, AbstractMinecart cart) {
        if (!(cart.level() instanceof ServerLevel level) || !cart.isAlive()) return;
        String dimension = level.dimension().location().toString();
        CompoundTag data =
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID selected =
                data.hasUUID(CART) && dimension.equals(data.getString(DIMENSION))
                        ? data.getUUID(CART)
                        : null;

        if (cart.getUUID().equals(selected)) {
            clearSelection(stack);
            message(player, "cleared");
            return;
        }

        MinecartCouplings links = MinecartCouplings.get(level);
        if (selected != null && links.unlinkAndDrop(level, selected, cart.getUUID())) {
            finish(stack, player, cart, level, links, selected, "unlinked", 0.8F);
            return;
        }
        if (BetterMinecartsLinks.hasLink(cart)) {
            message(player, "foreign");
            return;
        }
        if (selected == null
                || !(level.getEntity(selected) instanceof AbstractMinecart first)
                || !first.isAlive()) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> {
                        tag.putUUID(CART, cart.getUUID());
                        tag.putString(DIMENSION, dimension);
                        tag.putUUID(SESSION, session(level));
                    });
            message(player, "selected");
            return;
        }
        if (BetterMinecartsLinks.hasLink(first)) {
            clearSelection(stack);
            message(player, "foreign");
            return;
        }
        if (cart.distanceToSqr(first) > MAX_REACH_SQUARED
                || net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplingSafety
                                .length(first, cart)
                        > 2) {
            message(player, "too_far");
            return;
        }
        ItemStack chain = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var candidate = player.getInventory().getItem(slot);
            if (candidate.is(net.minecraft.world.item.Items.CHAIN)) {
                chain = candidate;
                break;
            }
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative && chain.isEmpty()) {
            message(player, "missing_chain");
            return;
        }
        switch (links.link(first, cart, net.minecraft.world.item.Items.CHAIN)) {
            case LINKED -> {
                if (!creative) {
                    chain.shrink(1);
                    player.getInventory().setChanged();
                }
                finish(stack, player, cart, level, links, selected, "linked", 1.15F);
            }
            case FULL -> message(player, "full");
            case ENGINE_LIMIT -> message(player, "engine_limit");
            case END_OCCUPIED -> message(player, "end_occupied");
            case REAR_ONLY -> message(player, "rear_only");
            case CYCLE -> message(player, "cycle");
        }
    }

    private static void finish(
            ItemStack stack,
            Player player,
            AbstractMinecart cart,
            ServerLevel level,
            MinecartCouplings links,
            UUID selected,
            String result,
            float pitch) {
        clearSelection(stack);
        links.refresh(level, selected);
        links.refresh(level, cart.getUUID());
        cart.playSound(SoundEvents.CHAIN_PLACE, 0.7F, pitch);
        message(player, result);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(
            net.minecraft.world.item.context.UseOnContext context) {
        var player = context.getPlayer();
        if (player == null
                || !player.isSecondaryUseActive()
                || !context.getLevel()
                        .getBlockState(context.getClickedPos())
                        .is(net.minecraft.world.level.block.Blocks.GRINDSTONE))
            return net.minecraft.world.InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            clearSelection(context.getItemInHand());
            message(player, "cleared");
            context.getLevel()
                    .playSound(
                            null,
                            context.getClickedPos(),
                            SoundEvents.GRINDSTONE_USE,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            .7f,
                            1f);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(
                context.getLevel().isClientSide());
    }

    public static void clearSelection(ItemStack stack) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.remove(CART);
                    tag.remove(DIMENSION);
                    tag.remove(SESSION);
                });
    }

    public static UUID selectedCart(ItemStack stack, net.minecraft.world.level.Level level) {
        if (!(stack.getItem() instanceof CartCouplingHook)) return null;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.hasUUID(CART)
                        && level.dimension().location().toString().equals(data.getString(DIMENSION))
                ? data.getUUID(CART)
                : null;
    }

    private static UUID session(ServerLevel level) {
        return SESSIONS.computeIfAbsent(level.getServer(), ignored -> UUID.randomUUID());
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            net.minecraft.world.level.Level world,
            net.minecraft.world.entity.Entity entity,
            int slot,
            boolean selected) {
        if (!(world instanceof ServerLevel level)) return;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.hasUUID(CART)
                && (!data.hasUUID(SESSION)
                        || !data.getUUID(SESSION).equals(session(level))
                        || !data.getString(DIMENSION)
                                .equals(level.dimension().location().toString())
                        || !(level.getEntity(data.getUUID(CART)) instanceof AbstractMinecart cart)
                        || !cart.isAlive())) clearSelection(stack);
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable(MESSAGE + key), true);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(MESSAGE + "tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(MESSAGE + "reset_tooltip").withStyle(ChatFormatting.GRAY));
    }
}
