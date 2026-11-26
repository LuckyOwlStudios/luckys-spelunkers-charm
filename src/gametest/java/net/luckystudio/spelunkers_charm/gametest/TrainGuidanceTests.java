// Train guidance tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartCoupler;
import net.luckystudio.spelunkers_charm.events.custom.MinecartEvents;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder("spelunkers_guidance_tests")
@PrefixGameTestTemplate(false)
public final class TrainGuidanceTests {
    @GameTest(template = "high_fall", timeoutTicks = 650)
    public static void longAirborneTailSettlesBehindLandingBrakedEngine(GameTestHelper h) {
        TrainHighFallTests.midairEngineBrakeDoesNotSuspendTwentyWagons(h);
    }

    @GameTest(template = "high_fall", timeoutTicks = 650)
    public static void fallingTrainPullsTheSupportedTailOverTheCliff(GameTestHelper h) {
        TrainHighFallTests.twentyFourWagonsLeaveEightyBlockCliffAndLand(h);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longTrainStartsUphillEast(GameTestHelper h) {
        hill(h, 0, false, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longTrainStartsUphillSouth(GameTestHelper h) {
        hill(h, 1, false, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longTrainStartsUphillWest(GameTestHelper h) {
        hill(h, 2, false, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longTrainStartsUphillNorth(GameTestHelper h) {
        hill(h, 3, false, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longPushedTrainStartsUphillEast(GameTestHelper h) {
        hill(h, 0, true, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longPushedTrainStartsUphillSouth(GameTestHelper h) {
        hill(h, 1, true, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longPushedTrainStartsUphillWest(GameTestHelper h) {
        hill(h, 2, true, 32);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void longPushedTrainStartsUphillNorth(GameTestHelper h) {
        hill(h, 3, true, 32);
    }

    private static Vec3 rotate(Vec3 p, int rotation) {
        for (int i = 0; i < rotation; i++) p = new Vec3(128 - p.z, p.y, p.x);
        return p;
    }

    private static void hill(GameTestHelper h, int rotation, boolean pushing, int wagons) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        for (int pass = 0; pass < 2; pass++)
            for (int x = 8; x <= 119; x++) {
                int y = 2 + Math.clamp(x - 75, 0, 18);
                var p = BlockPos.containing(rotate(new Vec3(x + .5, y, 40.5), rotation));
                h.getLevel()
                        .setBlock(h.absolutePos(p.below()), Blocks.STONE.defaultBlockState(), 2);
                if (x >= 75 && x < 93)
                    h.getLevel()
                            .setBlock(
                                    h.absolutePos(
                                            BlockPos.containing(
                                                    rotate(new Vec3(x + 1.5, y, 40.5), rotation))),
                                    Blocks.STONE.defaultBlockState(),
                                    2);
                var state =
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(
                                        RailBlock.SHAPE,
                                        x >= 75 && x < 93
                                                ? RailShape.ASCENDING_EAST
                                                : RailShape.EAST_WEST);
                for (int i = 0; i < rotation; i++)
                    state = state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
                h.getLevel().setBlock(h.absolutePos(p), state, 2);
            }
        Vec3 heading =
                rotate(new Vec3(65, 0, 64), rotation)
                        .subtract(rotate(new Vec3(64, 0, 64), rotation));
        var train = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 0; i <= wagons; i++) {
            double x = 75.2 - i * 1.55;
            Vec3 p =
                    h.absoluteVec(
                            rotate(new Vec3(x, 2.0625 + Math.max(0, x - 75), 40.5), rotation));
            AbstractMinecart cart;
            if (i == (pushing ? wagons : 0)) {
                var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
                Direction direction = Direction.getNearest(heading.x, 0, heading.z);
                engine.setDriveDirection(pushing ? direction.getOpposite() : direction);
                if (pushing) engine.switchDirection();
                engine.setItem(0, new ItemStack(Items.COAL, 16));
                cart = engine;
            } else cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            h.getLevel().addFreshEntity(cart);
            cart.setDeltaMovement(Vec3.ZERO);
            if (i > 0)
                h.assertTrue(
                        links.link(train.getLast(), cart) == MinecartCouplings.Result.LINKED,
                        "Hill train did not link");
            train.add(cart);
        }
        Vec3 start = train.getFirst().position();
        h.onEachTick(
                () -> {
                    if (h.getTick() > 5 && h.getTick() < 160)
                        for (var c : train)
                            h.assertTrue(
                                    c.getDeltaMovement().dot(heading) > -.025,
                                    "Powered train rolled backwards uphill");
                });
        h.runAfterDelay(
                160,
                () -> {
                    h.assertTrue(
                            train.getFirst().position().subtract(start).dot(heading) > 20,
                            "Long train could not start uphill: " + train.getFirst().position());
                    h.assertTrue(
                            train.getFirst().getY() > start.y + 16,
                            "Long train did not reach the top");
                    h.succeed();
                });
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void staleHookSelectionClearsAndCanBeSelectedAgain(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var a = cart(h, 10);
        var b = cart(h, 11.55);
        var stack = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) stack.getItem();
        hook.useOnCart(stack, player, a);
        hook.useOnCart(stack, player, a);
        h.assertFalse(
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .hasUUID("SelectedCart"),
                "Same-cart click did not cancel");
        hook.useOnCart(stack, player, a);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putUUID("SelectedSession", UUID.randomUUID()));
        hook.inventoryTick(stack, h.getLevel(), player, 0, true);
        h.assertFalse(
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .hasUUID("SelectedCart"),
                "Previous world selection survived");
        hook.useOnCart(stack, player, a);
        hook.useOnCart(stack, player, b);
        h.assertTrue(
                MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                "Cleared hook could not link again");
        h.succeed();
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void shiftAirClickAndLogoutClearBothCouplingTools(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var a = cart(h, 10);
        var b = cart(h, 11.55);
        var stack = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        hook.useOnCart(stack, player, a);
        player.setShiftKeyDown(true);
        MinecartEvents.cancelCoupling(
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(
                        player, InteractionHand.MAIN_HAND));
        h.assertFalse(
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .hasUUID("SelectedCart"),
                "Air click retained selection");
        player.setShiftKeyDown(false);
        CartCoupler.useOnCart(player, a, new ItemStack(Items.CHAIN, 4));
        MinecartEvents.clearCouplingOnLogout(
                new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(
                        player));
        CartCoupler.useOnCart(player, b, new ItemStack(Items.CHAIN, 4));
        h.assertFalse(
                MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                "Chain reused a selection from the previous login");
        h.succeed();
    }

    @GameTest(template = "matrix", timeoutTicks = 40)
    public static void savedCartKeepsBodyFacingAndHookEnds(GameTestHelper h) {
        var a = cart(h, 10);
        var b = cart(h, 11.55);
        MinecartCouplings.get(h.getLevel()).link(a, b);
        var motion = ((MinecartMotionVisuals) a).spelunkersCharm$motion();
        motion.visualYaw = motion.visualYawOld = 180;
        motion.visualYawReady = true;
        int side = MinecartHingeGeometry.side(a, b);
        var saved = new net.minecraft.nbt.CompoundTag();
        a.saveWithoutId(saved);
        var loaded = new Minecart(h.getLevel(), 0, 0, 0);
        loaded.load(saved);
        var state = ((MinecartMotionVisuals) loaded).spelunkersCharm$motion();
        h.assertTrue(
                state.visualYawReady && Math.abs(state.visualYaw - 180) < .01,
                "World reload flipped the body");
        h.assertTrue(
                state.hingeSides.get(b.getUUID()) == side, "World reload changed a fixed hook");
        for (int x = 8; x <= 14; x++) {
            h.setBlock(new BlockPos(x, 3, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 4, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        a.setPos(a.getX(), h.absolutePos(BlockPos.ZERO).getY() + 4.0625, a.getZ());
        b.setPos(b.getX(), a.getY(), b.getZ());
        a.saveWithoutId(saved);
        saved.remove("SpelunkersBodyYaw");
        var legacy = new Minecart(h.getLevel(), 0, 0, 0);
        legacy.load(saved);
        h.assertTrue(
                MinecartHingeGeometry.supportedAxis(legacy).x < -.99,
                "Legacy save did not recover its front from the saved hook");
        h.succeed();
    }

    private static Minecart cart(GameTestHelper h, double x) {
        Vec3 p = h.absoluteVec(new Vec3(x, 4, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    @GameTest(template = "flight", timeoutTicks = 310)
    public static void twelveWagonsFollowOneCurveWithoutReversing(GameTestHelper h) {
        flight(h, 12, false);
    }

    @GameTest(template = "flight", timeoutTicks = 310)
    public static void pushedWagonsFollowOneCurveWithoutReversing(GameTestHelper h) {
        flight(h, 8, true);
    }

    private static void flight(GameTestHelper h, int wagons, boolean pushing) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        TrainFlightOrderTests.tallRamp(h, wagons, pushing);
        var history = new ArrayList<Vec3>();
        var launched = new HashSet<UUID>();
        h.onEachTick(
                () -> {
                    var box =
                            new net.minecraft.world.phys.AABB(
                                    h.absoluteVec(Vec3.ZERO), h.absoluteVec(new Vec3(128, 64, 24)));
                    var carts = h.getLevel().getEntitiesOfClass(AbstractMinecart.class, box);
                    if (carts.isEmpty()) return;
                    var front =
                            carts.stream()
                                    .max(Comparator.comparingDouble(AbstractMinecart::getX))
                                    .orElseThrow();
                    history.add(front.position());
                    for (var cart : carts)
                        if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne()
                                && cart.getY() > h.absolutePos(BlockPos.ZERO).getY() + 2.4) {
                            h.assertTrue(
                                    cart.getDeltaMovement().x > -.025,
                                    "A cart reversed during its flight at tick "
                                            + h.getTick()
                                            + ", position="
                                            + cart.position()
                                            + ", velocity="
                                            + cart.getDeltaMovement());
                            if (!launched.add(cart.getUUID()) && cart != front) {
                                double error = Double.POSITIVE_INFINITY;
                                for (int i = 1; i < history.size(); i++) {
                                    Vec3 a = history.get(i - 1), line = history.get(i).subtract(a);
                                    double t =
                                            Math.clamp(
                                                    cart.position().subtract(a).dot(line)
                                                            / Math.max(1e-8, line.lengthSqr()),
                                                    0,
                                                    1);
                                    error =
                                            Math.min(
                                                    error,
                                                    cart.position()
                                                            .distanceTo(a.add(line.scale(t))));
                                }
                                h.assertTrue(
                                        error < .55,
                                        "A follower left the shared parabola: " + error);
                            }
                        }
                });
    }
}
