// Train ground braking checks.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

import java.util.*;

@GameTestHolder("spelunkers_ground_stop_tests")
@PrefixGameTestTemplate(false)
public final class GroundTrainStopTests {
    @GameTest(template = "course", timeoutTicks = 100)
    public static void unpoweredTrainStopsOnGround(GameTestHelper h) {
        ground(h, 0);
    }

    @GameTest(template = "course", timeoutTicks = 100)
    public static void locomotiveTrainStopsOnGround(GameTestHelper h) {
        ground(h, 1);
    }

    @GameTest(template = "course", timeoutTicks = 100)
    public static void furnaceTrainStopsOnGround(GameTestHelper h) {
        ground(h, 2);
    }

    private static void ground(GameTestHelper h, int engine) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var origin = h.absolutePos(BlockPos.ZERO);
        for (int x = 3; x < 50; x++)
            for (int z = 6; z <= 10; z++)
                h.getLevel().setBlock(origin.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 2);
        var carts = new ArrayList<AbstractMinecart>();
        var start = new ArrayList<Vec3>();
        for (int i = 0; i < 8; i++) {
            Vec3 p = Vec3.atLowerCornerOf(origin).add(25.5 - i * 1.55, 3, 8.5);
            AbstractMinecart cart;
            if (i == 0 && engine == 1) {
                var locomotive = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
                locomotive.setDriveDirection(net.minecraft.core.Direction.EAST);
                for (int slot = 0; slot < 3; slot++)
                    locomotive.setItem(slot, new ItemStack(Items.COAL, 8));
                cart = locomotive;
            } else if (i == 0 && engine == 2) {
                var furnace = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
                furnace.fuel = 6000;
                furnace.xPush = 1;
                cart = furnace;
            } else cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            TrainRegressionFixture.facing(cart, new Vec3(1, 0, 0));
            cart.setOnGround(true);
            cart.setDeltaMovement(.45, 0, 0);
            var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
            motion.flightLaunched = true;
            motion.flightRotationReady = true;
            motion.flightReferenceYaw = 0;
            h.getLevel().addFreshEntity(cart);
            if (i > 0)
                h.assertTrue(
                        MinecartCouplings.get(h.getLevel()).link(carts.getLast(), cart)
                                == MinecartCouplings.Result.LINKED,
                        "Ground train did not link");
            carts.add(cart);
            start.add(p);
        }
        final Vec3[][] stopped = {null};
        h.runAfterDelay(
                12,
                () -> {
                    stopped[0] = new Vec3[carts.size()];
                    TrainRegressionFixture.intact(h.getLevel(), carts);
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        h.assertTrue(cart.onGround(), "Ground train became airborne");
                        h.assertTrue(
                                cart.getDeltaMovement().horizontalDistanceSqr() == 0,
                                "Ground train retained speed after 12 ticks: "
                                        + cart.getDeltaMovement());
                        h.assertTrue(
                                cart.position().distanceTo(start.get(i)) < 1.25,
                                "Ground train slid too far: "
                                        + cart.position().distanceTo(start.get(i)));
                        stopped[0][i] = cart.position();
                    }
                });
        h.runAfterDelay(
                60,
                () -> {
                    TrainRegressionFixture.intact(h.getLevel(), carts);
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        h.assertTrue(
                                cart.getDeltaMovement().horizontalDistanceSqr() == 0,
                                "Ground train started moving again");
                        h.assertTrue(
                                cart.position().distanceTo(stopped[0][i]) < .03,
                                "Coupling forces kept the grounded train crawling: "
                                        + cart.position().distanceTo(stopped[0][i]));
                    }
                    h.succeed();
                });
    }
}
