// Minecart movement smoothing.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MinecartInterpolation {
    public static final double LEAD_TICKS = 2.5;
    private static final java.util.Map<AbstractMinecart, RenderRoute> RENDER_ROUTES =
            new java.util.WeakHashMap<>();

    private MinecartInterpolation() {}

    public static Vec3 predicted(AbstractMinecart cart, Vec3 target) {
        return predicted(cart, target, LEAD_TICKS);
    }

    public static Vec3 predicted(AbstractMinecart cart, Vec3 target, double lead) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (motion.airborne
                || ((MinecartMotionVisuals) cart).spelunkersCharm$flightTrain()
                || motion.clientTrainTick >= cart.level().getGameTime() - 1) return target;
        if (motion.clientVelocitySeen && motion.clientVelocity.lengthSqr() < 1e-8) return target;
        boolean arrival = motion.clientPacketRailVelocity.lengthSqr() > 1e-8;
        Vec3 velocity = arrival ? motion.clientPacketRailVelocity : motion.clientPacketVelocity;
        if (arrival && motion.clientVelocitySeen)
            velocity = velocity.normalize().scale(motion.clientVelocity.horizontalDistance());
        if (!arrival && motion.clientVelocitySeen && motion.clientVelocity.dot(velocity) > 0)
            velocity = motion.clientVelocity;
        if (target.distanceToSqr(cart.position()) > 36
                || velocity.lengthSqr() < 1e-8
                || ((MinecartMotionVisuals) cart).spelunkersCharm$braking() > .01
                || cart instanceof LocomotiveMinecart engine && engine.emergencyBrake())
            return target;
        MinecartTrack track = MinecartTrack.at(cart, BlockPos.containing(target));
        if (track == null) track = MinecartTrack.at(cart, BlockPos.containing(target).below());
        if (track != null
                && (!motion.airborne || velocity.y <= 0)
                && track.point(track.offset(target)).distanceToSqr(target) < .01) {
            double distance = Math.min(2.25, velocity.horizontalDistance() * lead);
            int sign = track.tangent().dot(velocity) < 0 ? -1 : 1;
            double offset = track.offset(target);
            for (int step = 0; step < 4; step++) {
                double available = sign > 0 ? track.length() - offset : offset;
                if (distance <= available) return track.point(offset + sign * distance);
                Vec3 exit = track.point(sign > 0 ? track.length() : 0);
                distance -= available;
                var next = track.next(cart, sign);
                if (next == null)
                    return track.point(
                            sign > 0
                                    ? Math.max(0, track.length() - .0001)
                                    : Math.min(track.length(), .0001));
                track = next;
                offset = track.offset(exit);
                sign = offset < track.length() * .5 ? 1 : -1;
            }
            return target;
        }
        if (velocity.lengthSqr() > 16) return target;
        return target.add(velocity.scale(lead));
    }

    @Nullable
    public static Vec3 next(AbstractMinecart cart, Vec3 target, int steps) {
        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) return null;
        if (steps <= 0
                || !MinecartSpeedRules.managed(cart)
                || target.distanceToSqr(cart.position()) > 36) return null;
        MinecartTrack start = MinecartTrack.current(cart);
        if (start == null
                || start.point(start.offset(cart.position())).distanceToSqr(cart.position()) > 0.01)
            return null;
        BlockPos cell = BlockPos.containing(target);
        MinecartTrack end = MinecartTrack.at(cart, cell);
        if (end == null) end = MinecartTrack.at(cart, cell.below());
        if (end == null || end.point(end.offset(target)).distanceToSqr(target) > 0.01) return null;
        Path forward = path(cart, start, end, cart.position(), target, 1);
        Path reverse = path(cart, start, end, cart.position(), target, -1);
        Path path =
                forward == null
                        ? reverse
                        : reverse == null || forward.length <= reverse.length ? forward : reverse;
        if (path == null) return null;
        double remaining = smoothDistance(cart, path.length / steps, steps);
        for (Segment segment : path.segments) {
            double span = Math.abs(segment.to - segment.from);
            if (remaining <= span + 1.0E-7) {
                var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
                var first = path.segments.getFirst();
                MinecartBodyOrientation.followRail(
                        cart,
                        first.track.tangent().scale(first.sign),
                        segment.track.tangent().scale(segment.sign));
                motion.railPosition = segment.track.pos();
                motion.railSign = segment.sign;
                motion.travelHeading = segment.track.tangent().scale(segment.sign);
                return segment.track.point(segment.from + segment.sign * Math.min(remaining, span));
            }
            remaining -= span;
        }
        return target;
    }

    public static Vec3 fallback(AbstractMinecart cart, Vec3 target, int steps) {
        Vec3 start = cart.position();
        Vec3 delta = target.subtract(start);
        double fraction = 1.0 / Math.max(1, steps);
        if (delta.lengthSqr() > 4096) {
            ((MinecartMotionVisuals) cart).spelunkersCharm$motion().clientStepSpeed = Double.NaN;
            return start.lerp(target, fraction);
        }
        double horizontal = delta.horizontalDistance();
        if (horizontal > 1.0E-6)
            fraction =
                    Math.min(
                            fraction,
                            smoothDistance(cart, horizontal * fraction, steps) / horizontal);
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        double vertical = Math.abs(delta.y), verticalFraction = 1.0 / Math.max(1, steps);
        if (vertical > 1e-6) {
            double velocity =
                    Math.abs(
                            (motion.clientVelocitySeen
                                            ? motion.clientVelocity
                                            : cart.getDeltaMovement())
                                    .y);
            double packet =
                    cart.level().getGameTime() <= motion.clientVerticalUntil
                            ? motion.clientPacketVerticalSpeed
                            : 0;
            double previous =
                    Double.isFinite(motion.clientVerticalStep)
                            ? motion.clientVerticalStep
                            : velocity;
            double catchup =
                    Math.min(3.9, Math.sqrt(2 * Math.max(.02, cart.getGravity()) * vertical));
            double observed =
                    Math.max(Math.max(velocity, packet), Math.max(catchup, previous * .98));
            double limit = Math.max(.08, Math.min(observed * 1.1 + .04, previous + .08));
            verticalFraction = Math.min(verticalFraction, limit / vertical);
        }
        return start.add(delta.x * fraction, delta.y * verticalFraction, delta.z * fraction);
    }

    public static boolean freeRender(AbstractMinecart cart) {
        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) return true;
        var track = MinecartTrack.current(cart);
        return track != null
                && track.point(track.offset(cart.position())).distanceToSqr(cart.position()) > .01;
    }

    @Nullable
    public static Vec3 renderPosition(AbstractMinecart cart, Vec3 interpolated, double offset) {
        Vec3 from = cart.getPosition(0), delta = cart.position().subtract(from);
        double length = delta.lengthSqr();
        double fraction =
                length < 1e-10
                        ? 1
                        : Math.clamp(interpolated.subtract(from).dot(delta) / length, 0, 1);
        if (from.add(delta.scale(fraction)).distanceToSqr(interpolated) > 1e-8) return null;
        var frame = renderFrame(cart, fraction);
        return frame == null ? null : frame.sample(cart, offset);
    }

    public static Vec3 railVelocity(AbstractMinecart cart, Vec3 from, Vec3 target, double elapsed) {
        if (!MinecartSpeedRules.managed(cart)
                || ((MinecartMotionVisuals) cart).spelunkersCharm$airborne()
                || from.distanceToSqr(target) > 36) return Vec3.ZERO;
        var start = trackAt(cart, from);
        var end = trackAt(cart, target);
        if (start == null || end == null) return Vec3.ZERO;
        var forward = path(cart, start, end, from, target, 1);
        var reverse = path(cart, start, end, from, target, -1);
        var route =
                forward == null
                        ? reverse
                        : reverse == null || forward.length <= reverse.length ? forward : reverse;
        if (route == null) return Vec3.ZERO;
        var last = route.segments.getLast();
        return last.track.tangent().scale(last.sign * route.length / Math.max(1, elapsed));
    }

    @Nullable
    public static Vec3 renderAxis(AbstractMinecart cart, float partialTick, Vec3 frontAtEnd) {
        var frame = renderFrame(cart, partialTick);
        if (frame == null) return null;
        Vec3 axis = frame.sample(cart, .45).subtract(frame.sample(cart, -.45));
        if (axis.lengthSqr() < 1e-8) return null;
        return axis.normalize().scale(frontAtEnd.dot(frame.endHeading) < 0 ? -1 : 1);
    }

    @Nullable
    private static RenderFrame renderFrame(AbstractMinecart cart, double fraction) {
        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()) return null;
        Vec3 from = cart.getPosition(0), target = cart.position();
        var cached = RENDER_ROUTES.get(cart);
        Path route;
        if (cached != null
                && cached.tick == cart.tickCount
                && cached.from.equals(from)
                && cached.target.equals(target)) route = cached.path;
        else {
            route = renderRoute(cart, from, target);
            RENDER_ROUTES.put(cart, new RenderRoute(cart.tickCount, from, target, route));
        }
        if (route == null) return null;
        var last = route.segments.getLast();
        Vec3 endHeading = last.track.tangent().scale(last.sign);
        double remaining = route.length * Math.clamp(fraction, 0, 1);
        for (var part : route.segments) {
            double span = Math.abs(part.to - part.from);
            if (remaining <= span + 1e-8)
                return new RenderFrame(
                        part.track,
                        part.from + part.sign * Math.min(remaining, span),
                        part.sign,
                        endHeading);
            remaining -= span;
        }
        return new RenderFrame(last.track, last.to, last.sign, endHeading);
    }

    @Nullable
    private static Path renderRoute(AbstractMinecart cart, Vec3 from, Vec3 target) {
        var start = trackAt(cart, from);
        var end = trackAt(cart, target);
        if (start == null || end == null || from.distanceToSqr(target) > 4) return null;
        var forward = path(cart, start, end, from, target, 1);
        var reverse = path(cart, start, end, from, target, -1);
        return forward == null
                ? reverse
                : reverse == null || forward.length <= reverse.length ? forward : reverse;
    }

    /** Reuses one immutable rail route across the rendered frames of a cart tick. */
    private record RenderRoute(int tick, Vec3 from, Vec3 target, @Nullable Path path) {}

    /** Keeps the displayed position and wheel direction on the same connected rail path. */
    private record RenderFrame(MinecartTrack track, double offset, int sign, Vec3 endHeading) {
        Vec3 sample(AbstractMinecart cart, double distance) {
            int direction = distance < 0 ? -sign : sign;
            var next = MinecartTrack.advance(cart, track, offset, direction, Math.abs(distance));
            return next != null ? next.position() : track.point(direction > 0 ? track.length() : 0);
        }
    }

    private static double smoothDistance(AbstractMinecart cart, double requested, int steps) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        Vec3 velocity = motion.clientVelocitySeen ? motion.clientVelocity : cart.getDeltaMovement();
        double packetSpeed =
                cart.level().getGameTime() <= motion.clientPacketUntil
                        ? motion.clientPacketSpeed
                        : 0;
        double maximum =
                Math.max(
                        .65,
                        Math.max(
                                                velocity.horizontalDistance(),
                                                motion.clientPacketVelocity.horizontalDistance())
                                        * 1.3
                                + .10);
        if (steps <= 3) {
            double received = motion.clientPacketVelocity.horizontalDistance();
            double cap =
                    received > .005
                            ? received * 1.3 + .02
                            : velocity.horizontalDistance() * 1.05 + .02;
            if (received < .005 && velocity.horizontalDistanceSqr() < .0001)
                cap = Math.min(.65, Math.max(.08, requested));
            motion.clientStepSpeed = Math.min(requested, Math.min(maximum, Math.max(.04, cap)));
            return motion.clientStepSpeed;
        }
        double observedSpeed =
                Math.max(
                        Math.max(velocity.horizontalDistance(), packetSpeed),
                        Math.min(.5, requested));
        double previous =
                Double.isFinite(motion.clientStepSpeed)
                        ? motion.clientStepSpeed
                        : velocity.horizontalDistance();
        double limit = Math.max(observedSpeed * 1.10 + 0.02, previous);
        double accelerationLimit =
                steps <= 1 ? Math.max(observedSpeed, previous + .035) : previous + .035;
        double speed = Math.min(maximum, Math.min(requested, Math.min(limit, accelerationLimit)));
        motion.clientStepSpeed = speed;
        return speed;
    }

    @Nullable
    public static Vec3 betweenSnapshots(
            AbstractMinecart cart, Vec3 from, Vec3 target, double fraction) {
        if (from.distanceToSqr(target) < 1e-10) return from;
        var first = trackAt(cart, from);
        var last = trackAt(cart, target);
        if (first == null || last == null) return null;
        var forward = path(cart, first, last, from, target, 1);
        var reverse = path(cart, first, last, from, target, -1);
        var route =
                forward == null
                        ? reverse
                        : reverse == null || forward.length <= reverse.length ? forward : reverse;
        if (route == null) return null;
        double remaining = route.length * fraction;
        for (var part : route.segments) {
            double span = Math.abs(part.to - part.from);
            if (remaining <= span + 1e-8)
                return part.track.point(part.from + part.sign * Math.min(remaining, span));
            remaining -= span;
        }
        return target;
    }

    @Nullable
    private static MinecartTrack trackAt(AbstractMinecart cart, Vec3 position) {
        var cell = BlockPos.containing(position);
        var track = MinecartTrack.at(cart, cell);
        if (track == null) track = MinecartTrack.at(cart, cell.below());
        return track != null && track.point(track.offset(position)).distanceToSqr(position) < .0025
                ? track
                : null;
    }

    @Nullable
    private static Path path(
            AbstractMinecart cart,
            MinecartTrack start,
            MinecartTrack end,
            Vec3 from,
            Vec3 target,
            int sign) {
        List<Segment> segments = new ArrayList<>();
        MinecartTrack track = start;
        double offset = start.offset(from);
        double length = 0;
        for (int guard = 0; guard < 12 && length < 6; guard++) {
            boolean last = track.pos().equals(end.pos());
            double finish = last ? end.offset(target) : sign > 0 ? track.length() : 0;
            if (last && (finish - offset) * sign < -1.0E-6) return null;
            segments.add(new Segment(track, sign, offset, finish));
            length += Math.abs(finish - offset);
            if (last) return new Path(segments, length);
            Vec3 exit = track.point(finish);
            track = track.next(cart, sign);
            if (track == null || track.pos().equals(start.pos())) return null;
            offset = track.offset(exit);
            sign = offset < track.length() * 0.5 ? 1 : -1;
        }
        return null;
    }

    /** Stores one step of a client rail path. */
    private record Segment(MinecartTrack track, int sign, double from, double to) {}

    /** Stores the client rail segments leading to a target. */
    private record Path(List<Segment> segments, double length) {}
}
