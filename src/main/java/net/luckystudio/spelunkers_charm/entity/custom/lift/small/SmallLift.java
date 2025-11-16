package net.luckystudio.spelunkers_charm.entity.custom.lift.small;

import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;

// Heavily copied from the Boat class in Minecraft, with modifications for a small lift entity.
public class SmallLift extends AbstractLift {


    public SmallLift(EntityType<?> entityType, Level level) {
        super(entityType, level, Type.SMALL);
    }

    public SmallLift(Level level, double x, double y, double z) {
        super(ModEntityType.SMALL_LIFT.get(), level, Type.SMALL, x, y, z);
        this.maxUpStep();
    }

    @Override
    protected List<BlockPos> trackPositions() {
        Direction direction = this.getDirection();
        Direction leftDirection = direction.getCounterClockWise();
        Direction rightDirection = direction.getClockWise();
        BlockPos leftTrackPos = this.blockPosition().relative(leftDirection, 2);
        BlockPos rightTrackPos = this.blockPosition().relative(rightDirection, 2);
        return List.of(leftTrackPos, rightTrackPos);
    }
}
