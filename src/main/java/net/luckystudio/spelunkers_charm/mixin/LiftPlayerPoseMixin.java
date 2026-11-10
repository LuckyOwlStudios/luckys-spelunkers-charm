package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftPoseCollisions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vanilla pose rules, allowing only the player's actual supporting lift deck. */
@Mixin(value = Player.class, remap = false)
public abstract class LiftPlayerPoseMixin {
    @Inject(
            method = "canPlayerFitWithinBlocksAndEntitiesWhen",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void spelunkersCharm$checkDeckPose(Pose pose, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValueZ()
                && LiftPoseCollisions.canFitOnSupportingLift((Player) (Object) this, pose)) {
            result.setReturnValue(true);
        }
    }
}
