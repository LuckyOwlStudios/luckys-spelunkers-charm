// Minecart coupler anchors.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartCouplerAnchors {
    public static final double END_OFFSET = 10.5 / 16;

    private MinecartCouplerAnchors() {}

    public static Vec3 axis(AbstractMinecart cart, float partialTick) {
        if (cart.level().isClientSide() && MinecartSpeedRules.managed(cart))
            return MinecartBodyPose.axis(cart, partialTick);
        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne())
            return MinecartFlightPhysics.axis(cart);
        Vec3 position = cart.getPosition(partialTick);
        Vec3 front = cart.getPosOffs(position.x, position.y, position.z, 0.3);
        Vec3 back = cart.getPosOffs(position.x, position.y, position.z, -0.3);
        if (front != null && back != null && front.distanceToSqr(back) > 0.0001)
            return front.subtract(back).normalize();
        double yaw = Math.toRadians(cart.getYRot());
        double pitch = Math.toRadians(Mth.lerp(partialTick, cart.xRotO, cart.getXRot()));
        return new Vec3(
                Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
    }

    public static Vec3 endpoint(
            AbstractMinecart cart, float partialTick, Vec3 center, Vec3 towardPeer) {
        Vec3 axis = axis(cart, partialTick);
        double reach = cart instanceof LocomotiveMinecart ? 11.5 / 16 : END_OFFSET;
        return center.add(axis.scale(axis.dot(towardPeer) >= 0 ? reach : -reach));
    }

    public static Vec3 endpoint(
            AbstractMinecart cart, AbstractMinecart peer, float partialTick, Vec3 center) {
        return center.add(
                axis(cart, partialTick)
                        .scale(
                                MinecartHingeGeometry.side(cart, peer)
                                        * MinecartHingeGeometry.reach(cart)));
    }

    public static Vec3 up(Vec3 axis) {
        double horizontal = axis.horizontalDistance();
        return horizontal < 1e-5
                ? new Vec3(0, 1, 0)
                : new Vec3(
                        -axis.x * axis.y / horizontal, horizontal, -axis.z * axis.y / horizontal);
    }

    public static Vec3 center(AbstractMinecart cart, float partialTick, Vec3 position) {
        return position.add(0, .375, 0).add(up(cart, partialTick).scale(1.0 / 16));
    }

    public static Vec3 up(AbstractMinecart cart, float partialTick) {
        float yaw = MinecartBodyPose.yaw(cart, partialTick),
                pitch = MinecartBodyPose.pitch(cart, yaw, partialTick);
        return MinecartHingeGeometry.up(Math.toRadians(yaw), Math.toRadians(pitch));
    }
}
