package net.luckystudio.splelunkers_charm.entity.custom.lift.large;

import net.luckystudio.splelunkers_charm.entity.ModEntityType;
import net.luckystudio.splelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;

// Heavily copied from the Boat class in Minecraft, with modifications for a small lift entity.
public class LargeLift extends AbstractLift {

    public LargeLift(EntityType<?> entityType, Level level) {
        super(entityType, level, Type.LARGE);
    }

    public LargeLift(Level level, double x, double y, double z) {
        super(ModEntityType.LARGE_LIFT.get(), level, Type.LARGE, x, y, z);
    }

    @Override
    protected List<BlockPos> trackPositions() {
        Direction direction = this.getDirection();

        Direction leftDirection = direction.getCounterClockWise();
        Direction rightDirection = direction.getClockWise();

        BlockPos frontLinePos = this.blockPosition().relative(direction, 4);
        BlockPos leftLinePos = this.blockPosition().relative(direction.getCounterClockWise(), 4);
        BlockPos rightLinePos = this.blockPosition().relative(direction.getClockWise(), 4);
        BlockPos backLinePos = this.blockPosition().relative(direction.getOpposite(), 4);

        BlockPos frontTrackOnePos = frontLinePos.relative(leftDirection, 2);
        BlockPos frontTrackTwoPos = frontLinePos.relative(rightDirection, 2);

        BlockPos leftTrackOnePos = leftLinePos.relative(direction, 2);
        BlockPos leftTrackTwoPos = leftLinePos.relative(direction.getOpposite(), 2);

        BlockPos rightTrackOnePos = rightLinePos.relative(direction, 2);
        BlockPos rightTrackTwoPos = rightLinePos.relative(direction.getOpposite(), 2);

        BlockPos backTrackOnePos = backLinePos.relative(leftDirection, 2);
        BlockPos backTrackTwoPos = backLinePos.relative(rightDirection, 2);

        return List.of(frontTrackOnePos, frontTrackTwoPos, leftTrackOnePos, leftTrackTwoPos, rightTrackOnePos, rightTrackTwoPos, backTrackOnePos, backTrackTwoPos);
    }
}
