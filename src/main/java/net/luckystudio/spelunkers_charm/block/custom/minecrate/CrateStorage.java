// Crate storage.
package net.luckystudio.spelunkers_charm.block.custom.minecrate;

import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandler;

public final class CrateStorage implements IItemHandler {
    public static final int CAPACITY = 2048;
    private final Runnable onChange;
    private ItemStack contents = ItemStack.EMPTY;

    public CrateStorage(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? contents.copy() : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? CAPACITY : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && !stack.has(ModDataComponents.CRATE_CONTENTS.get());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()
                || !isItemValid(slot, stack)
                || (!contents.isEmpty() && !ItemStack.isSameItemSameComponents(contents, stack)))
            return stack;
        int inserted = Math.min(stack.getCount(), CAPACITY - contents.getCount());
        if (inserted == 0) return stack;
        if (!simulate) {
            contents = stack.copyWithCount(contents.getCount() + inserted);
            onChange.run();
        }
        return stack.copyWithCount(stack.getCount() - inserted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0 || contents.isEmpty()) return ItemStack.EMPTY;
        int removed = Math.min(Math.min(amount, contents.getMaxStackSize()), contents.getCount());
        ItemStack result = contents.copyWithCount(removed);
        if (!simulate) {
            int left = contents.getCount() - removed;
            contents = left == 0 ? ItemStack.EMPTY : contents.copyWithCount(left);
            onChange.run();
        }
        return result;
    }

    public boolean use(Player player, ItemStack held, BlockPos pos) {
        if (held.isEmpty()) {
            ItemStack extracted = extractItem(0, CAPACITY, false);
            if (extracted.isEmpty()) return false;
            give(player, extracted);
            player.level()
                    .playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 1, 1);
            return true;
        }
        ItemStack remainder = insertItem(0, held, false);
        int inserted = held.getCount() - remainder.getCount();
        if (inserted == 0) return false;
        held.consume(inserted, player);
        player.level()
                .playSound(null, pos, ModSoundEvents.LOADED_CART.get(), SoundSource.BLOCKS, 1, 1);
        return true;
    }

    public static void give(Player player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack);
    }

    public ItemStack displayItem() {
        return contents.copyWithCount(1);
    }

    public int count() {
        return contents.getCount();
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        // A zero count keeps the client's empty-crate update from becoming an empty packet.
        tag.putInt("Count", contents.getCount());
        // Keep the serialized item at a legal stack size, independent of the crate's capacity.
        if (!contents.isEmpty()) {
            tag.put("Item", displayItem().save(registries));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack sample = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        int count = Math.clamp(tag.getInt("Count"), 0, CAPACITY);
        contents =
                count > 0 && isItemValid(0, sample) ? sample.copyWithCount(count) : ItemStack.EMPTY;
    }

    public CustomData pack(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        save(tag, registries);
        return CustomData.of(tag);
    }
}
