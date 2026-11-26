// Minecart crash.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import java.util.ArrayList;

/** Breaks carts and applies passenger fall damage after a hard landing off rails. */
public final class MinecartCrash {
    public static final double SAFE_DROP = 6;

    private MinecartCrash() {}

    public static void record(AbstractMinecart cart, double previousHeight) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (cart.isNoGravity() || cart.isInWaterOrBubble()) {
            clear(cart);
            return;
        }
        double peak = Math.max(previousHeight, cart.getY());
        motion.fallPeak = Double.isNaN(motion.fallPeak) ? peak : Math.max(motion.fallPeak, peak);
    }

    public static void clear(AbstractMinecart cart) {
        ((MinecartMotionVisuals) cart).spelunkersCharm$motion().fallPeak = Double.NaN;
        ((MinecartMotionVisuals) cart).spelunkersCharm$motion().flightLaunched = false;
        cart.resetFallDistance();
        for (var passenger : cart.getIndirectPassengers()) passenger.resetFallDistance();
    }

    public static void landed(AbstractMinecart cart) {
        if (cart.level().isClientSide() || !cart.isAlive() || !cart.onGround()) return;
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        double distance = Double.isNaN(motion.fallPeak) ? 0 : motion.fallPeak - cart.getY();
        boolean water =
                cart.isInWaterOrBubble()
                        || cart.level()
                                .getFluidState(cart.blockPosition())
                                .is(net.minecraft.tags.FluidTags.WATER)
                        || cart.level()
                                .getFluidState(
                                        net.minecraft.core.BlockPos.containing(
                                                cart.getX(), cart.getY() + .3, cart.getZ()))
                                .is(net.minecraft.tags.FluidTags.WATER);
        if (cart.isNoGravity() || water || distance <= SAFE_DROP) {
            clear(cart);
            return;
        }
        var riders = new ArrayList<Entity>();
        cart.getIndirectPassengers().forEach(riders::add);
        cart.ejectPassengers();
        for (var passenger : riders) {
            if (passenger instanceof LivingEntity living)
                living.causeFallDamage((float) distance, 1, living.damageSources().fall());
            passenger.resetFallDistance();
        }
        cart.level()
                .playSound(
                        null,
                        cart.blockPosition(),
                        ModSoundEvents.CART_LAND.get(),
                        SoundSource.NEUTRAL,
                        1.25F,
                        .55F);
        cart.level()
                .playSound(
                        null,
                        cart.blockPosition(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.NEUTRAL,
                        .6F,
                        1.3F);
        MinecartSafetyRules.breakCart(cart);
    }
}
