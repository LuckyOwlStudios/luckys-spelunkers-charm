package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Works out where a lift goes when a lift item is used.
 *
 * <p>A fully tracked spot wins. Failing that, the lift can go down without tracks: clicking a
 * single track puts the lift on the side that track faces, with that track as one of its guides,
 * and clicking the floor puts it there facing the way the player looks. The track outlines then
 * show where the rest of the tracks go. Runs on both sides; the client uses it for the preview.
 */
public final class LiftPlacement {
    public record Result(AbstractLift lift, boolean occupiedShaft) {}

    private final Level level;
    private final UseOnContext context;
    private final AbstractLift.Type type;
    private boolean occupiedShaft;

    private LiftPlacement(Level level, UseOnContext context, AbstractLift.Type type) {
        this.level = level;
        this.context = context;
        this.type = type;
    }

    public static Result find(Level level, UseOnContext context, AbstractLift.Type type) {
        LiftPlacement placement = new LiftPlacement(level, context, type);
        AbstractLift lift = placement.findCandidate();
        return new Result(lift, placement.occupiedShaft);
    }

    private AbstractLift findCandidate() {
        BlockPos clicked = context.getClickedPos();
        boolean onTrack = level.getBlockState(clicked).getBlock() instanceof LiftTrackBlock;
        AbstractLift tracked = onTrack ? findFromGuide(clicked) : findFromFloor();
        if (tracked != null || occupiedShaft) return tracked;
        return onTrack ? findBesideTrack(clicked) : findLooseOnFloor();
    }

    private AbstractLift findFromGuide(BlockPos clicked) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (AbstractLift candidate : aroundGuide(clicked, facing)) {
                if (isValid(candidate, true)) return candidate;
            }
        }
        return null;
    }

    private AbstractLift findFromFloor() {
        BlockPos cell = new BlockPlaceContext(context).getClickedPos();
        Direction facing = context.getHorizontalDirection();
        for (int turn = 0; turn < 4; ++turn) {
            AbstractLift candidate =
                    create(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, facing);
            if (isValid(candidate, true)) return candidate;
            facing = facing.getClockWise();
        }
        return null;
    }

    /** One track down: put the lift where that track is a guide and faces into the deck. */
    private AbstractLift findBesideTrack(BlockPos clicked) {
        BlockState track = level.getBlockState(clicked);
        Direction player = context.getHorizontalDirection();
        // Prefer the player's own facing, so the result matches what they expect to see.
        for (int turn = 0; turn < 4; ++turn) {
            Direction facing = turn == 0 ? player : player.getClockWise();
            player = facing;
            for (AbstractLift candidate : aroundGuide(clicked, facing)) {
                LiftGuideSystem guides = new LiftGuideSystem(candidate);
                if (track.getValue(LiftTrackBlock.FACING) == guides.inwardFacing(clicked)
                        && isValid(candidate, false)) return candidate;
            }
        }
        return null;
    }

    private AbstractLift findLooseOnFloor() {
        BlockPos cell = new BlockPlaceContext(context).getClickedPos();
        AbstractLift candidate =
                create(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, context.getHorizontalDirection());
        return isValid(candidate, false) ? candidate : null;
    }

    /** Every lift of this facing that would have {@code guide} as one of its track spots. */
    private Iterable<AbstractLift> aroundGuide(BlockPos guide, Direction facing) {
        AbstractLift probe = create(guide.getX() + 0.5, guide.getY(), guide.getZ() + 0.5, facing);
        java.util.List<AbstractLift> result = new java.util.ArrayList<>();
        for (BlockPos spot : probe.getGuidePositions()) {
            result.add(
                    create(
                            probe.getX() + guide.getX() - spot.getX(),
                            guide.getY(),
                            probe.getZ() + guide.getZ() - spot.getZ(),
                            facing));
        }
        return result;
    }

    private AbstractLift create(double x, double y, double z, Direction facing) {
        AbstractLift lift =
                level instanceof ServerLevel server
                        ? AbstractLift.createLift(
                                server, x, y, z, type, context.getItemInHand(), context.getPlayer())
                        : switch (type) {
                            case SMALL -> new SmallLift(level, x, y, z);
                            case MEDIUM -> new MediumLift(level, x, y, z);
                            case LARGE -> new LargeLift(level, x, y, z);
                        };
        lift.setYRot(facing.toYRot());
        lift.setYHeadRot(facing.toYRot());
        return lift;
    }

    private boolean isValid(AbstractLift lift, boolean needTracks) {
        if (needTracks && !lift.hasValidTracks()) return false;
        if (!level.getWorldBorder().isWithinBounds(lift.getBoundingBox())) return false;
        if (needTracks && level instanceof ServerLevel server && LiftShaft.isOccupied(server, lift)) {
            occupiedShaft = true;
            return false;
        }
        return level.noCollision(lift, lift.getBoundingBox().deflate(1.0E-7))
                && level.isUnobstructed(lift, Shapes.create(lift.getBoundingBox().deflate(1.0E-7)));
    }
}
