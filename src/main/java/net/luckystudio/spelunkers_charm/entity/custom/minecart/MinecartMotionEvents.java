// Minecart motion events.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID)
public final class MinecartMotionEvents {
    private MinecartMotionEvents() {}

    @SubscribeEvent
    public static void before(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof AbstractMinecart cart && !cart.level().isClientSide()) {
            var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
            state.beforeVelocity = cart.getDeltaMovement();
            state.beforePosition = cart.position();
            state.held = false;
            state.controlledMovement = false;
            MinecartHandbrake.beforeTick(cart);
        }
    }

    @SubscribeEvent
    public static void after(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractMinecart cart) || cart.level().isClientSide())
            return;
        var visuals = (MinecartMotionVisuals) cart;
        var state = visuals.spelunkersCharm$motion();
        if (MinecartTrainBrakes.onRail(cart)) {
            MinecartHingeGeometry.supportedAxis(cart);
            visuals.spelunkersCharm$setBodyYaw(
                    cart instanceof LocomotiveMinecart engine ? engine.bodyYaw() : state.visualYaw);
        }
        MinecartHandbrake.hold(cart, cart.getDeltaMovement().horizontalDistance());
        visuals.spelunkersCharm$setBraking(state.effectTicks > 0 ? state.brakeStrength : 0);
        if (state.effectTicks > 0) state.effectTicks--;
    }
}
