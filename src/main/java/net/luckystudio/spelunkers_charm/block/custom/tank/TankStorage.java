// Tank storage.
package net.luckystudio.spelunkers_charm.block.custom.tank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class TankStorage extends FluidTank {
    public static final int CAPACITY = 8 * FluidType.BUCKET_VOLUME;
    private final Runnable onChange;

    public TankStorage(Runnable onChange) {
        super(CAPACITY);
        this.onChange = onChange;
    }

    @Override
    protected void onContentsChanged() {
        onChange.run();
    }

    public CompoundTag snapshot(HolderLookup.Provider registries) {
        return writeToNBT(registries, new CompoundTag());
    }

    public CustomData pack(HolderLookup.Provider registries) {
        return CustomData.of(snapshot(registries));
    }
}
