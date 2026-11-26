// Minecart rolling sound mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartSpeedRules;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        value = {MinecartSoundInstance.class, RidingMinecartSoundInstance.class},
        remap = false)
public abstract class MinecartRollingSoundMixin {
    @Unique private Vec3 spelunkersCharm$previousSoundPosition;

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 spelunkersCharm$useActualRollingMovement(AbstractMinecart cart) {
        Vec3 position = cart.position();
        Vec3 delta =
                spelunkersCharm$previousSoundPosition == null
                        ? Vec3.ZERO
                        : position.subtract(spelunkersCharm$previousSoundPosition);
        spelunkersCharm$previousSoundPosition = position;
        if (!MinecartSpeedRules.managed(cart)) return cart.getDeltaMovement();
        double speed = delta.horizontalDistance();
        return speed > .005 && speed < 2 ? delta.scale(2) : Vec3.ZERO;
    }
}
