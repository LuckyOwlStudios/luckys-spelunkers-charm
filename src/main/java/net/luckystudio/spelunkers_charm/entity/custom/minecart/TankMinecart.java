// Tank minecart.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.tank.TankStorage;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class TankMinecart extends AbstractMinecart {
    private static final EntityDataAccessor<CompoundTag> DISPLAY_FLUID =
            SynchedEntityData.defineId(TankMinecart.class, EntityDataSerializers.COMPOUND_TAG);
    private final TankStorage storage = new TankStorage(this::syncFluid);

    public TankMinecart(EntityType<?> type, Level level) {
        super(type, level);
    }

    TankMinecart(Minecart original, ItemStack tank) {
        super(
                ModEntityType.TANK_MINECART.get(),
                original.level(),
                original.getX(),
                original.getY(),
                original.getZ());
        var contents = tank.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null) {
            storage.readFromNBT(registryAccess(), contents.copyTag());
            syncFluid();
        }
    }

    public TankStorage storage() {
        return storage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISPLAY_FLUID, new CompoundTag());
    }

    private void syncFluid() {
        if (!level().isClientSide())
            entityData.set(DISPLAY_FLUID, storage.snapshot(registryAccess()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide() && DISPLAY_FLUID.equals(key)) {
            storage.readFromNBT(registryAccess(), entityData.get(DISPLAY_FLUID));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Tank", storage.snapshot(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        storage.readFromNBT(registryAccess(), tag.getCompound("Tank"));
        syncFluid();
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.TANK.get().defaultBlockState();
    }

    @Override
    public Type getMinecartType() {
        return Type.RIDEABLE;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()) {
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            Minecart empty = new Minecart(level(), getX(), getY(), getZ());
            if (!MinecartCouplings.replaceEntity(this, empty)) return InteractionResult.FAIL;
            player.getInventory().placeItemBackInInventory(packedTank());
            return InteractionResult.SUCCESS;
        }
        if (FluidUtil.getFluidHandler(held).isEmpty()) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        return FluidUtil.interactWithFluidHandler(player, hand, storage)
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }

    private ItemStack packedTank() {
        ItemStack tank = ModBlocks.TANK.toStack();
        if (!storage.getFluid().isEmpty())
            tank.set(ModDataComponents.TANK_CONTENTS.get(), storage.pack(registryAccess()));
        return tank;
    }

    @Override
    public void destroy(Item dropItem) {
        if (!level().isClientSide()
                && level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(ModBlocks.TANK.toStack());
        }
        super.destroy(dropItem);
    }
}
