package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/** Pose clearance must not mistake the supporting deck's next step for a ceiling. */
public final class LiftPoseCollisions {
    private static final double DECK_STEP_TOLERANCE = 0.20;

    private LiftPoseCollisions() {}

    public static boolean canFitOnSupportingLift(Player player, Pose pose) {
        if (player.isPassenger()
                || player.isSpectator()
                || player.getAbilities().flying
                || player.getDeltaMovement().y > 0.1) return false;
        Level level = player.level();
        AABB bounds = player.getDimensions(pose).makeBoundingBox(player.position()).deflate(1.0E-7);
        if (!level.noBlockCollision(player, bounds)) return false;
        boolean supportingDeck = false;
        for (Entity obstacle :
                level.getEntities(
                        player,
                        bounds.inflate(1.0E-7),
                        EntitySelector.NO_SPECTATORS.and(player::canCollideWith))) {
            if (obstacle instanceof AbstractLift lift && isSupportingDeck(player, lift)) {
                supportingDeck = true;
            } else {
                return false;
            }
        }
        var border = level.getWorldBorder();
        return supportingDeck
                && (!border.isInsideCloseToBorder(player, bounds)
                        || !Shapes.joinIsNotEmpty(
                                border.getCollisionShape(), Shapes.create(bounds), BooleanOp.AND));
    }

    private static boolean isSupportingDeck(Player player, AbstractLift lift) {
        AABB feet = player.getBoundingBox();
        AABB deck = lift.getBoundingBox();
        return !lift.isRemoved()
                && Math.abs(feet.minY - deck.maxY) <= DECK_STEP_TOLERANCE
                && LiftCollisionGeometry.overlaps(feet.minX, feet.maxX, deck.minX, deck.maxX)
                && LiftCollisionGeometry.overlaps(feet.minZ, feet.maxZ, deck.minZ, deck.maxZ);
    }
}
