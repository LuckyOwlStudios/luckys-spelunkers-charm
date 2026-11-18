package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Placement ownership: one platform per continuous, complete vertical guide route. */
public final class LiftShaft {
    private static final double EPSILON = LiftCollisionGeometry.EPSILON;

    private LiftShaft() {}

    public static boolean isOccupied(ServerLevel level, AbstractLift candidate) {
        AABB deck = candidate.getBoundingBox().deflate(EPSILON, 0, EPSILON);
        AABB column =
                new AABB(
                        deck.minX,
                        level.getMinBuildHeight(),
                        deck.minZ,
                        deck.maxX,
                        level.getMaxBuildHeight(),
                        deck.maxZ);
        List<AbstractLift> existing =
                level.getEntitiesOfClass(
                        AbstractLift.class, column, lift -> lift != candidate && !lift.isRemoved());
        if (existing.isEmpty()) return false;

        LiftGuideSystem guides = new LiftGuideSystem(candidate);
        int bottom = Mth.floor(candidate.getY() + EPSILON);
        int top = bottom;
        while (bottom > level.getMinBuildHeight() && guides.hasTracksAtY(bottom - 1)) --bottom;
        while (top < level.getMaxBuildHeight() - 1 && guides.hasTracksAtY(top + 1)) ++top;
        for (AbstractLift lift : existing) {
            if (lift.getBoundingBox().maxY > bottom + EPSILON && lift.getY() < top + 1 - EPSILON) {
                return true;
            }
        }
        return false;
    }
}
