// Tank block entity.
package net.luckystudio.spelunkers_charm.block.custom.tank;

import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TankBlockEntity extends BlockEntity {
    private final TankStorage storage = new TankStorage(this::contentsChanged);

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityType.TANK.get(), pos, state);
    }

    public TankStorage storage() {
        return storage;
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", storage.snapshot(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (level != null && !storage.getFluid().isEmpty()) {
            components.set(
                    ModDataComponents.TANK_CONTENTS.get(), storage.pack(level.registryAccess()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        var packed = components.get(ModDataComponents.TANK_CONTENTS.get());
        if (level != null && packed != null) {
            storage.readFromNBT(level.registryAccess(), packed.copyTag());
            contentsChanged();
        }
    }
}
