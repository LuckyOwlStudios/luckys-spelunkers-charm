// Optimization regression checks.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder("spelunkers_cleanup_tests")
@PrefixGameTestTemplate(false)
public final class OptimizationTests {
    @GameTest(template = "course")
    public static void sharedGeometryMatchesOriginalMath(GameTestHelper h) {
        var random = new Random(17);
        for (var shape : net.minecraft.world.level.block.state.properties.RailShape.values()) {
            var pos = h.absolutePos(new BlockPos(8 + shape.ordinal() * 3, 3, 8));
            h.getLevel()
                    .setBlock(
                            pos.below(),
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                            2);
            h.getLevel()
                    .setBlock(
                            pos,
                            net.minecraft.world.level.block.Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(
                                            net.minecraft.world.level.block.RailBlock.SHAPE, shape),
                            2);
            var cart =
                    new net.minecraft.world.entity.vehicle.Minecart(
                            h.getLevel(), pos.getX() + .5, pos.getY() + .0625, pos.getZ() + .5);
            var track =
                    net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartTrack.at(
                            cart, pos);
            var a = endpoint(pos, track.first());
            var b = endpoint(pos, track.second());
            var delta = b.subtract(a);
            var tangent = new net.minecraft.world.phys.Vec3(delta.x, 0, delta.z).normalize();
            sameBits(h, tangent.x, track.tangent().x, shape + " tangent x");
            sameBits(h, tangent.z, track.tangent().z, shape + " tangent z");
            sameBits(h, delta.horizontalDistance(), track.length(), shape + " length");
            for (int i = 0; i < 1000; i++) {
                var point =
                        a.add(
                                random.nextDouble() * 4 - 2,
                                random.nextDouble() * 4 - 2,
                                random.nextDouble() * 4 - 2);
                double expected =
                        net.minecraft.util.Mth.clamp(
                                point.subtract(a).dot(tangent), 0, delta.horizontalDistance());
                sameBits(h, expected, track.offset(point), shape + " offset");
            }
        }
        h.succeed();
    }

    private static net.minecraft.world.phys.Vec3 endpoint(
            BlockPos pos,
            net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartTrack.Port port) {
        return new net.minecraft.world.phys.Vec3(
                pos.getX() + .5 + port.direction().getStepX() * .5,
                pos.getY() + .0625 + port.height(),
                pos.getZ() + .5 + port.direction().getStepZ() * .5);
    }

    private static void sameBits(GameTestHelper h, double expected, double actual, String label) {
        h.assertTrue(
                Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual),
                label + ": " + expected + " != " + actual);
    }

    @GameTest(template = "course")
    public static void linkSnapshotsSurviveChangesAndReload(GameTestHelper h) {
        var data = new MinecartCouplings();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        data.link(a, b);
        List<UUID> snapshot = data.neighbors(b);
        data.link(b, c);
        h.assertTrue(snapshot.equals(List.of(a)), "Existing neighbor snapshot changed");
        h.assertTrue(data.neighbors(b).equals(List.of(a, c)), "New link missing from neighbors");
        data.unlink(a, b);
        h.assertTrue(
                data.neighbors(a).isEmpty() && data.neighbors(b).equals(List.of(c)),
                "Detached neighbor remained cached");
        var loaded =
                MinecartCouplings.load(
                        data.save(new CompoundTag(), h.getLevel().registryAccess()),
                        h.getLevel().registryAccess());
        h.assertTrue(
                loaded.linked(b, c) && loaded.neighbors(c).equals(List.of(b)),
                "Reload lost neighbor snapshots");
        loaded.unlink(b, c);
        h.assertTrue(
                loaded.neighbors(b).isEmpty() && loaded.neighbors(c).isEmpty(),
                "Unlink after reload left stale neighbors");
        h.succeed();
    }

    @GameTest(template = "course")
    public static void liftGuidesFollowMovesAndRotation(GameTestHelper h) {
        for (var type : AbstractLift.Type.values()) {
            var p = h.absolutePos(new BlockPos(30, 5, 30));
            var lift =
                    AbstractLift.createLift(
                            h.getLevel(),
                            p.getX() + .5,
                            p.getY(),
                            p.getZ() + .5,
                            type,
                            ItemStack.EMPTY,
                            null);
            for (int move = 0; move < 3; move++) {
                lift.setPos(p.getX() + .5 + move * 10, p.getY() + move * .75, p.getZ() + .5);
                for (var facing : Direction.Plane.HORIZONTAL) {
                    lift.setYRot(facing.toYRot());
                    var center = lift.blockPosition();
                    var guides = lift.getGuidePositions();
                    h.assertTrue(
                            guides.size()
                                    == (type == AbstractLift.Type.SMALL
                                            ? 2
                                            : type == AbstractLift.Type.MEDIUM ? 4 : 8),
                            "Lift guide count changed");
                    for (var guide : guides) {
                        int dx = guide.getX() - center.getX(), dz = guide.getZ() - center.getZ();
                        int along = Math.abs(dx * facing.getStepX() + dz * facing.getStepZ());
                        int across = Math.abs(dx * facing.getStepZ() - dz * facing.getStepX());
                        boolean valid =
                                switch (type) {
                                    case SMALL -> along == 0 && across == 2;
                                    case MEDIUM -> along == 1 && across == 3;
                                    case LARGE ->
                                            along == 4 && across == 2 || along == 2 && across == 4;
                                };
                        h.assertTrue(
                                valid && guide.getY() == center.getY(),
                                "Lift kept guide positions from before a move or rotation");
                    }
                }
            }
        }
        h.succeed();
    }
}
