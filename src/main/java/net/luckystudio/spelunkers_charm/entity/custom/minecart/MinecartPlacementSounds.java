// Minecart placement sounds.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public final class MinecartPlacementSounds {
    private MinecartPlacementSounds() {}

    public static boolean add(ServerLevel level, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof AbstractMinecart cart) MinecartRailPlacement.align(cart);
        boolean added = level.addFreshEntity(entity);
        if (!added
                || !(entity instanceof AbstractMinecart cart)
                || MinecartTrack.current(cart) == null) return added;
        float pitch =
                (cart instanceof LocomotiveMinecart ? .78f : 1f)
                        * (.95f + level.random.nextFloat() * .1f);
        level.playSound(
                null,
                cart.getX(),
                cart.getY(),
                cart.getZ(),
                ModSoundEvents.CART_PLACE.get(),
                SoundSource.NEUTRAL,
                .75f,
                pitch);
        return true;
    }
}
