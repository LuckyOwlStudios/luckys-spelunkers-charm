package net.luckystudio.splelunkers_charm.entity.custom.lift;

import net.luckystudio.splelunkers_charm.datagen.itemTags.ModItemTags;
import net.luckystudio.splelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.splelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.splelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.splelunkers_charm.entity.util.MineralType;
import net.luckystudio.splelunkers_charm.entity.util.MovementDirection;
import net.luckystudio.splelunkers_charm.entity.util.WoodType;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.luckystudio.splelunkers_charm.particles.ModParticleTypes;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.List;

// Heavily copied from the Boat class in Minecraft, with modifications for a small lift entity.
public abstract class AbstractLift extends VehicleEntity {

    private static final EntityDataAccessor<Integer> DATA_ID_DIRECTION = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_WOOD_TYPE = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_MINERAL_TYPE = SynchedEntityData.defineId(AbstractLift.class, EntityDataSerializers.INT);
    private boolean wasOnGround = false;
    private float currentLeverAngle = 0.0F;
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
    public void tick() {
        super.tick();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Detect landing (transition from not on ground → on ground)
        boolean currentlyOnGround = this.onGround();
        if (!wasOnGround && currentlyOnGround) {
            this.onLanding(); // Only trigger once on landing
        }
        wasOnGround = currentlyOnGround; // Update the last ground state

        snapToGrid();

        // Handle damage cooldowns, etc.
        if (this.getHurtTime() > 0) this.setHurtTime(this.getHurtTime() - 1);
        if (this.getDamage() > 0.0F) this.setDamage(this.getDamage() - 1.0F);
    }

    protected void onLanding() {
        // Play a sound when the entity lands
        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS,
                getLevelBasedOnType(),
                getPitchBasedOnType()
        );
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
            case SMALL -> 0.75F;
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

    private void snapToGrid() {
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

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(ItemTags.PLANKS)) {
            WoodType woodType = WoodType.getWoodType(itemStack);
            if (woodType != null) return super.interact(player, hand);
            setWoodType(woodType);
            return InteractionResult.sidedSuccess(this.level().isClientSide);

        } else if (itemStack.is(ModItemTags.MINERALS)) {
            MineralType mineralType = MineralType.getMineralType(itemStack);
            if (mineralType == null) return super.interact(player, hand);
            setMineralType(mineralType);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return toggleMovementDirection();
        }
    }

    private InteractionResult toggleMovementDirection() {
        if (this.getMovementDirection() != MovementDirection.STATIONARY) return InteractionResult.FAIL;
        if (!hasTracks()) return InteractionResult.FAIL;
        System.out.println("test");
        this.setMovementDirection(MovementDirection.UP);
        this.level().playSound(this, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.1F);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    protected abstract List<BlockPos> trackPositions();

    protected boolean hasTracks() {
        for (BlockPos pos : this.trackPositions()) {
            if (this.level().getBlockState(pos).getBlock() != Blocks.IRON_BLOCK) {
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_DIRECTION, MovementDirection.STATIONARY.ordinal());
        builder.define(DATA_ID_WOOD_TYPE, WoodType.SPRUCE.ordinal());
        builder.define(DATA_ID_MINERAL_TYPE, MineralType.IRON.ordinal()); // Default to no mineral type
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("movementDirection", this.getMovementDirection().getSerializedName());
        compound.putString("woodType", this.getWoodType().getSerializedName());
        compound.putString("mineralType", this.getMineralType().getSerializedName());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("movementDirection", 8)) {
            this.setMovementDirection(MovementDirection.byName(compound.getString("movementDirection")));
        }
        if (compound.contains("woodType", 8)) {
            this.setWoodType(WoodType.byName(compound.getString("woodType")));
        }
        if (compound.contains("mineralType", 8)) {
            this.setMineralType(MineralType.byName(compound.getString("mineralType")));
        }
    }

    public MovementDirection getMovementDirection() {
        return MovementDirection.byId(this.entityData.get(DATA_ID_DIRECTION));
    }

    public void setMovementDirection(MovementDirection direction) {
        this.entityData.set(DATA_ID_DIRECTION, direction.ordinal());
    }

    public WoodType getWoodType() {
        return WoodType.byId(this.entityData.get(DATA_ID_WOOD_TYPE));
    }

    public void setWoodType(WoodType variant) {
        this.entityData.set(DATA_ID_WOOD_TYPE, variant.ordinal());
    }

    public MineralType getMineralType() {
        return MineralType.byId(this.entityData.get(DATA_ID_MINERAL_TYPE));
    }

    public void setMineralType(MineralType variant) {
        this.entityData.set(DATA_ID_MINERAL_TYPE, variant.ordinal());
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
    public void setOnGround(boolean onGround) {
        super.setOnGround(onGround);
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    public void setLeverAngle(AbstractLift entity, ModelPart lever) {
        MovementDirection movementDirection = entity.getMovementDirection();
        float targetAngle = switch (movementDirection) {
            case UP -> 30.0F;
            case DOWN -> -30.0F;
            case STATIONARY -> 0.0F;
        };

        // Smoothly interpolate toward the target angle
        float currentAngle = entity.getCurrentLeverAngle();
        float lerpedAngle = lerp(0.2F, currentAngle, targetAngle); // 0.2F is the smoothing speed

        // Store the new interpolated angle back to the entity
        entity.setCurrentLeverAngle(lerpedAngle);

        // Apply the angle to the model part
        lever.zRot = (float) Math.toRadians(lerpedAngle);
    }

    private float lerp(float delta, float start, float end) {
        return start + (end - start) * delta;
    }

    public enum Type {
        SMALL,
        MEDIUM,
        LARGE
    }
}
