// Paths between minecarts.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.block.custom.rail.CrossingRailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public record MinecartRailPath(
        double distance, Vec3 firstToward, Vec3 secondToward, double spatialDistance) {
    public MinecartRailPath(double distance, Vec3 firstToward, Vec3 secondToward) {
        this(distance, firstToward, secondToward, distance);
    }

    private static final int MAX_SEGMENTS = 8;

    @Nullable
    public static MinecartRailPath between(AbstractMinecart first, AbstractMinecart second) {
        if (((MinecartMotionVisuals) first).spelunkersCharm$airborne()
                || ((MinecartMotionVisuals) second).spelunkersCharm$airborne()) return null;
        if (!first.canUseRail() || !second.canUseRail()) return null;
        MinecartTrack firstTrack = MinecartTrack.current(first);
        MinecartTrack secondTrack = MinecartTrack.current(second);
        Track start =
                track(
                        first,
                        firstTrack == null ? first.getCurrentRailPosition() : firstTrack.pos(),
                        null);
        Track end =
                track(
                        second,
                        secondTrack == null ? second.getCurrentRailPosition() : secondTrack.pos(),
                        null);
        if (start == null || end == null) return null;

        if (start.pos.equals(end.pos)) {
            if (!start.equals(end)) return null;
            double separation = start.offset(second.position()) - start.offset(first.position());
            boolean forward = separation > 0 || separation == 0 && first.getId() < second.getId();
            Vec3 direction = start.toward(forward ? 1 : 0);
            return new MinecartRailPath(
                    Math.abs(separation),
                    direction,
                    direction.scale(-1),
                    Math.abs(separation) * start.metric());
        }

        MinecartRailPath left = follow(first, second, start, end, 0);
        MinecartRailPath right = follow(first, second, start, end, 1);
        if (left == null) return right;
        if (right == null) return left;
        return left.distance <= right.distance ? left : right;
    }

    @Nullable
    private static MinecartRailPath follow(
            AbstractMinecart first,
            AbstractMinecart second,
            Track start,
            Track end,
            int firstExit) {
        Track current = start;
        int exit = firstExit;
        double distance = start.distanceTo(first.position(), exit);
        double spatial = distance * start.metric();
        for (int step = 0; step < MAX_SEGMENTS; step++) {
            Port port = current.port(exit);
            BlockPos edge = current.pos.relative(port.direction).above(port.height);
            Direction approach = port.direction.getOpposite();
            Track next = track(first, edge, approach);
            int entry = entryIndex(next, approach, edge.getY());
            if (entry < 0) {
                next = track(first, edge.below(), approach);
                entry = entryIndex(next, approach, edge.getY());
            }
            if (entry < 0 || next.pos.equals(start.pos)) return null;
            if (next.pos.equals(end.pos)) {
                if (!next.equals(end)) return null;
                return new MinecartRailPath(
                        distance + end.distanceTo(second.position(), entry),
                        start.toward(firstExit),
                        end.toward(entry),
                        spatial + end.distanceTo(second.position(), entry) * end.metric());
            }
            distance += next.length();
            spatial += next.length() * next.metric();
            current = next;
            exit = 1 - entry;
        }
        return null;
    }

    private static int entryIndex(@Nullable Track track, Direction direction, int elevation) {
        if (track == null) return -1;
        for (int i = 0; i < 2; i++) {
            Port port = track.port(i);
            if (port.direction == direction && track.pos.getY() + port.height == elevation)
                return i;
        }
        return -1;
    }

    @Nullable
    private static Track track(AbstractMinecart cart, BlockPos pos, @Nullable Direction approach) {
        Level level = cart.level();
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock rail)) return null;
        RailShape shape =
                rail instanceof CrossingRailBlock && approach != null
                        ? CrossingRailBlock.shapeFor(approach.getAxis())
                        : rail.getRailDirection(state, level, pos, cart);
        return switch (shape) {
            case NORTH_SOUTH -> new Track(pos, Port.NORTH, Port.SOUTH);
            case EAST_WEST -> new Track(pos, Port.WEST, Port.EAST);
            case ASCENDING_EAST -> new Track(pos, Port.WEST, Port.EAST.raised());
            case ASCENDING_WEST -> new Track(pos, Port.WEST.raised(), Port.EAST);
            case ASCENDING_NORTH -> new Track(pos, Port.NORTH.raised(), Port.SOUTH);
            case ASCENDING_SOUTH -> new Track(pos, Port.NORTH, Port.SOUTH.raised());
            case SOUTH_EAST -> new Track(pos, Port.SOUTH, Port.EAST);
            case SOUTH_WEST -> new Track(pos, Port.SOUTH, Port.WEST);
            case NORTH_WEST -> new Track(pos, Port.NORTH, Port.WEST);
            case NORTH_EAST -> new Track(pos, Port.NORTH, Port.EAST);
        };
    }

    /** Describes one connected end of a rail segment. */
    private record Port(Direction direction, int height) {
        private static final Port NORTH = new Port(Direction.NORTH, 0);
        private static final Port SOUTH = new Port(Direction.SOUTH, 0);
        private static final Port WEST = new Port(Direction.WEST, 0);
        private static final Port EAST = new Port(Direction.EAST, 0);

        private Port raised() {
            return new Port(direction, 1);
        }

        private Vec3 point(BlockPos pos) {
            return new Vec3(
                    pos.getX() + 0.5 + direction.getStepX() * 0.5,
                    0,
                    pos.getZ() + 0.5 + direction.getStepZ() * 0.5);
        }
    }

    /** Stores a visited rail segment while searching a path. */
    private record Track(BlockPos pos, Port first, Port second) {
        private double metric() {
            return first.height == second.height ? 1 : Math.sqrt(2);
        }

        private Port port(int index) {
            return index == 0 ? first : second;
        }

        private double length() {
            return first.point(pos).distanceTo(second.point(pos));
        }

        private Vec3 toward(int index) {
            return port(index).point(pos).subtract(port(1 - index).point(pos)).normalize();
        }

        private double offset(Vec3 location) {
            return Mth.clamp(location.subtract(first.point(pos)).dot(toward(1)), 0, length());
        }

        private double distanceTo(Vec3 location, int index) {
            double offset = offset(location);
            return index == 0 ? offset : length() - offset;
        }
    }
}
