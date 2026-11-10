package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.datagen.itemTags.ModItemTags;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.spelunkers_charm.entity.util.ControlType;
import net.luckystudio.spelunkers_charm.entity.util.MineralType;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.entity.util.WoodType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.BlockUtil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import javax.annotation.Nullable;

/** Server-controlled rack lift with a solid, freely walkable deck. */
public abstract class AbstractLift extends VehicleEntity {
    private static final EntityDataAccessor<Integer> CONTROL_TYPE =
            SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOVEMENT_DIRECTION =
            SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WOOD_TYPE =
            SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MINERAL_TYPE =
            SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final double SPEED = 0.15;
    private static final double EPSILON = 1.0E-6;
    public AnimationState upAnimationState = new AnimationState();
    public AnimationState downAnimationState = new AnimationState();
    private final Type type;
    private float currentLeverAngle;
    private MovementType preferredDirection = MovementType.UP;
    private MovementType pausedForPower = MovementType.STATIONARY;
    private int manualOverrideStationY = Integer.MIN_VALUE;
    private boolean plateWasOccupied;
    private int interpolationSteps;
    private double targetX, targetY, targetZ;
    private float targetYaw, targetPitch;
    private final LiftGuideSystem guides = new LiftGuideSystem(this);

    public AbstractLift(EntityType<?> entityType, Level level, Type type) {
        super(entityType, level);
        this.type = type;
        this.blocksBuilding = true;
    }

    protected AbstractLift(
            EntityType<?> entityType, Level level, Type type, double x, double y, double z) {
        this(entityType, level, type);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    public static AbstractLift createLift(
            ServerLevel level,
            double x,
            double y,
            double z,
            Type type,
            ItemStack stack,
            @Nullable Player player) {
        AbstractLift lift =
                switch (type) {
                    case SMALL -> new SmallLift(level, x, y, z);
                    case MEDIUM -> new MediumLift(level, x, y, z);
                    case LARGE -> new LargeLift(level, x, y, z);
                };
        EntityType.createDefaultStackConfig(level, stack, player).accept(lift);
        return lift;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONTROL_TYPE, ControlType.AUTOMATIC.ordinal());
        builder.define(MOVEMENT_DIRECTION, MovementType.STATIONARY.ordinal());
        builder.define(WOOD_TYPE, WoodType.SPRUCE.ordinal());
        builder.define(MINERAL_TYPE, MineralType.IRON.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("controlType", getControlType().getSerializedName());
        tag.putString("movementDirection", getMovementType().getSerializedName());
        tag.putString("woodType", getWoodType().getSerializedName());
        tag.putString("mineralType", getMineralType().getSerializedName());
        tag.putString("liftPreferredDirection", preferredDirection.getSerializedName());
        tag.putString("liftPausedForPower", pausedForPower.getSerializedName());
        guides.save(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 5.0.6 wrote an integer but attempted to read a string. Accept both.
        if (tag.contains("controlType", 8))
            setControlType(ControlType.byName(tag.getString("controlType")));
        else if (tag.contains("controlType", 3))
            setControlType(ControlType.byId(tag.getInt("controlType")));
        if (tag.contains("movementDirection", 8))
            setMovementType(MovementType.byName(tag.getString("movementDirection")));
        if (tag.contains("woodType", 8)) setWoodType(WoodType.byName(tag.getString("woodType")));
        if (tag.contains("mineralType", 8))
            setMineralType(MineralType.byName(tag.getString("mineralType")));
        if (tag.contains("liftPreferredDirection", 8)) {
            MovementType saved = MovementType.byName(tag.getString("liftPreferredDirection"));
            if (saved == MovementType.UP || saved == MovementType.DOWN) preferredDirection = saved;
        }
        if (tag.contains("liftPausedForPower", 8))
            pausedForPower = MovementType.byName(tag.getString("liftPausedForPower"));
        guides.load(tag);
    }

    public ControlType getControlType() {
        return ControlType.byId(entityData.get(CONTROL_TYPE));
    }

    public void setControlType(ControlType control) {
        entityData.set(CONTROL_TYPE, control.ordinal());
    }

    public MovementType getMovementType() {
        return MovementType.byId(entityData.get(MOVEMENT_DIRECTION));
    }

    public void setMovementType(MovementType movement) {
        entityData.set(MOVEMENT_DIRECTION, movement.ordinal());
    }

    public WoodType getWoodType() {
        return WoodType.byId(entityData.get(WOOD_TYPE));
    }

    public void setWoodType(WoodType wood) {
        entityData.set(WOOD_TYPE, wood.ordinal());
    }

    public MineralType getMineralType() {
        return MineralType.byId(entityData.get(MINERAL_TYPE));
    }

    public void setMineralType(MineralType mineral) {
        entityData.set(MINERAL_TYPE, mineral.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        AABB oldBounds = getBoundingBox();
        if (level().isClientSide) {
            // Only the server simulates rails and commands. Interpolate its
            // snapshots; carry local feet through precisely the same deck delta.
            if (interpolationSteps > 0) {
                double fraction = 1.0 / interpolationSteps;
                setPos(
                        Mth.lerp(fraction, getX(), targetX),
                        Mth.lerp(fraction, getY(), targetY),
                        Mth.lerp(fraction, getZ(), targetZ));
                setYRot((float) (getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) * fraction));
                setXRot((float) Mth.lerp(fraction, getXRot(), targetPitch));
                --interpolationSteps;
            }
            LiftCollision.carry(this, oldBounds, getBoundingBox());
            setupAnimationStates();
            double dy = getBoundingBox().minY - oldBounds.minY;
            LiftClientSounds.update(this, dy);
            if (Math.abs(dy) > EPSILON
                    && (getMovementType() == MovementType.UP
                            || getMovementType() == MovementType.DOWN))
                LiftClientEffects.spawnGearSparks(this, dy);
        } else {
            tickServer(oldBounds);
            updateStoppedGuideState();
            updateDetectors(oldBounds, getBoundingBox());
        }
        if (getHurtTime() > 0) setHurtTime(getHurtTime() - 1);
        if (getDamage() > 0) setDamage(Math.max(0, getDamage() - 1));
    }

    private void tickServer(AABB oldBounds) {
        boolean guided = hasValidTracks();
        MovementType movement = getMovementType();
        if (guided) {
            if (movement == MovementType.FALLING) {
                stop(false);
                movement = MovementType.STATIONARY;
            }
            if (isAtFloor()) {
                if (manualOverrideStationY == (int) Math.rint(getY())
                        && !stationHasInput((int) Math.rint(getY())))
                    manualOverrideStationY = Integer.MIN_VALUE;
                MovementType command =
                        manualOverrideStationY == (int) Math.rint(getY())
                                ? MovementType.STATIONARY
                                : stationCommand();
                if (command == MovementType.UP || command == MovementType.DOWN) {
                    if (movement != command) {
                        if (canStart(command)) start(command);
                        else if (hasTracksAtY(nextRow(command))) pausedForPower = command;
                    }
                } else if (command == MovementType.FALLING) {
                    // Two opposite powered arrows are deliberately a stop.
                    pausedForPower = MovementType.STATIONARY;
                    stop(true);
                }
                boolean occupied = hasDeckOccupants();
                if (getControlType() == ControlType.PRESSURE_PLATE
                        && occupied
                        && !plateWasOccupied
                        && getMovementType() == MovementType.STATIONARY)
                    startAvailableDirection(preferredDirection);
                plateWasOccupied = occupied;
            }
            if (getMovementType() == MovementType.STATIONARY
                    && (pausedForPower == MovementType.UP || pausedForPower == MovementType.DOWN)
                    && canStart(pausedForPower)) start(pausedForPower);
            movement = getMovementType();
            double requested =
                    movement == MovementType.UP || movement == MovementType.DOWN
                            ? plannedMotion(movement)
                            : 0.0;
            double limited = LiftCollision.limitMovementForRiders(this, oldBounds, requested);
            setDeltaMovement(0, limited, 0);
            move(MoverType.SELF, getDeltaMovement());
            double actual = getY() - oldBounds.minY;
            if (manualOverrideStationY != Integer.MIN_VALUE
                    && Math.abs(getY() - manualOverrideStationY) > EPSILON)
                manualOverrideStationY = Integer.MIN_VALUE;
            LiftCollision.carry(this, oldBounds, getBoundingBox());
            if (movement == MovementType.UP || movement == MovementType.DOWN) {
                if (Math.abs(actual - requested) > EPSILON || Math.abs(requested) < EPSILON) {
                    if (Math.abs(requested) < EPSILON
                            && hasTracksAtY(nextRow(movement))
                            && !motionRowsPowered(movement)) pausedForPower = movement;
                    else if (!hasTracksAtY(nextRow(movement)))
                        preferredDirection = opposite(movement);
                    stop(true);
                } else if (isAtFloor() && isStationAtY((int) Math.rint(getY()))) {
                    preferredDirection = movement;
                    stop(true);
                }
            }
        } else {
            pausedForPower = MovementType.STATIONARY;
            setMovementType(onGround() ? MovementType.STATIONARY : MovementType.FALLING);
            setDeltaMovement(0, Math.max(-1.5, getDeltaMovement().y - getGravity()), 0);
            double limited =
                    LiftCollision.limitMovementForRiders(this, oldBounds, getDeltaMovement().y);
            setDeltaMovement(0, limited, 0);
            move(MoverType.SELF, getDeltaMovement());
            LiftCollision.carry(this, oldBounds, getBoundingBox());
            if (onGround() || Math.abs(limited) < EPSILON) {
                setDeltaMovement(Vec3.ZERO);
                setMovementType(MovementType.STATIONARY);
            }
        }
    }

    private double plannedMotion(MovementType movement) {
        double y = getY();
        int lower = Mth.floor(y + EPSILON);
        int upper = (int) Math.ceil(y - EPSILON);
        if (!hasTracksAtY(lower) || !hasTracksAtY(upper)) return 0;
        int next = nextRow(movement);
        if (!hasTracksAtY(next)) return 0;
        if (!motionRowsPowered(movement)) return 0;
        double distance = next - y;
        return Math.copySign(Math.min(SPEED, Math.abs(distance)), distance);
    }

    private int nextRow(MovementType movement) {
        return movement == MovementType.UP
                ? Mth.floor(getY() + EPSILON) + 1
                : (int) Math.ceil(getY() - EPSILON) - 1;
    }

    private boolean motionRowsPowered(MovementType movement) {
        if (movement == MovementType.DOWN) return true;
        return rowCanDrive(Mth.floor(getY() + EPSILON))
                && rowCanDrive((int) Math.ceil(getY() - EPSILON))
                && rowCanDrive(nextRow(movement));
    }

    private boolean rowCanDrive(int y) {
        return guides.rowCanDrive(y);
    }

    private boolean canStart(MovementType movement) {
        if (!hasValidTracks()) return false;
        double requested = plannedMotion(movement);
        if (Math.abs(requested) < EPSILON) return false;
        double allowed =
                Entity.collideBoundingBox(
                                this,
                                new Vec3(0, requested, 0),
                                getBoundingBox(),
                                level(),
                                List.of())
                        .y;
        return Math.abs(allowed) > EPSILON
                && Math.abs(LiftCollision.limitMovementForRiders(this, getBoundingBox(), allowed))
                        > EPSILON;
    }

    private void start(MovementType direction) {
        pausedForPower = MovementType.STATIONARY;
        preferredDirection = direction;
        setMovementType(direction);
        playStartSound();
    }

    private boolean startAvailableDirection(MovementType first) {
        if (canStart(first)) {
            start(first);
            return true;
        }
        MovementType other = opposite(first);
        if (canStart(other)) {
            start(other);
            return true;
        }
        return false;
    }

    private void stop(boolean sound) {
        MovementType old = getMovementType();
        if (old == MovementType.UP || old == MovementType.DOWN) {
            if (sound) playStopSound();
        }
        setMovementType(MovementType.STATIONARY);
        setDeltaMovement(Vec3.ZERO);
    }

    private static MovementType opposite(MovementType movement) {
        return movement == MovementType.DOWN ? MovementType.UP : MovementType.DOWN;
    }

    private boolean isAtFloor() {
        return Math.abs(getY() - Math.rint(getY())) < EPSILON;
    }

    public final List<BlockPos> getGuidePositions() {
        return trackPositions();
    }

    protected abstract List<BlockPos> trackPositions();

    public final boolean hasValidTracks() {
        return hasTracksAtY(Mth.floor(getY() + EPSILON))
                || hasTracksAtY((int) Math.ceil(getY() - EPSILON));
    }

    protected boolean hasTracks(int yOffset) {
        return hasTracksAtY(getBlockY() + yOffset);
    }

    private boolean hasTracksAtY(int y) {
        return guides.hasTracksAtY(y);
    }

    public final boolean isStationAtY(int y) {
        return guides.isStationAtY(y);
    }

    /** One directional rack marks the station; only powered arrows dispatch it. */
    private boolean stationHasInput(int y) {
        return guides.stationHasInput(y);
    }

    private MovementType stationCommand() {
        return guides.stationCommand();
    }

    private boolean hasDeckOccupants() {
        AABB box = getBoundingBox();
        AABB top =
                new AABB(box.minX, box.maxY - 0.05, box.minZ, box.maxX, box.maxY + 0.15, box.maxZ);
        return !level().getEntities(
                        this,
                        top,
                        e ->
                                !(e instanceof AbstractLift)
                                        && !e.isSpectator()
                                        && !e.noPhysics
                                        && !e.isPassenger())
                .isEmpty();
    }

    private void updateStoppedGuideState() {
        guides.updateStoppedGuideState();
    }

    private void updateDetectors(AABB before, AABB after) {
        guides.updateDetectors(before, after);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide
                && reason != RemovalReason.UNLOADED_TO_CHUNK
                && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            guides.clearStoppedGuideState();
        }
        super.remove(reason);
    }

    private void setupAnimationStates() {
        upAnimationState.animateWhen(getMovementType() == MovementType.UP, tickCount);
        downAnimationState.animateWhen(getMovementType() == MovementType.DOWN, tickCount);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        targetPitch = pitch;
        interpolationSteps = Math.max(1, steps);
    }

    private void playStartSound() {
        level().playSound(
                        null,
                        blockPosition(),
                        ModSoundEvents.LIFT_START.get(),
                        SoundSource.BLOCKS,
                        getLevelBasedOnType(),
                        getPitchBasedOnType());
    }

    private void playStopSound() {
        level().playSound(
                        null,
                        blockPosition(),
                        ModSoundEvents.LIFT_STOP.get(),
                        SoundSource.BLOCKS,
                        getLevelBasedOnType(),
                        getPitchBasedOnType());
    }

    public boolean onCeiling() {
        return !level().noCollision(this, getBoundingBox().move(0, EPSILON * 2, 0));
    }

    protected void onArise() {}

    protected void onLanding() {}

    @Override
    protected Item getDropItem() {
        return switch (type) {
            case SMALL -> ModItems.SMALL_LIFT.get();
            case MEDIUM -> ModItems.MEDIUM_LIFT.get();
            case LARGE -> ModItems.LARGE_LIFT.get();
        };
    }

    protected float getLevelBasedOnType() {
        return switch (type) {
            case SMALL -> 1;
            case MEDIUM -> 1.25F;
            case LARGE -> 1.5F;
        };
    }

    protected float getPitchBasedOnType() {
        return switch (type) {
            case SMALL -> 1;
            case MEDIUM -> 0.75F;
            case LARGE -> 0.25F;
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.isDirect() && super.hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (stack.is(ItemTags.PLANKS)) {
            WoodType wood = WoodType.getWoodType(stack);
            if (wood != null) {
                setWoodType(wood);
                return InteractionResult.CONSUME;
            }
        }
        if (stack.is(ModItemTags.MINERALS)) {
            MineralType mineral = MineralType.getMineralType(stack);
            if (mineral != null) {
                setMineralType(mineral);
                return InteractionResult.CONSUME;
            }
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof LeverBlock) {
                setControlType(ControlType.LEVER);
                return InteractionResult.CONSUME;
            }
            if (blockItem.getBlock() instanceof PressurePlateBlock) {
                setControlType(ControlType.PRESSURE_PLATE);
                return InteractionResult.CONSUME;
            }
        }
        // A player's command takes precedence over the arrow on this floor.
        // Once the deck leaves, the next station can control it normally.
        manualOverrideStationY = isAtFloor() ? (int) Math.rint(getY()) : Integer.MIN_VALUE;
        pausedForPower = MovementType.STATIONARY;
        if (player.isSecondaryUseActive()) {
            pausedForPower = MovementType.STATIONARY;
            MovementType requested =
                    opposite(
                            getMovementType() == MovementType.UP
                                            || getMovementType() == MovementType.DOWN
                                    ? getMovementType()
                                    : preferredDirection);
            stop(true);
            if (canStart(requested)) start(requested);
        } else if (getMovementType() == MovementType.UP || getMovementType() == MovementType.DOWN) {
            pausedForPower = MovementType.STATIONARY;
            stop(true);
        } else if (!startAvailableDirection(preferredDirection)) {
            player.displayClientMessage(
                    Component.translatable(
                            hasValidTracks()
                                    ? "message.spelunkers_charm.lift_cannot_move"
                                    : "message.spelunkers_charm.lift_tracks"),
                    true);
        }
        level().playSound(
                        null,
                        blockPosition(),
                        SoundEvents.LEVER_CLICK,
                        SoundSource.BLOCKS,
                        0.5F,
                        1);
        return InteractionResult.CONSUME;
    }

    public float getCurrentLeverAngle() {
        return currentLeverAngle;
    }

    public void setCurrentLeverAngle(float angle) {
        currentLeverAngle = angle;
    }

    @Override
    public void animateHurt(float yaw) {
        setHurtDir(-getHurtDir());
        setHurtTime(10);
        setDamage(getDamage() * 11);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.EVENTS;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(getDropItem().getDescriptionId());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return entity instanceof AbstractLift && canVehicleCollide(this, entity);
    }

    public static boolean canVehicleCollide(Entity vehicle, Entity entity) {
        return !vehicle.isPassengerOfSameVehicle(entity)
                && (entity.canBeCollidedWith() || entity.isPushable());
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(
                super.getRelativePortalPosition(axis, portal));
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    public void setLeverAngle(AbstractLift lift, ModelPart lever) {
        float target =
                lift.getMovementType() == MovementType.UP
                        ? 30
                        : lift.getMovementType() == MovementType.DOWN ? -30 : 0;
        lift.setCurrentLeverAngle(Mth.lerp(0.2F, lift.getCurrentLeverAngle(), target));
        lever.zRot = (float) Math.toRadians(lift.getCurrentLeverAngle());
    }

    public enum Type {
        SMALL,
        MEDIUM,
        LARGE
    }
}
