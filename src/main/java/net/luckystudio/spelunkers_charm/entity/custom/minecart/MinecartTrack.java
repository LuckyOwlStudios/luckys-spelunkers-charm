// Rail segments and connections.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public record MinecartTrack(BlockPos pos, BlockState state, Port first, Port second) {
    @Nullable
    public static MinecartTrack current(AbstractMinecart cart) {
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        MinecartTrack remembered =
                motion.railPosition == null ? null : at(cart, motion.railPosition);
        if (remembered != null
                && remembered
                                .point(remembered.offset(cart.position()))
                                .distanceToSqr(cart.position())
                        < 0.00001) return remembered;
        return at(cart, cart.getCurrentRailPosition());
    }

    @Nullable
    public static MinecartTrack at(AbstractMinecart cart, BlockPos pos) {
        if (!cart.level().hasChunkAt(pos)) return null;
        BlockState state = cart.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock rail)) return null;
        return switch (rail.getRailDirection(state, cart.level(), pos, cart)) {
            case NORTH_SOUTH -> new MinecartTrack(pos, state, Port.NORTH, Port.SOUTH);
            case EAST_WEST -> new MinecartTrack(pos, state, Port.WEST, Port.EAST);
            case ASCENDING_EAST -> new MinecartTrack(pos, state, Port.WEST, Port.EAST.raised());
            case ASCENDING_WEST -> new MinecartTrack(pos, state, Port.WEST.raised(), Port.EAST);
            case ASCENDING_NORTH -> new MinecartTrack(pos, state, Port.NORTH.raised(), Port.SOUTH);
            case ASCENDING_SOUTH -> new MinecartTrack(pos, state, Port.NORTH, Port.SOUTH.raised());
            case SOUTH_EAST -> new MinecartTrack(pos, state, Port.SOUTH, Port.EAST);
            case SOUTH_WEST -> new MinecartTrack(pos, state, Port.SOUTH, Port.WEST);
            case NORTH_WEST -> new MinecartTrack(pos, state, Port.NORTH, Port.WEST);
            case NORTH_EAST -> new MinecartTrack(pos, state, Port.NORTH, Port.EAST);
        };
    }

    public Vec3 point(double offset) {
        return first.point(pos).lerp(second.point(pos), Math.clamp(offset / length(), 0, 1));
    }

    public double length() {
        return second.point(pos).subtract(first.point(pos)).horizontalDistance();
    }

    public Vec3 tangent() {
        Vec3 difference = second.point(pos).subtract(first.point(pos));
        return new Vec3(difference.x, 0, difference.z).normalize();
    }

    public double offset(Vec3 position) {
        return Mth.clamp(position.subtract(first.point(pos)).dot(tangent()), 0, length());
    }

    public int slope(Vec3 heading) {
        return (int) Math.signum((second.height - first.height) * tangent().dot(heading));
    }

    @Nullable
    public static Step advance(
            AbstractMinecart cart, MinecartTrack track, double offset, int sign, double distance) {
        for (int guard = 0; guard < 128; guard++) {
            double remaining = sign > 0 ? track.length() - offset : offset;
            if (distance <= remaining + 1.0E-7)
                return new Step(
                        track, track.point(offset + sign * Math.min(distance, remaining)), sign);
            distance -= remaining;
            Vec3 edge = track.point(sign > 0 ? track.length() : 0);
            track = track.next(cart, sign);
            if (track == null) return null;
            offset = track.offset(edge);
            sign = offset < track.length() * .5 ? 1 : -1;
        }
        return null;
    }

    /** Describes the next connected rail and travel direction. */
    public record Step(MinecartTrack track, Vec3 position, int sign) {}

    @Nullable
    public MinecartTrack next(AbstractMinecart cart, int sign) {
        Port exit = sign > 0 ? second : first;
        BlockPos candidate = pos.relative(exit.direction).above(exit.height);
        Vec3 end = exit.point(pos);
        for (BlockPos check : new BlockPos[] {candidate, candidate.below()}) {
            MinecartTrack next = at(cart, check);
            if (next != null
                    && (next.first.point(check).distanceToSqr(end) < 1.0E-6
                            || next.second.point(check).distanceToSqr(end) < 1.0E-6)) return next;
        }
        return null;
    }

    /** Describes the position and height of a rail endpoint. */
    public record Port(Direction direction, int height) {
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
                    pos.getY() + 0.0625 + height,
                    pos.getZ() + 0.5 + direction.getStepZ() * 0.5);
        }
    }
}
