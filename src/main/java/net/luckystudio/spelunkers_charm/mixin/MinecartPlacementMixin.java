// Minecart placement mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartPlacementSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MinecartItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = MinecartItem.class, remap = false)
public abstract class MinecartPlacementMixin {
    @Redirect(
            method = "useOn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean spelunkersCharm$place(ServerLevel level, Entity cart) {
        return MinecartPlacementSounds.add(level, cart);
    }
}
