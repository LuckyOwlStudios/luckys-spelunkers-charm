// Coupling safety tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.events.custom.MinecartEvents;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("spelunkers_coupling_safety_tests")
@PrefixGameTestTemplate(false)
public final class CouplingSafetyTests {
    private static Minecart cart(GameTestHelper h, double x) {
        var p = h.absoluteVec(new Vec3(x, 4, 8.5));
        var c = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(c);
        return c;
    }

    private static int drops(GameTestHelper h, Minecart cart) {
        return h
                .getLevel()
                .getEntitiesOfClass(ItemEntity.class, cart.getBoundingBox().inflate(6))
                .stream()
                .filter(e -> e.getItem().is(Items.CHAIN))
                .mapToInt(e -> e.getItem().getCount())
                .sum();
    }

    @GameTest(template = "empty")
    public static void landingSlackPropagatesWithoutBreakingFollowingLinks(GameTestHelper h) {
        for (int x = 5; x < 15; x++) {
            h.setBlock(new BlockPos(x, 3, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 4, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        var a = cart(h, 11.5);
        var b = cart(h, 10.1);
        var c = cart(h, 9.45);
        var d = cart(h, 8.9);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, b, Items.CHAIN);
        links.link(b, c, Items.CHAIN);
        links.link(c, d, Items.CHAIN);
        for (var cart : List.of(a, b)) {
            cart.setPos(cart.getX(), cart.getY() + .0625, cart.getZ());
            ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(true);
        }
        for (var cart : List.of(c, d)) {
            var v = (MinecartMotionVisuals) cart;
            v.spelunkersCharm$setAirborne(true);
            v.spelunkersCharm$motion().flightRotationReady = true;
            v.spelunkersCharm$motion().physicalPitch = -Math.toRadians(55);
        }
        c.setPos(c.getX(), c.getY() + 1.8, c.getZ());
        d.setPos(d.getX(), d.getY() + 3.25, d.getZ());
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b, c, d), links);
        h.assertTrue(
                links.linked(b.getUUID(), c.getUUID()) && links.linked(c.getUUID(), d.getUUID()),
                "Landing slack broke a normal following link");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hookRequiresConsumesAndRefundsOneChain(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        var a = cart(h, 8);
        var b = cart(h, 9.55);
        var stack = ModItems.CART_COUPLING_HOOK.toStack();
        var hook = (CartCouplingHook) stack.getItem();
        var links = MinecartCouplings.get(h.getLevel());
        hook.useOnCart(stack, player, a);
        hook.useOnCart(stack, player, b);
        h.assertFalse(
                links.linked(a.getUUID(), b.getUUID()),
                "Hook coupled carts without an inventory chain");
        player.getInventory().setItem(10, new ItemStack(Items.CHAIN, 2));
        hook.useOnCart(stack, player, b);
        h.assertTrue(links.linked(a.getUUID(), b.getUUID()), "Paid coupling failed");
        h.assertTrue(
                player.getInventory().getItem(10).getCount() == 1,
                "Coupling did not consume exactly one chain");
        hook.useOnCart(stack, player, a);
        hook.useOnCart(stack, player, b);
        h.assertFalse(links.linked(a.getUUID(), b.getUUID()), "Hook failed to uncouple");
        h.assertTrue(
                drops(h, a) == 1 && player.getInventory().getItem(10).getCount() == 1,
                "Uncoupling lost or duplicated a chain");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chainInHandCannotCoupleCarts(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHAIN, 4));
        var a = cart(h, 8);
        var b = cart(h, 9.55);
        MinecartEvents.onMinecartInteract(
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                        player, InteractionHand.MAIN_HAND, a));
        MinecartEvents.onMinecartInteract(
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                        player, InteractionHand.MAIN_HAND, b));
        h.assertFalse(
                MinecartCouplings.get(h.getLevel()).linked(a.getUUID(), b.getUUID()),
                "Bare chains still coupled carts");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void grindstoneResetsSelectedHookWithoutConsumingIt(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var a = cart(h, 8);
        var stack = ModItems.CART_COUPLING_HOOK.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ((CartCouplingHook) stack.getItem()).useOnCart(stack, player, a);
        var pos = h.absolutePos(new BlockPos(5, 3, 5));
        h.getLevel().setBlockAndUpdate(pos, Blocks.GRINDSTONE.defaultBlockState());
        player.setShiftKeyDown(true);
        var event =
                new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                        player,
                        InteractionHand.MAIN_HAND,
                        pos,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        MinecartEvents.resetHookOnGrindstone(event);
        h.assertTrue(event.isCanceled(), "Grindstone intercepted the hook reset");
        h.assertFalse(
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .hasUUID("SelectedCart"),
                "Grindstone kept selected cart");
        h.assertTrue(stack.getCount() == 1, "Reset consumed the tool");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void overstretchedHookBreaksOnceAndDropsOneChain(GameTestHelper h) {
        var a = cart(h, 8);
        var b = cart(h, 9.55);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, b, Items.CHAIN);
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        h.assertTrue(links.linked(a.getUUID(), b.getUUID()), "Normal coupling broke");
        b.setPos(a.position().add(4.5, 0, 0));
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        h.assertFalse(links.linked(a.getUUID(), b.getUUID()), "Overstretched coupling survived");
        h.assertTrue(drops(h, a) == 1, "Broken coupling did not refund exactly one chain");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void idleGroundCartsUnlinkAtOneMinute(GameTestHelper h) {
        var a = cart(h, 8);
        var b = cart(h, 9.55);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, b, Items.CHAIN);
        for (var c : List.of(a, b)) {
            c.setOnGround(true);
            c.setDeltaMovement(Vec3.ZERO);
            var s = ((MinecartMotionVisuals) c).spelunkersCharm$motion();
            s.beforePosition = c.position();
            s.groundIdleTicks = 1198;
        }
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        h.assertTrue(links.linked(a.getUUID(), b.getUUID()), "Ground link expired before a minute");
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        h.assertFalse(
                links.linked(a.getUUID(), b.getUUID()),
                "Idle ground carts stayed linked after a minute");
        h.assertTrue(drops(h, a) == 1, "Ground timeout lost or duplicated chain");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void railCartsNeverUseGroundTimeout(GameTestHelper h) {
        for (int x = 6; x < 13; x++) {
            h.setBlock(new BlockPos(x, 3, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 4, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        var a = cart(h, 8);
        var b = cart(h, 9.55);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, b, Items.CHAIN);
        for (var c : List.of(a, b)) {
            c.setPos(c.getX(), c.getY() + .0625, c.getZ());
            c.setOnGround(true);
            var v = (MinecartMotionVisuals) c;
            v.spelunkersCharm$setOnRails(true);
            v.spelunkersCharm$motion().groundIdleTicks = 1199;
        }
        MinecartCouplingSafety.tick(h.getLevel(), List.of(a, b), links);
        h.assertTrue(
                links.linked(a.getUUID(), b.getUUID()), "Parked train on rails was disconnected");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void groundedFueledLocomotiveStaysLevelAndStill(GameTestHelper h) {
        for (int x = 2; x < 15; x++)
            for (int z = 3; z < 12; z++) h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
        var pos = h.absoluteVec(new Vec3(7.5, 3, 8.5));
        var engine = new LocomotiveMinecart(h.getLevel(), pos.x, pos.y, pos.z);
        engine.setDriveDirection(Direction.EAST);
        engine.setItem(0, new ItemStack(Items.COAL, 4));
        engine.setOnGround(true);
        engine.setDeltaMovement(.4, 0, 0);
        h.getLevel().addFreshEntity(engine);
        var state = ((MinecartMotionVisuals) engine).spelunkersCharm$motion();
        state.physicalPitch = -1;
        state.flightRotationReady = true;
        h.runAfterDelay(
                35,
                () -> {
                    h.assertTrue(
                            engine.position().distanceTo(pos) < .9
                                    && engine.getDeltaMovement().horizontalDistance() < .003,
                            "Fueled locomotive kept driving on bare ground");
                    h.assertTrue(
                            Math.abs(state.physicalPitch) < .01,
                            "Grounded engine kept a falling pose");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void brakedFuelStaysUnlitAndResumesItsRemainingBurn(GameTestHelper h) {
        var p = h.absoluteVec(new Vec3(8, 4, 8));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setItem(0, new ItemStack(Items.COAL, 3));
        engine.toggleEmergencyBrake();
        engine.tick();
        h.assertTrue(
                engine.getItem(0).getCount() == 3 && engine.menuData().get(0) == 0,
                "Braked fuel started a new fire");
        engine.releaseEmergencyBrake();
        engine.tick();
        int remaining = engine.fuelRemaining(0);
        h.assertTrue(
                remaining > 0 && engine.menuData().get(0) > 0,
                "Released engine did not light its fire");
        engine.toggleEmergencyBrake();
        engine.tick();
        h.assertTrue(
                engine.fuelRemaining(0) == remaining && engine.menuData().get(0) == 0,
                "Brake did not pause and hide burning fuel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void landingUsesThePhysicalFrontInsteadOfStaleRailFacing(GameTestHelper h) {
        for (int z = 5; z < 13; z++) {
            h.setBlock(new BlockPos(8, 3, z), Blocks.STONE);
            h.setBlock(
                    new BlockPos(8, 4, z),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        }
        var c = cart(h, 8.5);
        c.setPos(c.getX(), c.getY() + .0625, c.getZ());
        var visual = (MinecartMotionVisuals) c;
        var state = visual.spelunkersCharm$motion();
        for (int sign : new int[] {-1, 1}) {
            state.visualYaw = sign > 0 ? -90 : 90;
            state.visualYawReady = true;
            state.physicalYaw = sign * Math.PI / 2;
            state.flightRotationReady = true;
            visual.spelunkersCharm$setAirborne(true);
            MinecartBodyOrientation.land(c, MinecartTrack.current(c));
            var axis = MinecartHingeGeometry.supportedAxis(c);
            h.assertTrue(axis.z * sign > .99, "Rail capture flipped the wagon's fixed front");
        }
        h.succeed();
    }
}
