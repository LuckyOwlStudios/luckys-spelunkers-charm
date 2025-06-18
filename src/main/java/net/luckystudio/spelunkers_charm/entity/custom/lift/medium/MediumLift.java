package net.luckystudio.spelunkers_charm.entity.custom.lift.medium;

import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;

// Heavily copied from the Boat class in Minecraft, with modifications for a small lift entity.
public class MediumLift extends AbstractLift {

    public MediumLift(EntityType<?> entityType, Level level) {
        super(entityType, level, Type.MEDIUM);
    }

    public MediumLift(Level level, double x, double y, double z) {
        super(ModEntityType.MEDIUM_LIFT.get(), level, Type.MEDIUM, x, y, z);
    }

    @Override
    protected List<BlockPos> trackPositions() {
        Direction direction = this.getDirection();
        Direction leftDirection = direction.getCounterClockWise();
        Direction rightDirection = direction.getClockWise();
        BlockPos leftOffsetPos = this.blockPosition().relative(leftDirection, 3);
        BlockPos rightOffsetPos = this.blockPosition().relative(rightDirection, 3);

        BlockPos leftTrackOnePos = leftOffsetPos.relative(direction);
        BlockPos leftTrackTwoPos = leftOffsetPos.relative(direction.getOpposite());
        BlockPos rightTrackOnePos = rightOffsetPos.relative(direction);
        BlockPos rightTrackTwoPos = rightOffsetPos.relative(direction.getOpposite());
        return List.of(leftTrackOnePos, leftTrackTwoPos, rightTrackOnePos, rightTrackTwoPos);
    }
}
