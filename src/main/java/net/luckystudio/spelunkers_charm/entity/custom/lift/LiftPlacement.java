package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.shapes.Shapes;

/** Resolve rack clicks and floor clicks, then validate clearance and shaft ownership. */
public final class LiftPlacement {
    public record Result(AbstractLift lift, boolean occupiedShaft) {}

    private final ServerLevel level;
    private final UseOnContext context;
    private final AbstractLift.Type type;
    private boolean occupiedShaft;

    private LiftPlacement(ServerLevel level, UseOnContext context, AbstractLift.Type type) {
        this.level = level;
        this.context = context;
        this.type = type;
    }

    public static Result find(ServerLevel level, UseOnContext context, AbstractLift.Type type) {
        LiftPlacement placement = new LiftPlacement(level, context, type);
        AbstractLift lift = placement.findCandidate();
        return new Result(lift, placement.occupiedShaft);
    }

    private AbstractLift findCandidate() {
        BlockPos clicked = context.getClickedPos();
        return level.getBlockState(clicked).getBlock() instanceof LiftTrackBlock
                ? findFromGuide(clicked)
                : findFromFloor();
    }

    private AbstractLift findFromGuide(BlockPos clicked) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            AbstractLift probe =
                    create(clicked.getX() + 0.5, clicked.getY(), clicked.getZ() + 0.5, facing);
            for (BlockPos guide : probe.getGuidePositions()) {
                AbstractLift candidate =
                        create(
                                probe.getX() + clicked.getX() - guide.getX(),
                                clicked.getY(),
                                probe.getZ() + clicked.getZ() - guide.getZ(),
                                facing);
                if (isValid(candidate)) return candidate;
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
            if (isValid(candidate)) return candidate;
            facing = facing.getClockWise();
        }
        return null;
    }

    private AbstractLift create(double x, double y, double z, Direction facing) {
        AbstractLift lift =
                AbstractLift.createLift(
                        level, x, y, z, type, context.getItemInHand(), context.getPlayer());
        lift.setYRot(facing.toYRot());
        lift.setYHeadRot(facing.toYRot());
        return lift;
    }

    private boolean isValid(AbstractLift lift) {
        if (!lift.hasValidTracks() || !level.getWorldBorder().isWithinBounds(lift.getBoundingBox()))
            return false;
        if (LiftShaft.isOccupied(level, lift)) {
            occupiedShaft = true;
            return false;
        }
        return level.noCollision(lift, lift.getBoundingBox().deflate(1.0E-7))
                && level.isUnobstructed(lift, Shapes.create(lift.getBoundingBox().deflate(1.0E-7)));
    }
}
