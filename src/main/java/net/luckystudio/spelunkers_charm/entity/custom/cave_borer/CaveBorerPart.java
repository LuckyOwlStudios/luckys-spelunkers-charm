package net.luckystudio.spelunkers_charm.entity.custom.cave_borer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One body segment of a {@link CaveBorer}. It only exists as a hitbox: hitting it hurts the worm.
 * Its position is set every tick by the worm, on both the server and the client.
 */
public class CaveBorerPart extends PartEntity<CaveBorer> {
    /** Segment index along the body. The head is segment 0, so parts start at 1. */
    public final int segment;

    public CaveBorerPart(CaveBorer parent, int segment) {
        super(parent);
        this.segment = segment;
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    @Override
    public boolean isPickable() {
        return this.getParent().isSegmentActive(this.segment);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!this.isPickable() || this.isInvulnerableTo(source)) return false;
        return this.getParent().hurt(source, amount);
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float width = this.getParent().getSegmentHitboxWidth(this.segment);
        return EntityDimensions.scalable(width, width);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }
}
