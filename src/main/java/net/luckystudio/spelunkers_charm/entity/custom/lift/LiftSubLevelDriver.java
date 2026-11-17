package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Decides where a Sable lift deck should go, using the same lift track rules as the entity lift.
 *
 * <p>A lift entity is kept here only as a probe: it's never added to the world. Moving it to the
 * deck's position lets the existing {@link LiftGuideSystem} read the tracks around the deck. The
 * deck itself is a Sable sub-level, driven by setting its vertical velocity each physics step.
 */
public final class LiftSubLevelDriver {
    /** Blocks per second. The entity lift moved 0.15 blocks a tick. */
    public static final double SPEED = 3.0;
    /** Physics can leave the deck a hair off the row; anything this close counts as on it. */
    private static final double FLOOR_EPSILON = 0.05;
    private static final int STALL_TICKS = 10;

    private final AbstractLift probe;
    private final LiftGuideSystem guides;
    private double x, z;
    /** World bounds of the deck and everything built on it, as of the last tick. */
    @Nullable private AABB bounds;
    private double deckYNow;

    private MovementType movement = MovementType.STATIONARY;
    private MovementType preferred = MovementType.UP;
    private MovementType pausedForPower = MovementType.STATIONARY;
    private int holdRow = Integer.MIN_VALUE;
    private int targetRow = Integer.MIN_VALUE;
    private boolean stopsAtTarget;
    private int manualOverrideRow = Integer.MIN_VALUE;
    private double lastDeckY = Double.NaN;
    private int stalledTicks;

    public LiftSubLevelDriver(Level level, AbstractLift.Type type, Direction facing, double x, double z) {
        this.x = x;
        this.z = z;
        this.probe =
                switch (type) {
                    case SMALL -> new SmallLift(level, x, 0, z);
                    case MEDIUM -> new MediumLift(level, x, 0, z);
                    case LARGE -> new LargeLift(level, x, 0, z);
                };
        probe.setYRot(facing.toYRot());
        this.guides = new LiftGuideSystem(probe);
    }

    // ---------------------------------------------------------------- save state

    public MovementType movement() {
        return movement;
    }

    public MovementType preferred() {
        return preferred;
    }

    public int holdRow() {
        return holdRow;
    }

    public void restore(MovementType movement, MovementType preferred, int holdRow) {
        // A deck that was moving when saved finishes at the row it last held.
        this.movement =
                movement == MovementType.FALLING ? MovementType.FALLING : MovementType.STATIONARY;
        if (preferred == MovementType.UP || preferred == MovementType.DOWN) this.preferred = preferred;
        this.holdRow = holdRow;
    }

    // ---------------------------------------------------------------- server tick

    /** Whether the deck should be held on its rail: it's on tracks. Off them it's loose. */
    public boolean onTracks() {
        return movement != MovementType.FALLING;
    }

    /**
     * Runs once a game tick with the world position of the deck's bottom center. The deck may have
     * fallen off its tracks and come to rest somewhere else, so the probe follows it everywhere.
     */
    public void tick(double x, double deckY, double z, @Nullable AABB bounds) {
        this.x = x;
        this.z = z;
        this.bounds = bounds;
        this.deckYNow = deckY;
        AABB before = probe.getBoundingBox();
        placeProbe(deckY);
        if (!probe.hasValidTracks()) {
            // Off the tracks: let Sable drop and tumble it like any other loose structure.
            movement = MovementType.FALLING;
            pausedForPower = MovementType.STATIONARY;
            probe.setMovementType(movement);
            guides.updateStoppedGuideState(); // releases the tracks it was resting on
            return;
        }
        if (movement == MovementType.FALLING || holdRow == Integer.MIN_VALUE) {
            movement = MovementType.STATIONARY;
            holdRow = (int) Math.rint(deckY);
        }

        if (movement == MovementType.UP || movement == MovementType.DOWN) tickMoving(deckY);
        else tickStationary(deckY);

        probe.setMovementType(movement);
        guides.updateStoppedGuideState();
        guides.updateDetectors(before, probe.getBoundingBox());
    }

    private void tickStationary(double deckY) {
        if (!onRow(deckY, holdRow)) return;
        if (manualOverrideRow == holdRow && !guides.stationHasInput(holdRow))
            manualOverrideRow = Integer.MIN_VALUE;
        MovementType command =
                manualOverrideRow == holdRow ? MovementType.STATIONARY : guides.stationCommand();
        if (command == MovementType.UP || command == MovementType.DOWN) {
            if (canMove(command, holdRow)) start(command);
            else if (!blocked(command, holdRow) && guides.hasTracksAtY(holdRow + step(command)))
                pausedForPower = command;
        } else if (command == MovementType.FALLING) {
            // Two opposite powered arrows are deliberately a stop.
            pausedForPower = MovementType.STATIONARY;
        }
        if (movement == MovementType.STATIONARY
                && (pausedForPower == MovementType.UP || pausedForPower == MovementType.DOWN)
                && canMove(pausedForPower, holdRow)) start(pausedForPower);
    }

    private void tickMoving(double deckY) {
        int dir = step(movement);
        if (manualOverrideRow != Integer.MIN_VALUE && !onRow(deckY, manualOverrideRow))
            manualOverrideRow = Integer.MIN_VALUE;

        // Blocked by something (a block built into the shaft, the ceiling...): go back a row.
        if (!Double.isNaN(lastDeckY) && Math.abs(deckY - lastDeckY) < 1.0E-3) {
            if (++stalledTicks >= STALL_TICKS) {
                stop(true);
                return;
            }
        } else stalledTicks = 0;
        lastDeckY = deckY;

        boolean arrived = dir > 0 ? deckY >= targetRow - FLOOR_EPSILON : deckY <= targetRow + FLOOR_EPSILON;
        if (arrived) {
            holdRow = targetRow;
            if (guides.isStationAtY(targetRow)) {
                preferred = movement;
                stop(true);
                return;
            }
            if (blocked(movement, targetRow)) {
                // Something's in the way of the deck or what's built on it: stop here.
                stop(true);
                return;
            }
            if (canMove(movement, targetRow)) {
                targetRow += dir;
            } else {
                if (guides.hasTracksAtY(targetRow + dir)) pausedForPower = movement;
                else preferred = opposite(movement);
                stop(true);
                return;
            }
        }
        stopsAtTarget = guides.isStationAtY(targetRow) || !canMove(movement, targetRow);
    }

    /** Down needs no power; up needs the current and next rows to be able to drive. */
    private boolean canMove(MovementType direction, int fromRow) {
        int next = fromRow + step(direction);
        if (!guides.hasTracksAtY(fromRow) || !guides.hasTracksAtY(next)) return false;
        if (direction == MovementType.UP
                && !(guides.rowCanDrive(fromRow) && guides.rowCanDrive(next))) return false;
        return !blocked(direction, fromRow);
    }

    /**
     * Whether a solid world block sits in the layer the deck (and anything built on it) would move
     * into, leaving {@code fromRow} in {@code direction}. Lift tracks don't count: the gears run in
     * them.
     */
    private boolean blocked(MovementType direction, int fromRow) {
        if (bounds == null) return false;
        AABB box = bounds.move(0, fromRow - deckYNow, 0);
        int y =
                direction == MovementType.UP
                        ? (int) Math.ceil(box.maxY - FLOOR_EPSILON)
                        : (int) Math.floor(box.minY + FLOOR_EPSILON) - 1;
        int minX = (int) Math.floor(box.minX + FLOOR_EPSILON);
        int maxX = (int) Math.ceil(box.maxX - FLOOR_EPSILON) - 1;
        int minZ = (int) Math.floor(box.minZ + FLOOR_EPSILON);
        int maxZ = (int) Math.ceil(box.maxZ - FLOOR_EPSILON) - 1;
        Level level = probe.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int bx = minX; bx <= maxX; ++bx)
            for (int bz = minZ; bz <= maxZ; ++bz) {
                pos.set(bx, y, bz);
                if (!level.isLoaded(pos)) return true;
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof LiftTrackBlock) continue;
                if (!state.getCollisionShape(level, pos).isEmpty()) return true;
            }
        return false;
    }

    private void start(MovementType direction) {
        pausedForPower = MovementType.STATIONARY;
        preferred = direction;
        movement = direction;
        targetRow = holdRow + step(direction);
        stopsAtTarget = guides.isStationAtY(targetRow) || !canMove(direction, targetRow);
        stalledTicks = 0;
        lastDeckY = Double.NaN;
        playSound(ModSoundEvents.LIFT_START.get());
    }

    private void stop(boolean sound) {
        if ((movement == MovementType.UP || movement == MovementType.DOWN) && sound)
            playSound(ModSoundEvents.LIFT_STOP.get());
        movement = MovementType.STATIONARY;
        stalledTicks = 0;
        lastDeckY = Double.NaN;
    }

    // ---------------------------------------------------------------- physics

    /**
     * The vertical velocity the deck should have this physics step, in blocks per second, or NaN to
     * leave it to Sable's own physics (falling with no tracks).
     */
    public double targetVelocity(double deckY, double timeStep) {
        if (movement == MovementType.FALLING) return Double.NaN;
        // Keep the correction stable however long Sable's physics step is.
        double gain = Math.min(20.0, 0.5 / Math.max(timeStep, 1.0E-4));
        if (movement == MovementType.UP || movement == MovementType.DOWN) {
            int dir = step(movement);
            double remaining = targetRow - deckY;
            if (!stopsAtTarget) return dir * SPEED;
            if (remaining * dir <= 0) return remaining * gain;
            return dir * Math.min(SPEED, Math.abs(remaining) * gain + 0.25);
        }
        if (holdRow == Integer.MIN_VALUE) return 0;
        return clamp((holdRow - deckY) * gain, -SPEED, SPEED);
    }

    // ---------------------------------------------------------------- player use

    /** Right-click: start or stop. Sneak: reverse. */
    public void use(Player player, double deckY) {
        placeProbe(deckY);
        if (movement == MovementType.FALLING) {
            player.displayClientMessage(
                    Component.translatable("message.spelunkers_charm.lift_tracks"), true);
            return;
        }
        pausedForPower = MovementType.STATIONARY;
        boolean moving = movement == MovementType.UP || movement == MovementType.DOWN;
        if (moving) {
            if (player.isSecondaryUseActive()) {
                // Reverse: head back to the row it just left.
                int dir = step(movement);
                movement = opposite(movement);
                preferred = movement;
                targetRow -= dir;
                stopsAtTarget = guides.isStationAtY(targetRow) || !canMove(movement, targetRow);
                stalledTicks = 0;
                lastDeckY = Double.NaN;
                playSound(ModSoundEvents.LIFT_START.get());
            } else {
                // Finish the row in progress, then hold there.
                holdRow = targetRow;
                stop(true);
                manualOverrideRow = holdRow;
            }
            return;
        }
        manualOverrideRow = holdRow;
        MovementType first = player.isSecondaryUseActive() ? opposite(preferred) : preferred;
        if (canMove(first, holdRow)) start(first);
        else if (canMove(opposite(first), holdRow)) start(opposite(first));
        else
            player.displayClientMessage(
                    Component.translatable("message.spelunkers_charm.lift_cannot_move"), true);
    }

    // ---------------------------------------------------------------- helpers

    public void clearGuides() {
        guides.clearStoppedGuideState();
    }

    private void placeProbe(double deckY) {
        // Snap to the row when close, so the guide system's exact floor checks line up.
        double rounded = Math.rint(deckY);
        probe.setPos(x, Math.abs(deckY - rounded) < FLOOR_EPSILON ? rounded : deckY, z);
    }

    private static boolean onRow(double deckY, int row) {
        return Math.abs(deckY - row) < FLOOR_EPSILON;
    }

    private static int step(MovementType direction) {
        return direction == MovementType.DOWN ? -1 : 1;
    }

    private static MovementType opposite(MovementType direction) {
        return direction == MovementType.DOWN ? MovementType.UP : MovementType.DOWN;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void playSound(SoundEvent sound) {
        probe.level()
                .playSound(
                        null,
                        BlockPos.containing(probe.getX(), probe.getY(), probe.getZ()),
                        sound,
                        SoundSource.BLOCKS,
                        probe.getLevelBasedOnType(),
                        probe.getPitchBasedOnType());
    }
}
