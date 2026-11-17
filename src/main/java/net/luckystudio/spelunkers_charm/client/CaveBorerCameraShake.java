package net.luckystudio.spelunkers_charm.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.client.compat.CameraOverhaulCompat;
import net.luckystudio.spelunkers_charm.entity.custom.cave_borer.CaveBorer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Shakes the camera around large Cave Borers, through Camera Overhaul if it is installed.
 * A short, hard shake when a worm bursts out of the ground, and a light rumble while its
 * body burrows back in. Sizes 0 and 1 don't shake at all, and it grows with size.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CaveBorerCameraShake {
    // Smallest size that shakes the camera.
    private static final int MIN_SIZE = 2;
    // Burst trauma (0 to 1) at MIN_SIZE, and how much each size above it adds.
    private static final float BURST_BASE = 0.25F;
    private static final float BURST_PER_SIZE = 0.11F;
    private static final float BURST_SECONDS = 0.4F;
    private static final float BURST_FREQUENCY = 2.5F;
    // The burrowing rumble is this share of the burst.
    private static final float BURROW_SHARE = 0.3F;
    private static final float BURROW_LINGER_SECONDS = 0.3F;
    private static final float BURROW_FREQUENCY = 1.5F;
    // Shake reaches this far (in blocks) at MIN_SIZE, plus this much per size above it, fading with distance.
    private static final float RADIUS_BASE = 10.0F;
    private static final float RADIUS_PER_SIZE = 3.0F;

    private static final Int2ObjectMap<CaveBorer.State> LAST_STATE = new Int2ObjectOpenHashMap<>();
    private static final Int2LongMap BURROW_HANDLES = new Int2LongOpenHashMap();

    private CaveBorerCameraShake() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            LAST_STATE.clear();
            BURROW_HANDLES.clear();
            return;
        }
        if (minecraft.isPaused()) return;
        if (!SpelunkersCharmConfig.CAVE_BORER_CAMERA_SHAKE.get() || !CameraOverhaulCompat.isAvailable()) return;
        float strength = SpelunkersCharmConfig.CAVE_BORER_CAMERA_SHAKE_STRENGTH.get().floatValue();
        if (strength <= 0.0F) return;

        float maxRadius = radiusFor(CaveBorer.MAX_SIZE);
        Int2ObjectMap<CaveBorer.State> states = new Int2ObjectOpenHashMap<>();
        Int2LongMap handles = new Int2LongOpenHashMap();
        for (CaveBorer borer : minecraft.level.getEntitiesOfClass(CaveBorer.class, player.getBoundingBox().inflate(maxRadius))) {
            CaveBorer.State state = borer.getState();
            int id = borer.getId();
            states.put(id, state);
            int size = borer.getSize();
            if (size < MIN_SIZE || borer.isDeadOrDying()) continue;

            float radius = radiusFor(size);
            float burst = Mth.clamp((BURST_BASE + BURST_PER_SIZE * (size - MIN_SIZE)) * strength, 0.0F, 1.0F);
            Vec3 pos = borer.getCenter();

            CaveBorer.State last = LAST_STATE.get(id);
            if (state == CaveBorer.State.LUNGE && last != null && last != CaveBorer.State.LUNGE) {
                // Bursting out of the wall: one short, hard shake
                CameraOverhaulCompat.shake(0L, pos.x, pos.y, pos.z, burst, radius, BURST_FREQUENCY, BURST_SECONDS);
            } else if (state == CaveBorer.State.DIVE) {
                // Body pouring back into the ground: keep a light rumble going until it's gone
                long handle = CameraOverhaulCompat.shake(BURROW_HANDLES.get(id), pos.x, pos.y, pos.z,
                        burst * BURROW_SHARE, radius, BURROW_FREQUENCY, BURROW_LINGER_SECONDS);
                if (handle != 0L) handles.put(id, handle);
            }
        }
        LAST_STATE.clear();
        LAST_STATE.putAll(states);
        BURROW_HANDLES.clear();
        BURROW_HANDLES.putAll(handles);
    }

    private static float radiusFor(int size) {
        return RADIUS_BASE + RADIUS_PER_SIZE * Math.max(0, size - MIN_SIZE);
    }
}
