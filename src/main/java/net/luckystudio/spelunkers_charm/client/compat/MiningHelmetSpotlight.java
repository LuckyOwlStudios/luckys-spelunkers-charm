package net.luckystudio.spelunkers_charm.client.compat;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.luckystudio.spelunkers_charm.client.helmet.MiningHelmetBeam;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A LambDynamicLights light shaped like the helmet's square beam: a bright square pool of light where the mining
 * helmet points, and a widening square beam of dimmer light along the way there. Only loaded when LambDynamicLights is installed.
 * <p>
 * LambDynamicLights reads {@link #lightAtPos} from its chunk-building threads, so the beam is kept as one immutable
 * {@link Beam} swapped in whole. The light only moves when the beam moved a noticeable amount, so standing still
 * doesn't re-light chunks every tick.
 */
final class MiningHelmetSpotlight implements DynamicLightBehavior {
    // Widening of the lit beam per block of distance, matching the visible beam's halo.
    private static final double SPREAD = 0.2;
    private static final double START_RADIUS = 0.6;
    // Light along the beam starts at this level and drops this much per block.
    private static final double SHAFT_LIGHT = 13.0;
    private static final double SHAFT_LIGHT_DROP = 0.2;
    // Light of the pool where the beam lands, and how much dimmer it gets per block of distance.
    private static final double POOL_LIGHT = 15.0;
    private static final double POOL_LIGHT_DROP = 0.15;
    // How fast the pool fades out around the spot, relative to an ordinary light.
    private static final double POOL_FALLOFF = 1.4;
    // The pool's center is pulled back off the surface, so less light bleeds through to the other side of the wall.
    private static final double POOL_PULL_BACK = 0.6;
    // The beam doesn't light the first block or so in front of the wearer: the helmet's own glow covers that.
    private static final double SHAFT_START = 1.0;

    // How far the beam must move before the world is re-lit. The wearer's position uses the same 0.1 block step
    // LambDynamicLights uses for held and worn lights, so the beam moves as smoothly as the candle helmet's glow.
    private static final double MOVE_THRESHOLD_SQR = 0.15 * 0.15;
    private static final double ORIGIN_THRESHOLD_SQR = 0.1 * 0.1;
    // If it moved a little but less than that, it catches up after this many ticks.
    private static final int SETTLE_TICKS = 2;

    /** One snapshot of the beam. {@code poolLight} is 0 when the beam hit nothing. */
    record Beam(double ox, double oy, double oz, double dx, double dy, double dz,
                double sx, double sy, double sz, double ux, double uy, double uz, double length,
                double px, double py, double pz, double poolLight) {
        double endRadius() {
            return START_RADIUS + SPREAD * this.length;
        }
    }

    private volatile Beam pending;
    private volatile Beam active;
    private int ticksSinceMoved;

    MiningHelmetSpotlight(Beam beam) {
        this.pending = beam;
        this.active = beam;
    }

    void update(Beam beam) {
        this.pending = beam;
    }

    static Beam trace(LivingEntity living, double range) {
        Vec3 eye = living.getEyePosition();
        // The square beam turns with the head: side and up are the head's own axes.
        Vec3 look = Vec3.directionFromRotation(living.getXRot(), living.getYHeadRot());
        Vec3 up = Vec3.directionFromRotation(living.getXRot() - 90.0F, living.getYHeadRot());
        Vec3 side = look.cross(up).normalize();
        BlockHitResult hit = MiningHelmetBeam.trace(living.level(), eye, look, range, living);
        double length = range;
        double poolLight = 0.0;
        if (hit.getType() != HitResult.Type.MISS) {
            length = hit.getLocation().distanceTo(eye);
            poolLight = Math.max(0.0, POOL_LIGHT - POOL_LIGHT_DROP * length);
        }
        double pull = Math.min(POOL_PULL_BACK, length);
        Vec3 pool = eye.add(look.scale(length - pull));
        return new Beam(eye.x, eye.y, eye.z, look.x, look.y, look.z, side.x, side.y, side.z, up.x, up.y, up.z,
                length, pool.x, pool.y, pool.z, poolLight);
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        Beam beam = this.active;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        double light = 0.0;

        // Square beam of light, widening with distance like the visible beam.
        double vx = x - beam.ox;
        double vy = y - beam.oy;
        double vz = z - beam.oz;
        double along = vx * beam.dx + vy * beam.dy + vz * beam.dz;
        if (along > SHAFT_START && along < beam.length + 2.0) {
            double rx = vx - beam.dx * along;
            double ry = vy - beam.dy * along;
            double rz = vz - beam.dz * along;
            // Distance from the axis measured square, so the lit area is a square too.
            double radial = Math.max(Math.abs(rx * beam.sx + ry * beam.sy + rz * beam.sz),
                    Math.abs(rx * beam.ux + ry * beam.uy + rz * beam.uz));
            double radius = START_RADIUS + SPREAD * along;
            // Full light through the middle of the beam, fading out softly past its edge.
            double edge = 1.0 - smoothstep(radius * 0.6, radius + 0.75, radial);
            // Fade in over the first blocks, so it blends into the helmet's own glow.
            double start = smoothstep(SHAFT_START, SHAFT_START + 2.0, along);
            // Quickly gone past the surface the beam lands on.
            double past = along > beam.length ? Mth.clamp(1.0 - (along - beam.length) / 2.0, 0.0, 1.0) : 1.0;
            light = Math.max(0.0, SHAFT_LIGHT - SHAFT_LIGHT_DROP * along) * edge * start * past;
        }

        // Square pool of light where it lands.
        if (beam.poolLight > 0.0) {
            double px = x - beam.px;
            double py = y - beam.py;
            double pz = z - beam.pz;
            double distance = Math.max(Math.abs(px * beam.dx + py * beam.dy + pz * beam.dz), Math.max(
                    Math.abs(px * beam.sx + py * beam.sy + pz * beam.sz),
                    Math.abs(px * beam.ux + py * beam.uy + pz * beam.uz)));
            light = Math.max(light, beam.poolLight - distance * falloffRatio * POOL_FALLOFF);
        }

        return Mth.clamp(light, 0.0, 15.0);
    }

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        Beam beam = this.active;
        // The square's corners reach farther from the axis than its sides.
        double pad = beam.endRadius() * Math.sqrt(2.0) + 0.75;
        double ex = beam.ox + beam.dx * beam.length;
        double ey = beam.oy + beam.dy * beam.length;
        double ez = beam.oz + beam.dz * beam.length;
        return new BoundingBox(
                Mth.floor(Math.min(beam.ox, ex) - pad),
                Mth.floor(Math.min(beam.oy, ey) - pad),
                Mth.floor(Math.min(beam.oz, ez) - pad),
                Mth.ceil(Math.max(beam.ox, ex) + pad),
                Mth.ceil(Math.max(beam.oy, ey) + pad),
                Mth.ceil(Math.max(beam.oz, ez) + pad));
    }

    @Override
    public boolean hasChanged() {
        Beam next = this.pending;
        Beam current = this.active;
        if (next == current) return false;

        boolean lightChanged = Math.abs(next.poolLight - current.poolLight) >= 1.0
                || (next.poolLight > 0.0) != (current.poolLight > 0.0);
        double originMoved = distanceSqr(next.ox, next.oy, next.oz, current.ox, current.oy, current.oz);
        double endMoved = distanceSqr(
                next.ox + next.dx * next.length, next.oy + next.dy * next.length, next.oz + next.dz * next.length,
                current.ox + current.dx * current.length, current.oy + current.dy * current.length, current.oz + current.dz * current.length);
        if (!lightChanged && originMoved < 1.0E-4 && endMoved < 1.0E-4) return false; // standing still
        boolean moved = originMoved > ORIGIN_THRESHOLD_SQR || endMoved > MOVE_THRESHOLD_SQR;

        if (lightChanged || moved || ++this.ticksSinceMoved >= SETTLE_TICKS) {
            this.active = next;
            this.ticksSinceMoved = 0;
            return true;
        }
        return false;
    }

    private static double distanceSqr(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
