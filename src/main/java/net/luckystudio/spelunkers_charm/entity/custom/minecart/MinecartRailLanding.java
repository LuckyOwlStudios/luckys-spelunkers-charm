// Minecart rail landing.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public final class MinecartRailLanding {
    private MinecartRailLanding() {}

    public static void landed(AbstractMinecart cart, double verticalSpeed) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        long tick = cart.level().getGameTime();
        if (cart.level().isClientSide()
                || verticalSpeed > .02
                || !motion.airborne
                || motion.freeTicks < 1
                || (motion.railLandingTick != Long.MIN_VALUE && tick - motion.railLandingTick < 6))
            return;
        motion.railLandingTick = tick;
        float volume = (float) Math.clamp(.35 - verticalSpeed * .5, .35, 1.1);
        float pitch =
                (cart instanceof LocomotiveMinecart ? .78f : 1.0f) + (cart.getId() % 7 - 3) * .025f;
        cart.level()
                .playSound(
                        null,
                        cart.getX(),
                        cart.getY(),
                        cart.getZ(),
                        ModSoundEvents.CART_LAND.get(),
                        SoundSource.NEUTRAL,
                        volume,
                        pitch);
    }
}
