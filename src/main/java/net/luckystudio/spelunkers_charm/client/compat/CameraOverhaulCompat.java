package net.luckystudio.spelunkers_charm.client.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional support for Camera Overhaul (Mirsario). Uses its screen shake system
 * ({@code mirsario.cameraoverhaul.ScreenShakes}) through reflection, so Camera Overhaul is not
 * needed to build or run Spelunker's Charm. If it is missing, or its internals change, this
 * turns itself off and nothing happens.
 */
public final class CameraOverhaulCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "cameraoverhaul";

    private static boolean initialized;
    private static boolean available;

    private static Method recreate;
    private static Method get;
    private static Field trauma;
    private static Field radius;
    private static Field frequency;
    private static Field lengthInSeconds;
    private static Field position;

    private CameraOverhaulCompat() {}

    public static boolean isAvailable() {
        if (!initialized) {
            initialized = true;
            available = ModList.get().isLoaded(MOD_ID) && init();
        }
        return available;
    }

    private static boolean init() {
        try {
            Class<?> screenShakes = Class.forName("mirsario.cameraoverhaul.ScreenShakes");
            Class<?> slot = Class.forName("mirsario.cameraoverhaul.ScreenShakes$Slot");
            recreate = screenShakes.getMethod("recreate", long.class);
            get = screenShakes.getMethod("get", long.class);
            trauma = slot.getField("trauma");
            radius = slot.getField("radius");
            frequency = slot.getField("frequency");
            lengthInSeconds = slot.getField("lengthInSeconds");
            position = slot.getField("position");
            LOGGER.info("Camera Overhaul found, tremors will shake the camera");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Camera Overhaul is installed but its screen shake API wasn't found, tremor camera shake is off", e);
            return false;
        }
    }

    /**
     * Starts or refreshes a positioned screen shake.
     *
     * @param handle the handle returned by the last call for this shake, or 0 for a new one
     * @return the handle to pass next time
     */
    public static long shake(long handle, double x, double y, double z, float shakeTrauma, float shakeRadius,
                             float shakeFrequency, float seconds) {
        if (!isAvailable()) return 0L;
        try {
            long newHandle = (long) recreate.invoke(null, handle);
            Object instance = get.invoke(null, newHandle);
            trauma.setFloat(instance, shakeTrauma);
            radius.setFloat(instance, shakeRadius);
            frequency.setFloat(instance, shakeFrequency);
            lengthInSeconds.setFloat(instance, seconds);
            ((Vector3d) position.get(instance)).set(x, y, z);
            return newHandle;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Camera Overhaul screen shake failed, tremor camera shake is off", e);
            available = false;
            return 0L;
        }
    }
}
