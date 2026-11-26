// Cart loader menu.
package net.luckystudio.spelunkers_charm.block.custom.cartloader;

import net.luckystudio.spelunkers_charm.block.custom.blaster.ModMenuTypes;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CartLoaderMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public CartLoaderMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(1), ContainerLevelAccess.NULL, () -> {});
    }

    CartLoaderMenu(
            int id,
            Inventory inventory,
            IItemHandler items,
            ContainerLevelAccess access,
            Runnable onSlotChanged) {
        super(ModMenuTypes.CART_LOADER_MENU.get(), id);
        this.access = access;
        addSlot(
                new SlotItemHandler(items, 0, 80, 20) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        onSlotChanged.run();
                    }
                });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 109));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CART_LOADER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved =
                index == 0
                        ? moveItemStackTo(stack, 1, slots.size(), true)
                        : moveItemStackTo(stack, 0, 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
