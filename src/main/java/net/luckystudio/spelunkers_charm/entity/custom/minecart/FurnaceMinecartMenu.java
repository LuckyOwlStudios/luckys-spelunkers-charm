package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.blaster.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A screen for the vanilla furnace minecart that only shows how much fuel it has left. No slots. */
public class FurnaceMinecartMenu extends AbstractContainerMenu {

    /** The most fuel a furnace minecart can hold, in ticks. */
    public static final int MAX_FUEL = 32000;

    @Nullable
    private final MinecartFurnace cart;
    private final ContainerData data;

    // Client side
    public FurnaceMinecartMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(1));
    }

    // Server side
    public FurnaceMinecartMenu(int containerId, Inventory playerInventory, @Nullable MinecartFurnace cart, ContainerData data) {
        super(ModMenuTypes.FURNACE_MINECART_MENU.get(), containerId);
        checkContainerDataCount(data, 1);
        this.cart = cart;
        this.data = data;
        this.addDataSlots(data);
    }

    /** For the server: reads the cart's remaining fuel. */
    public static ContainerData dataFor(MinecartFurnace cart) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return Math.min(cart.fuel, MAX_FUEL);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    /** Remaining fuel in ticks. */
    public int getFuel() {
        return this.data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.cart == null || (!this.cart.isRemoved() && player.distanceToSqr(this.cart) < 64.0);
    }
}
