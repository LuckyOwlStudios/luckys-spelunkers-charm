// Train movement smoothing.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class MinecartTrainInterpolation {
    private MinecartTrainInterpolation() {}

    public static void accept(Level level, MinecartTrainPayload frame) {
        long now = level.getGameTime();
        for (var entry : frame.carts())
            if (level.getEntity(entry.id()) instanceof AbstractMinecart cart) {
                var state = state(cart);
                var samples = state.clientTrainSamples;
                if (!samples.isEmpty() && samples.getLast().tick() >= frame.tick()) continue;
                samples.addLast(new Sample(frame.tick(), entry.position(), entry.axis()));
                while (samples.size() > 24) samples.removeFirst();
                state.clientTrainReceived = now;
            }
    }

    public static void apply(List<AbstractMinecart> carts) {
        var visited = Collections.newSetFromMap(new IdentityHashMap<AbstractMinecart, Boolean>());
        for (var start : carts) {
            if (!visited.add(start)) continue;
            var group = new ArrayList<AbstractMinecart>();
            var queue = new ArrayDeque<AbstractMinecart>();
            queue.add(start);
            long now = start.level().getGameTime();
            while (!queue.isEmpty()) {
                var cart = queue.removeFirst();
                group.add(cart);
                var links = (MinecartCouplingVisuals) cart;
                for (int id :
                        new int[] {
                            links.spelunkersCharm$firstCouplingId(),
                            links.spelunkersCharm$secondCouplingId()
                        })
                    if (cart.level().getEntity(id) instanceof AbstractMinecart peer
                            && MinecartSpeedRules.managed(peer)
                            && visited.add(peer)) queue.addLast(peer);
            }
            if (group.size() < 2) continue;
            long common = Long.MAX_VALUE, entered = Long.MIN_VALUE;
            boolean ready = true;
            for (var cart : group) {
                var state = state(cart);
                if (state.clientTrainSamples.isEmpty() || now - state.clientTrainReceived > 5) {
                    ready = false;
                    break;
                }
                common = Math.min(common, (long) state.clientTrainSamples.getLast().tick());
                if (state.clientTrainTick < now - 1) {
                    state.clientTrainEntered = now;
                    state.clientTrainTime = Double.NaN;
                }
                entered = Math.max(entered, state.clientTrainEntered);
            }
            if (!ready) continue;
            group.sort(Comparator.comparingInt(AbstractMinecart::getId));
            var clock = state(group.getFirst());
            double time =
                    Double.isFinite(clock.clientTrainTime)
                            ? Math.min(
                                    common,
                                    clock.clientTrainTime
                                            + Math.max(1, now - clock.clientTrainTick)
                                                    * Math.clamp(
                                                            1
                                                                    + (common
                                                                                    - 1
                                                                                    - clock.clientTrainTime)
                                                                            * .05,
                                                            .85,
                                                            1.1))
                            : common - 1;
            double blend = Math.clamp((now - entered + 1) / 5.0, 0, 1);
            for (var cart : group) {
                var state = state(cart);
                Vec3 previous =
                        state.clientTrainTick == now - 1 && state.clientTrainPosition != null
                                ? state.clientTrainPosition
                                : cart.position();
                var target = sample(cart, state.clientTrainSamples, time);
                Vec3 displayed = previous.lerp(target.position(), blend);
                if (state.clientTrainHandoff) {
                    double elapsed = Math.max(1, now - state.clientTrainTick);
                    double limit =
                            target.position()
                                            .distanceTo(
                                                    sample(
                                                                    cart,
                                                                    state.clientTrainSamples,
                                                                    time - elapsed)
                                                            .position())
                                    + .2 * elapsed;
                    double distance = previous.distanceTo(displayed);
                    if (distance > limit) {
                        double fraction = limit / distance;
                        Vec3 rail =
                                MinecartInterpolation.betweenSnapshots(
                                        cart, previous, displayed, fraction);
                        displayed = rail != null ? rail : previous.lerp(displayed, fraction);
                    }
                    if (displayed.distanceToSqr(target.position()) < 1e-6)
                        state.clientTrainHandoff = false;
                }
                cart.setPos(displayed);
                cart.xOld = cart.xo = previous.x;
                cart.yOld = cart.yo = previous.y;
                cart.zOld = cart.zo = previous.z;
                state.clientVerticalStep = Math.abs(displayed.y - previous.y);
                Vec3 priorAxis = state.clientTrainAxis;
                float before =
                        priorAxis != null && priorAxis.horizontalDistanceSqr() > .0004
                                ? (float) Math.toDegrees(Math.atan2(priorAxis.z, priorAxis.x))
                                : state.visualYaw;
                float facing =
                        target.axis().horizontalDistanceSqr() > .0004
                                ? (float)
                                        Math.toDegrees(Math.atan2(target.axis().z, target.axis().x))
                                : before;
                state.visualYawOld = before;
                state.visualYaw = before + net.minecraft.util.Mth.wrapDegrees(facing - before);
                state.visualYawReady = true;
                state.visualYawTick = now;
                state.facingPosition = displayed;
                state.clientTrainAxis = target.axis();
                state.clientTrainPosition = displayed;
                state.clientTrainTime = time;
                state.clientTrainTick = now;
                for (var passenger : cart.getPassengers()) cart.positionRider(passenger);
            }
        }
    }

    private static Sample sample(AbstractMinecart cart, Deque<Sample> samples, double time) {
        Sample previous = samples.getFirst();
        if (time <= previous.tick()) return previous;
        for (var next : samples) {
            if (next.tick() >= time && next.tick() > previous.tick()) {
                double fraction =
                        Math.clamp(
                                (time - previous.tick()) / (next.tick() - previous.tick()), 0, 1);
                Vec3 rail =
                        MinecartInterpolation.betweenSnapshots(
                                cart, previous.position(), next.position(), fraction);
                return new Sample(
                        time,
                        rail != null ? rail : previous.position().lerp(next.position(), fraction),
                        previous.axis().lerp(next.axis(), fraction).normalize());
            }
            previous = next;
        }
        return samples.getLast();
    }

    private static MinecartMotionState state(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
    }

    /** Stores one cart's position and fixed body axis at a server tick. */
    public record Sample(double tick, Vec3 position, Vec3 axis) {}
}
