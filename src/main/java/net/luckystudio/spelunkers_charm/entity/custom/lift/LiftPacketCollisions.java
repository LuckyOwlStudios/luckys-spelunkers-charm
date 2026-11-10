package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** The ordinary collision rules with one validated, supporting deck omitted. */
public final class LiftPacketCollisions {
    private LiftPacketCollisions() {}

    public static Iterable<VoxelShape> getCollisions(
            LevelReader reader, Entity mover, AABB bounds) {
        if (!(reader instanceof Level level)) return reader.getCollisions(mover, bounds);
        List<VoxelShape> result = entityCollisions(level, mover, bounds);
        for (VoxelShape block : reader.getBlockCollisions(mover, bounds)) result.add(block);
        return result;
    }

    public static boolean noCollision(ServerLevel level, Entity mover, AABB bounds) {
        if (!level.noBlockCollision(mover, bounds)
                || !entityCollisions(level, mover, bounds).isEmpty()) {
            return false;
        }
        WorldBorder border = level.getWorldBorder();
        return !border.isInsideCloseToBorder(mover, bounds)
                || !Shapes.joinIsNotEmpty(
                        border.getCollisionShape(), Shapes.create(bounds), BooleanOp.AND);
    }

    private static List<VoxelShape> entityCollisions(Level level, Entity mover, AABB bounds) {
        List<VoxelShape> result = new ArrayList<>();
        if (bounds.getSize() < 1.0E-7) return result;
        for (Entity obstacle :
                level.getEntities(
                        mover,
                        bounds.inflate(1.0E-7),
                        EntitySelector.NO_SPECTATORS.and(mover::canCollideWith))) {
            if (obstacle instanceof AbstractLift lift
                    && LiftCollision.isPacketSupport(mover, lift, bounds)) {
                continue;
            }
            result.add(Shapes.create(obstacle.getBoundingBox()));
        }
        return result;
    }
}
