// Minecart coupler sides.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartCouplerSides {
    private MinecartCouplerSides() {}

    public static int mask(AbstractMinecart cart, float partialTick) {
        Vec3 axis =
                cart.level().isClientSide()
                        ? MinecartBodyPose.axis(cart, partialTick)
                        : new Vec3(
                                Math.cos(Math.toRadians(cart.getYRot())),
                                0,
                                Math.sin(Math.toRadians(cart.getYRot())));
        if (cart instanceof LocomotiveMinecart engine) axis = engine.bodyDirection();
        int mask = 0;
        if (cart.level() instanceof ServerLevel level) {
            for (var id : MinecartCouplings.get(level).neighbors(cart.getUUID()))
                if (level.getEntity(id) instanceof AbstractMinecart peer)
                    mask |= MinecartHingeGeometry.side(cart, peer) < 0 ? 1 : 2;
        } else {
            var links = (MinecartCouplingVisuals) cart;
            for (int id :
                    new int[] {
                        links.spelunkersCharm$firstCouplingId(),
                        links.spelunkersCharm$secondCouplingId()
                    })
                if (cart.level().getEntity(id) instanceof AbstractMinecart peer)
                    mask |= MinecartHingeGeometry.side(cart, peer) < 0 ? 1 : 2;
        }
        return mask;
    }

    public static boolean rear(AbstractMinecart cart, AbstractMinecart peer) {
        if (!(cart instanceof LocomotiveMinecart engine)) return true;
        var path = MinecartRailPath.between(cart, peer);
        Vec3 toward =
                path != null
                        ? path.firstToward()
                        : peer.position().subtract(cart.position()).multiply(1, 0, 1).normalize();
        return engine.bodyDirection().dot(toward) < -.1;
    }
}
