package net.luckystudio.spelunkers_charm.client.helmet;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Draws the mining helmet's light beam: a soft, glowing cone of light coming out of the lamp.
 * <p>
 * The cone is two nested, additive shells (a narrow bright core and a wide soft halo). Each vertex is weighted by
 * roughly how much beam the camera looks through at that point: bright through the middle, melting away at the
 * silhouette, and brighter when looking along the beam, the way a real beam in dusty air looks, instead of a solid
 * plastic cone. It also fades out along its length, and stops where the beam hits a block. A soft glow marks the spot
 * it lands on.
 * <p>
 * Third person and other entities: drawn by {@link MiningHelmetBeamLayer} from the actual head model.
 * First person: drawn here after particles, from where the lamp sits just above the camera.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class HelmetBeamRenderer {
    /** How far the visible beam reaches before it has faded out completely, in blocks. */
    public static final float VISIBLE_LENGTH = 14.0F;

    // Warm white, a little yellow like an old carbide or incandescent lamp.
    private static final float RED = 1.0F;
    private static final float GREEN = 0.9F;
    private static final float BLUE = 0.7F;

    // Two nested cone shells: {radius at the lens, radius growth per block, brightness}.
    private static final float[][] SHELLS = {
            {0.07F, 0.085F, 0.24F}, // core
            {0.12F, 0.20F, 0.13F},  // halo
    };
    private static final int SEGMENTS = 24;
    private static final int RINGS = 14;
    // How strongly the fade toward the cone's silhouette bites. Higher is softer edges.
    private static final float EDGE_SOFTNESS = 1.6F;
    // Looking along the beam you look through more of it, so it gets brighter, up to this many times.
    private static final float MAX_ALONG_VIEW_BOOST = 3.0F;

    // First person: the camera is inside the beam, so it shows as a faint haze that's thickest toward the middle of
    // the view. The lamp sits just above the camera, so the start of the beam is faded in over this distance,
    // otherwise its underside fills the top of the screen.
    private static final float FIRST_PERSON_FADE_IN = 2.5F;
    private static final float FIRST_PERSON_BRIGHTNESS = 0.2F;
    // Inside the beam, haze builds up over this many blocks of looking through it.
    private static final float INSIDE_HAZE_DEPTH = 6.0F;

    // Glow where the beam lands, and the small flare on the lens itself.
    private static final float SPOT_BRIGHTNESS = 0.22F;
    private static final float LENS_FLARE_RADIUS = 0.16F;
    private static final float LENS_FLARE_BRIGHTNESS = 0.55F;

    // Entity renderers are also used for the player in the inventory screen. The beam must only be drawn
    // while the world itself is being drawn, so this is set around the world's entity pass.
    private static boolean drawingWorld;

    private HelmetBeamRenderer() {}

    static boolean isDrawingWorld() {
        return drawingWorld;
    }

    static boolean enabled() {
        return SpelunkersCharmConfig.MINING_HELMET_BEAM.get();
    }

    static float opacity() {
        return SpelunkersCharmConfig.MINING_HELMET_BEAM_OPACITY.get().floatValue();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY || stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            drawingWorld = true;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            drawingWorld = false;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            drawingWorld = false;
            renderFirstPerson(event);
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            drawingWorld = false;
        }
    }

    private static void renderFirstPerson(RenderLevelStageEvent event) {
        if (!enabled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        if (camera.isDetached()) return;
        Entity cameraEntity = camera.getEntity();
        if (!(cameraEntity instanceof LivingEntity living) || !MiningHelmetBeam.hasLitLamp(living)) return;
        if (living.isSleeping()) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 lamp = MiningHelmetBeam.lampPosition(living, partialTick);
        float pitch = living.getViewXRot(partialTick);
        float yaw = Mth.lerp(partialTick, living.yHeadRotO, living.yHeadRot);
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        Vec3 up = Vec3.directionFromRotation(pitch - 90.0F, yaw);
        double length = MiningHelmetBeam.traceDistance(living.level(), lamp, forward, VISIBLE_LENGTH, living);

        Vec3 cam = camera.getPosition();
        Vector3f origin = new Vector3f((float) (lamp.x - cam.x), (float) (lamp.y - cam.y), (float) (lamp.z - cam.z));

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(HelmetBeamRenderType.BEAM);
        drawBeam(consumer, origin, forward.toVector3f(), up.toVector3f(), (float) length, 1.0F,
                FIRST_PERSON_FADE_IN, opacity() * FIRST_PERSON_BRIGHTNESS, true, false);
        buffers.endBatch(HelmetBeamRenderType.BEAM);
    }

    /**
     * Draws a beam. All positions are relative to the camera, which is at the origin.
     *
     * @param origin     center of the lamp lens
     * @param forward    beam direction, normalized
     * @param up         any direction perpendicular to {@code forward}, used to orient the mesh
     * @param length     distance to the block the beam hits, or {@link #VISIBLE_LENGTH}
     * @param scale      size of the wearer, so baby zombies get a smaller lamp
     * @param fadeIn     distance over which the start of the beam fades in (0 for none)
     * @param brightness overall opacity multiplier
     * @param inside     the camera is inside the beam (your own beam in first person)
     * @param lensFlare  draw the small glow on the lens (third person only)
     */
    static void drawBeam(VertexConsumer consumer, Vector3f origin, Vector3f forward, Vector3f up, float length,
                         float scale, float fadeIn, float brightness, boolean inside, boolean lensFlare) {
        if (brightness <= 0.0F || length <= 0.05F) return;

        Vector3f side = new Vector3f(forward).cross(up);
        if (side.lengthSquared() < 1.0E-6F) {
            side = new Vector3f(forward).cross(0.0F, 1.0F, 0.0F);
            if (side.lengthSquared() < 1.0E-6F) side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize();
        Vector3f realUp = new Vector3f(side).cross(forward).normalize();

        // Precompute the ring directions around the axis.
        float[] cos = new float[SEGMENTS + 1];
        float[] sin = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (i * Math.PI * 2.0 / SEGMENTS);
            cos[i] = Mth.cos(angle);
            sin[i] = Mth.sin(angle);
        }

        // The beam ends a little past where its middle hits, since its edges reach the surface later
        // (or never, if they pass the block's edge). Depth testing hides whatever goes into the block.
        float end = Math.min(length + 0.5F, VISIBLE_LENGTH);

        for (float[] shell : SHELLS) {
            float startRadius = shell[0] * scale;
            float spread = shell[1];
            float shellAlpha = shell[2] * brightness;

            // Rings are packed closer near the lens, where the cone changes fastest.
            float[] ringDistance = new float[RINGS + 1];
            for (int r = 0; r <= RINGS; r++) {
                float t = (float) r / RINGS;
                ringDistance[r] = end * t * t;
            }

            // Slope of the cone wall, to build outward normals.
            float normalAxial = -spread;
            float normalLength = Mth.sqrt(1.0F + spread * spread);

            Vector3f[] previous = null;
            float[] previousAlpha = null;
            for (int r = 0; r <= RINGS; r++) {
                float distance = ringDistance[r];
                float radius = startRadius + spread * distance;
                float along = alongFade(distance, end, length, fadeIn);

                Vector3f[] ring = new Vector3f[SEGMENTS + 1];
                float[] ringAlpha = new float[SEGMENTS + 1];
                for (int i = 0; i <= SEGMENTS; i++) {
                    float rx = side.x * cos[i] + realUp.x * sin[i];
                    float ry = side.y * cos[i] + realUp.y * sin[i];
                    float rz = side.z * cos[i] + realUp.z * sin[i];
                    Vector3f pos = new Vector3f(
                            origin.x + forward.x * distance + rx * radius,
                            origin.y + forward.y * distance + ry * radius,
                            origin.z + forward.z * distance + rz * radius);
                    // Outward normal of the cone wall.
                    float nx = (rx + forward.x * normalAxial) / normalLength;
                    float ny = (ry + forward.y * normalAxial) / normalLength;
                    float nz = (rz + forward.z * normalAxial) / normalLength;
                    ring[i] = pos;
                    ringAlpha[i] = shellAlpha * along * viewWeight(pos, nx, ny, nz, forward, inside);
                }

                if (previous != null) {
                    for (int i = 0; i < SEGMENTS; i++) {
                        vertex(consumer, previous[i], previousAlpha[i]);
                        vertex(consumer, previous[i + 1], previousAlpha[i + 1]);
                        vertex(consumer, ring[i + 1], ringAlpha[i + 1]);
                        vertex(consumer, ring[i], ringAlpha[i]);
                    }
                }
                previous = ring;
                previousAlpha = ringAlpha;
            }
        }

        // Soft glow where the beam lands, pulled back toward the lamp so it doesn't sink into the block.
        if (length < VISIBLE_LENGTH - 0.5F) {
            float pull = Math.min(0.35F, length * 0.5F);
            Vector3f spot = new Vector3f(forward).mul(length - pull).add(origin);
            float spotRadius = (0.25F + 0.1F * length) * scale;
            float spotAlpha = SPOT_BRIGHTNESS * brightness * alongFade(length, VISIBLE_LENGTH, VISIBLE_LENGTH, 0.0F);
            if (fadeIn > 0.0F) spotAlpha *= smoothstep(0.0F, fadeIn, length);
            glow(consumer, spot, spotRadius, spotAlpha);
        }

        if (lensFlare) {
            glow(consumer, origin, LENS_FLARE_RADIUS * scale, LENS_FLARE_BRIGHTNESS * brightness);
        }
    }

    /**
     * Roughly how much beam the camera sees through at this point of the shell (the camera is at the origin).
     * From outside, a view ray's path through a cone grows with how squarely it crosses the wall around the axis,
     * and with how closely it runs along the axis. From inside, it grows with how far the wall is.
     */
    private static float viewWeight(Vector3f pos, float nx, float ny, float nz, Vector3f forward, boolean inside) {
        float viewLength = pos.length();
        if (viewLength < 1.0E-4F) return 0.0F;
        if (inside) return Math.min(viewLength, INSIDE_HAZE_DEPTH) / INSIDE_HAZE_DEPTH;

        float alongAxis = (pos.x * forward.x + pos.y * forward.y + pos.z * forward.z) / viewLength;
        float sinAxis = Mth.sqrt(Math.max(0.0F, 1.0F - alongAxis * alongAxis));
        float facing = Math.abs(nx * pos.x + ny * pos.y + nz * pos.z) / viewLength;
        // How squarely the view crosses the wall, seen down the axis: 1 through the middle, 0 at the silhouette.
        float across = Mth.clamp(facing / Math.max(sinAxis, 1.0E-3F), 0.0F, 1.0F);
        float boost = Math.min(1.0F / Math.max(sinAxis, 1.0E-3F), MAX_ALONG_VIEW_BOOST);
        return (float) Math.pow(across, EDGE_SOFTNESS) * boost;
    }

    /** Fade along the beam: in at the start (first person only), out with distance, and quickly past the hit. */
    private static float alongFade(float distance, float end, float length, float fadeIn) {
        float t = Mth.clamp(distance / VISIBLE_LENGTH, 0.0F, 1.0F);
        float fade = (1.0F - t) * (1.0F - t);
        if (fadeIn > 0.0F) fade *= smoothstep(0.0F, fadeIn, distance);
        if (distance > length) fade *= Mth.clamp(1.0F - (distance - length) / Math.max(end - length, 0.01F), 0.0F, 1.0F);
        return fade;
    }

    /** A round, camera-facing glow that fades from its center to its rim. */
    private static void glow(VertexConsumer consumer, Vector3f center, float radius, float alpha) {
        if (alpha <= 0.002F) return;
        float distance = center.length();
        if (distance < 1.0E-3F) return;
        Vector3f toCamera = new Vector3f(center).div(-distance);
        Vector3f right = new Vector3f(toCamera).cross(0.0F, 1.0F, 0.0F);
        if (right.lengthSquared() < 1.0E-6F) right.set(1.0F, 0.0F, 0.0F);
        right.normalize();
        Vector3f up = new Vector3f(right).cross(toCamera).normalize();

        // Rings at these fractions of the radius, with these shares of the center brightness:
        // close to a gaussian falloff, so there's no visible edge.
        float[] ringRadius = {0.0F, 0.2F, 0.45F, 0.72F, 1.0F};
        float[] ringShare = {1.0F, 0.78F, 0.42F, 0.14F, 0.0F};
        int segments = 16;
        for (int r = 0; r < ringRadius.length - 1; r++) {
            for (int i = 0; i < segments; i++) {
                float a0 = (float) (i * Math.PI * 2.0 / segments);
                float a1 = (float) ((i + 1) * Math.PI * 2.0 / segments);
                vertex(consumer, ringPoint(center, right, up, a0, radius * ringRadius[r]), alpha * ringShare[r]);
                vertex(consumer, ringPoint(center, right, up, a1, radius * ringRadius[r]), alpha * ringShare[r]);
                vertex(consumer, ringPoint(center, right, up, a1, radius * ringRadius[r + 1]), alpha * ringShare[r + 1]);
                vertex(consumer, ringPoint(center, right, up, a0, radius * ringRadius[r + 1]), alpha * ringShare[r + 1]);
            }
        }
    }

    private static Vector3f ringPoint(Vector3f center, Vector3f right, Vector3f up, float angle, float radius) {
        float c = Mth.cos(angle) * radius;
        float s = Mth.sin(angle) * radius;
        return new Vector3f(
                center.x + right.x * c + up.x * s,
                center.y + right.y * c + up.y * s,
                center.z + right.z * c + up.z * s);
    }

    private static void vertex(VertexConsumer consumer, Vector3f pos, float alpha) {
        consumer.addVertex(pos.x, pos.y, pos.z).setColor(RED, GREEN, BLUE, Mth.clamp(alpha, 0.0F, 1.0F));
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
