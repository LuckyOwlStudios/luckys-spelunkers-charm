// Locomotive controls and fuel.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LocomotiveMinecart extends AbstractMinecartContainer {
    private static final EntityDataAccessor<Boolean> DATA_REVERSED =
            SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_POWER =
            SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BRAKE =
            SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_BODY_YAW =
            SynchedEntityData.defineId(LocomotiveMinecart.class, EntityDataSerializers.FLOAT);
    public static final int FUEL_MULTIPLIER = 1;
    public static final int MENU_DATA_SIZE = 6;
    private final LocomotiveFuel fuel = new LocomotiveFuel(this);
    private boolean emergencyBrake;
    private double brakeSpeed;
    private Vec3 brakeAnchor;
    private double xPush;
    private double zPush;
    private boolean legacyFacing;

    private final ContainerData dataAccess =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0, 1, 2 -> emergencyBrake ? 0 : fuel.percent(index);
                        case 3 -> fuel.enabledMask();
                        case 4 -> isReversed() ? 1 : 0;
                        case 5 -> emergencyBrake ? 1 : 0;
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {}

                @Override
                public int getCount() {
                    return MENU_DATA_SIZE;
                }
            };

    public LocomotiveMinecart(EntityType<? extends LocomotiveMinecart> type, Level level) {
        super(type, level);
    }

    public LocomotiveMinecart(Level level, double x, double y, double z) {
        super(ModEntityType.LOCOMOTIVE.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVERSED, false);
        builder.define(DATA_POWER, 0);
        builder.define(DATA_BRAKE, false);
        builder.define(DATA_BODY_YAW, 0.0F);
    }

    public boolean isReversed() {
        return entityData.get(DATA_REVERSED);
    }

    public int activeBurners() {
        return entityData.get(DATA_POWER);
    }

    public boolean isBurning() {
        return activeBurners() > 0;
    }

    public boolean emergencyBrake() {
        return level().isClientSide() ? entityData.get(DATA_BRAKE) : emergencyBrake;
    }

    public float bodyYaw() {
        return entityData.get(DATA_BODY_YAW);
    }

    public Vec3 bodyDirection() {
        double angle = Math.toRadians(bodyYaw());
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    public float leverAngle() {
        return emergencyBrake() ? 0 : isReversed() ? -30 : 30;
    }

    private void updateBodyYaw() {
        if (xPush * xPush + zPush * zPush > 1e-6)
            entityData.set(
                    DATA_BODY_YAW,
                    (float) Math.toDegrees(Math.atan2(zPush, xPush)) + (isReversed() ? 180 : 0));
    }

    public void alignRearToward(Vec3 toward) {
        if (bodyDirection().dot(toward) > 0) {
            entityData.set(DATA_REVERSED, !isReversed());
            entityData.set(DATA_BODY_YAW, bodyYaw() + 180);
        }
    }

    public void landFacing(float facing) {
        entityData.set(DATA_BODY_YAW, facing);
        Vec3 front = bodyDirection();
        xPush = front.x * (isReversed() ? -1 : 1);
        zPush = front.z * (isReversed() ? -1 : 1);
    }

    public void initializeBodyDirection(Vec3 facing) {
        if (facing.horizontalDistanceSqr() > 1e-6)
            entityData.set(DATA_BODY_YAW, (float) Math.toDegrees(Math.atan2(facing.z, facing.x)));
    }

    public void resolveLegacyFacing(Vec3 toward) {
        if (legacyFacing && toward.horizontalDistanceSqr() > 1e-6) {
            if (xPush * xPush + zPush * zPush < 1e-6) initializeBodyDirection(toward.scale(-1));
            alignRearToward(toward);
            legacyFacing = false;
        }
    }

    public boolean brakeAnchored() {
        return emergencyBrake && brakeAnchor != null;
    }

    public boolean burnerEnabled(int slot) {
        return fuel.enabled(slot);
    }

    public int fuelRemaining(int slot) {
        return fuel.remaining(slot);
    }

    public ContainerData menuData() {
        return dataAccess;
    }

    public Vec3 driveVector() {
        return new Vec3(xPush, 0, zPush).normalize();
    }

    public void toggleBurner(int slot) {
        fuel.toggle(slot);
        entityData.set(DATA_POWER, emergencyBrake ? 0 : fuel.active());
    }

    public void toggleEmergencyBrake() {
        if (emergencyBrake) releaseEmergencyBrake();
        else {
            emergencyBrake = true;
            entityData.set(DATA_BRAKE, true);
            brakeSpeed = getDeltaMovement().horizontalDistance();
            brakeAnchor = brakeSpeed < 0.003 ? position() : null;
            entityData.set(DATA_POWER, 0);
            ((MinecartMotionVisuals) this).spelunkersCharm$motion().brake(brakeSpeed);
            MinecartHandbrake.changed(this, true);
        }
    }

    public void releaseEmergencyBrake() {
        emergencyBrake = false;
        entityData.set(DATA_BRAKE, false);
        brakeAnchor = null;
        entityData.set(DATA_POWER, fuel.active());
        MinecartHandbrake.changed(this, false);
    }

    public double brakingSpeed(double incoming) {
        return emergencyBrake ? Math.min(brakeSpeed, incoming) : incoming;
    }

    public void updateBrakeHold(double speed) {
        if (!emergencyBrake) return;
        if (!MinecartTrainBrakes.onRail(this)) {
            brakeAnchor = null;
            return;
        }
        brakeSpeed = speed < 0.003 ? 0 : Math.min(brakeSpeed, speed);
        if (brakeSpeed == 0) {
            if (!isOnRails() && !onGround()) {
                brakeAnchor = null;
                setDeltaMovement(0, getDeltaMovement().y, 0);
                return;
            }
            if (brakeAnchor == null) brakeAnchor = position();
            setPos(brakeAnchor.x, brakeAnchor.y, brakeAnchor.z);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (!emergencyBrake || !MinecartTrainBrakes.onRail(this)) super.push(x, y, z);
    }

    @Override
    public void move(net.minecraft.world.entity.MoverType type, Vec3 movement) {
        if (!brakeAnchored() || !MinecartTrainBrakes.onRail(this)) super.move(type, movement);
    }

    @Override
    public boolean isPushable() {
        return (!emergencyBrake || !MinecartTrainBrakes.onRail(this)) && super.isPushable();
    }

    public void switchDirection() {
        float facing = bodyYaw();
        ((MinecartMotionVisuals) this).spelunkersCharm$motion().reversalRequested = true;
        entityData.set(DATA_REVERSED, !isReversed());
        xPush = -xPush;
        zPush = -zPush;
        entityData.set(DATA_BODY_YAW, facing);
        playSound(SoundEvents.LEVER_CLICK, 0.6F, isReversed() ? 0.5F : 0.6F);
    }

    public void selectDirection(boolean reversed) {
        Vec3 facing = bodyDirection();
        boolean changing = reversed != isReversed();
        entityData.set(DATA_REVERSED, reversed);
        xPush = facing.x * (reversed ? -1 : 1);
        zPush = facing.z * (reversed ? -1 : 1);
        if (changing)
            ((MinecartMotionVisuals) this).spelunkersCharm$motion().reversalRequested = true;
        releaseEmergencyBrake();
    }

    public void cycleDriveMode() {
        if (emergencyBrake) selectDirection(!isReversed());
        else if (driveVector().lengthSqr() < 1e-6 && getDeltaMovement().horizontalDistance() < .003)
            selectDirection(isReversed());
        else toggleEmergencyBrake();
        playSound(SoundEvents.LEVER_CLICK, .6F, emergencyBrake ? .7F : isReversed() ? .5F : .6F);
    }

    public static double pistonFrequency(double speed) {
        return speed <= .008 ? 0 : .8 + Math.clamp(speed / .5, 0, 1) * 5.2;
    }

    public void setDriveDirection(Direction direction) {
        if (tickCount > 0 && xPush * xPush + zPush * zPush > 1e-6) {
            double facing =
                    bodyDirection().x * direction.getStepX()
                            + bodyDirection().z * direction.getStepZ();
            if (Math.abs(facing) > .1) entityData.set(DATA_REVERSED, facing < 0);
        }
        xPush = direction.getStepX();
        zPush = direction.getStepZ();
        updateBodyYaw();
    }

    public boolean isDrivingAgainst(Direction direction) {
        return xPush * direction.getStepX() + zPush * direction.getStepZ() < -1.0E-4;
    }

    public void followCurve(Vec3 previous, Vec3 next) {
        double turn = Math.atan2(next.z, next.x) - Math.atan2(previous.z, previous.x);
        float facing = bodyYaw() + net.minecraft.util.Mth.wrapDegrees((float) Math.toDegrees(turn));
        entityData.set(DATA_BODY_YAW, facing);
        Vec3 front = bodyDirection();
        xPush = front.x * (isReversed() ? -1 : 1);
        zPush = front.z * (isReversed() ? -1 : 1);
    }

    @Override
    public Type getMinecartType() {
        return Type.FURNACE;
    }

    @Override
    public int getContainerSize() {
        return LocomotiveFuel.SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!fuel.enabled(slot) || !isFuel(stack)) return false;
        int reserve = getItem(slot).getCount() + (fuel.remaining(slot) > 0 ? 1 : 0);
        for (int other = 0; other < LocomotiveFuel.SLOTS; other++) {
            if (fuel.enabled(other)
                    && getItem(other).getCount() + (fuel.remaining(other) > 0 ? 1 : 0) < reserve)
                return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new LocomotiveMenu(id, inventory, this, dataAccess);
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (emergencyBrake) updateBrakeHold(brakeSpeed);
            fuel.tick(emergencyBrake);
            entityData.set(DATA_POWER, emergencyBrake ? 0 : fuel.active());
        }
        super.tick();
        if (!level().isClientSide() && emergencyBrake)
            updateBrakeHold(getDeltaMovement().horizontalDistance());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) return super.interact(player, hand);
        if (!level().isClientSide()) {
            cycleDriveMode();
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    protected double getMaxSpeed() {
        return 0.4;
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return 1.0F;
    }

    @Override
    public boolean isPoweredCart() {
        return true;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.BLAST_FURNACE
                .defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.NORTH)
                .setValue(AbstractFurnaceBlock.LIT, isBurning());
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LOCOMOTIVE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return ModItems.LOCOMOTIVE.toStack();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        fuel.save(tag);
        tag.putBoolean("Reversed", isReversed());
        tag.putBoolean("EmergencyBrake", emergencyBrake);
        tag.putDouble("BrakeSpeed", brakeSpeed);
        tag.putDouble("PushX", xPush);
        tag.putDouble("PushZ", zPush);
        tag.putFloat("BodyYaw", bodyYaw());
        tag.putBoolean(
                "FlightBrake", ((MinecartMotionVisuals) this).spelunkersCharm$motion().flightBrake);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuel.load(tag);
        entityData.set(DATA_REVERSED, tag.getBoolean("Reversed"));
        emergencyBrake = tag.getBoolean("EmergencyBrake");
        ((MinecartMotionVisuals) this).spelunkersCharm$motion().flightBrake =
                tag.getBoolean("FlightBrake");
        entityData.set(DATA_BRAKE, emergencyBrake);
        brakeSpeed =
                tag.contains("BrakeSpeed")
                        ? tag.getDouble("BrakeSpeed")
                        : getDeltaMovement().horizontalDistance();
        brakeAnchor = emergencyBrake && brakeSpeed < 0.003 ? position() : null;
        entityData.set(DATA_POWER, emergencyBrake ? 0 : fuel.active());
        xPush = tag.getDouble("PushX");
        zPush = tag.getDouble("PushZ");
        legacyFacing = !tag.contains("BodyYaw");
        if (!legacyFacing) entityData.set(DATA_BODY_YAW, tag.getFloat("BodyYaw"));
        else updateBodyYaw();
    }
}
