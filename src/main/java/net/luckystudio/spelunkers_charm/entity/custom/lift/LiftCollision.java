package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Collision for an entity-backed lift deck; no additional mod dependency. */
public final class LiftCollision {
    private static final double EPSILON = LiftCollisionGeometry.EPSILON;
    private static final double SUPPORT_TOLERANCE = LiftCollisionGeometry.SUPPORT_TOLERANCE;
    private static final String CONTACT_LIFT = "spelunkers_charm:lift_contact_uuid";
    private static final String CONTACT_TIME = "spelunkers_charm:lift_contact_time";
    private static final double MAX_CLIENT_FOOT_LAG = 1.0;

    private LiftCollision() {}

    public static void carry(AbstractLift lift, AABB oldBounds, AABB newBounds) {
        long supportSerial = lift.level().isClientSide ? 0 : LiftSupportSync.begin(lift);
        AABB search = oldBounds.minmax(newBounds).inflate(0.01, 2.0, 0.01);
        double dx = newBounds.minX - oldBounds.minX;
        double dz = newBounds.minZ - oldBounds.minZ;
        for (Entity entity : lift.level().getEntities(lift, search, LiftCollision::canAffect)) {
            // Remote entities have their own vanilla interpolation. Rewriting
            // their physical position every lift tick fought those lerp targets.
            // Their support is applied after entity interpolation and at render.
            if (lift.level().isClientSide && !LiftClientSupport.controlsLocally(entity)) continue;
            AABB box = entity.getBoundingBox();
            if (!overlapsDeck(box, oldBounds) && !overlapsDeck(box, newBounds)) {
                continue;
            }
            double previousFeet = previousFeet(entity, box);
            Vec3 velocity = entity.getDeltaMovement();
            // ItemEntity reflects a tiny floor velocity upward, which can clear
            // onGround before the lift's next tick. Close contact identifies that
            // passive resting bounce; airborne thrown items keep their velocity.
            double supportVelocity =
                    entity instanceof ItemEntity
                                    && Math.abs(box.minY - oldBounds.maxY) <= SUPPORT_TOLERANCE
                                    && velocity.y > 0.0
                                    && velocity.y <= 0.04
                            ? 0.0
                            : velocity.y;
            boolean allowPull =
                    !entity.isNoGravity()
                            && !(entity instanceof Player player && player.getAbilities().flying);
            boolean retainedServerContact =
                    !lift.level().isClientSide
                            && clientControls(entity)
                            && supportVelocity <= EPSILON
                            && allowPull
                            && hasRecentContact(entity, lift)
                            && Math.abs(box.minY - oldBounds.maxY) <= MAX_CLIENT_FOOT_LAG;
            if (retainedServerContact
                    || LiftCollisionGeometry.supports(
                            oldBounds.maxY,
                            newBounds.maxY,
                            box.minY,
                            previousFeet,
                            supportVelocity,
                            allowPull)) {
                // Position absolutely, rather than adding the lift's velocity:
                // server correction and local prediction cannot carry twice.
                boolean landingFromFall =
                        entity.fallDistance > 3.0F
                                && previousFeet > oldBounds.maxY + SUPPORT_TOLERANCE;
                // ItemEntity checks noCollision with a deflated box, while
                // EntityGetter inflates its query by the same epsilon. At an
                // exactly touching deck, rounding can falsely trigger its
                // embedded-item ejection (+0.2Y). A tiny skin avoids that path.
                double skin = entity instanceof ItemEntity ? EPSILON : 0.0;
                entity.setPos(
                        entity.getX() + dx,
                        entity.getY() + newBounds.maxY + skin - box.minY,
                        entity.getZ() + dz);
                entity.setDeltaMovement(velocity.x, 0.0, velocity.z);
                entity.setOnGround(true);
                entity.verticalCollision = true;
                entity.verticalCollisionBelow = true;
                if (landingFromFall && !lift.level().isClientSide) {
                    entity.causeFallDamage(
                            entity.fallDistance, 1.0F, entity.damageSources().fall());
                }
                markSupported(lift, entity);
                if (!lift.level().isClientSide)
                    LiftSupportSync.support(lift, entity, supportSerial);
                positionPassengers(entity);
            } else if (LiftCollisionGeometry.pushedByBottom(
                    oldBounds.minY, newBounds.minY, box.maxY, previousFeet + box.getYsize())) {
                double targetY = entity.getY() + newBounds.minY - box.maxY;
                entity.setPos(entity.getX(), targetY, entity.getZ());
                if (velocity.y > 0.0) {
                    entity.setDeltaMovement(velocity.x, 0.0, velocity.z);
                }
                positionPassengers(entity);
            }
        }
        if (!lift.level().isClientSide) LiftSupportSync.finish(lift, supportSerial);
    }

    /** Stops the deck before a supported rider's head reaches a solid ceiling. */
    public static double limitMovementForRiders(
            AbstractLift lift, AABB oldBounds, double requestedY) {
        if (Math.abs(requestedY) < EPSILON) {
            return 0.0;
        }
        double limited = requestedY;
        AABB search =
                oldBounds
                        .expandTowards(0.0, requestedY, 0.0)
                        .inflate(0.01, SUPPORT_TOLERANCE, 0.01);
        for (Entity entity : lift.level().getEntities(lift, search, LiftCollision::canAffect)) {
            AABB box = entity.getBoundingBox();
            if (!overlapsDeck(box, oldBounds)) {
                continue;
            }
            if (requestedY > 0.0
                    && box.minY >= oldBounds.maxY - SUPPORT_TOLERANCE
                    && box.minY <= oldBounds.maxY + requestedY + SUPPORT_TOLERANCE) {
                double gap = Math.max(0.0, box.minY - oldBounds.maxY);
                double push = Math.max(0.0, requestedY - gap);
                double allowed = limitUpwardWithPassengers(entity, push);
                limited = Math.min(limited, gap + allowed);
            } else if (requestedY < 0.0
                    && box.maxY <= oldBounds.minY + EPSILON
                    && box.maxY >= oldBounds.minY + requestedY - EPSILON) {
                // A descending deck must not squash a creature against a floor.
                double gap = Math.max(0.0, oldBounds.minY - box.maxY);
                double push = Math.min(0.0, requestedY + gap);
                Vec3 allowed = collideWithBlocks(entity, push);
                limited = Math.max(limited, -gap + allowed.y);
            }
        }
        return requestedY > 0.0 ? Math.max(0.0, limited) : Math.min(0.0, limited);
    }

    private static double limitUpwardWithPassengers(Entity entity, double requestedY) {
        double allowed = collideWithBlocks(entity, requestedY).y;
        for (Entity passenger : entity.getPassengers()) {
            allowed = Math.min(allowed, limitUpwardWithPassengers(passenger, requestedY));
        }
        return Math.max(0.0, allowed);
    }

    private static Vec3 collideWithBlocks(Entity entity, double dy) {
        return Entity.collideBoundingBox(
                entity, new Vec3(0.0, dy, 0.0), entity.getBoundingBox(), entity.level(), List.of());
    }

    private static boolean overlapsDeck(AABB entity, AABB deck) {
        return LiftCollisionGeometry.overlaps(entity.minX, entity.maxX, deck.minX, deck.maxX)
                && LiftCollisionGeometry.overlaps(entity.minZ, entity.maxZ, deck.minZ, deck.maxZ);
    }

    private static boolean canAffect(Entity entity) {
        return !(entity instanceof AbstractLift)
                && !entity.isRemoved()
                && !entity.isSpectator()
                && !entity.noPhysics
                && !entity.isPassenger()
                && entity.getPistonPushReaction() == PushReaction.NORMAL;
    }

    private static void positionPassengers(Entity vehicle) {
        for (Entity passenger : vehicle.getPassengers()) {
            vehicle.positionRider(passenger);
            passenger.resetFallDistance();
            resetFloatingTicks(passenger);
            positionPassengers(passenger);
        }
    }

    private static void markSupported(AbstractLift lift, Entity entity) {
        entity.resetFallDistance();
        resetFloatingTicks(entity);
        if (!entity.level().isClientSide && clientControls(entity)) {
            CompoundTag data = entity.getPersistentData();
            data.putUUID(CONTACT_LIFT, lift.getUUID());
            data.putLong(CONTACT_TIME, entity.level().getGameTime());
        }
    }

    /** A new entity or a teleport has no usable previous-tick sweep. */
    private static double previousFeet(Entity entity, AABB box) {
        double delta = entity.getY() - entity.yo;
        double maximumSweep = Math.max(4.0, Math.abs(entity.getDeltaMovement().y) + 2.0);
        if (entity.tickCount == 0 || Math.abs(delta) > maximumSweep) return box.minY;
        return box.minY - delta;
    }

    private static boolean clientControls(Entity entity) {
        return entity instanceof ServerPlayer
                || entity.getControllingPassenger() instanceof ServerPlayer;
    }

    private static boolean hasRecentContact(Entity entity, AbstractLift lift) {
        CompoundTag data = entity.getPersistentData();
        long age = entity.level().getGameTime() - data.getLong(CONTACT_TIME);
        return age >= 0
                && age <= 2
                && data.hasUUID(CONTACT_LIFT)
                && data.getUUID(CONTACT_LIFT).equals(lift.getUUID());
    }

    /**
     * Only vanilla packet validation uses this bounded interpolation exception. A server-observed
     * deck contact is required; blocks and other entities keep their normal collisions, and
     * ordinary entity physics stays fully solid.
     */
    public static boolean isPacketSupport(Entity entity, AbstractLift lift, AABB proposedBounds) {
        return !entity.level().isClientSide
                && clientControls(entity)
                && hasRecentContact(entity, lift)
                && overlapsDeck(proposedBounds, lift.getBoundingBox())
                && Math.abs(proposedBounds.minY - lift.getBoundingBox().maxY)
                        <= MAX_CLIENT_FOOT_LAG;
    }

    private static void resetFloatingTicks(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            player.connection.aboveGroundTickCount = 0;
            player.connection.aboveGroundVehicleTickCount = 0;
        }
    }
}
