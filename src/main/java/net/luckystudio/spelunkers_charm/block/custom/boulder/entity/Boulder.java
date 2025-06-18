package net.luckystudio.spelunkers_charm.block.custom.boulder.entity;

import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderBlock;
import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderType;
import net.luckystudio.spelunkers_charm.block.custom.boulder.HangingType;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModParticleTypes;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Mainly copied from the armor stand entity, but without the armor slots and other humanoid features.
public class Boulder extends VehicleEntity {

    private static final EntityDataAccessor<Integer> BOULDER_TYPE = SynchedEntityData.defineId(Boulder.class, EntityDataSerializers.INT);
    private int onGroundTicks = 0;

    public Boulder(EntityType<Boulder> boulderEntityType, Level level) {
        super(boulderEntityType, level);
    }

    public Boulder(Level level, BlockPos pos) {
        super(ModEntityType.BOULDER.get(), level);
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.xo = pos.getX() + 0.5;
        this.yo = pos.getY();
        this.zo = pos.getZ() + 0.5;
        this.setOnGround(false);
    }

    @Override
    protected Item getDropItem() {
        return Items.STONE;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOULDER_TYPE, BoulderType.STONE.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("boulderType", 8)) {
            this.setBoulderType(BoulderType.byName(compound.getString("boulderType")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("boulderType", this.entityData.get(BOULDER_TYPE));
    }

    public BoulderType getBoulderType() {
        return BoulderType.byId(this.entityData.get(BOULDER_TYPE));
    }

    public void setBoulderType(BoulderType direction) {
        this.entityData.set(BOULDER_TYPE, direction.ordinal());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Snap X and Z to the center of the nearest block
        double snappedX = Mth.floor(this.getX()) + 0.5;
        double snappedZ = Mth.floor(this.getZ()) + 0.5;
        this.setPos(snappedX, this.getY(), snappedZ);

        if (this.onGround() && this.getDeltaMovement().y == 0.0) {
            this.onGroundTicks++;
        }
        if (onGroundTicks >= 5) {
            this.placeBoulder();
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        float particleSpread = Mth.clamp(fallDistance * 0.1F / 2, 0.1F, 5.0F);
        ModParticleTypes.spawnSquareBorderParticles(this.level(), ParticleTypes.DUST_PLUME, this.blockPosition(), this.getBbWidth() / 2, (int) (20 * this.getBbWidth()), particleSpread);
        this.playSound(ModSoundEvents.BOULDER_LAND.get(), 1.0F, 1.0F);

        AtomicBoolean foundBlock = new AtomicBoolean(false);

        if (!this.level().isClientSide) {
            BlockPos center = this.blockPosition();

            Vec3 firstPos = new Vec3(center.getX() - 2, center.getY(), center.getZ() - 2);
            Vec3 secondPos = new Vec3(center.getX() + 3, center.getY() + 1, center.getZ() + 3);

            // Damage mobs in a 5x5 area at the boulder's level
            AABB damageArea = new AABB(firstPos, secondPos);
            List<Entity> entities = level().getEntitiesOfClass(Entity.class, damageArea, livingEntity -> livingEntity != Boulder.this);
            for (Entity entity : entities) {
                entity.hurt(this.damageSources().source(DamageTypes.FALLING_BLOCK), fallDistance * 1.5F);
            }

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos target = center.below().offset(dx, 0, dz);
                    BlockState state = level().getBlockState(target);
                    float hardness = state.getDestroySpeed(level(), target);

                    if (hardness >= 0 && hardness <= (fallDistance / 2.0f)) {
                        level().destroyBlock(target, true);

                        // If the block is not destroyed and is in 3x3 underground area, set foundBlock to true
                    } else if (dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1) {
                        foundBlock.set(true);
                    }
                }
            }
            if (foundBlock.get()) {
                placeBoulder();
            }
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    private void placeBoulder() {
        BoulderBlock.placeBoulder(level(), this.blockPosition(), getBoulderType(), HangingType.NONE);
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    // Makes it so that we can hti the boulder
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return true;
    }
}
