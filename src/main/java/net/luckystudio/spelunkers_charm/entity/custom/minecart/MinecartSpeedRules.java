// Minecart speed rules.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.block.custom.rail.WoodenRailBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.UUID;

public final class MinecartSpeedRules {
    public static final double SPRINT_BLOCKS_PER_SECOND = 5.612;
    public static final double SPEED_SCALE = 0.60;
    public static final double UPHILL_MOMENTUM = 0.18 * SPEED_SCALE;
    public static final double PLAYER_ACCELERATION = 0.018 * SPEED_SCALE;
    public static final double ENGINE_ACCELERATION = 0.012 * SPEED_SCALE;

    private MinecartSpeedRules() {}

    public static boolean managed(AbstractMinecart cart) {
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(cart.getType()).getNamespace();
        return (namespace.equals("minecraft") || namespace.equals("spelunkers_charm"))
                && !cart.noPhysics
                && !cart.isPassenger()
                && cart.canUseRail()
                && !BetterMinecartsLinks.hasLink(cart);
    }

    public static double vehicleBase(AbstractMinecart cart) {
        if (cart instanceof LocomotiveMinecart engine)
            return 5 + Math.max(0, engine.activeBurners() - 1);
        return cart instanceof MinecartFurnace ? 5 : SPRINT_BLOCKS_PER_SECOND * SPEED_SCALE;
    }

    public static double trainBase(AbstractMinecart cart) {
        double base = vehicleBase(cart);
        if (!(cart.level() instanceof ServerLevel level)) return base;
        MinecartMotionState motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (motion.trainSampleTick == level.getGameTime()) return Math.max(base, motion.trainBase);
        MinecartCouplings links = MinecartCouplings.get(level);
        ArrayDeque<AbstractMinecart> pending = new ArrayDeque<>();
        HashSet<UUID> visited = new HashSet<>();
        pending.add(cart);
        visited.add(cart.getUUID());
        var members = new java.util.ArrayList<AbstractMinecart>();
        int engines = 0;
        boolean locomotive = false;
        AbstractMinecart driver = null;
        while (!pending.isEmpty()) {
            AbstractMinecart next = pending.removeFirst();
            locomotive |= next instanceof LocomotiveMinecart;
            members.add(next);
            base = Math.max(base, vehicleBase(next));
            if (next instanceof LocomotiveMinecart engine
                    && engine.activeBurners() > 0
                    && !engine.emergencyBrake()) {
                base = Math.max(base, vehicleBase(engine));
                engines++;
                driver = next;
            }
            if (next instanceof MinecartFurnace furnace
                    && furnace.fuel > 0
                    && !MinecartHandbrake.active(furnace)) {
                base = Math.max(base, 5);
                engines++;
                driver = next;
            }
            for (UUID id : links.neighbors(next.getUUID())) {
                if (visited.add(id)
                        && level.getEntity(id) instanceof AbstractMinecart peer
                        && peer.isAlive()) pending.addLast(peer);
            }
        }
        double load = Math.max(1.0, members.size() / (double) Math.max(1, engines));
        for (var member : members) {
            var state = ((MinecartMotionVisuals) member).spelunkersCharm$motion();
            state.trainSampleTick = level.getGameTime();
            state.trainBase = base;
            state.tractionLoad = load;
            state.locomotiveTrain = locomotive;
            state.trainPowered = false;
            state.trainDrive = Vec3.ZERO;
        }
        if (driver != null && !MinecartGroundFriction.brakingContact(driver))
            shareDrive(driver, level, links);
        return base;
    }

    private static void shareDrive(
            AbstractMinecart driver, ServerLevel level, MinecartCouplings links) {
        Vec3 command =
                driver instanceof LocomotiveMinecart engine
                        ? engine.driveVector()
                        : driver instanceof MinecartFurnace furnace
                                ? new Vec3(furnace.xPush, 0, furnace.zPush).normalize()
                                : Vec3.ZERO;
        if (command.lengthSqr() < 1e-6) return;
        var queue = new ArrayDeque<AbstractMinecart>();
        var visited = new HashSet<UUID>();
        queue.add(driver);
        visited.add(driver.getUUID());
        ((MinecartMotionVisuals) driver).spelunkersCharm$motion().trainDrive = command;
        while (!queue.isEmpty()) {
            var parent = queue.removeFirst();
            var state = ((MinecartMotionVisuals) parent).spelunkersCharm$motion();
            state.trainPowered = true;
            for (UUID id : links.neighbors(parent.getUUID())) {
                if (!visited.add(id)
                        || !(level.getEntity(id) instanceof AbstractMinecart peer)
                        || !managed(peer)) continue;
                var path = MinecartRailPath.between(parent, peer);
                if (!MinecartTrainBrakes.onRail(parent) || !MinecartTrainBrakes.onRail(peer))
                    path = null;
                Vec3 toward =
                        path == null
                                ? peer.position()
                                        .subtract(parent.position())
                                        .multiply(1, 0, 1)
                                        .normalize()
                                : path.firstToward();
                double orientation = state.trainDrive.dot(toward);
                Vec3 direction =
                        path == null || Math.abs(orientation) < .1
                                ? ((MinecartMotionVisuals) peer)
                                        .spelunkersCharm$motion()
                                        .travelHeading
                                : path.secondToward().scale(orientation > 0 ? -1 : 1);
                if (direction.lengthSqr() < 1e-6) direction = state.trainDrive;
                ((MinecartMotionVisuals) peer).spelunkersCharm$motion().trainDrive = direction;
                queue.addLast(peer);
            }
        }
    }

    public static Vec3 trainDrive(AbstractMinecart cart) {
        trainBase(cart);
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion().trainDrive;
    }

    public static boolean trainPowered(AbstractMinecart cart) {
        trainBase(cart);
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion().trainPowered;
    }

    public static double accelerationScale(AbstractMinecart cart) {
        trainBase(cart);
        return cart instanceof LocomotiveMinecart
                        || ((MinecartMotionVisuals) cart).spelunkersCharm$motion().locomotiveTrain
                ? .70
                : 1;
    }

    public static double traction(AbstractMinecart cart) {
        trainBase(cart);
        return ((MinecartMotionVisuals) cart).spelunkersCharm$motion().tractionLoad;
    }

    public static int railBonus(BlockState state) {
        if (state.getBlock() instanceof WoodenRailBlock) return 0;
        return powered(state) ? 2 : 1;
    }

    public static boolean powered(BlockState state) {
        return state.getBlock() instanceof PoweredRailBlock rail
                && !rail.isActivatorRail()
                && !(rail instanceof DirectionalRailBlock)
                && state.getValue(PoweredRailBlock.POWERED);
    }

    public static double limit(AbstractMinecart cart, BlockState state, boolean downhill) {
        double limit = (trainBase(cart) + railBonus(state) + (downhill ? .5 : 0)) / 20.0;
        if (cart.isInWater()) limit *= 0.5;
        double flight = flightLimit(cart);
        return Math.min(
                Double.isFinite(flight) ? flight : limit,
                ((MinecartMotionVisuals) cart).spelunkersCharm$motion().externalSpeedCap);
    }

    public static double flightLimit(AbstractMinecart cart) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        if (cart.level().isClientSide()
                || !motion.flightFollower
                || motion.flightCaptured
                || motion.flightSpeedUntil < cart.level().getGameTime())
            return Double.POSITIVE_INFINITY;
        if (cart.level() instanceof ServerLevel level
                && MinecartCouplings.get(level).brakeSource(cart.getUUID()) != null)
            return Double.POSITIVE_INFINITY;
        var track = MinecartTrack.current(cart);
        double metric =
                track == null
                        ? 1
                        : track.point(track.length()).distanceTo(track.point(0)) / track.length();
        return motion.flightArcSpeed / metric;
    }

    public static double limit(AbstractMinecart cart) {
        MinecartTrack segment = MinecartTrack.current(cart);
        if (segment == null) return 0.4 * SPEED_SCALE;
        BlockState state = segment.state();
        boolean downhill = segment != null && segment.slope(cart.getDeltaMovement()) < 0;
        return limit(cart, state, downhill);
    }

    public static Vec3 playerInput(net.minecraft.world.entity.player.Player player) {
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(
                -Math.sin(yaw) * player.zza + Math.cos(yaw) * player.xxa,
                0,
                Math.cos(yaw) * player.zza + Math.sin(yaw) * player.xxa);
    }
}
