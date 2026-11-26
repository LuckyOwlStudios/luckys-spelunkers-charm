// Train stop spacing tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainStopSpacingTests {
    @GameTest(template = "rail_large", timeoutTicks = 350)
    public static void thirtyTwoPulledWagonsKeepEvenGapsAfterFastCurveStop(GameTestHelper h) {
        stop(h, 32, false, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 350)
    public static void thirtyTwoPushedWagonsKeepEvenGapsAfterFastCurveStop(GameTestHelper h) {
        stop(h, 32, true, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 350)
    public static void furnaceTrainKeepsEvenGapsAfterFastCurveStop(GameTestHelper h) {
        stop(h, 20, false, true);
    }

    private static void stop(GameTestHelper h, int count, boolean pushing, boolean furnace) {
        var route = new TrainPushTests.Route(h);
        route.build();
        Vec3 p = route.point(10);
        AbstractMinecart engine;
        if (furnace) {
            var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
            cart.fuel = 6000;
            cart.xPush = 1;
            engine = cart;
        } else {
            var cart = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
            cart.setDriveDirection(pushing ? Direction.WEST : Direction.EAST);
            if (pushing) cart.switchDirection();
            for (int slot = 0; slot < 3; slot++) cart.setItem(slot, new ItemStack(Items.COAL, 4));
            engine = cart;
        }
        h.getLevel().addFreshEntity(engine);
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(engine);
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 1; i <= count; i++) {
            Vec3 at = route.point(10 + (pushing ? 1 : -1) * i * 1.55);
            var cart = new Minecart(h.getLevel(), at.x, at.y, at.z);
            h.getLevel().addFreshEntity(cart);
            links.link(carts.getLast(), cart);
            carts.add(cart);
        }
        var previous = new ArrayList<Vec3>();
        for (var cart : carts) previous.add(cart.position());
        List<Vec3> parked = new ArrayList<>();
        h.runAfterDelay(180, () -> MinecartHandbrake.engage(engine));
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    if (tick >= 270) return;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        h.assertTrue(
                                cart.position().distanceTo(previous.get(i)) < .75,
                                "Brake spacing correction teleported wagon " + i);
                        previous.set(i, cart.position());
                        if (i > 0) {
                            var gap = MinecartRailPath.between(carts.get(i - 1), cart);
                            if (tick >= 120)
                                h.assertTrue(
                                        gap != null
                                                && gap.distance()
                                                        > (tick >= 180 && tick < 245 ? 1.28 : 1.05)
                                                && gap.distance() < 2.4,
                                        "Invalid curve clearance: tick="
                                                + tick
                                                + ", wagon="
                                                + i
                                                + ", gap="
                                                + gap
                                                + ", first="
                                                + carts.get(i - 1).position()
                                                + ", second="
                                                + cart.position());
                            if (tick >= 196 && tick < 245)
                                h.assertTrue(
                                        Math.abs(gap.distance() - 1.55) < .025,
                                        "Stopped curve coupling stretched: wagon="
                                                + i
                                                + ", tick="
                                                + tick
                                                + ", gap="
                                                + gap.distance());
                        }
                    }
                    if (tick == 200) for (var cart : carts) parked.add(cart.position());
                    if (tick > 200 && tick < 245)
                        for (int i = 0; i < carts.size(); i++)
                            h.assertTrue(
                                    carts.get(i).position().distanceTo(parked.get(i)) < .002,
                                    "Stopped train crept after spacing settled");
                });
        h.runAfterDelay(245, () -> MinecartHandbrake.release(engine));
        h.runAfterDelay(
                270,
                () -> {
                    h.assertTrue(
                            engine.getDeltaMovement().horizontalDistance() > .08,
                            "Aligned train failed to restart");
                    h.succeed();
                });
    }
}
