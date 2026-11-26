// Furnace minecart motion mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartRailMotion;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartSpeedRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecartFurnace.class, remap = false)
public abstract class FurnaceMinecartMotionMixin {
    @Inject(method = "moveAlongTrack", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$move(BlockPos pos, BlockState state, CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (MinecartSpeedRules.managed(cart)) {
            if (!cart.level().isClientSide()) MinecartRailMotion.move(cart, pos, state);
            info.cancel();
        }
    }
}
