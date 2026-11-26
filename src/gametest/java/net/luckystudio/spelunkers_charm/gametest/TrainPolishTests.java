// Train polish tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("spelunkers_train_polish_tests")
@PrefixGameTestTemplate(false)
public final class TrainPolishTests {
    private static ServerPlayer player(GameTestHelper h, boolean creative) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        return p;
    }

    private static Minecart cart(GameTestHelper h, double x, boolean rails) {
        for (int a = 2; a <= 22; a++)
            for (int z = 6; z <= 10; z++) h.setBlock(new BlockPos(a, 2, z), Blocks.STONE);
        if (rails)
            for (int a = 2; a <= 22; a++)
                h.setBlock(
                        new BlockPos(a, 3, 8),
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        var p = h.absoluteVec(new Vec3(x, rails ? 3.0625 : 3, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        TrainRegressionFixture.facing(cart, new Vec3(1, 0, 0));
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static void couple(GameTestHelper h, boolean creative, boolean carry) {
        var p = player(h, creative);
        if (carry) p.getInventory().setItem(10, new ItemStack(Items.CHAIN, 2));
        var a = cart(h, 7.5, true);
        var b = cart(h, 10.8, true);
        var tool = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) tool.getItem();
        h.assertTrue(MinecartCouplingSafety.length(a, b) > 1.9, "Range fixture was not distant");
        hook.useOnCart(tool, p, a);
        h.assertTrue(
                a.getUUID().equals(CartCouplingHook.selectedCart(tool, h.getLevel())),
                "Selection missing");
        hook.useOnCart(tool, p, b);
        h.assertTrue(
                MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                "Distant hook failed");
        h.assertTrue(
                CartCouplingHook.selectedCart(tool, h.getLevel()) == null,
                "Finished hook stayed selected");
        h.assertTrue(
                p.getInventory().getItem(10).getCount() == (carry ? (creative ? 2 : 1) : 0),
                "Incorrect chain cost");
        h.runAfterDelay(
                35,
                () -> {
                    h.assertTrue(
                            MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                            "New distant link broke before settling");
                    h.assertTrue(
                            MinecartCouplingSafety.length(a, b) < 1.4,
                            "Distant link did not settle");
                    h.succeed();
                });
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 60)
    public static void creativeCouplesAtTwoBlocksWithoutChains(GameTestHelper h) {
        couple(h, true, false);
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 60)
    public static void creativeKeepsCarriedChains(GameTestHelper h) {
        couple(h, true, true);
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 60)
    public static void survivalPaysForDistantCoupling(GameTestHelper h) {
        couple(h, false, true);
    }

    @GameTest(templateNamespace = "spelunkers_coupling_safety_tests", template = "empty")
    public static void hookRejectsMoreThanTwoBlocks(GameTestHelper h) {
        var p = player(h, true);
        var a = cart(h, 7.5, true);
        var b = cart(h, 11, true);
        var tool = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) tool.getItem();
        hook.useOnCart(tool, p, a);
        hook.useOnCart(tool, p, b);
        h.assertFalse(
                MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                "Hook exceeded reach");
        h.succeed();
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 55)
    public static void groundedCartStopsInsteadOfSliding(GameTestHelper h) {
        var c = cart(h, 7.5, false);
        var start = c.position();
        c.setOnGround(true);
        c.setDeltaMovement(.4, 0, 0);
        h.runAfterDelay(
                30,
                () -> {
                    double d = c.position().distanceTo(start);
                    h.assertTrue(
                            d > .05 && d < .9, "Ground cart slid too far or could not move: " + d);
                    h.assertTrue(
                            c.getDeltaMovement().horizontalDistance() < .003,
                            "Ground friction did not stop cart");
                    h.succeed();
                });
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 55)
    public static void pushingGroundCartTurnsItsBody(GameTestHelper h) {
        var c = cart(h, 7.5, false);
        c.setOnGround(true);
        h.onEachTick(
                () -> {
                    if (h.getTick() < 18) c.push(0, 0, .08);
                });
        h.runAfterDelay(
                20,
                () -> {
                    double yaw = ((MinecartMotionVisuals) c).spelunkersCharm$motion().visualYaw;
                    h.assertTrue(
                            yaw > 45 && yaw <= 91, "Ground body ignored sideways pushing: " + yaw);
                    h.succeed();
                });
    }

    @GameTest(template = "high", timeoutTicks = 320)
    public static void threeHangingCartsPullUnpoweredEngineAndOneCart(GameTestHelper h) {
        hanging(h, false, false, false);
    }

    @GameTest(template = "high", timeoutTicks = 320)
    public static void brakeHoldsUntilReleasedThenWeightPullsTrain(GameTestHelper h) {
        hanging(h, true, false, false);
    }

    @GameTest(template = "high", timeoutTicks = 320)
    public static void noRailsCannotAnchorBrakedEngine(GameTestHelper h) {
        hanging(h, true, true, false);
    }

    @GameTest(template = "high", timeoutTicks = 110)
    public static void CounterDriveResistsHangingWeight(GameTestHelper h) {
        hanging(h, false, false, true);
    }

    private static void hanging(
            GameTestHelper h, boolean brake, boolean removeRails, boolean drive) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var fixture = new TrainWeightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        var train = fixture.hanging(3, 2, 8, true);
        var engine = (LocomotiveMinecart) train.rails.getLast();
        var start = engine.position();
        if (drive) engine.setItem(0, new ItemStack(Items.COAL, 4));
        if (brake) MinecartHandbrake.engage(engine);
        if (removeRails)
            for (int x = 0; x <= 48; x++) h.setBlock(new BlockPos(x, 82, 8), Blocks.AIR);
        if (brake && !removeRails)
            h.runAfterDelay(
                    30,
                    () -> {
                        h.assertTrue(
                                engine.position().distanceTo(start) < .01,
                                "Brake failed to hold rails");
                        MinecartHandbrake.release(engine);
                    });
        h.runAfterDelay(
                drive ? 65 : 260,
                () -> {
                    if (drive)
                        h.assertTrue(
                                engine.getX() < start.x + .1,
                                "Counter-driven locomotive was dragged over edge");
                    else
                        h.assertTrue(
                                engine.getY() < start.y - 3,
                                "Hanging train did not pull its engine down: "
                                        + engine.position()
                                        + ", start="
                                        + start);
                    for (int i = 0; i < train.carts.size(); i++) {
                        var c = train.carts.get(i);
                        h.assertTrue(c.isAlive(), "Hanging cart was destroyed");
                        if (i > 0)
                            h.assertTrue(
                                    MinecartCouplings.get(h.getLevel())
                                            .linked(c.getUUID(), train.carts.get(i - 1).getUUID()),
                                    "Hanging load broke a normal hook");
                    }
                    h.succeed();
                });
    }

    @GameTest(
            templateNamespace = "spelunkers_coupling_safety_tests",
            template = "empty",
            timeoutTicks = 65)
    public static void removingSupportReleasesBrakeAnchor(GameTestHelper h) {
        var p = h.absoluteVec(new Vec3(7.5, 10.0625, 8.5));
        h.setBlock(new BlockPos(7, 9, 8), Blocks.STONE);
        h.setBlock(
                new BlockPos(7, 10, 8),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        var e = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        e.setDriveDirection(Direction.EAST);
        h.getLevel().addFreshEntity(e);
        h.runAfterDelay(
                3,
                () -> {
                    MinecartHandbrake.engage(e);
                    h.setBlock(new BlockPos(7, 10, 8), Blocks.AIR);
                    h.setBlock(new BlockPos(7, 9, 8), Blocks.AIR);
                });
        h.runAfterDelay(
                22,
                () -> {
                    h.assertTrue(e.getY() < p.y - 2, "Brake floated without rails");
                    h.succeed();
                });
    }
}
