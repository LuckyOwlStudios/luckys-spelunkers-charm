// Powered rail signal mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PoweredRailBlock.class, remap = false)
public abstract class PoweredRailSignalMixin {
    @Inject(method = "isSameRailWithPower", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$doNotRelayDirectionalRails(
            Level level,
            BlockPos neighbor,
            boolean forward,
            int depth,
            RailShape shape,
            CallbackInfoReturnable<Boolean> info) {
        if (level.getBlockState(neighbor).getBlock() instanceof DirectionalRailBlock)
            info.setReturnValue(false);
    }
}
