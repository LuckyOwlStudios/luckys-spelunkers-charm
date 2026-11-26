// Locomotive fuel.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class LocomotiveFuel {
    public static final int SLOTS = 3;
    private final LocomotiveMinecart cart;
    private final int[] remaining = new int[SLOTS];
    private final int[] duration = new int[SLOTS];
    private int enabledMask = 7;

    public LocomotiveFuel(LocomotiveMinecart cart) {
        this.cart = cart;
    }

    public boolean enabled(int slot) {
        return slot >= 0 && slot < SLOTS && (enabledMask & (1 << slot)) != 0;
    }

    public int enabledMask() {
        return enabledMask;
    }

    public int remaining(int slot) {
        return remaining[slot];
    }

    public int percent(int slot) {
        return duration[slot] == 0 ? 0 : (int) (remaining[slot] * 100L / duration[slot]);
    }

    public void toggle(int slot) {
        if (slot >= 0 && slot < SLOTS) enabledMask ^= 1 << slot;
    }

    public int active() {
        int result = 0;
        for (int slot = 0; slot < SLOTS; slot++) if (enabled(slot) && remaining[slot] > 0) result++;
        return result;
    }

    public void tick(boolean paused) {
        if (paused) return;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!enabled(slot)) continue;
            if (remaining[slot] <= 0) consume(slot);
            if (remaining[slot] > 0) remaining[slot]--;
        }
    }

    private void consume(int slot) {
        ItemStack stack = cart.getItem(slot);
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        if (stack.isEmpty() || burn <= 0) return;
        remaining[slot] =
                duration[slot] =
                        (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        burn * (long) LocomotiveMinecart.FUEL_MULTIPLIER);
        if (stack.getCount() == 1 && stack.hasCraftingRemainingItem())
            cart.setItem(slot, stack.getCraftingRemainingItem());
        else stack.shrink(1);
        cart.setChanged();
    }

    public void save(CompoundTag tag) {
        tag.putInt("FuelFormat", 2);
        tag.putInt("EnabledBurners", enabledMask);
        ListTag burners = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            CompoundTag burner = new CompoundTag();
            burner.putInt("Remaining", remaining[slot]);
            burner.putInt("Duration", duration[slot]);
            burners.add(burner);
        }
        tag.put("Burners", burners);
    }

    public void load(CompoundTag tag) {
        enabledMask = tag.contains("EnabledBurners") ? tag.getInt("EnabledBurners") & 7 : 7;
        ListTag burners = tag.getList("Burners", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < SLOTS; slot++) {
            CompoundTag burner =
                    slot < burners.size() ? burners.getCompound(slot) : new CompoundTag();
            remaining[slot] = Math.max(0, burner.getInt("Remaining"));
            duration[slot] = Math.max(remaining[slot], burner.getInt("Duration"));
        }
        if (burners.isEmpty() && tag.contains("BurnTime")) {
            remaining[0] = Math.max(0, tag.getInt("BurnTime"));
            duration[0] = Math.max(remaining[0], tag.getInt("BurnDuration"));
        }
        if (tag.getInt("FuelFormat") < 2)
            for (int slot = 0; slot < SLOTS; slot++) {
                remaining[slot] /= 2;
                duration[slot] /= 2;
            }
    }
}
