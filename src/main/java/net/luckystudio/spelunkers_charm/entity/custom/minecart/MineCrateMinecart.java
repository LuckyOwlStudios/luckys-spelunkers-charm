// Mine crate minecart.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.minecrate.CrateStorage;
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

public final class MineCrateMinecart extends AbstractMinecart {
    private static final EntityDataAccessor<ItemStack> DISPLAY_ITEM =
            SynchedEntityData.defineId(MineCrateMinecart.class, EntityDataSerializers.ITEM_STACK);
    private final CrateStorage storage = new CrateStorage(this::updateDisplay);

    public MineCrateMinecart(EntityType<?> type, Level level) {
        super(type, level);
    }

    MineCrateMinecart(Minecart original, ItemStack crate) {
        super(
                ModEntityType.MINE_CRATE_MINECART.get(),
                original.level(),
                original.getX(),
                original.getY(),
                original.getZ());
        var packed = crate.get(ModDataComponents.CRATE_CONTENTS.get());
        if (packed != null) {
            storage.load(packed.copyTag(), registryAccess());
            updateDisplay();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISPLAY_ITEM, ItemStack.EMPTY);
    }

    private void updateDisplay() {
        ItemStack item = storage.displayItem();
        if (!ItemStack.matches(item, displayItem())) entityData.set(DISPLAY_ITEM, item);
        setCustomName(CargoCountLabel.forAmount(storage.count()));
        if (storage.count() == 0) setCustomNameVisible(false);
    }

    public ItemStack displayItem() {
        return entityData.get(DISPLAY_ITEM);
    }

    public CrateStorage storage() {
        return storage;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        storage.save(tag, registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        storage.load(tag, registryAccess());
        updateDisplay();
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.MINE_CRATE.get().defaultBlockState();
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
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (held.isEmpty() && player.isSecondaryUseActive()) {
            Minecart empty = new Minecart(level(), getX(), getY(), getZ());
            if (!MinecartCouplings.replaceEntity(this, empty)) return InteractionResult.FAIL;
            CrateStorage.give(player, packedCrate());
            return InteractionResult.SUCCESS;
        }
        return storage.use(player, held, blockPosition())
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    private ItemStack packedCrate() {
        ItemStack crate = ModBlocks.MINE_CRATE.toStack();
        if (!storage.displayItem().isEmpty()) {
            crate.set(ModDataComponents.CRATE_CONTENTS.get(), storage.pack(registryAccess()));
        }
        return crate;
    }

    @Override
    public void destroy(Item dropItem) {
        if (!level().isClientSide()
                && level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(packedCrate());
        }
        super.destroy(dropItem);
    }
}
