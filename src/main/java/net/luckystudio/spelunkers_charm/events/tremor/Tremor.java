package net.luckystudio.spelunkers_charm.events.tremor;

import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;

public class Tremor extends Marker {
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LENGTH = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAGNITUDE = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    // How strong the tremor currently is, from 0 (still) to 1 (full strength). Ramps up at the start and fades out at the end.
    private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.FLOAT);
    // Ticks spent in the warning phase before the shaking starts.
    int startUp = 0;

    public Tremor(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    public int getLength() {
        return this.entityData.get(LENGTH);
    }

    public void setLength(int length) {
        int maxLength = SpelunkersCharmConfig.TREMOR_MAX_LENGTH.get() * 20;
        this.entityData.set(LENGTH, Mth.clamp(length, 1, maxLength));
    }

    public int getMagnitude() {
        return this.entityData.get(MAGNITUDE);
    }

    public void setMagnitude(int magnitude) {
        this.entityData.set(MAGNITUDE, Mth.clamp(magnitude, 1, SpelunkersCharmConfig.TREMOR_MAX_MAGNITUDE.get()));
    }

    public float getIntensity() {
        return this.entityData.get(INTENSITY);
    }

    public void setIntensity(float intensity) {
        this.entityData.set(INTENSITY, Mth.clamp(intensity, 0.0F, 1.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIFETIME, 0);
        builder.define(LENGTH, 200);
        builder.define(MAGNITUDE, 1);
        builder.define(INTENSITY, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (this.startUp < SpelunkersCharmConfig.TREMOR_WARNING_TIME.get() * 20) {
            startUp++;
        } else {
            this.entityData.set(LIFETIME, this.entityData.get(LIFETIME) + 1);
            if (this.entityData.get(LIFETIME) > this.entityData.get(LENGTH)) {
                this.discard();
            } else {
                TremorManager.tremorTick(this);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LIFETIME, compound.getInt("Lifetime"));
        if (compound.contains("Length")) this.entityData.set(LENGTH, compound.getInt("Length"));
        if (compound.contains("Magnitude")) this.entityData.set(MAGNITUDE, compound.getInt("Magnitude"));
        this.entityData.set(INTENSITY, compound.getFloat("Intensity"));
        this.startUp = compound.getInt("StartUp");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Lifetime", this.getLifetime());
        compound.putInt("Length", this.getLength());
        compound.putInt("Magnitude", this.getMagnitude());
        compound.putFloat("Intensity", this.getIntensity());
        compound.putInt("StartUp", this.startUp);
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
