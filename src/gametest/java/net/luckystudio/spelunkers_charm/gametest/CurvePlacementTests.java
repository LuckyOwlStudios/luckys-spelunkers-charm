// Curve placement tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder("spelunkers_curve_placement_tests")
@PrefixGameTestTemplate(false)
public final class CurvePlacementTests {
    private static final RailShape[] CURVES = {
        RailShape.SOUTH_EAST, RailShape.SOUTH_WEST, RailShape.NORTH_WEST, RailShape.NORTH_EAST
    };
    private static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    @GameTest(template = "course", timeoutTicks = 40)
    public static void placeLocomotiveOnEveryCurve(GameTestHelper h) {
        stationary(h, true, false);
    }

    @GameTest(template = "course", timeoutTicks = 40)
    public static void placeMinecartOnEveryCurve(GameTestHelper h) {
        stationary(h, false, false);
    }

    @GameTest(template = "course", timeoutTicks = 40)
    public static void placeCargoCartsOnEveryCurve(GameTestHelper h) {
        stationary(h, false, true);
    }

    @GameTest(template = "course", timeoutTicks = 350)
    public static void driveFromCurveClockwise(GameTestHelper h) {
        drive(h, 1);
    }

    @GameTest(template = "course", timeoutTicks = 350)
    public static void driveFromCurveCounterclockwise(GameTestHelper h) {
        drive(h, -1);
    }

    static AbstractMinecart place(
            ServerLevel level, ServerPlayer player, BlockPos pos, Item item, Direction facing) {
        player.setYRot(facing.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        var before = new HashSet<Integer>();
        for (var c : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(2)))
            before.add(c.getId());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (!item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction())
            throw new AssertionError("Item placement failed");
        return level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(2)).stream()
                .filter(c -> !before.contains(c.getId()))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("Placed cart missing: " + item + " at " + pos));
    }

    static void aligned(AbstractMinecart cart) {
        var track = MinecartTrack.current(cart);
        if (track == null) throw new AssertionError("Placed cart has no rail");
        if (track.point(track.offset(cart.position())).distanceToSqr(cart.position()) > 1e-8)
            throw new AssertionError("Item spawned beside curved rail: " + cart.position());
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        double yaw = cart instanceof LocomotiveMinecart e ? e.bodyYaw() : state.visualYaw;
        Vec3 front = MinecartHingeGeometry.axis(Math.toRadians(yaw), 0);
        if (Math.abs(front.dot(track.tangent())) < .999)
            throw new AssertionError("Placed body was not aligned with curve: " + yaw);
        if (!MinecartTrainBrakes.onRail(cart) || state.airborne)
            throw new AssertionError("New cart was treated as falling");
        if (cart instanceof LocomotiveMinecart e
                && (e.bodyDirection().dot(e.driveVector()) < .999 || e.emergencyBrake()))
            throw new AssertionError("Placed engine drive disagrees with its front");
    }

    private static void stationary(GameTestHelper h, boolean engine, boolean cargo) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        var carts = new ArrayList<AbstractMinecart>();
        var starts = new ArrayList<Vec3>();
        Item[] variants = {
            Items.CHEST_MINECART, Items.HOPPER_MINECART, Items.FURNACE_MINECART, Items.TNT_MINECART
        };
        for (int shape = 0; shape < 4; shape++)
            for (int direction = 0; direction < 4; direction++) {
                var p = h.absolutePos(new BlockPos(5 + shape * 6, 3, 5 + direction * 6));
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        h.getLevel()
                                .setBlock(p.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                h.getLevel()
                        .setBlock(
                                p,
                                Blocks.RAIL
                                        .defaultBlockState()
                                        .setValue(RailBlock.SHAPE, CURVES[shape]),
                                2);
                h.getLevel()
                        .setBlock(
                                p,
                                Blocks.RAIL
                                        .defaultBlockState()
                                        .setValue(RailBlock.SHAPE, CURVES[shape]),
                                2);
                var cart =
                        place(
                                h.getLevel(),
                                player,
                                p,
                                engine
                                        ? ModItems.LOCOMOTIVE.get()
                                        : cargo ? variants[direction] : Items.MINECART,
                                DIRECTIONS[direction]);
                aligned(cart);
                if (engine)
                    h.assertTrue(
                            ((LocomotiveMinecart) cart)
                                            .bodyDirection()
                                            .dot(
                                                    Vec3.atLowerCornerOf(
                                                            DIRECTIONS[direction].getNormal()))
                                    > .6,
                            "Engine ignored the selected placement direction");
                carts.add(cart);
                starts.add(cart.position());
            }
        h.runAfterDelay(
                15,
                () -> {
                    for (int i = 0; i < carts.size(); i++) {
                        aligned(carts.get(i));
                        h.assertTrue(
                                carts.get(i).position().distanceTo(starts.get(i)) < .001,
                                "Stationary placed cart jumped or drifted");
                    }
                    h.succeed();
                });
    }

    private static void drive(GameTestHelper h, int sign) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var route = new PassengerTrainRoute(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        route.build();
        double at = 0;
        BlockPos pos = null;
        Vec3 along = null;
        for (double d = .25; d < route.length; d += .25) {
            var p = route.point(d);
            var state = h.getLevel().getBlockState(BlockPos.containing(p));
            if (state.getBlock() == Blocks.RAIL) {
                pos = BlockPos.containing(p);
                along = route.point(d + .01).subtract(route.point(d - .01)).normalize().scale(sign);
                at = d;
                break;
            }
        }
        if (pos == null) throw new AssertionError("Curve fixture missing");
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        var engine =
                (LocomotiveMinecart)
                        place(
                                h.getLevel(),
                                player,
                                pos,
                                ModItems.LOCOMOTIVE.get(),
                                Direction.getNearest(along.x, 0, along.z));
        aligned(engine);
        for (int i = 0; i < 3; i++) engine.setItem(i, new ItemStack(Items.COAL, 8));
        double[] previous = {route.progress(engine.position())}, distance = {0};
        int[] curves = {0};
        boolean[] done = {false};
        h.onEachTick(
                () -> {
                    if (done[0]) return;
                    try {
                        var p = engine.position();
                        double progress = route.progress(p);
                        distance[0] += route.delta(previous[0], progress) * sign;
                        previous[0] = progress;
                        h.assertFalse(
                                engine.emergencyBrake() || engine.isReversed(),
                                "Curve placement changed drive mode");
                        if (MinecartTrainBrakes.onRail(engine)) {
                            var track = MinecartTrack.current(engine);
                            if (track.first().direction().getAxis()
                                    != track.second().direction().getAxis()) curves[0]++;
                            h.assertTrue(
                                    engine.bodyDirection()
                                                    .dot(
                                                            MinecartHingeGeometry.supportedAxis(
                                                                    engine))
                                            > .999,
                                    "Curve turned engine body away from rails");
                            if (engine.getDeltaMovement().horizontalDistance() > .03)
                                h.assertTrue(
                                        engine.driveVector()
                                                        .dot(engine.getDeltaMovement().normalize())
                                                > .999,
                                        "Curve opposed the selected engine drive");
                        }
                        if (h.getTick() >= 260) {
                            h.assertTrue(
                                    distance[0] > 60 && curves[0] > 6,
                                    "Engine placed on a curve stalled: " + distance[0]);
                            done[0] = true;
                            h.succeed();
                        }
                    } catch (Throwable e) {
                        done[0] = true;
                        h.fail(e.toString());
                    }
                });
    }
}
