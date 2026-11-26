// Minecart rail physics.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class MinecartRailMotion {
    private static final double STEP = 0.1;
    private static final double STOP_MARGIN = 0.12;

    private MinecartRailMotion() {}

    public static void move(AbstractMinecart cart, BlockPos pos, BlockState state) {
        move(cart, pos, state, 1);
    }

    public static void moveForDuration(AbstractMinecart cart, double duration) {
        var pos = cart.getCurrentRailPosition();
        move(cart, pos, cart.level().getBlockState(pos), duration);
    }

    private static void move(
            AbstractMinecart cart, BlockPos pos, BlockState state, double duration) {
        MinecartTrack track = MinecartTrack.current(cart);
        if (track == null) return;
        MinecartMotionState motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        boolean catching = motion.airborne || motion.freeTicks > 0;
        if (catching
                && (!cart.onGround() && cart.getDeltaMovement().y + cart.getGravity() > .01
                        || track.point(track.offset(cart.position())).distanceToSqr(cart.position())
                                > .0225)) {
            moveOffTrack(cart);
            return;
        }
        MinecartRailLanding.landed(cart, cart.getDeltaMovement().y + cart.getGravity());
        if (catching) MinecartRailCapture.land(cart, track, cart.getDeltaMovement());
        MinecartCrash.clear(cart);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(false);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(true);
        motion.freeTicks = 0;
        cart.setPos(track.point(track.offset(cart.position())));
        motion.held = false;
        motion.controlledMovement = true;
        cart.resetFallDistance();
        Vec3 velocity = cart.getDeltaMovement();
        Vec3 desired = drive(cart);
        double projection = track.tangent().dot(velocity);
        int sign =
                projection < -1.0E-5
                        ? -1
                        : projection > 1.0E-5
                                ? 1
                                : desired.lengthSqr() > 0.001
                                        ? track.tangent().dot(desired) < 0 ? -1 : 1
                                        : track.pos().equals(motion.railPosition)
                                                ? motion.railSign
                                                : 1;
        double speed = velocity.horizontalDistance();
        double flightSpeed = MinecartSpeedRules.flightLimit(cart);
        if (Double.isFinite(flightSpeed)) speed = flightSpeed;
        if (speed < 0.002
                && desired.horizontalDistanceSqr() < 0.001
                && track.slope(track.tangent().scale(sign)) > 0) sign = -sign;
        Cursor cursor = new Cursor(track, track.offset(cart.position()), sign);
        Set<BlockPos> visited = new HashSet<>();

        for (int step = 0;
                step < 10 && step * STEP < duration - 1e-8 && !cart.isRemoved();
                step++) {
            double dt = Math.min(STEP, duration - step * STEP);
            if (cursor.track == null) {
                speed = advanceFree(cart, cursor, speed, dt);
                continue;
            }
            track = cursor.track;
            state = track.state();
            Vec3 heading = track.tangent().scale(cursor.sign);
            if (track.slope(heading) != 0) MinecartFreeCouplings.protectTrain(cart);
            boolean directional =
                    state.getBlock() instanceof DirectionalRailBlock
                            && MinecartCouplings.isEngine(cart);
            boolean holding = directional && DirectionalRailBlock.isHolding(state);
            if (visited.add(track.pos()) && cart.shouldDoRailFunctions()) {
                ((BaseRailBlock) state.getBlock())
                        .onMinecartPass(state, cart.level(), track.pos(), cart);
                if (state.getBlock()
                                instanceof net.minecraft.world.level.block.PoweredRailBlock rail
                        && rail.isActivatorRail())
                    cart.activateMinecart(
                            track.pos().getX(),
                            track.pos().getY(),
                            track.pos().getZ(),
                            state.getValue(
                                    net.minecraft.world.level.block.PoweredRailBlock.POWERED));
            }
            if (directional
                    && state.getValue(net.minecraft.world.level.block.PoweredRailBlock.POWERED)) {
                Direction direction = DirectionalRailBlock.travelDirection(state);
                desired = new Vec3(direction.getStepX(), 0, direction.getStepZ());
            } else desired = drive(cart);

            double command = desired.dot(heading);
            boolean emergency = MinecartHandbrake.active(cart);
            boolean reversing = command < -0.08;
            int slope = track.slope(heading);
            double limit = MinecartSpeedRules.limit(cart, state, slope < 0);
            speed = MinecartHandbrake.speed(cart, speed);
            if (command > 0.08 && speed > 0.003) motion.reversalRequested = false;
            double oldSpeed = speed;
            double distance;
            boolean reverseAfterMove = false;

            if (holding) {
                double remaining =
                        cursor.sign > 0
                                ? track.length() - STOP_MARGIN - cursor.offset
                                : cursor.offset - STOP_MARGIN;
                if (speed < 0.003 || remaining <= 0.003) {
                    speed = 0;
                    distance = 0;
                    motion.held = true;
                } else {
                    double deceleration = Math.max(0.12, speed * speed / (2 * remaining));
                    speed = Math.max(0, speed - deceleration * dt);
                    distance = Math.min(remaining, (oldSpeed + speed) * 0.5 * dt);
                    motion.brake(oldSpeed);
                }
            } else if (emergency || reversing) {
                double deceleration = emergency ? 0.28 : 0.14;
                speed = Math.max(0, speed - deceleration * dt);
                distance = (oldSpeed + speed) * 0.5 * dt;
                if (emergency || !MinecartCouplings.isEngine(cart) || motion.reversalRequested)
                    motion.brake(oldSpeed);
                if (speed < 0.003 && reversing && !emergency) {
                    speed = 0;
                    reverseAfterMove = true;
                }
            } else {
                boolean engine = poweredEngine(cart);
                boolean player = cart.getFirstPassenger() instanceof Player && command > 0.08;
                double acceleration = 0;
                double throttle = MinecartSpeedRules.accelerationScale(cart);
                if (engine && command > 0.01)
                    acceleration +=
                            MinecartSpeedRules.ENGINE_ACCELERATION
                                    * Math.min(1.6, MinecartSpeedRules.traction(cart))
                                    * throttle;
                if (player && slope <= 0) acceleration += MinecartSpeedRules.PLAYER_ACCELERATION;
                if (MinecartSpeedRules.powered(state)
                        && (speed > 0.002 || state.getBlock() instanceof DirectionalRailBlock))
                    acceleration += 0.045 * MinecartSpeedRules.SPEED_SCALE * throttle;
                if (slope < 0) acceleration += 0.022 * MinecartSpeedRules.SPEED_SCALE;
                if (slope > 0) {
                    acceleration -= 0.028 * MinecartSpeedRules.SPEED_SCALE;
                    if (!engine
                            && !MinecartSpeedRules.powered(state)
                            && speed < MinecartSpeedRules.UPHILL_MOMENTUM)
                        acceleration -= 0.012 * MinecartSpeedRules.SPEED_SCALE;
                }
                if (engine && slope > 0 && command > 0)
                    acceleration += 0.028 * MinecartSpeedRules.SPEED_SCALE;
                double drag = Math.pow(cart.isVehicle() ? 0.997 : engine ? 0.998 : 0.985, dt);
                speed = Math.max(0, speed * drag + acceleration * dt);
                if (oldSpeed > limit + 0.004) {
                    speed = Math.max(limit, speed - 0.04 * dt);
                } else speed = Math.min(speed, limit);
                if (speed < 0.0005 && slope > 0 && !engine && !MinecartSpeedRules.powered(state)) {
                    speed = 0;
                    reverseAfterMove = true;
                }
                distance = (oldSpeed + speed) * 0.5 * dt;
            }
            cart.setDeltaMovement(heading.scale(speed));
            double moved = advance(cart, cursor, distance);
            speed = cart.getDeltaMovement().horizontalDistance();
            if (moved + 0.002 < distance && !cursor.couplingLimited) {
                speed = 0;
            }
            if (reverseAfterMove) cursor.sign = -cursor.sign;
        }
        Vec3 finalHeading =
                cursor.track == null
                        ? cursor.freeHeading
                        : cursor.track.tangent().scale(cursor.sign);
        cart.setDeltaMovement(
                finalHeading.x * speed,
                cursor.track == null ? cart.getDeltaMovement().y : 0,
                finalHeading.z * speed);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(cursor.track != null);
        ((MinecartMotionVisuals) cart)
                .spelunkersCharm$setAirborne(cursor.track == null && !cart.onGround());
        MinecartHandbrake.hold(cart, speed);
        motion.railPosition = cursor.track == null ? null : cursor.track.pos();
        motion.railSign = cursor.sign;
        motion.travelHeading = finalHeading;
        cart.hasImpulse = true;
    }

    public static void moveOffTrack(AbstractMinecart cart) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        motion.controlledMovement = true;
        Vec3 velocity = cart.getDeltaMovement();
        double speed = velocity.horizontalDistance();
        if (MinecartHandbrake.active(cart) && MinecartTrainBrakes.onRail(cart)) {
            speed = Math.max(0, MinecartHandbrake.speed(cart, speed) - 0.28);
            motion.brake(velocity.horizontalDistance());
            MinecartHandbrake.hold(cart, speed);
            if (speed < 0.003 && cart.onGround()) return;
        }
        if (speed < .0005 && cart.onGround()) {
            cart.setDeltaMovement(Vec3.ZERO);
            motion.railPosition = null;
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(false);
            ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(false);
            return;
        }
        Cursor cursor = new Cursor(null, 0, 1);
        cursor.freeHeading =
                velocity.horizontalDistance() > 1.0E-7
                        ? new Vec3(velocity.x, 0, velocity.z).normalize()
                        : motion.travelHeading;
        cursor.freeVertical = velocity.y + cart.getGravity();
        for (int step = 0; step < 10 && !cart.isRemoved(); step++) {
            if (cursor.track == null) speed = advanceFree(cart, cursor, speed);
            else {
                double distance = speed * STEP;
                double moved = advance(cart, cursor, distance);
                speed = cart.getDeltaMovement().horizontalDistance();
                if (moved + .002 < distance && !cursor.couplingLimited) speed = 0;
            }
        }
        Vec3 heading =
                cursor.track == null
                        ? cursor.freeHeading
                        : cursor.track.tangent().scale(cursor.sign);
        cart.setDeltaMovement(
                heading.x * speed,
                cursor.track == null ? cart.getDeltaMovement().y : 0,
                heading.z * speed);
        motion.railPosition = cursor.track == null ? null : cursor.track.pos();
        motion.railSign = cursor.sign;
        motion.travelHeading = heading;
        motion.freeTicks = cursor.track == null ? motion.freeTicks + 1 : 0;
        ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(cursor.track != null);
        ((MinecartMotionVisuals) cart)
                .spelunkersCharm$setAirborne(cursor.track == null && !cart.onGround());
        cart.hasImpulse = true;
    }

    private static double advanceFree(AbstractMinecart cart, Cursor cursor, double speed) {
        return advanceFree(cart, cursor, speed, STEP);
    }

    private static double advanceFree(
            AbstractMinecart cart, Cursor cursor, double speed, double duration) {
        cart.setDeltaMovement(
                cursor.freeHeading.x * speed, cursor.freeVertical, cursor.freeHeading.z * speed);
        MinecartFlightPhysics.defer(cart, duration);
        return speed;
    }

    private static Vec3 drive(AbstractMinecart cart) {
        if (MinecartHandbrake.active(cart)) return Vec3.ZERO;
        if (cart instanceof LocomotiveMinecart engine)
            return engine.activeBurners() > 0 ? engine.driveVector() : Vec3.ZERO;
        if (cart instanceof MinecartFurnace furnace && furnace.fuel > 0)
            return new Vec3(furnace.xPush, 0, furnace.zPush).normalize();
        if (MinecartSpeedRules.trainPowered(cart)) return MinecartSpeedRules.trainDrive(cart);
        return cart.getFirstPassenger() instanceof Player player
                        && !MinecartCouplings.hasEngine(cart)
                ? MinecartSpeedRules.playerInput(player)
                : Vec3.ZERO;
    }

    private static boolean poweredEngine(AbstractMinecart cart) {
        if (MinecartHandbrake.active(cart)) return false;
        return MinecartSpeedRules.trainPowered(cart);
    }

    private static double advance(AbstractMinecart cart, Cursor cursor, double distance) {
        cursor.couplingLimited = false;
        if (cursor.track != null) {
            ((MinecartMotionVisuals) cart).spelunkersCharm$motion().railPosition =
                    cursor.track.pos();
            double allowed =
                    MinecartCouplerPhysics.allowedTravel(
                            cart, cursor.track.tangent().scale(cursor.sign), distance);
            cursor.couplingLimited = allowed + 1e-7 < distance;
            if (cursor.couplingLimited)
                cart.setDeltaMovement(cart.getDeltaMovement().scale(allowed / distance));
            distance = allowed;
        }
        double moved = 0;
        for (int guard = 0; guard < 8 && distance > 1.0E-7; guard++) {
            MinecartTrack track = cursor.track;
            if (track == null) break;
            double available = cursor.sign > 0 ? track.length() - cursor.offset : cursor.offset;
            double part = Math.min(distance, Math.min(available, 0.10));
            if (part > 1.0E-7) {
                Vec3 target = track.point(cursor.offset + cursor.sign * part);
                Vec3 start = cart.position();
                double clearance =
                        track.first().height() != track.second().height()
                                ? track.pos().getY() + 1.0625
                                : target.y;
                cart.setPos(start.x, clearance, start.z);
                Vec3 displacement = new Vec3(target.x - start.x, 0, target.z - start.z);
                if (displacement.lengthSqr() < 1.0E-7) cart.setPos(target.x, clearance, target.z);
                else moveOnRail(cart, track, displacement);
                double actual =
                        Math.max(
                                0,
                                cart.position()
                                                .subtract(new Vec3(start.x, clearance, start.z))
                                                .dot(track.tangent())
                                        * cursor.sign);
                double fraction = Math.clamp(actual / part, 0, 1);
                cart.setPos(cart.getX(), start.y + (target.y - start.y) * fraction, cart.getZ());
                cursor.offset = Math.clamp(cursor.offset + cursor.sign * actual, 0, track.length());
                moved += actual;
                distance -= actual;
                if (actual + 1.0E-4 < part) {
                    cursor.couplingLimited = false;
                    return moved;
                }
            }
            if (available - part < 1.0E-6) {
                Vec3 previousHeading = track.tangent().scale(cursor.sign);
                MinecartTrack next = track.next(cart, cursor.sign);
                if (next == null) {
                    double speed = cart.getDeltaMovement().horizontalDistance();
                    var port = cursor.sign > 0 ? track.second() : track.first();
                    Vec3 exitHeading =
                            new Vec3(port.direction().getStepX(), 0, port.direction().getStepZ());
                    followCurve(cart, previousHeading, exitHeading);
                    cursor.track = null;
                    cursor.freeHeading = exitHeading;
                    cursor.freeVertical = speed * track.slope(previousHeading);
                    var flight = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
                    Vec3 launchAxis =
                            new Vec3(exitHeading.x, track.slope(previousHeading), exitHeading.z);
                    double bodyYaw =
                            Math.toRadians(
                                    cart instanceof LocomotiveMinecart engine
                                            ? engine.bodyYaw()
                                            : flight.visualYawReady
                                                    ? flight.visualYaw
                                                    : cart.getYRot());
                    if (launchAxis.dot(new Vec3(Math.cos(bodyYaw), 0, Math.sin(bodyYaw))) < 0)
                        launchAxis = launchAxis.scale(-1);
                    MinecartFlightPhysics.launch(cart, launchAxis);
                    flight.flightForwardSign = launchAxis.dot(exitHeading) < 0 ? -1 : 1;
                    double yaw = Math.toRadians(cart.getYRot());
                    double facing =
                            Math.cos(yaw) * previousHeading.x + Math.sin(yaw) * previousHeading.z;
                    flight.flightPitch =
                            (float)
                                    (Math.toDegrees(Math.atan(track.slope(previousHeading)))
                                            * (facing < 0 ? -1 : 1));
                    ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(false);
                    ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
                    cart.setDeltaMovement(
                            exitHeading.x * speed, cursor.freeVertical, exitHeading.z * speed);
                    Vec3 before = cart.position();
                    if (distance > 1e-7 && speed > 1e-7)
                        advanceFree(cart, cursor, speed, Math.min(STEP, distance / speed));
                    moved += distance;
                    return moved;
                }
                cursor.track = next;
                cursor.offset = next.offset(cart.position());
                cursor.sign = cursor.offset < next.length() * 0.5 ? 1 : -1;
                Vec3 nextHeading = next.tangent().scale(cursor.sign);
                followCurve(cart, previousHeading, nextHeading);
            }
        }
        return moved;
    }

    private static void followCurve(AbstractMinecart cart, Vec3 previous, Vec3 next) {
        MinecartBodyOrientation.followRail(cart, previous, next);
    }

    private static void moveOnRail(AbstractMinecart cart, MinecartTrack track, Vec3 displacement) {
        var original = cart.getBoundingBox();
        Vec3 start = cart.position();
        boolean curve = track.first().direction().getAxis() != track.second().direction().getAxis();
        if (curve)
            cart.setBoundingBox(
                    new net.minecraft.world.phys.AABB(
                            start.x - .16,
                            original.minY,
                            start.z - .16,
                            start.x + .16,
                            original.maxY,
                            start.z + .16));
        try {
            cart.move(MoverType.SELF, displacement);
        } finally {
            if (curve) cart.setBoundingBox(original.move(cart.position().subtract(start)));
        }
    }

    /** Tracks progress along rails and through flight substeps. */
    private static final class Cursor {
        private MinecartTrack track;
        private double offset;
        private int sign;
        private Vec3 freeHeading = Vec3.ZERO;
        private double freeVertical;
        private boolean couplingLimited;

        private Cursor(MinecartTrack track, double offset, int sign) {
            this.track = track;
            this.offset = offset;
            this.sign = sign;
        }
    }
}
