// Locomotive menu.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.blaster.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LocomotiveMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT_X = 55;
    public static final int FUEL_SLOT_Y = 34;
    public static final int FUEL_SLOT_SPACING = 25;
    public static final int EMERGENCY_BUTTON = 3;
    public static final int BACKWARD_BUTTON = 4;
    public static final int STOP_BUTTON = 5;
    public static final int FORWARD_BUTTON = 6;
    private static final int PLAYER_START = 3;
    private static final int PLAYER_END = 39;
    private final Container container;
    private final ContainerData data;

    public LocomotiveMenu(int id, Inventory inventory) {
        this(
                id,
                inventory,
                new SimpleContainer(3),
                new SimpleContainerData(LocomotiveMinecart.MENU_DATA_SIZE));
    }

    public LocomotiveMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.LOCOMOTIVE_MENU.get(), id);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, LocomotiveMinecart.MENU_DATA_SIZE);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);
        for (int slot = 0; slot < 3; slot++) {
            final int burner = slot;
            addSlot(
                    new Slot(container, slot, FUEL_SLOT_X + slot * FUEL_SLOT_SPACING, FUEL_SLOT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return burnerEnabled(burner) && LocomotiveMinecart.isFuel(stack);
                        }
                    });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int getFuelPercent() {
        return getFuelPercent(0);
    }

    public int getFuelPercent(int slot) {
        return data.get(slot);
    }

    public boolean burnerEnabled(int slot) {
        return (data.get(3) & (1 << slot)) != 0;
    }

    public boolean isReversed() {
        return data.get(4) != 0;
    }

    public boolean emergencyBrake() {
        return data.get(5) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.containerMenu != this
                || !stillValid(player)
                || !(container instanceof LocomotiveMinecart engine)) return false;
        if (id >= 0 && id < 3) engine.toggleBurner(id);
        else if (id == EMERGENCY_BUTTON) engine.toggleEmergencyBrake();
        else if (id == BACKWARD_BUTTON) engine.selectDirection(true);
        else if (id == STOP_BUTTON) {
            if (!engine.emergencyBrake()) engine.toggleEmergencyBrake();
        } else if (id == FORWARD_BUTTON) engine.selectDirection(false);
        else return false;
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (LocomotiveMinecart.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, PLAYER_START, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
