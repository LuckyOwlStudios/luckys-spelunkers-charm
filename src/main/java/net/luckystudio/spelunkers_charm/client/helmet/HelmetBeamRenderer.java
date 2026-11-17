package net.luckystudio.spelunkers_charm.client.helmet;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Draws the mining helmet's light beam: a soft, glowing, square beam of light coming out of the square lamp.
 * <p>
 * The beam is two nested, additive square shells (a narrow bright core and a wide soft halo) that widen with distance
 * and turn with the head. Each vertex is weighted by roughly how much beam the camera looks through at that point:
 * faces seen square-on glow, faces seen edge-on melt away, and the beam gets brighter when looking along it, the way
 * a real beam in dusty air looks. It also fades out along its length, and stops where the beam hits a block.
 * A soft square glow marks the spot it lands on.
 * <p>
 * Drawn by {@link MiningHelmetBeamLayer} from the actual head model, so you see it on other players and mobs, and on
 * yourself in third person. Your own beam isn't drawn in first person; you just see the light it casts.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class HelmetBeamRenderer {
    /** How far the visible beam reaches before it has faded out completely, in blocks. */
    public static final float VISIBLE_LENGTH = 10.0F;

    // Warm white, a little yellow like an old carbide or incandescent lamp.
    private static final float RED = 1.0F;
    private static final float GREEN = 0.9F;
    private static final float BLUE = 0.7F;

    // Two nested square shells: {half-width at the lens, half-width growth per block, brightness}.
    // The core starts the size of the lens (2.5px across).
    private static final float[][] SHELLS = {
            {0.075F, 0.075F, 0.24F}, // core
            {0.11F, 0.17F, 0.13F},   // halo
    };
    // Vertices across each face of a shell, so the fade toward its edges can vary across the face.
    private static final int FACE_STEPS = 4;
    private static final int RINGS = 12;
    // How strongly the fade on faces seen edge-on bites. Higher is softer.
    private static final float EDGE_SOFTNESS = 1.6F;
    // Looking along the beam you look through more of it, so it gets brighter, up to this many times.
    private static final float MAX_ALONG_VIEW_BOOST = 3.0F;

    // Glow where the beam lands, and the small flare on the lens itself.
    private static final float SPOT_BRIGHTNESS = 0.22F;
    private static final float LENS_FLARE_SIZE = 0.14F;
    private static final float LENS_FLARE_BRIGHTNESS = 0.55F;

    // The four faces of a shell, as the direction they face: {along side, along up}.
    private static final int[][] FACES = {
            {1, 0}, {0, 1}, {-1, 0}, {0, -1}
    };

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
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES || stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            drawingWorld = false;
        }
    }

    /**
     * Draws a beam. All positions are relative to the camera, which is at the origin.
     *
     * @param origin     center of the lamp lens
     * @param forward    beam direction, normalized
     * @param up         the head's up direction, perpendicular to {@code forward}; the square turns with it
     * @param length     distance to the block the beam hits, or {@link #VISIBLE_LENGTH}
     * @param scale      size of the wearer, so baby zombies get a smaller lamp
     * @param brightness overall opacity multiplier
     */
    static void drawBeam(VertexConsumer consumer, Vector3f origin, Vector3f forward, Vector3f up, float length,
                         float scale, float brightness) {
        if (brightness <= 0.0F || length <= 0.05F) return;

        Vector3f side = new Vector3f(forward).cross(up);
        if (side.lengthSquared() < 1.0E-6F) {
            side = new Vector3f(forward).cross(0.0F, 1.0F, 0.0F);
            if (side.lengthSquared() < 1.0E-6F) side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize();
        Vector3f realUp = new Vector3f(side).cross(forward).normalize();

        // The beam ends a little past where its middle hits, since its edges reach the surface later
        // (or never, if they pass the block's edge). Depth testing hides whatever goes into the block.
        float end = Math.min(length + 0.5F, VISIBLE_LENGTH);

        // Rings are packed closer near the lens, where the beam changes fastest.
        float[] ringDistance = new float[RINGS + 1];
        for (int r = 0; r <= RINGS; r++) {
            float t = (float) r / RINGS;
            ringDistance[r] = end * t * t;
        }

        for (float[] shell : SHELLS) {
            float startHalf = shell[0] * scale;
            float spread = shell[1];
            float shellAlpha = shell[2] * brightness;
            float normalLength = Mth.sqrt(1.0F + spread * spread);

            for (int[] face : FACES) {
                // Outward axis of this face, and the axis it spans across.
                Vector3f out = new Vector3f(side).mul(face[0]).add(new Vector3f(realUp).mul(face[1]));
                Vector3f across = new Vector3f(realUp).mul(face[0]).sub(new Vector3f(side).mul(face[1]));
                // The face tilts outward as the beam widens.
                float nx = (out.x - forward.x * spread) / normalLength;
                float ny = (out.y - forward.y * spread) / normalLength;
                float nz = (out.z - forward.z * spread) / normalLength;

                Vector3f[] previous = null;
                float[] previousAlpha = null;
                for (int r = 0; r <= RINGS; r++) {
                    float distance = ringDistance[r];
                    float half = startHalf + spread * distance;
                    float along = alongFade(distance, end, length);

                    Vector3f[] row = new Vector3f[FACE_STEPS + 1];
                    float[] rowAlpha = new float[FACE_STEPS + 1];
                    for (int i = 0; i <= FACE_STEPS; i++) {
                        float t = -1.0F + 2.0F * i / FACE_STEPS;
                        Vector3f pos = new Vector3f(
                                origin.x + forward.x * distance + (out.x + across.x * t) * half,
                                origin.y + forward.y * distance + (out.y + across.y * t) * half,
                                origin.z + forward.z * distance + (out.z + across.z * t) * half);
                        row[i] = pos;
                        rowAlpha[i] = shellAlpha * along * viewWeight(pos, nx, ny, nz, forward);
                    }

                    if (previous != null) {
                        for (int i = 0; i < FACE_STEPS; i++) {
                            vertex(consumer, previous[i], previousAlpha[i]);
                            vertex(consumer, previous[i + 1], previousAlpha[i + 1]);
                            vertex(consumer, row[i + 1], rowAlpha[i + 1]);
                            vertex(consumer, row[i], rowAlpha[i]);
                        }
                    }
                    previous = row;
                    previousAlpha = rowAlpha;
                }
            }
        }

        // Soft square glow where the beam lands, pulled back toward the lamp so it doesn't sink into the block.
        if (length < VISIBLE_LENGTH - 0.5F) {
            float pull = Math.min(0.35F, length * 0.5F);
            Vector3f spot = new Vector3f(forward).mul(length - pull).add(origin);
            float spotHalf = (0.2F + 0.08F * length) * scale;
            float spotAlpha = SPOT_BRIGHTNESS * brightness * alongFade(length, VISIBLE_LENGTH, VISIBLE_LENGTH);
            squareGlow(consumer, spot, side, realUp, spotHalf, spotAlpha);
        }

        squareGlow(consumer, origin, side, realUp, LENS_FLARE_SIZE * scale, LENS_FLARE_BRIGHTNESS * brightness);
    }

    /**
     * Roughly how much beam the camera sees through at this point of a face (the camera is at the origin).
     * A view ray's path through the beam grows with how squarely it crosses the face around the axis,
     * and with how closely it runs along the axis.
     */
    private static float viewWeight(Vector3f pos, float nx, float ny, float nz, Vector3f forward) {
        float viewLength = pos.length();
        if (viewLength < 1.0E-4F) return 0.0F;
        float alongAxis = (pos.x * forward.x + pos.y * forward.y + pos.z * forward.z) / viewLength;
        float sinAxis = Mth.sqrt(Math.max(0.0F, 1.0F - alongAxis * alongAxis));
        float facing = Math.abs(nx * pos.x + ny * pos.y + nz * pos.z) / viewLength;
        // How squarely the view crosses the face, seen down the axis: 1 head-on, 0 edge-on.
        float across = Mth.clamp(facing / Math.max(sinAxis, 1.0E-3F), 0.0F, 1.0F);
        float boost = Math.min(1.0F / Math.max(sinAxis, 1.0E-3F), MAX_ALONG_VIEW_BOOST);
        return (float) Math.pow(across, EDGE_SOFTNESS) * boost;
    }

    /** Fade along the beam: out with distance, and quickly past where it hits. */
    private static float alongFade(float distance, float end, float length) {
        float t = Mth.clamp(distance / VISIBLE_LENGTH, 0.0F, 1.0F);
        float fade = (1.0F - t) * (1.0F - t);
        if (distance > length) fade *= Mth.clamp(1.0F - (distance - length) / Math.max(end - length, 0.01F), 0.0F, 1.0F);
        return fade;
    }

    /**
     * A square glow across the beam, lined up with the lamp, fading from its center to its edges.
     * Built from nested square bands so the falloff is smooth.
     */
    private static void squareGlow(VertexConsumer consumer, Vector3f center, Vector3f side, Vector3f up,
                                   float half, float alpha) {
        if (alpha <= 0.002F) return;
        // Bands at these fractions of the half-width, with these shares of the center brightness.
        float[] bandSize = {0.0F, 0.25F, 0.5F, 0.75F, 1.0F};
        float[] bandShare = {1.0F, 0.75F, 0.4F, 0.13F, 0.0F};
        // The four corners of a unit square, in order around it.
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int b = 0; b < bandSize.length - 1; b++) {
            for (int c = 0; c < 4; c++) {
                float[] c0 = corners[c];
                float[] c1 = corners[(c + 1) % 4];
                float inner = half * bandSize[b];
                float outer = half * bandSize[b + 1];
                vertex(consumer, squarePoint(center, side, up, c0, inner), alpha * bandShare[b]);
                vertex(consumer, squarePoint(center, side, up, c1, inner), alpha * bandShare[b]);
                vertex(consumer, squarePoint(center, side, up, c1, outer), alpha * bandShare[b + 1]);
                vertex(consumer, squarePoint(center, side, up, c0, outer), alpha * bandShare[b + 1]);
            }
        }
    }

    private static Vector3f squarePoint(Vector3f center, Vector3f side, Vector3f up, float[] corner, float half) {
        float s = corner[0] * half;
        float u = corner[1] * half;
        return new Vector3f(
                center.x + side.x * s + up.x * u,
                center.y + side.y * s + up.y * u,
                center.z + side.z * s + up.z * u);
    }

    private static void vertex(VertexConsumer consumer, Vector3f pos, float alpha) {
        consumer.addVertex(pos.x, pos.y, pos.z).setColor(RED, GREEN, BLUE, Mth.clamp(alpha, 0.0F, 1.0F));
    }
}
