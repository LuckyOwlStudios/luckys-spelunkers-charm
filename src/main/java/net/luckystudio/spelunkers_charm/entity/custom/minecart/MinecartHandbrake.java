// Minecart handbrake.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.phys.Vec3;

public final class MinecartHandbrake {
    private MinecartHandbrake() {}

    public static boolean active(AbstractMinecart cart) {
        return local(cart) || source(cart) != null;
    }

    public static boolean local(AbstractMinecart cart) {
        return cart instanceof LocomotiveMinecart engine
                ? engine.emergencyBrake()
                : cart instanceof MinecartFurnace && state(cart).handbrake;
    }

    private static java.util.UUID source(AbstractMinecart cart) {
        if (!(cart.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        return MinecartTrainBrakes.source(cart);
    }

    public static void changed(AbstractMinecart cart, boolean active) {
        if (cart.level() instanceof net.minecraft.server.level.ServerLevel level
                && MinecartCouplings.isEngine(cart)) {
            if (!active) state(cart).flightBrake = false;
            MinecartCouplings.get(level).setEngineBrake(cart.getUUID(), active);
            MinecartTrainBrakes.invalidate(level);
        }
    }

    public static void prepare(AbstractMinecart cart) {
        var state = state(cart);
        java.util.UUID root = source(cart);
        if (root == null) {
            state.trainBrakeRoot = null;
            state.trainBrakeAnchor = null;
            return;
        }
        if (!root.equals(state.trainBrakeRoot)) {
            state.trainBrakeRoot = root;
            state.trainBrakeSpeed = cart.getDeltaMovement().horizontalDistance();
            state.trainBrakeAnchor = null;
        }
    }

    public static void engage(AbstractMinecart cart) {
        if (cart instanceof LocomotiveMinecart engine) {
            if (!engine.emergencyBrake()) engine.toggleEmergencyBrake();
        } else if (cart instanceof MinecartFurnace && !active(cart)) {
            var state = state(cart);
            state.handbrake = true;
            state.handbrakeSpeed = cart.getDeltaMovement().horizontalDistance();
            state.handbrakeAnchor = state.handbrakeSpeed < 0.003 ? cart.position() : null;
            changed(cart, true);
        }
    }

    public static void release(AbstractMinecart cart) {
        if (cart instanceof LocomotiveMinecart engine) engine.releaseEmergencyBrake();
        else {
            state(cart).handbrake = false;
            state(cart).handbrakeAnchor = null;
            changed(cart, false);
        }
    }

    public static double speed(AbstractMinecart cart, double incoming) {
        if (cart instanceof LocomotiveMinecart engine && engine.emergencyBrake())
            return engine.brakingSpeed(incoming);
        if (local(cart)) return Math.min(incoming, state(cart).handbrakeSpeed);
        prepare(cart);
        return source(cart) != null ? Math.min(incoming, state(cart).trainBrakeSpeed) : incoming;
    }

    public static void hold(AbstractMinecart cart, double speed) {
        if (!MinecartTrainBrakes.onRail(cart)) {
            state(cart).handbrakeAnchor = null;
            state(cart).trainBrakeAnchor = null;
            if (cart instanceof LocomotiveMinecart engine) engine.updateBrakeHold(speed);
            return;
        }
        if (cart instanceof LocomotiveMinecart engine && engine.emergencyBrake()) {
            engine.updateBrakeHold(speed);
            return;
        }
        if (!active(cart)) return;
        var state = state(cart);
        if (!local(cart)) {
            prepare(cart);
            state.trainBrakeSpeed = speed < .003 ? 0 : Math.min(state.trainBrakeSpeed, speed);
            if (state.trainBrakeSpeed == 0) {
                if (!cart.isOnRails() && !cart.onGround()) {
                    state.trainBrakeAnchor = null;
                    cart.setDeltaMovement(0, cart.getDeltaMovement().y, 0);
                    return;
                }
                if (state.trainBrakeAnchor == null) state.trainBrakeAnchor = cart.position();
                Vec3 anchor = state.trainBrakeAnchor;
                cart.setPos(anchor.x, anchor.y, anchor.z);
                cart.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        state.handbrakeSpeed = speed < 0.003 ? 0 : Math.min(state.handbrakeSpeed, speed);
        if (state.handbrakeSpeed == 0) {
            if (!cart.isOnRails() && !cart.onGround()) {
                state.handbrakeAnchor = null;
                cart.setDeltaMovement(0, cart.getDeltaMovement().y, 0);
                return;
            }
            if (state.handbrakeAnchor == null) state.handbrakeAnchor = cart.position();
            Vec3 anchor = state.handbrakeAnchor;
            cart.setPos(anchor.x, anchor.y, anchor.z);
            cart.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static boolean anchored(AbstractMinecart cart) {
        if (!MinecartTrainBrakes.onRail(cart)) return false;
        if (cart instanceof LocomotiveMinecart engine && engine.brakeAnchored()) return true;
        return local(cart)
                ? state(cart).handbrakeAnchor != null
                : source(cart) != null && state(cart).trainBrakeAnchor != null;
    }

    public static void beforeTick(AbstractMinecart cart) {
        prepare(cart);
        if (cart instanceof LocomotiveMinecart engine && engine.emergencyBrake()) return;
        if (active(cart))
            hold(cart, local(cart) ? state(cart).handbrakeSpeed : state(cart).trainBrakeSpeed);
    }

    private static MinecartMotionState state(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
    }
}
