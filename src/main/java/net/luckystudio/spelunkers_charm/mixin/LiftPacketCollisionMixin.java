package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftPacketCollisions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve vanilla validation while allowing a supported client's deck lag. */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public abstract class LiftPacketCollisionMixin {
    @Redirect(
            method = "isPlayerCollidingWithAnythingNew",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/LevelReader;getCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"),
            require = 1,
            remap = false)
    private Iterable<VoxelShape> spelunkersCharm$validatePlayerCollisions(
            LevelReader level, Entity mover, AABB bounds) {
        return LiftPacketCollisions.getCollisions(level, mover, bounds);
    }

    @Redirect(
            method = "handleMoveVehicle",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"),
            require = 2,
            allow = 2,
            remap = false)
    private boolean spelunkersCharm$validateVehicleCollisions(
            ServerLevel level, Entity mover, AABB bounds) {
        return LiftPacketCollisions.noCollision(level, mover, bounds);
    }
}
