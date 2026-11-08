package net.luckystudio.spelunkers_charm.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.client.compat.CameraOverhaulCompat;
import net.luckystudio.spelunkers_charm.events.tremor.Tremor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Shakes the camera while the player is near a tremor, through Camera Overhaul if it is installed.
 * Each tick, every tremor in range refreshes a short shake, so the shake follows the tremor's
 * intensity as it builds up and fades, and dies out quickly once the tremor ends.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TremorCameraShake {
    // Strongest trauma (0 to 1) a full strength, max magnitude tremor gives at strength 1.0.
    private static final float MAX_TRAUMA = 0.6F;
    // Share of MAX_TRAUMA that even a magnitude 1 tremor gets, so small tremors are still felt.
    private static final float MIN_MAGNITUDE_SHARE = 0.3F;
    // Shake reaches this many times the tremor's effect radius, fading out with distance.
    private static final float RADIUS_SCALE = 1.5F;
    // Low frequency, for a slow rumble rather than a sharp jolt.
    private static final float FREQUENCY = 0.35F;
    // How long a shake lasts after its last refresh.
    private static final float LINGER_SECONDS = 0.75F;

    // Camera Overhaul shake handle for each tremor, by entity id.
    private static final Int2LongMap HANDLES = new Int2LongOpenHashMap();

    private TremorCameraShake() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            HANDLES.clear();
            return;
        }
        if (!SpelunkersCharmConfig.TREMOR_CAMERA_SHAKE.get() || !CameraOverhaulCompat.isAvailable()) return;

        float strength = SpelunkersCharmConfig.TREMOR_CAMERA_SHAKE_STRENGTH.get().floatValue();
        if (strength <= 0.0F) return;

        float radius = SpelunkersCharmConfig.TREMOR_EFFECT_RADIUS.get() * RADIUS_SCALE;
        int maxMagnitude = SpelunkersCharmConfig.TREMOR_MAX_MAGNITUDE.get();
        List<Tremor> tremors = minecraft.level.getEntitiesOfClass(Tremor.class, player.getBoundingBox().inflate(radius));

        Int2LongMap active = new Int2LongOpenHashMap();
        for (Tremor tremor : tremors) {
            float intensity = tremor.getIntensity();
            if (intensity <= 0.0F) continue;

            float magnitudeShare = maxMagnitude <= 1 ? 1.0F : Mth.clamp((tremor.getMagnitude() - 1) / (float) (maxMagnitude - 1), 0.0F, 1.0F);
            float magnitudeFactor = Mth.lerp(magnitudeShare, MIN_MAGNITUDE_SHARE, 1.0F);
            float trauma = Mth.clamp(MAX_TRAUMA * magnitudeFactor * intensity * strength, 0.0F, 1.0F);

            long handle = CameraOverhaulCompat.shake(HANDLES.get(tremor.getId()), tremor.getX(), tremor.getY(), tremor.getZ(),
                    trauma, radius, FREQUENCY, LINGER_SECONDS);
            if (handle != 0L) active.put(tremor.getId(), handle);
        }
        // Tremors that ended or went out of range are dropped; their last shake fades out on its own.
        HANDLES.clear();
        HANDLES.putAll(active);
    }
}
