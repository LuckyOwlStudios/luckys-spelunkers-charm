// Minecart replacement visuals.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, value = Dist.CLIENT)
public final class MinecartReplacementVisuals {
    private static final Map<Level, Map<Integer, Entry>> RECENT = new IdentityHashMap<>();

    private MinecartReplacementVisuals() {}

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof AbstractMinecart cart) {
            var cache = RECENT.computeIfAbsent(cart.level(), ignored -> new HashMap<>());
            long now = cart.level().getGameTime();
            cache.entrySet().removeIf(entry -> now - entry.getValue().time > 40);
            cache.put(cart.getId(), new Entry(cart, now));
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        RECENT.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) return;
        var cache = RECENT.get(event.getLevel());
        if (cache == null) return;
        long now = event.getLevel().getGameTime();
        cache.entrySet().removeIf(entry -> now - entry.getValue().time > 40);
        if (cache.isEmpty()) RECENT.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof AbstractMinecart cart && cart.level().isClientSide())
            accept(cart);
    }

    public static void accept(AbstractMinecart replacement) {
        if (!replacement.level().isClientSide()) return;
        var visuals = (MinecartMotionVisuals) replacement;
        var state = visuals.spelunkersCharm$motion();
        if (state.replacementHandled) return;
        int source = visuals.spelunkersCharm$replacedCartId();
        if (source < 0) return;
        var cache = RECENT.computeIfAbsent(replacement.level(), ignored -> new HashMap<>());
        long now = replacement.level().getGameTime();
        cache.entrySet().removeIf(entry -> now - entry.getValue().time > 40);
        Entry recent = cache.get(source);
        AbstractMinecart old = recent == null ? null : recent.cart;
        if (old == null && replacement.level().getEntity(source) instanceof AbstractMinecart cart)
            old = cart;
        if (old == null || old == replacement) {
            if (replacement.tickCount > 20) state.replacementHandled = true;
            return;
        }
        Vec3 target = replacement.position();
        if (old.position().distanceToSqr(target) > 144) {
            state.replacementHandled = true;
            return;
        }
        float targetYaw = replacement.getYRot(), targetPitch = replacement.getXRot();
        ((MinecartMotionVisuals) old).spelunkersCharm$motion().copyMotionTo(state);
        state.clientTrainPosition = old.position();
        state.clientTrainHandoff = true;
        if (replacement instanceof LocomotiveMinecart) {
            state.visualYawReady = false;
            state.visualYawTick = Long.MIN_VALUE;
        }
        replacement.setPos(old.getX(), old.getY(), old.getZ());
        replacement.xOld = old.xOld;
        replacement.yOld = old.yOld;
        replacement.zOld = old.zOld;
        replacement.xo = old.xo;
        replacement.yo = old.yo;
        replacement.zo = old.zo;
        replacement.setYRot(old.getYRot());
        replacement.setXRot(old.getXRot());
        replacement.yRotO = old.yRotO;
        replacement.xRotO = old.xRotO;
        replacement.tickCount = old.tickCount;
        visuals.spelunkersCharm$setOnRails(old.isOnRails());
        replacement.lerpTo(target.x, target.y, target.z, targetYaw, targetPitch, 3);
        state.replacementHandled = true;
        if (!old.isRemoved()) old.discard();
        cache.put(source, new Entry(replacement, now));
    }

    /** Keeps the old cart pose until its replacement arrives. */
    private record Entry(AbstractMinecart cart, long time) {}
}
