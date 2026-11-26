// Shared train flight path.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class MinecartFlightGuide {
    private static final double SPACING = 1.55;
    private static final Map<ServerLevel, Map<UUID, Trail>> PATHS = new IdentityHashMap<>();

    private MinecartFlightGuide() {}

    public static void clear(ServerLevel level) {
        PATHS.remove(level);
    }

    public static void coast(ServerLevel level) {
        var paths = PATHS.get(level);
        if (paths == null) return;
        var links = MinecartCouplings.get(level);
        paths.entrySet()
                .removeIf(
                        entry -> {
                            var trail = entry.getValue();
                            if (level.getGameTime() - trail.seen > 40
                                    || !(level.getEntity(entry.getKey())
                                            instanceof AbstractMinecart head)
                                    || !head.isAlive()) return true;
                            boolean joined = true;
                            AbstractMinecart previous = null;
                            for (UUID id : trail.ids) {
                                if (!(level.getEntity(id) instanceof AbstractMinecart cart)
                                        || !cart.isAlive()) return true;
                                if (previous != null) {
                                    if (!links.linked(previous.getUUID(), id)) return true;
                                    joined &=
                                            MinecartTrainBrakes.onRail(previous)
                                                    && MinecartTrainBrakes.onRail(cart)
                                                    && MinecartRailPath.between(previous, cart)
                                                            != null;
                                }
                                previous = cart;
                            }
                            if (joined) return true;
                            trail.append(head.position(), trail.ids.size() * 3.2 + 16);
                            return false;
                        });
        if (paths.isEmpty()) PATHS.remove(level);
    }

    public static void replace(AbstractMinecart oldCart, AbstractMinecart replacement) {
        if (!(oldCart.level() instanceof ServerLevel level)) return;
        var paths = PATHS.get(level);
        if (paths == null) return;
        for (var trail : paths.values())
            for (int i = 0; i < trail.ids.size(); i++)
                if (trail.ids.get(i).equals(oldCart.getUUID())) {
                    trail.ids.set(i, replacement.getUUID());
                    if (trail.entered.remove(oldCart.getUUID()))
                        trail.entered.add(replacement.getUUID());
                }
        var leading = paths.remove(oldCart.getUUID());
        if (leading != null) paths.put(replacement.getUUID(), leading);
    }

    static List<Guide> prepare(
            ServerLevel level,
            Collection<MinecartFlightPhysics.Body> bodies,
            MinecartCouplings links) {
        var paths = PATHS.computeIfAbsent(level, ignored -> new HashMap<>());
        paths.entrySet().removeIf(e -> level.getGameTime() - e.getValue().seen > 40);
        var present = new HashMap<UUID, MinecartFlightPhysics.Body>();
        for (var b : bodies) present.put(b.cart.getUUID(), b);
        var visited = new HashSet<UUID>();
        var guides = new ArrayList<Guide>();
        for (var start : bodies) {
            if (!visited.add(start.cart.getUUID())) continue;
            var members = new ArrayList<MinecartFlightPhysics.Body>();
            var queue = new ArrayDeque<MinecartFlightPhysics.Body>();
            queue.add(start);
            boolean anchored = false, free = false;
            while (!queue.isEmpty()) {
                var b = queue.removeFirst();
                members.add(b);
                free |= !b.rail;
                anchored |= b.rail && MinecartHandbrake.anchored(b.cart);
                for (UUID id : links.neighbors(b.cart.getUUID()))
                    if (present.containsKey(id) && visited.add(id)) queue.addLast(present.get(id));
            }
            if (members.size() < 2
                    || anchored
                    || members.stream().allMatch(b -> !b.rail && b.cart.onGround())) {
                for (var b : members) paths.remove(b.cart.getUUID());
                continue;
            }
            if (!free) continue;
            var endpoints = new ArrayList<MinecartFlightPhysics.Body>();
            for (var b : members)
                if (links.neighbors(b.cart.getUUID()).stream().filter(present::containsKey).count()
                        == 1) endpoints.add(b);
            if (endpoints.size() != 2) continue;
            var head = endpoints.getFirst();
            for (var b : endpoints)
                if (paths.containsKey(b.cart.getUUID())) {
                    head = b;
                    break;
                }
            if (!paths.containsKey(head.cart.getUUID())
                    && score(endpoints.get(1), present, links) > score(head, present, links))
                head = endpoints.get(1);
            if (!paths.containsKey(head.cart.getUUID())
                    && !((MinecartMotionVisuals) head.cart).spelunkersCharm$motion().flightLaunched)
                continue;
            var order = new ArrayList<MinecartFlightPhysics.Body>();
            MinecartFlightPhysics.Body last = null, current = head;
            while (current != null && order.size() < members.size()) {
                order.add(current);
                MinecartFlightPhysics.Body next = null;
                for (UUID id : links.neighbors(current.cart.getUUID()))
                    if (present.containsKey(id) && present.get(id) != last) {
                        next = present.get(id);
                        break;
                    }
                last = current;
                current = next;
            }
            if (order.size() != members.size()) continue;
            var trail = paths.get(head.cart.getUUID());
            if (trail == null || !trail.matches(order)) {
                trail = new Trail(order);
                paths.put(head.cart.getUUID(), trail);
            }
            trail.seen = level.getGameTime();
            guides.add(new Guide(order, trail));
        }
        paths.entrySet().removeIf(e -> level.getGameTime() - e.getValue().seen > 40);
        return guides;
    }

    private static double score(
            MinecartFlightPhysics.Body body,
            Map<UUID, MinecartFlightPhysics.Body> present,
            MinecartCouplings links) {
        for (UUID id : links.neighbors(body.cart.getUUID()))
            if (present.containsKey(id)) {
                var peer = present.get(id).cart;
                var path = MinecartRailPath.between(body.cart, peer);
                Vec3 toward =
                        path == null
                                ? peer.position().subtract(body.p).normalize()
                                : path.firstToward();
                Vec3 velocity =
                        body.v.lengthSqr() > 1e-6
                                ? body.v
                                : ((MinecartMotionVisuals) body.cart)
                                        .spelunkersCharm$motion()
                                        .travelHeading;
                return -velocity.dot(toward);
            }
        return 0;
    }

    /** Advances followers behind a persistent leading cart without feeding impulses back into it. */
    static final class Guide {
        final List<MinecartFlightPhysics.Body> order;
        final Trail trail;
        final double[] coordinates;
        final int[] fronts;
        boolean active = true;

        Guide(List<MinecartFlightPhysics.Body> order, Trail trail) {
            this.order = order;
            this.trail = trail;
            coordinates = new double[order.size()];
            fronts = new int[order.size()];
            for (int i = 0; i < order.size(); i++) {
                var b = order.get(i);
                b.guide = this;
                b.guideIndex = i;
                fronts[i] =
                        i == 0
                                ? -MinecartHingeGeometry.side(b.cart, order.get(1).cart)
                                : MinecartHingeGeometry.side(b.cart, order.get(i - 1).cart);
            }
        }

        void predict(double time) {
            var head = order.getFirst();
            double progress = head.p.distanceTo(trail.points.getLast().p);
            double courseSpeed =
                    head.rail
                            ? head.v.horizontalDistance()
                                    / Math.max(.5, head.axis().horizontalDistance())
                            : head.v.length();
            double anticipated =
                    head.rail
                            ? courseSpeed
                            : new Vec3(
                                            head.v.x * Math.sqrt(MinecartFlightPhysics.AIR_DRAG),
                                            Math.max(
                                                    -MinecartFlightPhysics.TERMINAL,
                                                    head.v.y - head.cart.getGravity() * .5),
                                            head.v.z * Math.sqrt(MinecartFlightPhysics.AIR_DRAG))
                                    .length();
            active = progress > .00001 || !head.rail && !head.cart.onGround();
            if (head.cart.horizontalCollision && !MinecartRailCapture.nearLanding(head.cart))
                active = false;
            for (var b : order) {
                var motion = ((MinecartMotionVisuals) b.cart).spelunkersCharm$motion();
                motion.flightArcSpeed = anticipated;
                motion.flightSpeedUntil = b.cart.level().getGameTime() + 1;
                motion.flightFollower = b != head;
            }
            trail.append(head.p, order.size() * 3.2 + 16);
            coordinates[0] = trail.total;
            for (int i = 1; i < order.size(); i++) {
                var b = order.get(i);
                var parent = order.get(i - 1);
                if (!active || b.rail || !b.active || time <= b.start) {
                    b.predict(time);
                    coordinates[i] =
                            trail.project(b.p, coordinates[i - 1] - 3.2, coordinates[i - 1] - .8);
                    continue;
                }
                if (trail.entered.add(b.cart.getUUID())) {
                    double at =
                            trail.project(b.p, coordinates[i - 1] - 3.2, coordinates[i - 1] - .8);
                    double duration = Math.max(0, Math.min(.1, time - b.start));
                    trail.gaps[i] =
                            Math.clamp(
                                    coordinates[i - 1] - at - progress * duration / .1, 1.3, 2.4);
                }
                if (!parent.rail)
                    trail.gaps[i] +=
                            Math.clamp(
                                    SPACING - trail.gaps[i],
                                    -.012 / order.size(),
                                    .012 / order.size());
                double coordinate = coordinates[i - 1] - trail.gaps[i];
                Vec3 target = trail.sample(coordinate);
                while (parent.p.distanceToSqr(target) < 1.15 * 1.15 && trail.gaps[i] < 3.1) {
                    trail.gaps[i] += .04;
                    coordinate = coordinates[i - 1] - trail.gaps[i];
                    target = trail.sample(coordinate);
                }
                coordinates[i] = coordinate;
                Vec3 direction =
                        trail.sample(coordinate + .5)
                                .subtract(trail.sample(coordinate - .5))
                                .normalize();
                b.follow(time, target, direction.scale(fronts[i]), direction, courseSpeed);
                if (!b.rail) limitBend(parent, b, fronts[i - 1] * fronts[i]);
            }
        }

        private void limitBend(
                MinecartFlightPhysics.Body parent, MinecartFlightPhysics.Body child, int sign) {
            Vec3 axis = parent.axis().scale(sign), current = child.axis();
            double dot = Math.clamp(axis.dot(current), -1, 1);
            if (dot >= Math.cos(MinecartFlightPhysics.MAX_BEND)) return;
            Vec3 across = current.subtract(axis.scale(dot)).normalize();
            if (across.lengthSqr() < 1e-8) across = new Vec3(-axis.y, axis.x, 0).normalize();
            Vec3 wanted =
                    axis.scale(Math.cos(MinecartFlightPhysics.MAX_BEND))
                            .add(across.scale(Math.sin(MinecartFlightPhysics.MAX_BEND)));
            if (wanted.horizontalDistanceSqr() > 1e-8) child.yaw = Math.atan2(wanted.z, wanted.x);
            child.pitch = Math.atan2(wanted.y, wanted.horizontalDistance());
        }
    }

    /** Stores a bounded arc-length history, the fixed train order and small coupling slack. */
    private static final class Trail {
        final List<UUID> ids = new ArrayList<>();
        final List<Sample> points = new ArrayList<>();
        final Set<UUID> entered = new HashSet<>();
        final double[] gaps;
        double total;
        long seen;

        Trail(List<MinecartFlightPhysics.Body> order) {
            gaps = new double[order.size()];
            Arrays.fill(gaps, SPACING);
            for (var b : order) {
                ids.add(b.cart.getUUID());
                if (b.rail)
                    ((MinecartMotionVisuals) b.cart).spelunkersCharm$motion().flightCaptured =
                            false;
            }
            double s = 0;
            for (int i = order.size() - 1; i >= 0; i--) {
                Vec3 p = order.get(i).p;
                if (i < order.size() - 1) {
                    var source = order.get(i + 1).cart;
                    var track = MinecartTrack.current(source);
                    if (track != null && order.get(i + 1).rail) {
                        double offset = track.offset(source.position());
                        Vec3 heading =
                                source.getDeltaMovement().horizontalDistanceSqr() > 1e-6
                                        ? source.getDeltaMovement()
                                        : p.subtract(source.position());
                        int sign = track.tangent().dot(heading) < 0 ? -1 : 1;
                        for (int step = 0; step < 80; step++) {
                            var next = MinecartTrack.advance(source, track, offset, sign, .1);
                            if (next == null || next.position().distanceToSqr(p) < .1 * .1) break;
                            Vec3 point = next.position();
                            s += point.distanceTo(points.getLast().p);
                            points.add(new Sample(point, s));
                            track = next.track();
                            offset = track.offset(point);
                            sign = next.sign();
                        }
                    }
                }
                if (!points.isEmpty()) s += p.distanceTo(points.getLast().p);
                points.add(new Sample(p, s));
            }
            total = s;
        }

        boolean matches(List<MinecartFlightPhysics.Body> order) {
            if (order.size() != ids.size()) return false;
            for (int i = 0; i < ids.size(); i++)
                if (!ids.get(i).equals(order.get(i).cart.getUUID())) return false;
            return true;
        }

        void append(Vec3 p, double length) {
            double distance = p.distanceTo(points.getLast().p);
            if (distance > 1e-8) {
                total += distance;
                points.add(new Sample(p, total));
            }
            int remove = 0;
            while (remove + 2 < points.size() && points.get(remove + 1).s < total - length)
                remove++;
            if (remove > 0) points.subList(0, remove).clear();
        }

        int index(double s) {
            int low = 0, high = points.size() - 1;
            while (low < high) {
                int mid = (low + high) / 2;
                if (points.get(mid).s < s) low = mid + 1;
                else high = mid;
            }
            return low;
        }

        Vec3 sample(double s) {
            int i = index(s);
            if (i == 0) return points.getFirst().p;
            var a = points.get(i - 1);
            var b = points.get(i);
            return a.p.lerp(b.p, Math.clamp((s - a.s) / Math.max(1e-8, b.s - a.s), 0, 1));
        }

        double project(Vec3 p, double low, double high) {
            double closest = Math.max(points.getFirst().s, low),
                    distance = Double.POSITIVE_INFINITY;
            for (int i = Math.max(1, index(low));
                    i < points.size() && points.get(i - 1).s <= high;
                    i++) {
                var a = points.get(i - 1);
                var b = points.get(i);
                Vec3 line = b.p.subtract(a.p);
                double t =
                        Math.clamp(
                                p.subtract(a.p).dot(line) / Math.max(1e-8, line.lengthSqr()), 0, 1);
                double at =
                        Math.clamp(a.s + (b.s - a.s) * t, Math.max(low, a.s), Math.min(high, b.s));
                Vec3 point = sample(at);
                double error = p.distanceToSqr(point);
                if (error < distance) {
                    distance = error;
                    closest = at;
                }
            }
            return closest;
        }
    }

    /** Associates a point on the shared curve with its travelled distance. */
    private record Sample(Vec3 p, double s) {}
}
