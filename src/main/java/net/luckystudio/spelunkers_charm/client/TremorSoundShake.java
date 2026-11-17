package net.luckystudio.spelunkers_charm.client;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.client.compat.CameraOverhaulCompat;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.function.Supplier;

/**
 * Gives tremor sounds a very light camera rumble, through Camera Overhaul if it is installed.
 * When a warning rumble or a tremor sound starts playing, a faint, slow shake starts at the sound's
 * position and fades out over a few seconds. Camera Overhaul makes it weaker the farther away the
 * sound is. Each kind of sound reuses one shake, so overlapping sounds refresh it instead of stacking.
 * This sits on top of {@link TremorCameraShake}, which shakes harder while a tremor is actually running.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TremorSoundShake {
    // Very low trauma (0 to 1) so it reads as a faint rumble. Tremors themselves go up to 0.6.
    private static final float RUMBLE_TRAUMA = 0.12F;
    private static final float TREMOR_TRAUMA = 0.08F;
    // Low frequency, so it feels like a deep rumble rather than a rattle. Camera Overhaul multiplies
    // this by its own max frequency (6 by default), so 0.4 is about 2.4 shakes a second.
    private static final float FREQUENCY = 0.4F;
    // Blocks from the sound where the shake fades to nothing.
    private static final float RADIUS = 48.0F;
    // How long each shake takes to fade out. The warning rumble is about 10 seconds long; tremor
    // sounds replay every 2 to 4 seconds while a tremor runs, so their shake is kept short.
    private static final float RUMBLE_SECONDS = 8.0F;
    private static final float TREMOR_SECONDS = 5.0F;

    private static long rumbleHandle;
    private static long tremorHandle;

    private TremorSoundShake() {}

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;
        ResourceLocation id = sound.getLocation();

        boolean rumble = is(id, ModSoundEvents.RUMBLE_GENERIC) || is(id, ModSoundEvents.RUMBLE_ICY);
        boolean tremor = !rumble && (is(id, ModSoundEvents.TREMOR_GENERIC) || is(id, ModSoundEvents.TREMOR_ICY));
        if (!rumble && !tremor) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!SpelunkersCharmConfig.TREMOR_CAMERA_SHAKE.get() || !CameraOverhaulCompat.isAvailable()) return;
        float strength = SpelunkersCharmConfig.TREMOR_CAMERA_SHAKE_STRENGTH.get().floatValue();
        if (strength <= 0.0F) return;

        // Sounds attached to the listener (like the warning rumble) report a position relative to them.
        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();
        if (sound.isRelative()) {
            x += minecraft.player.getX();
            y += minecraft.player.getY();
            z += minecraft.player.getZ();
        }

        if (rumble) {
            rumbleHandle = CameraOverhaulCompat.shake(rumbleHandle, x, y, z,
                    Math.min(1.0F, RUMBLE_TRAUMA * strength), RADIUS, FREQUENCY, RUMBLE_SECONDS);
        } else {
            tremorHandle = CameraOverhaulCompat.shake(tremorHandle, x, y, z,
                    Math.min(1.0F, TREMOR_TRAUMA * strength), RADIUS, FREQUENCY, TREMOR_SECONDS);
        }
    }

    private static boolean is(ResourceLocation id, Supplier<SoundEvent> event) {
        return id.equals(event.get().getLocation());
    }
}
