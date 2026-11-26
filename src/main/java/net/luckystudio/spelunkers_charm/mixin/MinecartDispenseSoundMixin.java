// Minecart dispense sound mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartPlacementSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "net.minecraft.world.item.MinecartItem$1", remap = false)
public abstract class MinecartDispenseSoundMixin {
    @Redirect(
            method = "execute",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean spelunkersCharm$place(ServerLevel level, Entity cart) {
        return MinecartPlacementSounds.add(level, cart);
    }
}
