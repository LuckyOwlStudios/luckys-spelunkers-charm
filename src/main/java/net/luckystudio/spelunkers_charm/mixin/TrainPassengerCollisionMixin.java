// Train passenger collision mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartPassengers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Boat.class, remap = false)
public abstract class TrainPassengerCollisionMixin {
    @Inject(method = "canVehicleCollide", at = @At("HEAD"), cancellable = true)
    private static void spelunkersCharm$ignoreTrainPassengers(
            Entity vehicle, Entity other, CallbackInfoReturnable<Boolean> info) {
        if (MinecartPassengers.sharesTrain(vehicle, other)) info.setReturnValue(false);
    }
}
