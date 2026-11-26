// Rail exit braking checks.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder("spelunkers_rail_exit_tests")
@PrefixGameTestTemplate(false)
public final class RailExitStopTests {
    @GameTest(template = "course", timeoutTicks = 190)
    public static void soloLocomotiveLeavesRailsAndStops(GameTestHelper h) {
        exit(h, 1, 0);
    }

    @GameTest(template = "course", timeoutTicks = 190)
    public static void locomotiveTrainLeavesRailsAndStops(GameTestHelper h) {
        exit(h, 1, 7);
    }

    @GameTest(template = "course", timeoutTicks = 190)
    public static void furnaceTrainLeavesRailsAndStops(GameTestHelper h) {
        exit(h, 2, 7);
    }

    @GameTest(template = "course", timeoutTicks = 190)
    public static void soloFurnaceLeavesRailsAndStops(GameTestHelper h) {
        exit(h, 2, 0);
    }

    @GameTest(template = "course", timeoutTicks = 190)
    public static void unpoweredTrainLeavesRailsAndStops(GameTestHelper h) {
        exit(h, 0, 7);
    }

    static ArrayList<AbstractMinecart> spawn(
            net.minecraft.server.level.ServerLevel level, BlockPos origin, int kind, int wagons) {
        for (int x = 1; x <= 65; x++)
            for (int z = 6; z <= 10; z++)
                level.setBlock(origin.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 2);
        for (int x = 2; x <= 22; x++) {
            var p = origin.offset(x, 3, 8);
            level.setBlock(p.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            level.setBlock(
                    p,
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                            .setValue(PoweredRailBlock.POWERED, true),
                    2);
        }
        var carts = new ArrayList<AbstractMinecart>();
        for (int i = 0; i <= wagons; i++) {
            Vec3 p = Vec3.atLowerCornerOf(origin).add(20.5 - i * 1.55, 3.0625, 8.5);
            AbstractMinecart cart;
            if (i == 0 && kind == 1) {
                var engine = new LocomotiveMinecart(level, p.x, p.y, p.z);
                engine.setDriveDirection(Direction.EAST);
                for (int slot = 0; slot < 3; slot++)
                    engine.setItem(slot, new ItemStack(Items.COAL, 16));
                cart = engine;
            } else if (i == 0 && kind == 2) {
                var engine = new MinecartFurnace(level, p.x, p.y, p.z);
                engine.fuel = 6000;
                engine.xPush = 1;
                cart = engine;
            } else cart = new Minecart(level, p.x, p.y, p.z);
            TrainRegressionFixture.facing(cart, new Vec3(1, 0, 0));
            MinecartRailPlacement.align(cart);
            cart.setDeltaMovement(.4, 0, 0);
            level.addFreshEntity(cart);
            if (i > 0
                    && MinecartCouplings.get(level).link(carts.getLast(), cart)
                            != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Exit train did not link");
            carts.add(cart);
        }
        return carts;
    }

    private static void exit(GameTestHelper h, int kind, int wagons) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var level = h.getLevel();
        var origin = h.absolutePos(BlockPos.ZERO);
        var carts = spawn(level, origin, kind, wagons);
        var head = carts.getFirst();
        final long[] landed = {-1};
        final Vec3[] contact = {null}, stopped = {null};
        final boolean[] done = {false};
        h.onEachTick(
                () -> {
                    if (done[0]) return;
                    if (landed[0] < 0 && head.onGround() && !MinecartTrainBrakes.onRail(head)) {
                        landed[0] = h.getTick();
                        contact[0] = head.position();
                    }
                    if (landed[0] >= 0 && h.getTick() == landed[0] + 20) {
                        h.assertTrue(
                                head.getDeltaMovement().horizontalDistanceSqr() == 0,
                                "Engine kept driving after exiting rails: "
                                        + head.getDeltaMovement()
                                        + ", distance="
                                        + head.position().distanceTo(contact[0])
                                        + ", carts="
                                        + carts.stream()
                                                .map(
                                                        c -> {
                                                            var m =
                                                                    ((MinecartMotionVisuals) c)
                                                                            .spelunkersCharm$motion();
                                                            var t = MinecartTrack.current(c);
                                                            return "p="
                                                                    + c.position()
                                                                            .subtract(
                                                                                    Vec3
                                                                                            .atLowerCornerOf(
                                                                                                    origin))
                                                                    + ",v="
                                                                    + c.getDeltaMovement()
                                                                    + ",ground="
                                                                    + c.onGround()
                                                                    + ",rail="
                                                                    + MinecartTrainBrakes.onRail(c)
                                                                    + ",air="
                                                                    + m.airborne
                                                                    + ",ticks="
                                                                    + m.groundContactTicks
                                                                    + ",track="
                                                                    + (t == null
                                                                            ? null
                                                                            : t.pos()
                                                                                    .subtract(
                                                                                            origin));
                                                        })
                                                .toList());
                        h.assertTrue(
                                head.position().distanceTo(contact[0]) < 1.5,
                                "Engine drove too far over bare ground: "
                                        + head.position().distanceTo(contact[0]));
                        stopped[0] = head.position();
                    }
                    if (landed[0] >= 0 && h.getTick() == landed[0] + 70) {
                        TrainRegressionFixture.intact(level, carts);
                        h.assertTrue(
                                head.getDeltaMovement().horizontalDistanceSqr() == 0,
                                "Ground engine regained speed");
                        h.assertTrue(
                                head.position().distanceTo(stopped[0]) < .05,
                                "Train kept pushing the engine over ground: "
                                        + head.position().distanceTo(stopped[0]));
                        done[0] = true;
                        h.succeed();
                    }
                    if (h.getTick() > 170)
                        h.assertTrue(landed[0] >= 0, "Engine never reached bare ground");
                });
    }
}
