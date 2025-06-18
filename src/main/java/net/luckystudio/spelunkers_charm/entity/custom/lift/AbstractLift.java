package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.datagen.itemTags.ModItemTags;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.spelunkers_charm.entity.util.ControlType;
import net.luckystudio.spelunkers_charm.entity.util.MineralType;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.entity.util.WoodType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.init.ModParticleTypes;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.BlockUtil;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;

// Heavily copied from the Boat class in Minecraft, with modifications for a small lift entity.
public abstract class AbstractLift extends VehicleEntity {

    private static final EntityDataAccessor<Integer> CONTROL_TYPE = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOVEMENT_DIRECTION = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WOOD_TYPE = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MINERAL_TYPE = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    public AnimationState upAnimationState = new AnimationState();
    public AnimationState downAnimationState = new AnimationState();
    private boolean wasOnGround = false;
    private boolean wasOnCeiling = false;
    private float currentLeverAngle = 0.0F;
    private final float movementSpeed = 0.15F; // Default movement speed
    private final AbstractLift.Type type;

    public AbstractLift(EntityType<?> entityType, Level level, AbstractLift.Type type) {
        super(entityType, level);
        this.type = type;
    }

    protected AbstractLift(EntityType<?> entityType, Level level, AbstractLift.Type type, double x, double y, double z) {
        this(entityType, level, type);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static AbstractLift createLift (
            ServerLevel level,
            double x,
            double y,
            double z,
            Type type,
            ItemStack stack,
            @Nullable Player player
    ) {
        AbstractLift abstractLift = (switch (type) {
            case SMALL -> new SmallLift(level, x, y, z);
            case MEDIUM -> new MediumLift(level, x, y, z);
            case LARGE -> new LargeLift(level, x, y, z);
        });
        EntityType.<AbstractLift>createDefaultStackConfig(level, stack, player).accept(abstractLift);
        return abstractLift;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONTROL_TYPE, ControlType.AUTOMATIC.ordinal());
        builder.define(MOVEMENT_DIRECTION, MovementType.STATIONARY.ordinal());
        builder.define(WOOD_TYPE, WoodType.SPRUCE.ordinal());
        builder.define(MINERAL_TYPE, MineralType.IRON.ordinal()); // Default to no mineral type
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("controlType", this.entityData.get(CONTROL_TYPE));
        compound.putString("movementDirection", this.getMovementType().getSerializedName());
        compound.putString("woodType", this.getWoodType().getSerializedName());
        compound.putString("mineralType", this.getMineralType().getSerializedName());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("controlType", 8)) {
            this.setControlType(ControlType.byName(compound.getString("controlType")));
        }
        if (compound.contains("movementDirection", 8)) {
            this.setMovementType(MovementType.byName(compound.getString("movementDirection")));
        }
        if (compound.contains("woodType", 8)) {
            this.setWoodType(WoodType.byName(compound.getString("woodType")));
        }
        if (compound.contains("mineralType", 8)) {
            this.setMineralType(MineralType.byName(compound.getString("mineralType")));
        }
    }

    public ControlType getControlType() {
        return ControlType.byId(this.entityData.get(MOVEMENT_DIRECTION));
    }

    public void setControlType(ControlType direction) {
        this.entityData.set(CONTROL_TYPE, direction.ordinal());
    }

    public MovementType getMovementType() {
        return MovementType.byId(this.entityData.get(MOVEMENT_DIRECTION));
    }

    public void setMovementType(MovementType direction) {
        this.entityData.set(MOVEMENT_DIRECTION, direction.ordinal());
    }

    public WoodType getWoodType() {
        return WoodType.byId(this.entityData.get(WOOD_TYPE));
    }

    public void setWoodType(WoodType variant) {
        this.entityData.set(WOOD_TYPE, variant.ordinal());
    }

    public MineralType getMineralType() {
        return MineralType.byId(this.entityData.get(MINERAL_TYPE));
    }

    public void setMineralType(MineralType variant) {
        this.entityData.set(MINERAL_TYPE, variant.ordinal());
    }

    @Override
    public void tick() {
        super.tick();

        Direction direction = this.getDirection();
        Vec3 frontPos = new Vec3(this.getX(), this.getY(), this.getZ()).add(0,0.5,0).relative(direction, 3);
        this.level().addAlwaysVisibleParticle(ParticleTypes.FLAME,
                frontPos.x,
                frontPos.y,
                frontPos.z,
                0, 0, 0);

        snapHorizontallyToGrid();

        if (this.hasTracks(0)) {
            MovementType movement = this.getMovementType();

            // If falling while on track, become stationary
            if (movement == MovementType.FALLING) {
                this.setMovementType(MovementType.STATIONARY);
                movement = MovementType.STATIONARY;
            }

            // Snap to vertical grid if stationary
            if (movement == MovementType.STATIONARY) {
                snapVerticallyToGrid();
            }

            // Handle upward movement
            if (movement == MovementType.UP) {
                if (this.hasTracks(1) && !this.onCeiling()) {
                    this.setDeltaMovement(this.getDeltaMovement().x, movementSpeed, this.getDeltaMovement().z);
                } else {
                    playStopSound();
                    this.setMovementType(MovementType.STATIONARY);
                }
            }

            // Handle downward movement
            if (movement == MovementType.DOWN) {
                if (this.hasTracks(-1) && !this.onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().x, -movementSpeed, this.getDeltaMovement().z);
                } else {
                    playStopSound();
                    this.setMovementType(MovementType.STATIONARY);
                }
            }

        } else {
            if (this.onGround()) {
                this.setMovementType(MovementType.FALLING);
            } else {
                this.setMovementType(MovementType.STATIONARY);
            }
            // No track, fall
            this.applyGravity();
        }

        // Force entity to move based on current delta
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Landing arising detection
        boolean currentlyOnCeiling = this.onCeiling();
        if (!wasOnCeiling && currentlyOnCeiling) {
            this.onArise();
        }
        wasOnCeiling = currentlyOnCeiling;

        // Landing detection
        boolean currentlyOnGround = this.onGround();
        if (!wasOnGround && currentlyOnGround) {
            this.onLanding();
        }
        wasOnGround = currentlyOnGround;

        // Damage handling
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        // Update animation states
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
        this.upAnimationState.start(this.tickCount);
    }

    private void setupAnimationStates() {
        // Up Animation State
        if (this.getMovementType() == MovementType.UP) {
            this.upAnimationState.start(this.tickCount);
        } else {
            this.upAnimationState.stop();
        }
        // Down Animation State
        if (this.getMovementType() == MovementType.DOWN) {
            this.downAnimationState.startIfStopped(this.tickCount);
        } else {
            this.downAnimationState.stop();
        }
    }
    
    private void playStartSound() {
        this.level().playSound(
                this,
                this.blockPosition(),
                ModSoundEvents.LIFT_START.get(),
                SoundSource.BLOCKS,
                getLevelBasedOnType(),
                getPitchBasedOnType()
        );
    }

    private void playStopSound() {
        this.level().playSound(
                this,
                this.blockPosition(),
                ModSoundEvents.LIFT_STOP.get(),
                SoundSource.BLOCKS,
                getLevelBasedOnType(),
                getPitchBasedOnType()
        );
    }

    public boolean onCeiling() {
        AABB aabb = this.getBoundingBox();
        AABB ceilingCheckBox = new AABB(aabb.minX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY + 1.0E-6, aabb.maxZ);
        return this.level().findSupportingBlock(this, ceilingCheckBox).isPresent();
    }

    // Snap vertically when MovementType is STATIONARY and has tracks
    private void snapVerticallyToGrid() {
        double y = this.getBlockY();
        this.setPos(this.getX(), y, this.getZ());
        this.setDeltaMovement(this.getDeltaMovement().x, 0, this.getDeltaMovement().z);
    }

    // Always snap horizontally to the nearest grid point
    private void snapHorizontallyToGrid() {
        double x = this.getX();
        double z = this.getZ();

        double targetX = Math.floor(x) + 0.5;
        double targetZ = Math.floor(z) + 0.5;

        double dx = targetX - x;
        double dz = targetZ - z;

        if (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01) {
            this.setPos(targetX, this.getY(), targetZ);
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }
    }

    protected void onArise() {
        ModParticleTypes.spawnSquareBorderParticles(this.level(), ParticleTypes.DUST_PLUME, this.blockPosition().above(), this.getBbWidth() / 2, (int) (10 * this.getBbWidth()), 0.1F);
    }

    protected void onLanding() {
        ModParticleTypes.spawnSquareBorderParticles(this.level(), ParticleTypes.DUST_PLUME, this.blockPosition(), this.getBbWidth() / 2, (int) (10 * this.getBbWidth()), 0.1F);
    }

    @Override
    protected @NotNull Item getDropItem() {
        return switch (this.type) {
            case SMALL -> ModItems.SMALL_LIFT.get();
            case MEDIUM -> ModItems.MEDIUM_LIFT.get();
            case LARGE -> ModItems.LARGE_LIFT.get();
        };
    }

    protected float getLevelBasedOnType() {
        return switch (this.type) {
            case SMALL -> 1.0F;
            case MEDIUM -> 1.25F;
            case LARGE -> 1.5F;
        };
    }

    protected float getPitchBasedOnType() {
        return switch (this.type) {
            case SMALL -> 1.0F;
            case MEDIUM -> 0.75F;
            case LARGE -> 0.25F;
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.isDirect()) return false;
        return super.hurt(source, amount);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Change the wood type if the player is holding a plank
        if (itemStack.is(ItemTags.PLANKS)) {
            WoodType woodType = WoodType.getWoodType(itemStack);
            if (woodType == null) return super.interact(player, hand);
            setWoodType(woodType);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Change the mineral type if the player is holding a mineral item
        if (itemStack.is(ModItemTags.MINERALS)) {
            MineralType mineralType = MineralType.getMineralType(itemStack);
            if (mineralType == null) return super.interact(player, hand);
            setMineralType(mineralType);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Change the control type if the player is holding a lever or pressure plate
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof LeverBlock) {
                setControlType(ControlType.LEVER);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (block instanceof PressurePlateBlock) {
                setControlType(ControlType.PRESSURE_PLATE);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        // If the control type is LEVER, toggle the movement direction, as its the only control type that allows interaction
        if (getControlType() == ControlType.AUTOMATIC) {
            return toggleMovementDirection();
        }
        return super.interact(player, hand);
    }

    private InteractionResult toggleMovementDirection() {
        if (this.getMovementType() == MovementType.STATIONARY && hasTracks(0)) {
            if (this.hasTracks(1) && !this.onCeiling()) {
                playStartSound();
                this.level().playSound(this, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, getLevelBasedOnType(), getPitchBasedOnType());
                this.setMovementType(MovementType.UP);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            else if (this.hasTracks(-1) && !this.onGround()) {
                playStartSound();
                this.level().playSound(this, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, getLevelBasedOnType(), getPitchBasedOnType());
                this.setMovementType(MovementType.DOWN);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            else return InteractionResult.sidedSuccess(this.level().isClientSide); // No valid tracks to move on
        } else {
            playStopSound();
            this.level().playSound(this, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, getLevelBasedOnType(), getPitchBasedOnType());
            this.setMovementType(MovementType.STATIONARY);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
    }

    protected abstract List<BlockPos> trackPositions();

    protected boolean hasTracks(int yOffset) {
        for (BlockPos pos : this.trackPositions()) {
            if (this.level().getBlockState(pos.offset(0, yOffset, 0)).getBlock() != ModBlocks.POWERED_LIFT_TRACK.get()) {
                return false; // If any track position is air, we don't have tracks
            }
        }
        return true;
    }

    public float getCurrentLeverAngle() {
        return currentLeverAngle;
    }

    public void setCurrentLeverAngle(float angle) {
        this.currentLeverAngle = angle;
    }

    @Override
    public void animateHurt(float yaw) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.EVENTS;
    }

    @Override
    protected @NotNull Component getTypeName() {
        return Component.translatable(this.getDropItem().getDescriptionId());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }

    public static boolean canVehicleCollide(Entity vehicle, Entity entity) {
        return (entity.canBeCollidedWith() || entity.isPushable()) && !vehicle.isPassengerOfSameVehicle(entity);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portal));
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    public void setLeverAngle(AbstractLift entity, ModelPart lever) {
        MovementType movementDirection = entity.getMovementType();
        float targetAngle = switch (movementDirection) {
            case UP -> 30.0F;
            case DOWN -> -30.0F;
            default -> 0.0F;
        };

        // Smoothly interpolate toward the target angle
        float currentAngle = entity.getCurrentLeverAngle();
        float lerpedAngle = Mth.lerp(0.2F, currentAngle, targetAngle); // 0.2F is the smoothing speed

        // Store the new interpolated angle back to the entity
        entity.setCurrentLeverAngle(lerpedAngle);

        // Apply the angle to the model part
        lever.zRot = (float) Math.toRadians(lerpedAngle);
    }

    public enum Type {
        SMALL,
        MEDIUM,
        LARGE
    }
}
