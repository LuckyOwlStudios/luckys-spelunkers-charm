package net.luckystudio.spelunkers_charm.events.tremor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;

public class Tremor extends Marker {
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LENGTH = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAGNITUDE = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(Tremor.class, EntityDataSerializers.FLOAT);
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
        this.entityData.set(LENGTH, Math.min(length, 1000)); // Make sure length is capped at 1000
    }

    public int getMagnitude() {
        return this.entityData.get(MAGNITUDE);
    }

    public void setMagnitude(int magnitude) {
        this.entityData.set(MAGNITUDE, magnitude); // Make sure intensity is capped at 10
    }

    public float getIntensity() {
        return this.entityData.get(INTENSITY);
    }

    public void incrementIntensity() {
        this.entityData.set(INTENSITY, this.getIntensity() + 1); // Make sure intensity is capped at 10
    }

    public void decrementIntensity() {
        this.entityData.set(INTENSITY, this.getIntensity() - 1); // Make sure intensity is capped at 10
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
        if (this.startUp < 100) {
            startUp++;
        } else {
            this.entityData.set(LIFETIME, this.entityData.get(LIFETIME) + 1);
            if (this.entityData.get(LIFETIME) > this.entityData.get(LENGTH)) {
                this.remove(RemovalReason.DISCARDED);
            } else {
                TremorManager.tremorTick(this);
            }
        }
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
