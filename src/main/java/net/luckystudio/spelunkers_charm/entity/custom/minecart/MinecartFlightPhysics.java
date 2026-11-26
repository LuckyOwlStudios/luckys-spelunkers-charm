// Minecart flight physics.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class MinecartFlightPhysics {
    public static final double STEP = .1,
            AIR_DRAG = .997,
            TERMINAL = 3.9,
            MAX_BEND = Math.toRadians(55);
    private static final Map<ServerLevel, IdentityHashMap<AbstractMinecart, Double>> PENDING =
            new IdentityHashMap<>();
    private static final Map<ServerLevel, Map<Edge, Memory>> JOINTS = new IdentityHashMap<>();

    private MinecartFlightPhysics() {}

    public static void defer(AbstractMinecart cart, double duration) {
        if (!(cart.level() instanceof ServerLevel level) || duration <= 0) return;
        PENDING.computeIfAbsent(level, k -> new IdentityHashMap<>())
                .merge(cart, duration, (a, b) -> Math.min(1, a + b));
    }

    public static void launch(AbstractMinecart cart) {
        launch(cart, MinecartHingeGeometry.supportedAxis(cart));
    }

    public static void launch(AbstractMinecart cart, Vec3 axis) {
        var s = state(cart);
        axis = axis.normalize();
        s.physicalYaw = Math.atan2(axis.z, axis.x);
        s.physicalPitch = Math.asin(Math.clamp(axis.y, -1, 1));
        s.pitchRate = s.yawRate = 0;
        s.flightRotationReady = true;
        s.flightLaunched = true;
        s.flightCaptured = false;
        s.flightReferenceYaw = s.physicalYaw;
        s.flightForwardSign =
                cart.getDeltaMovement().dot(MinecartHingeGeometry.axis(s.physicalYaw, 0)) < 0
                        ? -1
                        : 1;
        s.flightRailSpeed = cart.getDeltaMovement().horizontalDistance();
        cart.setOnGround(false);
        sync(cart);
    }

    public static void clear(ServerLevel level) {
        PENDING.remove(level);
        JOINTS.remove(level);
        MinecartFlightGuide.clear(level);
    }

    public static void replace(AbstractMinecart oldCart, AbstractMinecart replacement) {
        MinecartFlightGuide.replace(oldCart, replacement);
        if (!(oldCart.level() instanceof ServerLevel level)) return;
        var pending = PENDING.get(level);
        if (pending != null && pending.containsKey(oldCart))
            pending.put(replacement, pending.remove(oldCart));
        var memory = JOINTS.get(level);
        for (var id : MinecartCouplings.get(level).neighbors(replacement.getUUID())) {
            if (memory != null) {
                var joint = memory.remove(Edge.of(oldCart.getUUID(), id));
                if (joint != null) memory.put(Edge.of(replacement.getUUID(), id), joint);
            }
            if (level.getEntity(id) instanceof AbstractMinecart peer) {
                var sides = state(peer).hingeSides;
                Integer side = sides.remove(oldCart.getUUID());
                if (side != null) sides.put(replacement.getUUID(), side);
            }
        }
    }

    public static Vec3 axis(AbstractMinecart cart) {
        var s = state(cart);
        return s.flightRotationReady
                ? MinecartHingeGeometry.axis(s.physicalYaw, s.physicalPitch)
                : MinecartHingeGeometry.supportedAxis(cart);
    }

    private static Vec3 initialAxis(AbstractMinecart cart) {
        Vec3 sum = Vec3.ZERO;
        if (cart.level() instanceof ServerLevel level)
            for (var id : MinecartCouplings.get(level).neighbors(cart.getUUID()))
                if (level.getEntity(id) instanceof AbstractMinecart peer) {
                    Vec3 delta = peer.position().subtract(cart.position());
                    if (delta.lengthSqr() > 1e-6)
                        sum =
                                sum.add(
                                        delta.normalize()
                                                .scale(MinecartHingeGeometry.side(cart, peer)));
                }
        return sum.lengthSqr() > 1e-6 ? sum.normalize() : MinecartHingeGeometry.supportedAxis(cart);
    }

    public static void step(
            ServerLevel level, List<AbstractMinecart> ticked, MinecartCouplings links) {
        var pending = PENDING.remove(level);
        if (pending == null || pending.isEmpty()) {
            MinecartFlightGuide.coast(level);
            return;
        }
        var active = Collections.newSetFromMap(new IdentityHashMap<AbstractMinecart, Boolean>());
        active.addAll(ticked);
        var bodies = new IdentityHashMap<AbstractMinecart, Body>();
        var queue = new ArrayDeque<AbstractMinecart>();
        for (var cart : pending.keySet())
            if (cart.isAlive() && MinecartSpeedRules.managed(cart)) {
                bodies.put(cart, new Body(cart, pending.get(cart), true));
                queue.add(cart);
            }
        while (!queue.isEmpty() && bodies.size() < 512) {
            var cart = queue.removeFirst();
            for (var id : links.neighbors(cart.getUUID()))
                if (level.getEntity(id) instanceof AbstractMinecart peer
                        && peer.isAlive()
                        && MinecartSpeedRules.managed(peer)
                        && !bodies.containsKey(peer)) {
                    bodies.put(peer, new Body(peer, 1, active.contains(peer)));
                    queue.addLast(peer);
                }
        }
        var memory = JOINTS.computeIfAbsent(level, k -> new HashMap<>());
        var joints = new ArrayList<Joint>();
        var damped = Collections.newSetFromMap(new IdentityHashMap<AbstractMinecart, Boolean>());
        for (var body : bodies.values())
            if (body.rail && MinecartHandbrake.anchored(body.cart)) {
                var settling = new ArrayDeque<AbstractMinecart>();
                settling.add(body.cart);
                damped.add(body.cart);
                while (!settling.isEmpty())
                    for (var id : links.neighbors(settling.removeFirst().getUUID()))
                        if (level.getEntity(id) instanceof AbstractMinecart peer
                                && bodies.containsKey(peer)
                                && damped.add(peer)) settling.addLast(peer);
            }
        for (var cart : damped) bodies.get(cart).damped = true;
        for (var entry : bodies.entrySet()) {
            var cart = entry.getKey();
            links.refresh(level, cart.getUUID());
            MinecartHingeGeometry.sync(cart);
            for (var id : links.neighbors(cart.getUUID()))
                if (level.getEntity(id) instanceof AbstractMinecart peer
                        && cart.getId() < peer.getId()
                        && bodies.containsKey(peer)
                        && cart.distanceToSqr(peer) < 36) {
                    var edge = Edge.of(cart.getUUID(), id);
                    var stored = memory.computeIfAbsent(edge, k -> new Memory());
                    stored.seen = level.getGameTime();
                    joints.add(
                            new Joint(
                                    entry.getValue(),
                                    bodies.get(peer),
                                    MinecartHingeGeometry.side(cart, peer),
                                    MinecartHingeGeometry.side(peer, cart),
                                    stored));
                }
        }
        memory.entrySet().removeIf(e -> level.getGameTime() - e.getValue().seen > 20);
        joints.sort(
                Comparator.comparingInt((Joint j) -> j.a.cart.getId())
                        .thenComparingInt(j -> j.b.cart.getId()));
        var list = new ArrayList<>(bodies.values());
        list.sort(Comparator.comparingInt(b -> b.cart.getId()));
        var guides = MinecartFlightGuide.prepare(level, list, links);
        for (int substep = 0; substep < 10; substep++) {
            double time = (substep + 1) * STEP;
            for (var joint : joints) joint.limitAnchoredContact();
            for (var b : list) if (b.guide == null || b.guideIndex == 0) b.predict(time);
            for (var guide : guides) guide.predict(time);
            for (var joint : joints) joint.begin();
            int iterations = Math.min(32, Math.max(12, joints.size() + 4));
            boolean guided =
                    guides.size() == 1
                            && guides.getFirst().order.size() == list.size()
                            && guides.getFirst().active;
            for (int pass = 0; !guided && pass < iterations; pass++) {
                for (int i = 0; i < joints.size(); i++) {
                    var joint = joints.get(pass % 2 == 0 ? i : joints.size() - 1 - i);
                    joint.solve();
                }
                if (pass % 4 == 3)
                    for (int a = 0; a < list.size(); a++)
                        for (int b = a + 1; b < list.size(); b++)
                            separate(list.get(a), list.get(b));
            }
            for (var b : list) b.commit(time);
            for (var joint : joints) joint.dampStretch();
            for (int pass = 0; pass < 3; pass++) for (var joint : joints) joint.resolveContact();
        }
        for (var b : list) {
            if (!b.cart.isAlive()) continue;
            var s = state(b.cart);
            s.physicalYaw = b.yaw;
            s.physicalPitch = b.pitch;
            s.yawRate = b.yawRate;
            s.pitchRate = b.pitchRate;
            s.flightRotationReady = true;
            s.freeTicks = b.rail ? 0 : s.freeTicks + 1;
            s.railPosition = b.rail ? b.cart.getCurrentRailPosition() : null;
            var visual = (MinecartMotionVisuals) b.cart;
            visual.spelunkersCharm$setOnRails(b.rail);
            visual.spelunkersCharm$setAirborne(!b.rail && !b.cart.onGround());
            s.flightPitch = (float) Math.toDegrees(b.pitch);
            b.cart.setXRot(s.flightPitch);
            sync(b.cart);
            b.cart.hasImpulse = true;
            for (var rider : b.cart.getPassengers()) b.cart.positionRider(rider);
        }
    }

    private static void sync(AbstractMinecart cart) {
        var s = state(cart);
        ((MinecartMotionVisuals) cart)
                .spelunkersCharm$setFlightPose(
                        (float) Math.toDegrees(s.physicalYaw),
                        (float) Math.toDegrees(s.physicalPitch));
        if (cart.onGround() && !MinecartTrainBrakes.onRail(cart)) {
            s.visualYaw = (float) Math.toDegrees(s.physicalYaw);
            s.visualYawReady = true;
            ((MinecartMotionVisuals) cart).spelunkersCharm$setBodyYaw(s.visualYaw);
            if (cart instanceof LocomotiveMinecart engine) engine.landFacing(s.visualYaw);
        }
    }

    private static MinecartMotionState state(AbstractMinecart cart) {
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
    }

    private static Vec3 bounded(Vec3 v, double limit) {
        double length = v.length();
        return length > limit ? v.scale(limit / length) : v;
    }

    private static double angle(double v) {
        double angle = Math.atan2(Math.sin(v), Math.cos(v));
        return Math.abs(angle) < 1e-10 ? 0 : angle;
    }

    private static void separate(Body a, Body b) {
        if (a.guide != null && a.guide == b.guide && a.guide.active) return;
        double weight = a.weight + b.weight;
        if (weight == 0 || a.attached.contains(b) || a.p.distanceToSqr(b.p) > 3.24) return;
        Vec3 aa = a.axis().scale(.45),
                bb = b.axis().scale(.45),
                startA = a.p.add(0, .375, 0).subtract(aa),
                startB = b.p.add(0, .375, 0).subtract(bb);
        Vec3 da = aa.scale(2), db = bb.scale(2), r = startA.subtract(startB);
        double av = da.lengthSqr(),
                bv = db.lengthSqr(),
                ab = da.dot(db),
                ar = da.dot(r),
                br = db.dot(r),
                denominator = av * bv - ab * ab;
        double s = denominator > 1e-9 ? Math.clamp((ab * br - ar * bv) / denominator, 0, 1) : 0;
        double t = Math.clamp((ab * s + br) / bv, 0, 1);
        s = Math.clamp((ab * t - ar) / av, 0, 1);
        Vec3 difference = startB.add(db.scale(t)).subtract(startA.add(da.scale(s)));
        double distance = difference.length();
        if (distance >= .48) return;
        Vec3 normal =
                distance > .01 ? difference.scale(1 / distance) : b.p.subtract(a.p).normalize();
        if (normal.lengthSqr() < 1e-8) normal = MinecartHingeGeometry.up(a.yaw, a.pitch);
        Vec3 change = normal.scale(Math.min(.08, (.48 - distance) * .6) / weight);
        a.translate(change.scale(-a.weight));
        b.translate(change.scale(b.weight));
    }

    /** Holds one cart's translation and two allowed rotation axes during a physics tick. */
    static final class Body {
        final AbstractMinecart cart;
        double start;
        final boolean active;
        final Set<Body> attached = Collections.newSetFromMap(new IdentityHashMap<>());
        MinecartFlightGuide.Guide guide;
        int guideIndex;
        Vec3 p, v, before, predicted, railStart, railEnd, railShift = Vec3.ZERO;
        double yaw,
                pitch,
                yawRate,
                pitchRate,
                beforeYaw,
                beforePitch,
                dt,
                weight,
                inertia,
                railTime;
        boolean rail, groundContact, damped, capturedRail;

        Body(AbstractMinecart cart, double duration, boolean active) {
            this.cart = cart;
            this.active = active;
            start = 1 - duration;
            p = cart.position();
            v = cart.getDeltaMovement();
            var s = state(cart);
            rail = MinecartTrainBrakes.onRail(cart);
            Vec3 axis =
                    rail
                            ? MinecartHingeGeometry.supportedAxis(cart)
                            : s.flightRotationReady
                                    ? MinecartFlightPhysics.axis(cart)
                                    : initialAxis(cart);
            yaw = s.flightRotationReady && !rail ? s.physicalYaw : Math.atan2(axis.z, axis.x);
            pitch =
                    s.flightRotationReady && !rail
                            ? s.physicalPitch
                            : Math.asin(Math.clamp(axis.y, -1, 1));
            if (!rail && !Double.isFinite(s.flightReferenceYaw))
                s.flightReferenceYaw = s.visualYawReady ? Math.toRadians(s.visualYaw) : yaw;
            yawRate = s.yawRate;
            pitchRate = s.pitchRate;
            if (rail) {
                railEnd = p;
                railStart = new Vec3(cart.xo, cart.yo, cart.zo);
                if (railStart.distanceToSqr(railEnd) > 4) railStart = railEnd;
            }
        }

        Vec3 axis() {
            return MinecartHingeGeometry.axis(yaw, pitch);
        }

        double linearWeight(Vec3 normal) {
            double projection = rail ? normal.dot(axis()) : 1;
            return weight * projection * projection;
        }

        void translate(Vec3 change) {
            p = p.add(rail ? axis().scale(change.dot(axis())) : change);
        }

        Vec3 anchor(int side) {
            return p.add(0, MinecartHingeGeometry.HEIGHT, 0)
                    .add(MinecartHingeGeometry.offset(cart, yaw, pitch, side));
        }

        void predict(double time) {
            if (rail && capturedRail) {
                MinecartRailMotion.moveForDuration(cart, Math.max(0, time - railTime));
                railTime = time;
                v = cart.getDeltaMovement();
                if (!cart.isOnRails()) {
                    var pending = PENDING.get((ServerLevel) cart.level());
                    Double deferred = pending == null ? null : pending.remove(cart);
                    start = time - (deferred == null ? 0 : deferred);
                    rail = false;
                    capturedRail = false;
                    yaw = state(cart).physicalYaw;
                    pitch = state(cart).physicalPitch;
                } else {
                    p = predicted = railStart = railEnd = cart.position();
                    railShift = Vec3.ZERO;
                    dt = STEP;
                    Vec3 direction = MinecartHingeGeometry.supportedAxis(cart);
                    yaw = Math.atan2(direction.z, direction.x);
                    pitch = Math.asin(Math.clamp(direction.y, -1, 1));
                    weight = active && damped && !MinecartHandbrake.anchored(cart) ? 1 : 0;
                    inertia = 0;
                    return;
                }
            }
            if (rail) {
                p =
                        predicted =
                                railStart
                                        .lerp(
                                                railEnd,
                                                Math.clamp(
                                                        (time - railTime)
                                                                / Math.max(.001, 1 - railTime),
                                                        0,
                                                        1))
                                        .add(railShift);
                dt = STEP;
                weight = active && damped && !MinecartHandbrake.anchored(cart) ? 1 : 0;
                inertia = 0;
                return;
            }
            before = p = cart.position();
            beforeYaw = yaw;
            beforePitch = pitch;
            dt = Math.max(0, Math.min(STEP, time - start));
            weight =
                    active
                                    && !rail
                                    && dt > 1e-6
                                    && !(MinecartHandbrake.anchored(cart) && cart.onGround())
                            ? (dt / STEP) * (dt / STEP)
                            : 0;
            inertia = weight * 3;
            if (weight == 0 || !cart.isAlive()) {
                predicted = p;
                return;
            }
            double gravity = cart.isNoGravity() ? 0 : cart.getGravity();
            boolean water = water();
            double vertical = Math.max(-TERMINAL, v.y - gravity * dt * (water ? .125 : 1));
            double drag =
                    Math.pow(water ? .90 : cart.onGround() ? .50 : damped ? .94 : AIR_DRAG, dt);
            Vec3 next = new Vec3(v.x * drag, vertical, v.z * drag);
            var motion = state(cart);
            motion.flightRailSpeed =
                    Math.max(next.horizontalDistance(), motion.flightRailSpeed * drag);
            Vec3 displacement =
                    new Vec3(
                            (v.x + next.x) * .5 * dt,
                            (v.y + vertical) * .5 * dt,
                            (v.z + next.z) * .5 * dt);
            displacement = MinecartRailCapture.approach(cart, displacement);
            double oldY = cart.getY();
            cart.move(MoverType.SELF, displacement);
            p = cart.position();
            groundContact = cart.onGround();
            MinecartCrash.record(cart, oldY);
            v =
                    new Vec3(
                            cart.horizontalCollision ? 0 : next.x,
                            cart.verticalCollision ? 0 : next.y,
                            cart.horizontalCollision ? 0 : next.z);
            predicted = p;
            if (capture(time, next.y)) return;
            if (water) MinecartCrash.clear(cart);
            else if (groundContact) {
                state(cart).flightRailSpeed = 0;
                MinecartCrash.landed(cart);
            }
            if (!cart.isAlive()) {
                weight = inertia = 0;
                return;
            }
            int peers =
                    MinecartCouplings.get((ServerLevel) cart.level())
                            .neighbors(cart.getUUID())
                            .size();
            if (groundContact && peers == 0 && v.horizontalDistanceSqr() > .000004) {
                double target = Math.atan2(v.z, v.x), turn = angle(target - yaw);
                if (turn > Math.PI / 2) turn -= Math.PI;
                else if (turn < -Math.PI / 2) turn += Math.PI;
                yaw += Math.clamp(turn, -.025, .025);
                yawRate = 0;
            }
            if (!cart.onGround()
                    && v.lengthSqr() > .0001
                    && (peers == 0
                            || peers == 1
                                    && state(cart).flightLaunched
                                    && v.horizontalDistance() > .025)) {
                double sign = state(cart).flightForwardSign;
                Vec3 target = v.scale(sign == 0 ? 1 : sign).normalize();
                double wantedYaw =
                        target.horizontalDistanceSqr() > .0004
                                ? Math.atan2(target.z, target.x)
                                : yaw;
                double wantedPitch = Math.atan2(target.y, target.horizontalDistance());
                yawRate += Math.clamp(angle(wantedYaw - yaw) * .06, -.025, .025) * dt;
                pitchRate += Math.clamp((wantedPitch - pitch) * .06, -.025, .025) * dt;
            }
            double damping = Math.pow(damped ? .75 : .88, dt);
            yawRate *= Math.pow(.5, dt);
            pitchRate *= damping;
            yaw += Math.clamp(yawRate * dt, -.014, .014);
            pitch =
                    Math.clamp(
                            pitch + Math.clamp(pitchRate * dt, -.014, .014),
                            -Math.PI / 2,
                            Math.PI / 2);
        }

        void rotate(double dyaw, double dpitch) {
            if (inertia == 0) return;
            if (Math.abs(pitch) > Math.toRadians(70)) dyaw = 0;
            yaw = beforeYaw + Math.clamp(angle(yaw + dyaw - beforeYaw), -.020, .020);
            pitch =
                    Math.clamp(
                            beforePitch + Math.clamp(pitch + dpitch - beforePitch, -.020, .020),
                            -Math.PI / 2,
                            Math.PI / 2);
        }

        void follow(double time, Vec3 target, Vec3 axis, Vec3 direction, double speed) {
            before = p = cart.position();
            beforeYaw = yaw;
            beforePitch = pitch;
            dt = Math.max(0, Math.min(STEP, time - start));
            weight = active && dt > 1e-6 ? 1 : 0;
            inertia = weight * 3;
            if (weight == 0 || !cart.isAlive()) {
                predicted = p;
                return;
            }
            Vec3 change = bounded(target.subtract(p), Math.max(.02, (speed + .12) * dt));
            change = MinecartRailCapture.approach(cart, change);
            double oldY = cart.getY();
            cart.move(MoverType.SELF, change);
            p = predicted = cart.position();
            groundContact = cart.onGround();
            MinecartCrash.record(cart, oldY);
            v = direction.scale(speed);
            state(cart).flightRailSpeed =
                    Math.max(
                            v.horizontalDistance(),
                            state(cart).flightRailSpeed * Math.pow(AIR_DRAG, dt));
            if (cart.horizontalCollision) v = new Vec3(0, v.y, 0);
            if (cart.verticalCollision) v = new Vec3(v.x, 0, v.z);
            if (axis.lengthSqr() > 1e-8) {
                double desiredYaw =
                        axis.horizontalDistanceSqr() > 1e-7 ? Math.atan2(axis.z, axis.x) : yaw;
                yaw += Math.clamp(angle(desiredYaw - yaw), -.020, .020);
                pitch =
                        Math.clamp(
                                pitch
                                        + Math.clamp(
                                                Math.atan2(axis.y, axis.horizontalDistance())
                                                        - pitch,
                                                -.020,
                                                .020),
                                -Math.PI / 2,
                                Math.PI / 2);
            }
            if (capture(time, v.y)) return;
            if (water()) MinecartCrash.clear(cart);
            else if (groundContact) MinecartCrash.landed(cart);
            if (!cart.isAlive()) weight = inertia = 0;
        }

        boolean water() {
            return cart.isInWaterOrBubble()
                    || cart.level().getFluidState(cart.blockPosition()).is(FluidTags.WATER)
                    || cart.level()
                            .getFluidState(
                                    BlockPos.containing(cart.getX(), cart.getY() + .3, cart.getZ()))
                            .is(FluidTags.WATER);
        }

        void commit(double time) {
            if (!rail
                    && !MinecartCouplings.get((ServerLevel) cart.level())
                            .neighbors(cart.getUUID())
                            .isEmpty()) {
                double reference = state(cart).flightReferenceYaw;
                if (Double.isFinite(reference))
                    yaw = reference + Math.clamp(angle(yaw - reference), -MAX_BEND, MAX_BEND);
            }
            if (weight == 0 || !cart.isAlive()) return;
            if (rail) {
                commitRail();
                return;
            }
            Vec3 correction = bounded(p.subtract(predicted), damped ? .08 : .22);
            if (correction.lengthSqr() > 1e-12) cart.move(MoverType.SELF, correction);
            p = cart.position();
            if (groundContact && correction.y <= 1e-7) cart.setOnGround(true);
            v = v.add(bounded(p.subtract(predicted).scale(1 / dt), damped ? .015 : .045));
            if (cart.onGround() && v.y < 0 || cart.verticalCollision) v = new Vec3(v.x, 0, v.z);
            yawRate = Math.clamp(angle(yaw - beforeYaw) / dt, -.20, .20);
            pitchRate = Math.clamp((pitch - beforePitch) / dt, -.20, .20);
            if (!rotationClear()) {
                yaw = beforeYaw;
                pitch = beforePitch;
                yawRate = pitchRate = 0;
            }
            cart.setDeltaMovement(v);
            if (capture(time, v.y)) return;
            if (water()) MinecartCrash.clear(cart);
            else if (cart.onGround()) MinecartCrash.landed(cart);
        }

        void commitRail() {
            Vec3 correction = bounded(p.subtract(predicted), .06);
            var track = MinecartTrack.current(cart);
            if (track == null) {
                p = predicted;
                return;
            }
            double along = correction.dot(track.tangent()), speed = v.dot(track.tangent());
            var next =
                    MinecartTrack.advance(
                            cart,
                            track,
                            track.offset(cart.position()),
                            along < 0 ? -1 : 1,
                            Math.abs(along));
            Vec3 actual = next == null ? Vec3.ZERO : next.position().subtract(cart.position());
            if (next != null) {
                MinecartBodyOrientation.followRail(
                        cart,
                        track.tangent().scale(along < 0 ? -1 : 1),
                        next.track().tangent().scale(next.sign()));
                speed *= next.sign() * (along < 0 ? -1 : 1);
                cart.setPos(next.position());
                state(cart).railPosition = next.track().pos();
                track = next.track();
            }
            railShift = railShift.add(actual);
            p = predicted.add(actual);
            double impulse = Math.clamp(along / dt, -.045, .045);
            speed =
                    Math.clamp(
                            speed + impulse,
                            -cart.getMaxSpeedWithRail(),
                            cart.getMaxSpeedWithRail());
            if (damped) {
                speed *= Math.pow(.25, dt);
                if (Math.abs(speed) < .015) speed = 0;
            }
            v = track.tangent().scale(speed);
            cart.setDeltaMovement(v);
            Vec3 direction = MinecartHingeGeometry.supportedAxis(cart);
            yaw = Math.atan2(direction.z, direction.x);
            pitch = Math.asin(Math.clamp(direction.y, -1, 1));
        }

        boolean capture(double time, double impact) {
            var track = MinecartTrack.at(cart, cart.getCurrentRailPosition());
            if (track == null)
                track = MinecartTrack.at(cart, cart.getCurrentRailPosition().below());
            Vec3 landing = track == null ? p : track.point(track.offset(p));
            double wheelReach =
                    Math.max(0, .375 * Math.cos(pitch) + .49 * Math.abs(Math.sin(pitch)) - .375);
            if (track != null
                    && impact <= .001
                    && Math.abs(landing.y - p.y) < .15 + wheelReach
                    && landing.subtract(p).horizontalDistanceSqr() < .2025) {
                MinecartRailLanding.landed(cart, impact);
                MinecartRailCapture.land(cart, track, v);
                yaw = state(cart).physicalYaw;
                MinecartCrash.clear(cart);
                cart.setPos(track.point(track.offset(p)));
                v = cart.getDeltaMovement();
                ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(true);
                ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(false);
                state(cart).railPosition = track.pos();
                state(cart).flightCaptured = true;
                rail = true;
                capturedRail = true;
                weight = inertia = 0;
                if (MinecartCouplings.get((ServerLevel) cart.level()).hasBrakedEngines())
                    MinecartTrainBrakes.invalidate((ServerLevel) cart.level());
                Vec3 direction = MinecartHingeGeometry.supportedAxis(cart);
                yaw = Math.atan2(direction.z, direction.x);
                pitch = Math.asin(Math.clamp(direction.y, -1, 1));
                railStart = railEnd = cart.position();
                railTime = time;
                p = predicted = railStart;
                v = cart.getDeltaMovement();
                return true;
            }
            return false;
        }

        boolean rotationClear() {
            Vec3 center = p.add(0, .375, 0), axis = axis();
            for (double end : new double[] {-.45, .45}) {
                Vec3 point = center.add(axis.scale(end));
                if (cart.level()
                        .getBlockCollisions(cart, AABB.ofSize(point, .32, .32, .32))
                        .iterator()
                        .hasNext()) return false;
            }
            return true;
        }
    }

    /** Keeps the slack measured on a ramp until the end joint settles in the air. */
    private static final class Memory {
        double rest = .2375;
        long seen;
        boolean initialized;
    }

    /** Identifies an unordered physical connection without keeping entity references. */
    private record Edge(UUID first, UUID second) {
        static Edge of(UUID a, UUID b) {
            return a.compareTo(b) < 0 ? new Edge(a, b) : new Edge(b, a);
        }
    }

    /** Solves one elastic end-to-end joint with no roll and bounded articulation. */
    private static final class Joint {
        final Body a, b;
        final int sa, sb;
        final Memory memory;
        double lambda;

        Joint(Body a, Body b, int sa, int sb, Memory memory) {
            this.a = a;
            this.b = b;
            this.sa = sa;
            this.sb = sb;
            this.memory = memory;
            a.attached.add(b);
            b.attached.add(a);
            if (!memory.initialized) {
                memory.rest = Math.clamp(a.anchor(sa).distanceTo(b.anchor(sb)), .2375, .95);
                memory.initialized = true;
            }
        }

        void begin() {
            lambda = 0;
            if (a.rail && b.rail && (a.damped || b.damped)) {
                double horizontal =
                        (a.axis().horizontalDistance() + b.axis().horizontalDistance()) * .5;
                memory.rest =
                        Math.clamp(.2375 + 1.55 * (1 / Math.max(.7, horizontal) - 1), .2375, .95);
            } else if (a.rail && b.rail)
                memory.rest = Math.clamp(a.anchor(sa).distanceTo(b.anchor(sb)), .2375, .95);
            else memory.rest += (.2375 - memory.rest) * .004;
        }

        void solve() {
            if (a.guide != null && a.guide == b.guide && a.guide.active) return;
            if (!a.cart.isAlive() || !b.cart.isAlive() || a.weight + b.weight == 0) return;
            Vec3 centers = b.p.subtract(a.p);
            double centerDistance = centers.length();
            if (centerDistance > 1e-6 && centerDistance < 1.10) {
                Vec3 move =
                        centers.scale(
                                Math.min(.08, 1.10 - centerDistance)
                                        / (centerDistance * (a.weight + b.weight)));
                a.translate(move.scale(-a.weight));
                b.translate(move.scale(b.weight));
            }
            Vec3 difference = b.anchor(sb).subtract(a.anchor(sa));
            double length = difference.length();
            if (length < 1e-7) return;
            Vec3 outward = a.axis().scale(sa).subtract(b.axis().scale(sb)).normalize();
            double clearance = difference.dot(outward);
            if (clearance < .10) {
                correct(clearance - .10, outward, 0);
                difference = b.anchor(sb).subtract(a.anchor(sa));
                length = difference.length();
                if (length < 1e-7) return;
            }
            double target =
                    Math.clamp(length, Math.max(.08, memory.rest - .045), memory.rest + .045);
            double error = length - target;
            if (Math.abs(error) > .00005) {
                correct(error, difference.scale(1 / length), .0015 / (STEP * STEP));
            }
            Vec3 first = a.axis().scale(sa), second = b.axis().scale(-sb);
            double yawDifference = angle(b.yaw + (sa == sb ? Math.PI : 0) - a.yaw);
            double flex = Math.clamp(yawDifference * .08, -.015, .015),
                    angular = a.inertia + b.inertia;
            if (angular > 0) {
                a.rotate(flex * a.inertia / angular, 0);
                b.rotate(-flex * b.inertia / angular, 0);
            }
            double bend = Math.acos(Math.clamp(first.dot(second), -1, 1));
            if (bend > MAX_BEND) {
                double amount = Math.min(.06, (bend - MAX_BEND) * .2);
                align(
                        a,
                        second.scale(sa),
                        amount * a.inertia / Math.max(.001, a.inertia + b.inertia));
                align(
                        b,
                        first.scale(-sb),
                        amount * b.inertia / Math.max(.001, a.inertia + b.inertia));
            }
        }

        void correct(double error, Vec3 normal, double alpha) {
            double ap =
                    -normal.dot(MinecartHingeGeometry.pitchDerivative(a.cart, a.yaw, a.pitch, sa));
            double ay =
                    -normal.dot(MinecartHingeGeometry.yawDerivative(a.cart, a.yaw, a.pitch, sa));
            double bp =
                    normal.dot(MinecartHingeGeometry.pitchDerivative(b.cart, b.yaw, b.pitch, sb));
            double by = normal.dot(MinecartHingeGeometry.yawDerivative(b.cart, b.yaw, b.pitch, sb));
            double weight =
                    a.linearWeight(normal)
                            + b.linearWeight(normal)
                            + a.inertia * (ap * ap + ay * ay)
                            + b.inertia * (bp * bp + by * by);
            if (weight < 1e-8) return;
            double change = Math.clamp((-error - alpha * lambda) / (weight + alpha), -.12, .12);
            lambda += change;
            a.translate(normal.scale(-change * a.weight));
            b.translate(normal.scale(change * b.weight));
            a.rotate(change * a.inertia * ay, change * a.inertia * ap);
            b.rotate(change * b.inertia * by, change * b.inertia * bp);
        }

        void align(Body body, Vec3 target, double amount) {
            double yaw =
                    target.horizontalDistanceSqr() > .0001
                            ? Math.atan2(target.z, target.x)
                            : body.yaw;
            double pitch = Math.atan2(target.y, target.horizontalDistance());
            body.rotate(
                    Math.clamp(angle(yaw - body.yaw), -amount, amount),
                    Math.clamp(pitch - body.pitch, -amount, amount));
        }

        void dampStretch() {
            if (a.guide != null && a.guide == b.guide && a.guide.active) return;
            if (!a.cart.isAlive() || !b.cart.isAlive()) return;
            Vec3 delta = b.anchor(sb).subtract(a.anchor(sa));
            double length = delta.length();
            if (length < memory.rest + .15) return;
            Vec3 normal = delta.scale(1 / length);
            double opening = b.v.subtract(a.v).dot(normal);
            double wa = !a.rail ? a.weight : 0, wb = !b.rail ? b.weight : 0, weight = wa + wb;
            if (opening <= .01 || weight < 1e-8) return;
            Vec3 impulse = normal.scale(opening * .65 / weight);
            if (wa > 0) {
                a.v = a.v.add(impulse.scale(wa));
                a.cart.setDeltaMovement(a.v);
            }
            if (wb > 0) {
                b.v = b.v.subtract(impulse.scale(wb));
                b.cart.setDeltaMovement(b.v);
            }
        }

        void limitAnchoredContact() {
            if (!(a.rail && MinecartHandbrake.active(a.cart)
                    || b.rail && MinecartHandbrake.active(b.cart))) return;
            double wa = !a.rail && a.active ? 1 : 0,
                    wb = !b.rail && b.active ? 1 : 0,
                    total = wa + wb;
            if (total == 0) return;
            Vec3 delta = b.cart.position().subtract(a.cart.position());
            double distance = delta.length();
            if (distance < 1e-7) return;
            Vec3 normal = delta.scale(1 / distance);
            double approach = -b.v.subtract(a.v).dot(normal);
            double allowed = Math.max(0, distance - 1.25) / STEP;
            if (approach <= allowed) return;
            Vec3 impulse = normal.scale((approach - allowed) / total);
            if (wa > 0) a.v = a.v.subtract(impulse.scale(wa));
            if (wb > 0) b.v = b.v.add(impulse.scale(wb));
        }

        void resolveContact() {
            if (!a.cart.isAlive() || !b.cart.isAlive()) return;
            double wa = !a.rail ? a.weight : 0, wb = !b.rail ? b.weight : 0, total = wa + wb;
            if (total == 0) return;
            Vec3 delta = b.cart.position().subtract(a.cart.position());
            double distance = delta.length();
            if (distance < 1e-6 || distance >= 1.08) return;
            Vec3 normal = delta.scale(1 / distance);
            double missing = Math.min(.12, 1.10 - distance);
            if (wa > 0) a.cart.move(MoverType.SELF, normal.scale(-missing * wa / total));
            if (wb > 0) b.cart.move(MoverType.SELF, normal.scale(missing * wb / total));
            double remaining = 1.08 - a.cart.distanceTo(b.cart);
            if (remaining > 0 && wa > 0)
                a.cart.move(MoverType.SELF, normal.scale(-Math.min(.12, remaining)));
            remaining = 1.08 - a.cart.distanceTo(b.cart);
            if (remaining > 0 && wb > 0)
                b.cart.move(MoverType.SELF, normal.scale(Math.min(.12, remaining)));
            double closing = b.v.subtract(a.v).dot(normal);
            if (closing < 0) {
                Vec3 impulse = normal.scale(-closing / total);
                if (wa > 0) {
                    a.v = a.v.subtract(impulse.scale(wa));
                    a.cart.setDeltaMovement(a.v);
                }
                if (wb > 0) {
                    b.v = b.v.add(impulse.scale(wb));
                    b.cart.setDeltaMovement(b.v);
                }
            }
            if (!a.rail) a.p = a.cart.position();
            if (!b.rail) b.p = b.cart.position();
        }
    }
}
