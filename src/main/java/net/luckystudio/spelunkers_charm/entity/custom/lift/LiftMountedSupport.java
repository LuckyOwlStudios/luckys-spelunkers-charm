// Lift mounted support.
package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.IdentityHashMap;
import java.util.Map;

public final class LiftMountedSupport {
    private static final Map<Entity, Double> PREVIOUS_Y = new IdentityHashMap<>();

    private LiftMountedSupport() {}

    public static void capture(Entity player) {
        PREVIOUS_Y.clear();
        if (player != null && player.isPassenger()) capturePassengers(player.getRootVehicle());
    }

    private static void capturePassengers(Entity entity) {
        PREVIOUS_Y.put(entity, entity.getY());
        for (Entity passenger : entity.getPassengers()) capturePassengers(passenger);
    }

    public static boolean hasLocalRider(Entity vehicle) {
        return PREVIOUS_Y.containsKey(vehicle);
    }

    public static void align(Entity vehicle, AbstractLift lift, float relativeY) {
        Double previousY = PREVIOUS_Y.get(vehicle);
        AABB bounds = vehicle.getBoundingBox();
        AABB deck = lift.getBoundingBox();
        if (previousY == null
                || !LiftCollisionGeometry.overlaps(bounds.minX, bounds.maxX, deck.minX, deck.maxX)
                || !LiftCollisionGeometry.overlaps(bounds.minZ, bounds.maxZ, deck.minZ, deck.maxZ)
                || !LiftCollisionGeometry.supports(
                        lift.yOld + lift.getBbHeight(),
                        deck.maxY,
                        bounds.minY,
                        previousY + bounds.minY - vehicle.getY(),
                        vehicle.getDeltaMovement().y,
                        !vehicle.isNoGravity())) return;

        double shift = lift.getY() + relativeY - vehicle.getY();
        vehicle.setPos(vehicle.getX(), lift.getY() + relativeY, vehicle.getZ());
        vehicle.yOld = lift.yOld + relativeY;
        vehicle.yo = vehicle.yOld;
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().x, 0, vehicle.getDeltaMovement().z);
        vehicle.setOnGround(true);
        alignPassengers(vehicle, shift, vehicle.yOld - previousY);
    }

    private static void alignPassengers(Entity vehicle, double shift, double oldShift) {
        for (Entity passenger : vehicle.getPassengers()) {
            passenger.setPos(passenger.getX(), passenger.getY() + shift, passenger.getZ());
            Double previousY = PREVIOUS_Y.get(passenger);
            if (previousY != null) {
                passenger.yOld = previousY + oldShift;
                passenger.yo = passenger.yOld;
            }
            alignPassengers(passenger, shift, oldShift);
        }
    }
}
