// Passenger train route.
package net.luckystudio.spelunkers_charm.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class PassengerTrainRoute {
    private final Level level;
    private final List<BlockPos> cells = new ArrayList<>();
    private final List<Vec3> entry = new ArrayList<>(), exit = new ArrayList<>();
    private final List<Double> offsets = new ArrayList<>();
    double length;

    PassengerTrainRoute(Level level, BlockPos origin) {
        this.level = level;
        int[][] corners = {
            {2, 24}, {2, 3}, {5, 3}, {5, 24}, {6, 24}, {6, 3}, {9, 3}, {9, 24}, {10, 24}, {10, 3},
            {13, 3}, {13, 9}, {28, 9}, {28, 12}, {13, 12}, {13, 15}, {24, 15}, {24, 18}, {13, 18},
            {13, 21}, {24, 21}, {24, 27}, {2, 27}, {2, 24}
        };
        for (int i = 0; i < corners.length - 1; i++) {
            var a = corners[i];
            var b = corners[i + 1];
            int dx = Integer.signum(b[0] - a[0]), dz = Integer.signum(b[1] - a[1]);
            for (int x = a[0], z = a[1]; x != b[0] || z != b[1]; x += dx, z += dz)
                cells.add(origin.offset(x, 2, z));
        }
        for (int i = 0; i < cells.size(); i++) {
            Vec3 middle = Vec3.atCenterOf(cells.get(i)).add(0, -.4375, 0);
            Vec3 before =
                    Vec3.atCenterOf(cells.get(Math.floorMod(i - 1, cells.size())))
                            .add(0, -.4375, 0);
            Vec3 after = Vec3.atCenterOf(cells.get((i + 1) % cells.size())).add(0, -.4375, 0);
            entry.add(middle.lerp(before, .5));
            exit.add(middle.lerp(after, .5));
            offsets.add(length);
            length += entry.getLast().distanceTo(exit.getLast());
        }
    }

    void build() {
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < cells.size(); i++) {
                var pos = cells.get(i);
                Vec3 middle = Vec3.atCenterOf(pos),
                        a = entry.get(i).subtract(middle),
                        b = exit.get(i).subtract(middle);
                RailShape shape;
                if (Math.abs(a.x - b.x) > .9) shape = RailShape.EAST_WEST;
                else if (Math.abs(a.z - b.z) > .9) shape = RailShape.NORTH_SOUTH;
                else if (a.x + b.x > 0)
                    shape = a.z + b.z > 0 ? RailShape.SOUTH_EAST : RailShape.NORTH_EAST;
                else shape = a.z + b.z > 0 ? RailShape.SOUTH_WEST : RailShape.NORTH_WEST;
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                var state =
                        shape == RailShape.EAST_WEST || shape == RailShape.NORTH_SOUTH
                                ? Blocks.POWERED_RAIL
                                        .defaultBlockState()
                                        .setValue(PoweredRailBlock.SHAPE, shape)
                                        .setValue(PoweredRailBlock.POWERED, true)
                                : Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
                level.setBlock(pos, state, 2);
            }
    }

    Vec3 point(double distance) {
        distance = (distance % length + length) % length;
        for (int i = cells.size() - 1; i >= 0; i--)
            if (distance >= offsets.get(i))
                return entry.get(i)
                        .lerp(
                                exit.get(i),
                                (distance - offsets.get(i)) / entry.get(i).distanceTo(exit.get(i)));
        return entry.getFirst();
    }

    double progress(Vec3 position) {
        double error = Double.POSITIVE_INFINITY, result = 0;
        for (int i = 0; i < cells.size(); i++) {
            Vec3 segment = exit.get(i).subtract(entry.get(i));
            double t =
                    Math.clamp(
                            position.subtract(entry.get(i)).dot(segment) / segment.lengthSqr(),
                            0,
                            1);
            double next = entry.get(i).add(segment.scale(t)).distanceToSqr(position);
            if (next < error) {
                error = next;
                result = offsets.get(i) + segment.length() * t;
            }
        }
        return result;
    }

    double delta(double before, double after) {
        double result = after - before;
        if (result < -length / 2) result += length;
        if (result > length / 2) result -= length;
        return result;
    }
}
