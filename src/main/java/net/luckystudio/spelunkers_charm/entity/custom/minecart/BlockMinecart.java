package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLinking;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class BlockMinecart extends AbstractMinecart implements Container {

    /** How many blocks the cart counts as full, for comparators and the cargo rail (27 stacks, like a chest). */
    public static final int FULL_AMOUNT = 27 * 64;

    private static final EntityDataAccessor<Integer> BLOCK_AMOUNT = SynchedEntityData.defineId(BlockMinecart.class, EntityDataSerializers.INT);

    public BlockMinecart(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BlockMinecart(Level level, double x, double y, double z) {
        super(ModEntityType.BLOCK_MINECART.get(), level, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLOCK_AMOUNT, 0);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BlockAmount", getBlockAmount());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBlockAmount(tag.getInt("BlockAmount"));
    }

    @Override
    protected @NotNull Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public Type getMinecartType() {
        return Type.RIDEABLE;
    }

    public int getBlockAmount() {
        return this.entityData.get(BLOCK_AMOUNT);
    }

    public void setBlockAmount(int amount) {
        this.entityData.set(BLOCK_AMOUNT, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || this.level().isClientSide) {
            return InteractionResult.PASS;
        }

        // ADDING blocks
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            if (getDisplayBlockState().isAir() || blockItem.getBlock().defaultBlockState() == getDisplayBlockState()) {
                this.setDisplayBlockState(blockItem.getBlock().defaultBlockState());
                this.setBlockAmount(getBlockAmount() + heldItem.getCount());
                heldItem.shrink(heldItem.getCount());
                level().playSound(this, this.blockPosition(),
                        blockItem.getBlock().defaultBlockState().getSoundType(level(), this.blockPosition(), this).getPlaceSound(),
                        player.getSoundSource(), 1.0F, 1.0F);
                updateName();
                return InteractionResult.SUCCESS;
            }
        }

        // REMOVING blocks (when not holding a block)
        else if (getBlockAmount() > 0) {
            int toRemove = Math.min(64, getBlockAmount());
            this.setBlockAmount(getBlockAmount() - toRemove);
            ItemStack dropStack = new ItemStack(getDisplayBlockState().getBlock(), toRemove);
            player.addItem(dropStack);

            // If that was the last of the blocks, clear the block type
            if (getBlockAmount() <= 0) {
                this.setCustomDisplay(false);
                this.setDisplayBlockState(Blocks.AIR.defaultBlockState());
                convertIfEmpty();
            }
            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            updateName();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public void updateName() {
        if (getBlockAmount() > 0) {
            this.setCustomName(Component.literal(getBlockAmount() + "x").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).applyFormat(ChatFormatting.BOLD)));
        } else {
            this.setCustomName(null);
            this.setCustomNameVisible(false);
        }
    }

    /** 0 when empty, up to 15 when full. */
    public int getComparatorLevel() {
        int amount = getBlockAmount();
        if (amount <= 0) return 0;
        return 1 + (int) Math.floor(14.0 * Math.min(amount, FULL_AMOUNT) / FULL_AMOUNT);
    }

    /** Turns the cart back into a plain minecart once its last block is gone. Server side. */
    private void convertIfEmpty() {
        if (getBlockAmount() > 0 || level().isClientSide() || isRemoved()) return;
        Minecart newMinecart = new Minecart(level(), this.getX(), this.getY(), this.getZ());
        newMinecart.setDeltaMovement(this.getDeltaMovement());
        newMinecart.setYRot(this.getYRot());
        newMinecart.setXRot(this.getXRot());
        level().addFreshEntity(newMinecart);
        CartLinking.transferLinks(this, newMinecart);
        this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void tick() {
        super.tick();
        convertIfEmpty();
    }

    // Container: this is what lets hoppers pull blocks out of the cart. It shows up as one slot
    // holding up to a stack of the blocks, and taking from it lowers the cart's block count.

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getBlockAmount() <= 0 || getDisplayBlockState().isAir();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0 || isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(getDisplayBlockState().getBlock(), Math.min(64, getBlockAmount()));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || isEmpty()) return ItemStack.EMPTY;
        int taken = Math.min(Math.min(amount, 64), getBlockAmount());
        ItemStack result = new ItemStack(getDisplayBlockState().getBlock(), taken);
        setBlockAmount(getBlockAmount() - taken);
        updateName();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 64);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Hoppers can't put blocks in.
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void kill() {
        // Drop the stored blocks as an ItemStack if any
        if (getBlockAmount() > 0 && !getDisplayBlockState().isAir()) {
            ItemStack dropStack = new ItemStack(getDisplayBlockState().getBlock(), getBlockAmount());
            this.spawnAtLocation(dropStack);
        }
        super.kill();
    }

    // Copeid from VehicleEntity but removed the custom name logic
    @Override
    public void destroy(Item dropItem) {
        this.kill();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemstack = new ItemStack(dropItem);
            this.spawnAtLocation(itemstack);
        }
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }
}
