package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A minecart that burns fuel to drive itself (and any carts coupled to it).
 * Right-click opens the fuel screen, shift-right-click starts or stops it.
 * Made by shift-right-clicking a blast furnace onto a minecart.
 */
public class LocomotiveMinecart extends AbstractMinecartContainer {

    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BURNING = SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.BOOLEAN);

    /** Fuel lasts this many times longer than it does in a furnace. Coal runs for 2 minutes 40 seconds. */
    public static final int FUEL_MULTIPLIER = 2;
    /** Top speed in blocks per tick. The furnace minecart is 0.2, normal rails cap carts at 0.4. */
    public static final float MAX_SPEED = 0.3F;

    private int burnTime;
    private int burnDuration;
    private double xPush;
    private double zPush;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // Remaining fuel as a percentage, kept small so it fits the menu's short data sync.
                case 0 -> burnDuration > 0 ? Math.max(0, burnTime) * 100 / burnDuration : 0;
                case 1 -> isRunning() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public LocomotiveMinecart(EntityType<? extends LocomotiveMinecart> entityType, Level level) {
        super(entityType, level);
    }

    public LocomotiveMinecart(Level level, double x, double y, double z) {
        super(ModEntityType.LOCOMOTIVE.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RUNNING, false);
        builder.define(DATA_BURNING, false);
    }

    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    private void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    public boolean isBurning() {
        return this.entityData.get(DATA_BURNING);
    }

    @Override
    public Type getMinecartType() {
        return Type.FURNACE;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isFuel(stack);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new LocomotiveMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (isRunning()) {
                if (this.burnTime <= 0) {
                    consumeFuel();
                }
                if (this.burnTime > 0) {
                    this.burnTime--;
                } else {
                    stop();
                    this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.0F);
                }
            }
            this.entityData.set(DATA_BURNING, isRunning() && this.burnTime > 0);
        } else if (isBurning() && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.9, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    /** Takes one item from the fuel slot. Returns false if there was nothing to burn. */
    private boolean consumeFuel() {
        ItemStack fuel = this.getItem(0);
        int fuelTime = fuel.getBurnTime(RecipeType.SMELTING);
        if (fuel.isEmpty() || fuelTime <= 0) return false;

        this.burnTime = fuelTime * FUEL_MULTIPLIER;
        this.burnDuration = this.burnTime;
        if (fuel.getCount() == 1 && fuel.hasCraftingRemainingItem()) {
            // Lava buckets leave their bucket behind.
            this.setItem(0, fuel.getCraftingRemainingItem());
        } else {
            fuel.shrink(1);
            this.setChanged();
        }
        return true;
    }

    private void stop() {
        setRunning(false);
        this.xPush = 0.0;
        this.zPush = 0.0;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) {
            // Opens the fuel screen.
            return super.interact(player, hand);
        }
        if (!this.level().isClientSide()) {
            if (isRunning()) {
                stop();
                this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F);
                player.displayClientMessage(Component.translatable("message.spelunkers_charm.locomotive.stopped"), true);
            } else if (this.burnTime > 0 || isFuel(this.getItem(0))) {
                setRunning(true);
                // Drive away from the player, like pushing a furnace minecart.
                this.xPush = this.getX() - player.getX();
                this.zPush = this.getZ() - player.getZ();
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.6F, 0.8F);
                player.displayClientMessage(Component.translatable("message.spelunkers_charm.locomotive.started"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.spelunkers_charm.locomotive.no_fuel"), true);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // Movement, based on the furnace minecart: a constant push in the direction of travel.

    @Override
    protected double getMaxSpeed() {
        return (this.isInWater() ? MAX_SPEED / 2.0F : MAX_SPEED);
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return MAX_SPEED;
    }

    @Override
    protected void moveAlongTrack(BlockPos pos, BlockState state) {
        super.moveAlongTrack(pos, state);
        // Keep the push pointing the way the cart is moving, so it follows curves.
        Vec3 motion = this.getDeltaMovement();
        double motionSqr = motion.horizontalDistanceSqr();
        double pushSqr = this.xPush * this.xPush + this.zPush * this.zPush;
        if (pushSqr > 1.0E-4 && motionSqr > 0.001) {
            double motionLength = Math.sqrt(motionSqr);
            double pushLength = Math.sqrt(pushSqr);
            this.xPush = motion.x / motionLength * pushLength;
            this.zPush = motion.z / motionLength * pushLength;
        }
    }

    @Override
    protected void applyNaturalSlowdown() {
        double pushSqr = this.xPush * this.xPush + this.zPush * this.zPush;
        if (isRunning() && pushSqr > 1.0E-7) {
            double pushLength = Math.sqrt(pushSqr);
            this.xPush /= pushLength;
            this.zPush /= pushLength;
            Vec3 motion = this.getDeltaMovement().multiply(0.8, 0.0, 0.8).add(this.xPush, 0.0, this.zPush);
            if (this.isInWater()) {
                motion = motion.scale(0.1);
            }
            this.setDeltaMovement(motion);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.0, 0.98));
        }
        super.applyNaturalSlowdown();
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.NORTH)
                .setValue(AbstractFurnaceBlock.LIT, isBurning());
    }

    @Override
    protected @NotNull Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.BLAST_FURNACE);
    }

    @Override
    public void destroy(Item dropItem) {
        super.destroy(dropItem);
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(Items.BLAST_FURNACE);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Running", isRunning());
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("BurnDuration", this.burnDuration);
        tag.putDouble("PushX", this.xPush);
        tag.putDouble("PushZ", this.zPush);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRunning(tag.getBoolean("Running"));
        this.burnTime = tag.getInt("BurnTime");
        this.burnDuration = tag.getInt("BurnDuration");
        this.xPush = tag.getDouble("PushX");
        this.zPush = tag.getDouble("PushZ");
    }
}
