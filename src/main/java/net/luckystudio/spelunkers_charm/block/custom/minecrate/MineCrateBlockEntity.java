// Mine crate block entity.
package net.luckystudio.spelunkers_charm.block.custom.minecrate;

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

public final class MineCrateBlockEntity extends BlockEntity {
    private final CrateStorage storage = new CrateStorage(this::contentsChanged);

    public MineCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityType.MINE_CRATE.get(), pos, state);
    }

    public CrateStorage storage() {
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
        storage.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.load(tag, registries);
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
        if (level != null && !storage.displayItem().isEmpty()) {
            components.set(
                    ModDataComponents.CRATE_CONTENTS.get(), storage.pack(level.registryAccess()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        var packed = components.get(ModDataComponents.CRATE_CONTENTS.get());
        if (level != null && packed != null) {
            storage.load(packed.copyTag(), level.registryAccess());
            contentsChanged();
        }
    }
}
