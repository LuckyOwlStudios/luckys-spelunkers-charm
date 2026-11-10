package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.block.custom.DetectorLiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.DirectionalLiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.PoweredLiftTrackBlock;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/** Guide validation, motor thresholds, station commands and track occupancy signals. */
public final class LiftGuideSystem {
    private static final double EPSILON = LiftCollisionGeometry.EPSILON;
    private final AbstractLift lift;
    private final Set<BlockPos> stoppedGuidePositions = new HashSet<>();

    public LiftGuideSystem(AbstractLift lift) {
        this.lift = lift;
    }

    void save(CompoundTag tag) {
        tag.putLongArray(
                "liftStoppedGuides",
                stoppedGuidePositions.stream().mapToLong(BlockPos::asLong).toArray());
    }

    void load(CompoundTag tag) {
        stoppedGuidePositions.clear();
        for (long packed : tag.getLongArray("liftStoppedGuides")) {
            if (stoppedGuidePositions.size() == 8) break;
            stoppedGuidePositions.add(BlockPos.of(packed));
        }
    }

    private boolean isAtFloor() {
        return Math.abs(lift.getY() - Math.rint(lift.getY())) < EPSILON;
    }

    void clearStoppedGuideState() {
        for (BlockPos pos : stoppedGuidePositions) setGuidePresent(pos, false);
        stoppedGuidePositions.clear();
    }

    boolean rowCanDrive(int y) {
        if (isStationAtY(y)) return true;
        if (!hasTracksAtY(y)) return false;
        int motors = 0;
        for (BlockPos guide : lift.getGuidePositions()) {
            BlockState state = lift.level().getBlockState(guide.atY(y));
            if (state.getBlock() instanceof PoweredLiftTrackBlock
                    && state.getValue(BlockStateProperties.POWERED)) ++motors;
        }
        return motors >= (lift.getGuidePositions().size() + 1) / 2;
    }

    boolean hasTracksAtY(int y) {
        for (BlockPos guide : lift.getGuidePositions()) {
            BlockState state = lift.level().getBlockState(guide.atY(y));
            if (!(state.getBlock() instanceof LiftTrackBlock)
                    || state.getValue(LiftTrackBlock.FACING) != inwardFacing(guide)) return false;
        }
        return true;
    }

    boolean isStationAtY(int y) {
        if (!hasTracksAtY(y)) return false;
        for (BlockPos guide : lift.getGuidePositions()) {
            if (lift.level()
                    .getBlockState(guide.atY(y))
                    .hasProperty(BlockStateProperties.VERTICAL_DIRECTION)) return true;
        }
        return false;
    }

    Direction inwardFacing(BlockPos pos) {
        double dx = lift.getX() - pos.getX() - 0.5;
        double dz = lift.getZ() - pos.getZ() - 0.5;
        return Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    boolean stationHasInput(int y) {
        for (BlockPos guide : lift.getGuidePositions()) {
            BlockState state = lift.level().getBlockState(guide.atY(y));
            if (state.hasProperty(DirectionalLiftTrackBlock.SIGNAL_HELD)
                    && (state.getValue(DirectionalLiftTrackBlock.SIGNAL_HELD)
                            || state.getValue(BlockStateProperties.POWERED))) return true;
        }
        return false;
    }

    MovementType stationCommand() {
        if (!isAtFloor()) return MovementType.STATIONARY;
        int y = (int) Math.rint(lift.getY());
        if (!isStationAtY(y)) return MovementType.STATIONARY;
        boolean up = false, down = false;
        for (BlockPos guide : lift.getGuidePositions()) {
            BlockState state = lift.level().getBlockState(guide.atY(y));
            if (!state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION)) continue;
            if (!state.getValue(BlockStateProperties.POWERED)) continue;
            if (state.getValue(BlockStateProperties.VERTICAL_DIRECTION) == Direction.UP) up = true;
            else down = true;
        }
        if (up && down) return MovementType.FALLING;
        return up ? MovementType.UP : down ? MovementType.DOWN : MovementType.STATIONARY;
    }

    void updateStoppedGuideState() {
        Set<BlockPos> present = new HashSet<>();
        if (lift.getMovementType() == MovementType.STATIONARY && lift.hasValidTracks()) {
            int row = Mth.floor(lift.getY() + 0.5);
            if (!hasTracksAtY(row)) row = Mth.floor(lift.getY());
            if (hasTracksAtY(row))
                for (BlockPos guide : lift.getGuidePositions())
                    present.add(guide.atY(row).immutable());
        }
        // Genuine block-state transitions let vanilla observers detect both
        // arrival and departure. A stationary lift never repeatedly updates it.
        for (BlockPos old : stoppedGuidePositions)
            if (!present.contains(old)) setGuidePresent(old, false);
        for (BlockPos pos : present) setGuidePresent(pos, true);
        stoppedGuidePositions.clear();
        stoppedGuidePositions.addAll(present);
    }

    void setGuidePresent(BlockPos pos, boolean present) {
        if (!lift.level().hasChunkAt(pos)) return;
        BlockState state = lift.level().getBlockState(pos);
        if (state.getBlock() instanceof LiftTrackBlock
                && state.getValue(LiftTrackBlock.LIFT_PRESENT) != present)
            lift.level().setBlock(pos, state.setValue(LiftTrackBlock.LIFT_PRESENT, present), 3);
    }

    void updateDetectors(AABB before, AABB after) {
        int low = Mth.floor(Math.min(before.minY, after.minY) + EPSILON);
        int high = Mth.floor(Math.max(before.maxY, after.maxY) - EPSILON);
        for (BlockPos guide : lift.getGuidePositions())
            for (int y = low; y <= high; ++y) {
                BlockPos pos = guide.atY(y);
                if (lift.level().getBlockState(pos).getBlock()
                        instanceof DetectorLiftTrackBlock detector)
                    detector.refresh(lift.level(), pos);
            }
    }
}
