// Minecart rail placement.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartRailPlacement {
    private MinecartRailPlacement() {}

    public static void align(AbstractMinecart cart) {
        if (!MinecartSpeedRules.managed(cart)) return;
        var track = MinecartTrack.current(cart);
        if (track == null) return;
        var visual = (MinecartMotionVisuals) cart;
        var motion = visual.spelunkersCharm$motion();
        double yaw =
                Math.toRadians(
                        cart instanceof LocomotiveMinecart engine
                                ? engine.bodyYaw()
                                : cart.getYRot());
        Vec3 front = track.point(track.length()).subtract(track.point(0)).normalize();
        if (front.dot(MinecartHingeGeometry.axis(yaw, 0)) < 0) front = front.scale(-1);
        cart.setPos(track.point(track.offset(cart.position())));
        MinecartBodyOrientation.landFacing(cart, front);
        visual.spelunkersCharm$setOnRails(true);
        visual.spelunkersCharm$setAirborne(false);
        motion.railPosition = track.pos();
        motion.freeTicks = 0;
        motion.flightRotationReady = true;
        motion.railSign = track.tangent().dot(front) < 0 ? -1 : 1;
        motion.travelHeading = track.tangent().scale(motion.railSign);
        cart.setYRot(motion.visualYaw);
        cart.yRotO = motion.visualYaw;
        cart.setXRot((float) Math.toDegrees(motion.physicalPitch));
        cart.xRotO = cart.getXRot();
        cart.xOld = cart.xo = cart.getX();
        cart.yOld = cart.yo = cart.getY();
        cart.zOld = cart.zo = cart.getZ();
    }
}
