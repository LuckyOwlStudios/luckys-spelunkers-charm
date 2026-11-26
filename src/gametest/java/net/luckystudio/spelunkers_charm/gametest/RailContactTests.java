// Rail contact tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class RailContactTests {
    @GameTest(template = "rail_large", timeoutTicks = 95)
    public static void locomotiveBreaksWoodenTrackWithoutPositionOrSpeedJump(GameTestHelper h) {
        lane(h, 8, powered());
        for (int x = 12; x < 50; x++)
            rail(
                    h,
                    x,
                    8,
                    ModBlocks.WOODEN_RAIL
                            .get()
                            .defaultBlockState()
                            .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        var engine = engine(h, 10.5, 8.5);
        engine.setDeltaMovement(0.5, 0, 0);
        Vec3[] previous = {engine.position()};
        double[] speed = {0.5};
        h.onEachTick(
                () -> {
                    Vec3 now = engine.position();
                    double movement = now.subtract(previous[0]).horizontalDistance();
                    if (h.getTick() > 0) {
                        h.assertTrue(
                                movement < 0.56, "Wood rail caused a position jump: " + movement);
                        h.assertTrue(
                                speed[0] - engine.getDeltaMovement().horizontalDistance() < 0.08,
                                "Wood rail abruptly cut speed");
                        if (h.getTick() < 30)
                            h.assertTrue(
                                    movement > 0.20, "Wood rail dropped part of the movement tick");
                    }
                    previous[0] = now;
                    speed[0] = engine.getDeltaMovement().horizontalDistance();
                    h.assertTrue(
                            ((MinecartMotionVisuals) engine).spelunkersCharm$braking() == 0,
                            "Breaking wood rail produced braking effects");
                });
        h.runAfterDelay(
                55,
                () -> {
                    h.assertTrue(
                            engine.getX() > h.absoluteVec(new Vec3(25, 2, 8)).x,
                            "Engine stalled while breaking wood rails");
                    h.assertTrue(
                            h.getBlockState(new BlockPos(13, 2, 8)).isAir(),
                            "Wood rail no longer breaks under locomotive");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 75)
    public static void fastEngineCoastsAcrossSingleRailGapWithoutLosingTick(GameTestHelper h) {
        lane(h, 8, powered());
        h.setBlock(new BlockPos(14, 2, 8), Blocks.AIR);
        var engine = engine(h, 11.5, 8.5);
        engine.setDeltaMovement(0.5, 0, 0);
        Vec3[] previous = {engine.position()};
        h.onEachTick(
                () -> {
                    double movement = engine.position().subtract(previous[0]).horizontalDistance();
                    if (h.getTick() > 0 && h.getTick() < 25) {
                        h.assertTrue(
                                movement > 0.39 && movement < 0.56,
                                "Gap stalled or teleported engine: " + movement);
                        h.assertTrue(
                                engine.getDeltaMovement().horizontalDistance() > 0.39,
                                "Gap applied vanilla off-rail speed clamp");
                    }
                    h.assertTrue(
                            ((MinecartMotionVisuals) engine).spelunkersCharm$braking() == 0,
                            "Coasting across gap produced sparks");
                    previous[0] = engine.position();
                });
        h.runAfterDelay(
                30,
                () -> {
                    h.assertTrue(
                            engine.getX() > h.absoluteVec(new Vec3(24, 2, 8)).x
                                    && engine.isOnRails(),
                            "Engine failed to rejoin track after gap");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 60)
    public static void directionalRailNeverEjectsOrControlsPassengerCart(GameTestHelper h) {
        lane(h, 8, iron());
        rail(h, 14, 8, directional(false));
        rail(h, 17, 8, directional(true));
        var cart = cart(h, 12.5, 8.5);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(cart, true);
        cart.setDeltaMovement(0.25, 0, 0);
        h.runAfterDelay(
                35,
                () -> {
                    h.assertTrue(player.getVehicle() == cart, "Directional rail ejected passenger");
                    h.assertTrue(
                            cart.getX() > h.absoluteVec(new Vec3(18.5, 2, 8)).x
                                    && cart.getDeltaMovement().x > 0.15,
                            "Directional rail stopped or reversed normal cart");
                    player.stopRiding();
                    h.assertTrue(
                            !player.isPassenger(),
                            "Directional dismount guard kept passenger in cart");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 65)
    public static void directionalRailDoesNotActivateTntCart(GameTestHelper h) {
        lane(h, 8, iron());
        rail(h, 14, 8, directional(true));
        Vec3 p = h.absoluteVec(new Vec3(12.5, 2.0625, 8.5));
        var cart = new MinecartTNT(h.getLevel(), p.x, p.y, p.z);
        cart.setDeltaMovement(0.25, 0, 0);
        h.getLevel().addFreshEntity(cart);
        h.runAfterDelay(
                25,
                () -> {
                    h.assertTrue(
                            cart.getFuse() < 0 && cart.getDeltaMovement().x > 0.1,
                            "Directional rail acted as activator or reversed TNT cart");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 110)
    public static void stationLatchesLocomotiveHandbrakeUntilExplicitRelease(GameTestHelper h) {
        lane(h, 8, iron());
        rail(h, 14, 8, directional(false));
        var engine = engine(h, 12.5, 8.5);
        engine.setDeltaMovement(0.4, 0, 0);
        h.runAfterDelay(
                20,
                () -> {
                    h.assertTrue(engine.emergencyBrake(), "Station did not latch emergency brake");
                    Vec3 anchor = engine.position();
                    rail(h, 14, 8, powered());
                    engine.setDeltaMovement(0.8, 0, 0);
                    h.runAfterDelay(
                            8,
                            () -> {
                                h.assertTrue(
                                        engine.position().distanceTo(anchor) < 0.001,
                                        "Replacing station with powered rail released brake");
                                engine.toggleEmergencyBrake();
                                h.runAfterDelay(
                                        20,
                                        () -> {
                                            h.assertTrue(
                                                    engine.getX() > anchor.x + 1,
                                                    "GUI/manual release did not restart engine");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 110)
    public static void stationLatchesFurnaceBrakeAndPersistsIt(GameTestHelper h) {
        lane(h, 8, iron());
        rail(h, 14, 8, directional(false));
        Vec3 p = h.absoluteVec(new Vec3(12.5, 2.0625, 8.5));
        var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
        cart.fuel = 10000;
        cart.xPush = 1;
        cart.setDeltaMovement(0.4, 0, 0);
        h.getLevel().addFreshEntity(cart);
        h.runAfterDelay(
                22,
                () -> {
                    h.assertTrue(
                            MinecartHandbrake.active(cart), "Station did not latch furnace brake");
                    Vec3 anchor = cart.position();
                    var tag = cart.saveWithoutId(new CompoundTag());
                    var restored = new MinecartFurnace(h.getLevel(), 0, 0, 0);
                    restored.load(tag);
                    h.assertTrue(
                            MinecartHandbrake.active(restored), "Furnace brake lost on save/load");
                    rail(h, 14, 8, powered());
                    cart.push(.7, 0, 0);
                    cart.setDeltaMovement(.8, 0, 0);
                    h.runAfterDelay(
                            8,
                            () -> {
                                h.assertTrue(
                                        cart.position().distanceTo(anchor) < 0.001,
                                        "Furnace brake moved under power or collision");
                                var player = h.makeMockServerPlayerInLevel();
                                player.setPos(anchor.x, anchor.y, anchor.z + 1);
                                MinecartHandbrake.release(cart);
                                h.runAfterDelay(
                                        20,
                                        () -> {
                                            h.assertTrue(
                                                    cart.getX() > anchor.x + 1
                                                            && !MinecartHandbrake.active(cart),
                                                    "Released furnace failed to restart");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 120)
    public static void pushingCartDoesNotProduceEngineBrakeSparks(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 8.5, 8.5);
        var cart = cart(h, 10.1, 8.5);
        double start = engine.getX();
        h.onEachTick(
                () -> {
                    h.assertTrue(
                            ((MinecartMotionVisuals) engine).spelunkersCharm$braking() == 0,
                            "Pushing produced false engine brake sparks");
                    h.assertTrue(
                            ((MinecartMotionVisuals) cart).spelunkersCharm$braking() == 0,
                            "Pushed cart produced brake sparks");
                });
        h.runAfterDelay(
                75,
                () -> {
                    h.assertTrue(
                            engine.getX() > start + 5 && cart.getX() > start + 6,
                            "Engine no longer pushes unlinked carts");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large")
    public static void locomotiveBodyDoesNotFlipWhenRailAxisOrDriveReverses(GameTestHelper h) {
        var engine = engine(h, 8.5, 8.5);
        float previous = MinecartBodyOrientation.stable(engine, 180);
        for (float input : new float[] {0, 180, -135, 45, -90, 90, 135, -45, 0, 180}) {
            engine.switchDirection();
            float yaw = MinecartBodyOrientation.stable(engine, input);
            h.assertTrue(
                    Math.abs(yaw - previous) <= 90.001,
                    "Locomotive model flipped through 180 degrees");
            previous = yaw;
        }
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void changingNetworkInterpolationWindowCannotDoubleClientSpeed(GameTestHelper h) {
        lane(h, 8, iron());
        var cart = cart(h, 10.5, 8.5);
        var motion = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        motion.clientVelocitySeen = true;
        motion.clientVelocity = new Vec3(.5, 0, 0);
        motion.clientStepSpeed = .45;
        Vec3 start = cart.position(), target = start.add(4.5, 0, 0);
        Vec3 first = MinecartInterpolation.next(cart, target, 10);
        cart.setPos(first.x, first.y, first.z);
        Vec3 second = MinecartInterpolation.next(cart, target.add(.5, 0, 0), 5);
        h.assertTrue(
                second != null && second.distanceTo(cart.position()) < .50,
                "Changing smoothing window doubled client speed");
        cart.setPos(second.x, second.y, second.z);
        Vec3 fallback = MinecartInterpolation.fallback(cart, cart.position().add(4.5, 0, 0), 1);
        h.assertTrue(
                fallback.distanceTo(cart.position()) < .56,
                "Missing rail bypassed client catch-up speed limit");
        h.succeed();
    }

    private static BlockState iron() {
        return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static BlockState powered() {
        return Blocks.POWERED_RAIL
                .defaultBlockState()
                .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                .setValue(PoweredRailBlock.POWERED, true);
    }

    private static BlockState directional(boolean powered) {
        return ModBlocks.DIRECTIONAL_RAIL
                .get()
                .defaultBlockState()
                .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST)
                .setValue(DirectionalRailBlock.FACING, Direction.WEST)
                .setValue(DirectionalRailBlock.POWERED, powered);
    }

    private static void lane(GameTestHelper h, int z, BlockState rail) {
        for (int x = 1; x < 62; x++) rail(h, x, z, rail);
    }

    private static void rail(GameTestHelper h, int x, int z, BlockState state) {
        boolean directional = state.getBlock() instanceof DirectionalRailBlock;
        boolean powered =
                state.hasProperty(PoweredRailBlock.POWERED)
                        && state.getValue(PoweredRailBlock.POWERED);
        if (directional)
            h.setBlock(new BlockPos(x, 2, z + 1), powered ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        h.setBlock(
                new BlockPos(x, 1, z),
                !directional && powered ? Blocks.REDSTONE_BLOCK : Blocks.STONE);
        h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 2, z)), state, Block.UPDATE_CLIENTS);
    }

    private static Minecart cart(GameTestHelper h, double x, double z) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, z));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static LocomotiveMinecart engine(GameTestHelper h, double x, double z) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, z));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 4));
        h.getLevel().addFreshEntity(engine);
        return engine;
    }
}
