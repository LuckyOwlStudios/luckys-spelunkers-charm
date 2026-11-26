// Ramp contact tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@GameTestHolder("spelunkers_ramp_tests")
@PrefixGameTestTemplate(false)
public final class RampContactTests {
    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_0_pulled(GameTestHelper h) {
        run(h, 0, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_0_pushed(GameTestHelper h) {
        run(h, 0, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_1_pulled(GameTestHelper h) {
        run(h, 0, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_1_pushed(GameTestHelper h) {
        run(h, 0, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_2_pulled(GameTestHelper h) {
        run(h, 0, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_2_pushed(GameTestHelper h) {
        run(h, 0, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_3_pulled(GameTestHelper h) {
        run(h, 0, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_3_pushed(GameTestHelper h) {
        run(h, 0, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_0_pulled(GameTestHelper h) {
        run(h, 1, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_0_pushed(GameTestHelper h) {
        run(h, 1, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_1_pulled(GameTestHelper h) {
        run(h, 1, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_1_pushed(GameTestHelper h) {
        run(h, 1, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_2_pulled(GameTestHelper h) {
        run(h, 1, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_2_pushed(GameTestHelper h) {
        run(h, 1, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_3_pulled(GameTestHelper h) {
        run(h, 1, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_3_pushed(GameTestHelper h) {
        run(h, 1, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_0_pulled(GameTestHelper h) {
        run(h, 2, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_0_pushed(GameTestHelper h) {
        run(h, 2, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_1_pulled(GameTestHelper h) {
        run(h, 2, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_1_pushed(GameTestHelper h) {
        run(h, 2, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_2_pulled(GameTestHelper h) {
        run(h, 2, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_2_pushed(GameTestHelper h) {
        run(h, 2, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_3_pulled(GameTestHelper h) {
        run(h, 2, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_3_pushed(GameTestHelper h) {
        run(h, 2, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_0_pulled(GameTestHelper h) {
        run(h, 3, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_0_pushed(GameTestHelper h) {
        run(h, 3, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_1_pulled(GameTestHelper h) {
        run(h, 3, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_1_pushed(GameTestHelper h) {
        run(h, 3, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_2_pulled(GameTestHelper h) {
        run(h, 3, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_2_pushed(GameTestHelper h) {
        run(h, 3, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_3_pulled(GameTestHelper h) {
        run(h, 3, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_3_pushed(GameTestHelper h) {
        run(h, 3, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_0_pulled(GameTestHelper h) {
        run(h, 4, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_0_pushed(GameTestHelper h) {
        run(h, 4, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_1_pulled(GameTestHelper h) {
        run(h, 4, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_1_pushed(GameTestHelper h) {
        run(h, 4, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_2_pulled(GameTestHelper h) {
        run(h, 4, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_2_pushed(GameTestHelper h) {
        run(h, 4, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_3_pulled(GameTestHelper h) {
        run(h, 4, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_3_pushed(GameTestHelper h) {
        run(h, 4, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_0_pulled(GameTestHelper h) {
        run(h, 5, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_0_pushed(GameTestHelper h) {
        run(h, 5, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_1_pulled(GameTestHelper h) {
        run(h, 5, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_1_pushed(GameTestHelper h) {
        run(h, 5, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_2_pulled(GameTestHelper h) {
        run(h, 5, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_2_pushed(GameTestHelper h) {
        run(h, 5, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_3_pulled(GameTestHelper h) {
        run(h, 5, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_3_pushed(GameTestHelper h) {
        run(h, 5, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_0_pulled(GameTestHelper h) {
        run(h, 6, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_0_pushed(GameTestHelper h) {
        run(h, 6, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_1_pulled(GameTestHelper h) {
        run(h, 6, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_1_pushed(GameTestHelper h) {
        run(h, 6, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_2_pulled(GameTestHelper h) {
        run(h, 6, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_2_pushed(GameTestHelper h) {
        run(h, 6, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_3_pulled(GameTestHelper h) {
        run(h, 6, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_3_pushed(GameTestHelper h) {
        run(h, 6, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_0_pulled(GameTestHelper h) {
        run(h, 7, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_0_pushed(GameTestHelper h) {
        run(h, 7, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_1_pulled(GameTestHelper h) {
        run(h, 7, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_1_pushed(GameTestHelper h) {
        run(h, 7, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_2_pulled(GameTestHelper h) {
        run(h, 7, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_2_pushed(GameTestHelper h) {
        run(h, 7, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_3_pulled(GameTestHelper h) {
        run(h, 7, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_3_pushed(GameTestHelper h) {
        run(h, 7, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_0_pulled(GameTestHelper h) {
        run(h, 8, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_0_pushed(GameTestHelper h) {
        run(h, 8, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_1_pulled(GameTestHelper h) {
        run(h, 8, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_1_pushed(GameTestHelper h) {
        run(h, 8, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_2_pulled(GameTestHelper h) {
        run(h, 8, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_2_pushed(GameTestHelper h) {
        run(h, 8, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_3_pulled(GameTestHelper h) {
        run(h, 8, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_3_pushed(GameTestHelper h) {
        run(h, 8, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_0_pulled(GameTestHelper h) {
        run(h, 9, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_0_pushed(GameTestHelper h) {
        run(h, 9, 0, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_1_pulled(GameTestHelper h) {
        run(h, 9, 1, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_1_pushed(GameTestHelper h) {
        run(h, 9, 1, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_2_pulled(GameTestHelper h) {
        run(h, 9, 2, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_2_pushed(GameTestHelper h) {
        run(h, 9, 2, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_3_pulled(GameTestHelper h) {
        run(h, 9, 3, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_3_pushed(GameTestHelper h) {
        run(h, 9, 3, true, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_solo(GameTestHelper h) {
        run(h, 0, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void rising_furnace(GameTestHelper h) {
        run(h, 0, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_solo(GameTestHelper h) {
        run(h, 1, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void flat_edge_furnace(GameTestHelper h) {
        run(h, 1, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_solo(GameTestHelper h) {
        run(h, 2, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void top_runout_furnace(GameTestHelper h) {
        run(h, 2, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_solo(GameTestHelper h) {
        run(h, 3, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void steps_furnace(GameTestHelper h) {
        run(h, 3, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_solo(GameTestHelper h) {
        run(h, 4, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_approach_furnace(GameTestHelper h) {
        run(h, 4, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_solo(GameTestHelper h) {
        run(h, 5, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void curved_tip_furnace(GameTestHelper h) {
        run(h, 5, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_solo(GameTestHelper h) {
        run(h, 6, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void hairpin_tip_furnace(GameTestHelper h) {
        run(h, 6, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_solo(GameTestHelper h) {
        run(h, 7, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void support_after_furnace(GameTestHelper h) {
        run(h, 7, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_solo(GameTestHelper h) {
        run(h, 8, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void under_tip_furnace(GameTestHelper h) {
        run(h, 8, 0, false, 6, true);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_solo(GameTestHelper h) {
        run(h, 9, 0, false, 0);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void wall_after_furnace(GameTestHelper h) {
        run(h, 9, 0, false, 6, true);
    }

    static void run(GameTestHelper h, int profile, int rotation, boolean pushing, int wagons) {
        run(h, profile, rotation, pushing, wagons, false);
    }

    static void run(
            GameTestHelper h,
            int profile,
            int rotation,
            boolean pushing,
            int wagons,
            boolean furnace) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var f = new Fixture(h, profile, rotation, furnace);
        f.build();
        var carts = f.train(wagons, pushing);
        var links = MinecartCouplings.get(h.getLevel());
        int[] launch = new int[carts.size()];
        Arrays.fill(launch, -1);
        Vec3[] start = new Vec3[carts.size()], previous = new Vec3[carts.size()];
        int[] reversedTicks = new int[carts.size()];
        boolean[] second = new boolean[carts.size()];
        double[] backward = {0}, minGap = {10};
        int[] samples = {0};
        var trace =
                new StringBuilder(
                        "tick,cart,x,y,z,vx,vy,vz,air,rail,yaw,pitch,collision,drive_dot,gap\n");
        String name =
                "ramp-" + profile + "-" + rotation + "-" + pushing + "-" + wagons + "-" + furnace;
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 180) return;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        var visual = (MinecartMotionVisuals) cart;
                        var state = visual.spelunkersCharm$motion();
                        boolean air = visual.spelunkersCharm$airborne();
                        Vec3 v = cart.getDeltaMovement();
                        if (air
                                && (launch[i] < 0
                                        || profile == 10
                                                && !second[i]
                                                && cart.position()
                                                                .subtract(
                                                                        h.absoluteVec(
                                                                                new Vec3(
                                                                                        73, 6,
                                                                                        40.5)))
                                                                .dot(f.direction(1, 0))
                                                        > -.1)) {
                            if (launch[i] >= 0) second[i] = true;
                            launch[i] = (int) h.getTick();
                            start[i] = cart.position();
                            reversedTicks[i] = 0;
                        }
                        double gap = i == 0 ? 0 : cart.distanceTo(carts.get(i - 1));
                        double drive =
                                cart instanceof LocomotiveMinecart engine
                                        ? engine.driveVector().dot(f.exit)
                                        : 1;
                        trace.append(h.getTick())
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX() - f.origin.getX())
                                .append(',')
                                .append(cart.getY() - f.origin.getY())
                                .append(',')
                                .append(cart.getZ() - f.origin.getZ())
                                .append(',')
                                .append(v.x)
                                .append(',')
                                .append(v.y)
                                .append(',')
                                .append(v.z)
                                .append(',')
                                .append(air)
                                .append(',')
                                .append(cart.isOnRails())
                                .append(',')
                                .append(Math.toDegrees(state.physicalYaw))
                                .append(',')
                                .append(Math.toDegrees(state.physicalPitch))
                                .append(',')
                                .append(cart.horizontalCollision)
                                .append(',')
                                .append(drive)
                                .append(',')
                                .append(gap)
                                .append('\n');
                        if (launch[i] >= 0 && h.getTick() <= launch[i] + 6 && cart.isAlive()) {
                            double forward = v.dot(f.exit);
                            backward[0] = Math.min(backward[0], forward);
                            samples[0]++;
                            reversedTicks[i] = forward < -.025 ? reversedTicks[i] + 1 : 0;
                            boolean takingOff = cart.getY() >= start[i].y - 1;
                            if (profile != 9 && takingOff)
                                require(
                                        h,
                                        forward > -.12 && reversedTicks[i] < 3,
                                        name,
                                        trace,
                                        "Takeoff reversed cart "
                                                + i
                                                + " at tick "
                                                + h.getTick()
                                                + ", velocity="
                                                + v
                                                + ", forward="
                                                + forward);
                            if (previous[i] != null && profile != 9 && takingOff)
                                require(
                                        h,
                                        cart.position().subtract(previous[i]).dot(f.exit) > -.20,
                                        name,
                                        trace,
                                        "Takeoff pushed cart backwards " + i);
                            if (takingOff) {
                                Vec3 front =
                                        new Vec3(
                                                Math.cos(state.physicalYaw),
                                                0,
                                                Math.sin(state.physicalYaw));
                                require(
                                        h,
                                        front.dot(f.exit) * f.initialFront.get(cart.getUUID())
                                                > -.2,
                                        name,
                                        trace,
                                        "Takeoff swapped a cart's fixed front: " + i);
                            }
                            if (cart instanceof LocomotiveMinecart engine) {
                                require(
                                        h,
                                        drive > .45,
                                        name,
                                        trace,
                                        "Takeoff reversed engine command: " + drive);
                                require(
                                        h,
                                        engine.bodyDirection().dot(f.exit) * (pushing ? -1 : 1)
                                                > .45,
                                        name,
                                        trace,
                                        "Takeoff swapped the engine front");
                            }
                            if (i > 0 && carts.get(i - 1).isAlive()) {
                                minGap[0] = Math.min(minGap[0], gap);
                                require(
                                        h,
                                        gap > .95,
                                        name,
                                        trace,
                                        "Takeoff overlapped cart " + i + ": " + gap);
                                require(
                                        h,
                                        links.linked(cart.getUUID(), carts.get(i - 1).getUUID()),
                                        name,
                                        trace,
                                        "Takeoff broke coupling " + i);
                            }
                        }
                        if (profile == 9 && cart instanceof LocomotiveMinecart engine)
                            require(
                                    h,
                                    engine.driveVector().dot(f.exit) > .9,
                                    name,
                                    trace,
                                    "Wall collision reversed engine command");
                        previous[i] = cart.position();
                    }
                });
        h.runAfterDelay(
                180,
                () -> {
                    write(name, trace);
                    if (profile != 9)
                        for (int i = 0; i < carts.size(); i++)
                            h.assertTrue(
                                    launch[i] >= 0,
                                    "Cart never left ramp: "
                                            + name
                                            + ", cart="
                                            + i
                                            + ", position="
                                            + carts.get(i).position());
                    if (profile == 10)
                        for (int i = 0; i < carts.size(); i++)
                            h.assertTrue(second[i], "Cart did not complete the second ramp: " + i);
                    write(
                            name + "-checks",
                            new StringBuilder(
                                    "PASS samples="
                                            + samples[0]
                                            + " minimum_forward="
                                            + backward[0]
                                            + " minimum_gap="
                                            + minGap[0]
                                            + "\n"));
                    h.succeed();
                });
    }

    private static void require(
            GameTestHelper h, boolean condition, String name, StringBuilder trace, String message) {
        if (!condition) {
            write(name, trace);
            h.assertTrue(false, name + ": " + message);
        }
    }

    private static void write(String name, StringBuilder text) {
        try {
            Files.writeString(Path.of(name + ".csv"), text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Builds a rotated ramp, its obstacle and its connected test train. */
    private static final class Fixture {
        final GameTestHelper h;
        final BlockPos origin;
        final int profile, rotation;
        final boolean furnace;
        Vec3 exit;
        int exitX = 41, exitZ = 40, exitY = 6;
        final Map<UUID, Integer> initialFront = new HashMap<>();

        Fixture(GameTestHelper h, int profile, int rotation, boolean furnace) {
            this.h = h;
            this.origin = h.absolutePos(BlockPos.ZERO);
            this.profile = profile;
            this.rotation = rotation;
            this.furnace = furnace;
            exit = direction(1, 0);
        }

        Vec3 direction(double x, double z) {
            for (int i = 0; i < rotation; i++) {
                double previous = x;
                x = -z;
                z = previous;
            }
            return new Vec3(x, 0, z);
        }

        Vec3 point(double x, double y, double z) {
            Vec3 d = direction(x - 64, z - 64);
            return new Vec3(origin.getX() + 64 + d.x, origin.getY() + y, origin.getZ() + 64 + d.z);
        }

        BlockPos block(int x, int y, int z) {
            Vec3 p = point(x + .5, y, z + .5);
            return BlockPos.containing(p);
        }

        void rail(int x, int y, int z, RailShape shape) {
            h.getLevel().setBlock(block(x, y - 1, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            if (shape.isAscending()) {
                int dx =
                        shape == RailShape.ASCENDING_EAST
                                ? 1
                                : shape == RailShape.ASCENDING_WEST ? -1 : 0;
                int dz =
                        shape == RailShape.ASCENDING_SOUTH
                                ? 1
                                : shape == RailShape.ASCENDING_NORTH ? -1 : 0;
                h.getLevel()
                        .setBlock(
                                block(x + dx, y, z + dz),
                                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                2);
            }
            var state =
                    shape.isAscending()
                                    || shape == RailShape.EAST_WEST
                                    || shape == RailShape.NORTH_SOUTH
                            ? Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, shape)
                                    .setValue(PoweredRailBlock.POWERED, true)
                            : Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
            for (int i = 0; i < rotation; i++)
                state = state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            h.getLevel().setBlock(block(x, y, z), state, 2);
        }

        void build() {
            for (int x = 6; x <= 106; x++)
                for (int z = 28; z <= 106; z++)
                    h.getLevel().setBlock(block(x, 1, z), Blocks.STONE.defaultBlockState(), 2);
            for (int pass = 0; pass < 2; pass++) {
                int base = profile == 1 ? 6 : 2;
                for (int x = 8; x <= 36; x++) rail(x, base, 40, RailShape.EAST_WEST);
                if (profile == 1) {
                    for (int x = 37; x <= 40; x++) rail(x, 6, 40, RailShape.EAST_WEST);
                } else if (profile == 4) {
                    rail(37, 2, 40, RailShape.SOUTH_WEST);
                    rail(37, 2, 41, RailShape.NORTH_SOUTH);
                    for (int z = 42; z <= 45; z++)
                        rail(37, 2 + z - 42, z, RailShape.ASCENDING_SOUTH);
                    exit = direction(0, 1);
                    exitX = 37;
                    exitZ = 46;
                } else if (profile == 3) {
                    int y = 2;
                    for (int x = 37; x <= 42; x++) {
                        boolean up = x % 2 == 1;
                        rail(x, y, 40, up ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST);
                        if (up) y++;
                    }
                    exitX = 43;
                    exitY = 5;
                } else {
                    for (int x = 37; x <= 40; x++)
                        rail(x, 2 + x - 37, 40, RailShape.ASCENDING_EAST);
                    if (profile == 2) {
                        for (int x = 41; x <= 43; x++) rail(x, 6, 40, RailShape.EAST_WEST);
                        exitX = 44;
                    }
                    if (profile == 5) {
                        rail(41, 6, 40, RailShape.SOUTH_WEST);
                        exit = direction(0, 1);
                        exitX = 41;
                        exitZ = 41;
                    }
                    if (profile == 6) {
                        rail(41, 6, 40, RailShape.SOUTH_WEST);
                        rail(41, 6, 41, RailShape.NORTH_SOUTH);
                        rail(41, 6, 42, RailShape.NORTH_WEST);
                        rail(40, 6, 42, RailShape.EAST_WEST);
                        exit = direction(-1, 0);
                        exitX = 40;
                        exitZ = 42;
                    }
                }
            }
            if (profile == 4 || profile == 5)
                for (int z = exitZ + 2; z <= 104; z++) rail(exitX, 2, z, RailShape.NORTH_SOUTH);
            else if (profile == 6)
                for (int x = 8; x < exitX - 1; x++) rail(x, 2, exitZ, RailShape.EAST_WEST);
            else for (int x = exitX + 2; x <= 104; x++) rail(x, 2, exitZ, RailShape.EAST_WEST);
            if (profile == 10) {
                for (int x = 69; x <= 72; x++) {
                    h.getLevel().setBlock(block(x, 2, 40), Blocks.AIR.defaultBlockState(), 2);
                }
                for (int pass = 0; pass < 2; pass++)
                    for (int x = 69; x <= 72; x++)
                        rail(x, 2 + x - 69, 40, RailShape.ASCENDING_EAST);
                h.getLevel().setBlock(block(73, 2, 40), Blocks.AIR.defaultBlockState(), 2);
                h.getLevel().setBlock(block(74, 2, 40), Blocks.AIR.defaultBlockState(), 2);
            }
            if (profile == 7 || profile == 8 || profile == 9)
                h.getLevel()
                        .setBlock(
                                block(
                                        exitX,
                                        exitY + (profile == 9 ? 0 : profile == 7 ? -1 : -2),
                                        exitZ),
                                Blocks.STONE.defaultBlockState(),
                                2);
            var states = new StringBuilder();
            for (int x = 35; x <= 44; x++)
                for (int z = 40; z <= 46; z++)
                    for (int y = 2; y <= 6; y++) {
                        var p = block(x, y, z);
                        var state = h.getLevel().getBlockState(p);
                        if (state.getBlock()
                                instanceof net.minecraft.world.level.block.BaseRailBlock)
                            states.append(x)
                                    .append(',')
                                    .append(y)
                                    .append(',')
                                    .append(z)
                                    .append(':')
                                    .append(state)
                                    .append('\n');
                    }
            write("ramp-layout-" + profile + "-" + rotation, states);
        }

        ArrayList<AbstractMinecart> train(int wagons, boolean pushing) {
            var carts = new ArrayList<AbstractMinecart>();
            var links = MinecartCouplings.get(h.getLevel());
            Vec3 forward = direction(1, 0);
            for (int i = 0; i <= wagons; i++) {
                Vec3 p = point(34.4 - i * 1.55, profile == 1 ? 6.0625 : 2.0625, 40.5);
                AbstractMinecart cart;
                if (i == (pushing ? wagons : 0) && (wagons > 0 || furnace)) {
                    if (furnace) {
                        var stove =
                                new net.minecraft.world.entity.vehicle.MinecartFurnace(
                                        h.getLevel(), p.x, p.y, p.z);
                        stove.fuel = 10000;
                        stove.xPush = forward.x;
                        stove.zPush = forward.z;
                        cart = stove;
                    } else {
                        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
                        Vec3 front = forward.scale(pushing ? -1 : 1);
                        engine.setDriveDirection(Direction.getNearest(front.x, 0, front.z));
                        if (pushing) engine.switchDirection();
                        for (int slot = 0; slot < 3; slot++)
                            engine.setItem(slot, new ItemStack(Items.COAL, 8));
                        cart = engine;
                    }
                } else cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
                cart.setDeltaMovement(forward.scale(.4));
                h.getLevel().addFreshEntity(cart);
                if (i > 0)
                    h.assertTrue(
                            links.link(carts.getLast(), cart) == MinecartCouplings.Result.LINKED,
                            "Fixture did not connect train");
                carts.add(cart);
            }
            for (var cart : carts)
                initialFront.put(
                        cart.getUUID(),
                        MinecartHingeGeometry.supportedAxis(cart).dot(forward) < 0 ? -1 : 1);
            return carts;
        }
    }
}
