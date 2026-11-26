// Rail control tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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
public final class RailControlTests {
    @GameTest(template = "rail_large", timeoutTicks = 70)
    public static void flatPoweredRailCannotLaunchCartEvenAgainstSolidBlock(GameTestHelper h) {
        lane(h, 8, powered());
        var cart = cart(h, 8.5, 8.5);
        h.setBlock(new BlockPos(7, 2, 8), Blocks.STONE);
        Vec3 start = cart.position();
        h.runAfterDelay(
                50,
                () -> {
                    h.assertTrue(
                            cart.position().distanceTo(start) < 0.001,
                            "Powered rail launched a stationary cart");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void poweredRailBoostsRollingCart(GameTestHelper h) {
        lane(h, 8, powered());
        var cart = cart(h, 8.5, 8.5);
        cart.setDeltaMovement(0.025, 0, 0);
        h.runAfterDelay(
                15,
                () -> {
                    h.assertTrue(
                            cart.getDeltaMovement().x > 0.20,
                            "Powered rail failed to boost a rolling cart");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void stationaryCartRollsDownEitherPoweredSlope(GameTestHelper h) {
        for (int z : new int[] {8, 12}) {
            boolean east = z == 8;
            for (int x = 4; x <= 10; x++) h.setBlock(new BlockPos(x, 1, z), Blocks.REDSTONE_BLOCK);
            h.setBlock(new BlockPos(east ? 8 : 6, 2, z), Blocks.REDSTONE_BLOCK);
            for (int x = east ? 4 : 8; x <= (east ? 6 : 10); x++) rail(h, x, 2, z, powered());
            rail(h, east ? 8 : 6, 3, z, powered());
            rail(
                    h,
                    7,
                    2,
                    z,
                    powered()
                            .setValue(
                                    PoweredRailBlock.SHAPE,
                                    east ? RailShape.ASCENDING_EAST : RailShape.ASCENDING_WEST));
        }
        Vec3 a = h.absoluteVec(new Vec3(7.5, 2.5625, 8.5));
        Vec3 b = h.absoluteVec(new Vec3(7.5, 2.5625, 12.5));
        var first = new Minecart(h.getLevel(), a.x, a.y, a.z);
        var second = new Minecart(h.getLevel(), b.x, b.y, b.z);
        h.getLevel().addFreshEntity(first);
        h.getLevel().addFreshEntity(second);
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(
                            first.getX() < a.x - 0.25 && second.getX() > b.x + 0.25,
                            "Stationary cart did not roll downhill on both slopes: "
                                    + first.position().subtract(a)
                                    + ", "
                                    + second.position().subtract(b)
                                    + ", rails="
                                    + h.getBlockState(new BlockPos(7, 2, 8))
                                    + ", "
                                    + h.getBlockState(new BlockPos(7, 2, 12)));
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 60)
    public static void unpoweredPoweredRailCoastsExactlyLikeOrdinaryRail(GameTestHelper h) {
        lane(h, 8, iron());
        lane(
                h,
                12,
                Blocks.POWERED_RAIL
                        .defaultBlockState()
                        .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST));
        var a = cart(h, 8.5, 8.5);
        var b = cart(h, 8.5, 12.5);
        a.setDeltaMovement(0.20, 0, 0);
        b.setDeltaMovement(0.20, 0, 0);
        h.runAfterDelay(
                35,
                () -> {
                    h.assertTrue(
                            Math.abs(a.getX() - b.getX()) < 0.002 && b.getDeltaMovement().x > 0.10,
                            "Unpowered powered rail braked a coasting cart");
                    h.assertTrue(
                            ((MinecartMotionVisuals) b).spelunkersCharm$braking() == 0,
                            "Coasting produced brake effects");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void emergencyBrakeCannotBeMovedByRailsImpactsOrCouplings(GameTestHelper h) {
        lane(h, 8, powered());
        var engine = engine(h, 12.5, 8.5, true);
        engine.setDeltaMovement(0.45, 0, 0);
        engine.toggleEmergencyBrake();
        double start = engine.getX();
        Minecart[] peer = {null};
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 6 && h.getTick() < 45) {
                        engine.push(0.7, 0, 0);
                        engine.move(MoverType.PISTON, new Vec3(0.4, 0, 0));
                        engine.setDeltaMovement(0.9, 0, 0);
                        if (peer[0] != null) peer[0].setDeltaMovement(0.3, 0, 0);
                    }
                });
        h.runAfterDelay(
                5,
                () -> {
                    h.assertTrue(
                            engine.getX() > start + 0.10,
                            "Emergency brake stopped instantaneously");
                    Vec3 anchor = engine.position();
                    peer[0] = cart(h, 14.8, 8.5);
                    MinecartCouplings.get(h.getLevel()).link(engine, peer[0]);
                    h.runAfterDelay(
                            42,
                            () -> {
                                h.assertTrue(
                                        engine.emergencyBrake()
                                                && engine.position().distanceTo(anchor) < 0.001,
                                        "Latched brake moved under an external force: anchor="
                                                + anchor
                                                + ", now="
                                                + engine.position()
                                                + ", brake="
                                                + engine.emergencyBrake());
                                engine.toggleEmergencyBrake();
                                h.runAfterDelay(
                                        20,
                                        () -> {
                                            h.assertTrue(
                                                    engine.getX() > anchor.x + 1
                                                            && !engine.emergencyBrake(),
                                                    "Manual brake release did not restart engine");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void poweredDirectionalRailReleasesAndReversesLatchedEngine(GameTestHelper h) {
        lane(h, 8, iron());
        rail(h, 20, 2, 8, directional(false, Direction.WEST));
        var engine = engine(h, 20.5, 8.5, true);
        engine.toggleEmergencyBrake();
        h.runAfterDelay(
                5,
                () -> {
                    rail(h, 20, 2, 8, directional(true, Direction.WEST));
                    h.runAfterDelay(
                            45,
                            () -> {
                                h.assertTrue(
                                        !engine.emergencyBrake()
                                                && engine.getX()
                                                        < h.absoluteVec(new Vec3(18, 2, 8)).x,
                                        "Powered directional rail failed to release and reverse brake");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large")
    public static void trainRejectsSecondEngineDirectlyAndThroughWagons(GameTestHelper h) {
        lane(h, 8, iron());
        var a = engine(h, 8.5, 8.5, false);
        var b = engine(h, 13.15, 8.5, false);
        a.setDriveDirection(Direction.WEST);
        var first = cart(h, 10.05, 8.5);
        var second = cart(h, 11.60, 8.5);
        var links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(
                links.link(a, b) == MinecartCouplings.Result.ENGINE_LIMIT,
                "Direct engine-to-engine link allowed");
        links.link(a, first);
        links.link(b, second);
        h.assertTrue(
                links.link(first, second) == MinecartCouplings.Result.ENGINE_LIMIT,
                "Indirect link joined two powered trains");
        var saved = links.save(new CompoundTag(), h.getLevel().registryAccess());
        var restored = MinecartCouplings.load(saved, h.getLevel().registryAccess());
        h.assertTrue(
                restored.link(first, second) == MinecartCouplings.Result.ENGINE_LIMIT,
                "Reload bypassed engine limit");
        var furnace = new MinecartFurnace(h.getLevel(), first.getX(), first.getY(), first.getZ());
        h.getLevel().addFreshEntity(furnace);
        h.assertTrue(
                links.link(first, furnace) == MinecartCouplings.Result.ENGINE_LIMIT,
                "Furnace allowed as second engine");
        h.assertFalse(
                MinecartCouplings.replaceEntity(
                        first, new LocomotiveMinecart(h.getLevel(), 0, 0, 0)),
                "Block loading created a second engine");
        h.assertFalse(
                MinecartCouplings.replaceEntity(first, new MinecartFurnace(h.getLevel(), 0, 0, 0)),
                "Furnace loading bypassed limit");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void chainAndHookCannotJoinTwoPoweredTrains(GameTestHelper h) {
        lane(h, 8, iron());
        var a = engine(h, 8.5, 8.5, false);
        var b = engine(h, 13.15, 8.5, false);
        a.setDriveDirection(Direction.WEST);
        var first = cart(h, 10.05, 8.5);
        var second = cart(h, 11.60, 8.5);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(a, first);
        links.link(b, second);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack chain = new ItemStack(Items.CHAIN, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, chain);
        player.interactOn(first, InteractionHand.MAIN_HAND);
        player.interactOn(second, InteractionHand.MAIN_HAND);
        h.assertTrue(
                chain.getCount() == 3 && !links.sameTrain(a.getUUID(), b.getUUID()),
                "Rejected train link consumed chain or joined trains");
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.CART_COUPLING_HOOK.toStack());
        player.interactOn(first, InteractionHand.MAIN_HAND);
        player.interactOn(second, InteractionHand.MAIN_HAND);
        h.assertFalse(links.sameTrain(a.getUUID(), b.getUUID()), "Hook bypassed engine limit");
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 120)
    public static void riderCannotBrakeOrReverseEngineTrain(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 8.5, 8.5, true);
        var wagon = cart(h, 6.95, 8.5);
        MinecartCouplings.get(h.getLevel()).link(engine, wagon);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(wagon, true);
        double start = engine.getX();
        h.onEachTick(
                () -> {
                    player.setYRot(90);
                    player.zza = 1;
                });
        h.runAfterDelay(
                80,
                () -> {
                    h.assertTrue(
                            wagon.getDeltaMovement().x > 0.25 && engine.getX() > start + 15,
                            "Rider slowed or reversed a powered train");
                    h.assertTrue(
                            ((MinecartMotionVisuals) wagon).spelunkersCharm$braking() == 0,
                            "Rider input triggered train brakes");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 70)
    public static void riderCannotDriveTrainWithUnfueledFurnace(GameTestHelper h) {
        lane(h, 8, iron());
        var wagon = cart(h, 8.5, 8.5);
        var furnace =
                new MinecartFurnace(h.getLevel(), wagon.getX() + 1.55, wagon.getY(), wagon.getZ());
        h.getLevel().addFreshEntity(furnace);
        MinecartCouplings.get(h.getLevel()).link(wagon, furnace);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(wagon, true);
        double start = wagon.getX();
        h.onEachTick(
                () -> {
                    player.setYRot(-90);
                    player.zza = 1;
                });
        h.runAfterDelay(
                45,
                () -> {
                    h.assertTrue(
                            Math.abs(wagon.getX() - start) < 0.01,
                            "Rider drove train whose engine had no fuel");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void railSpeedChangesDoNotProduceBrakeEffects(GameTestHelper h) {
        lane(h, 8, powered());
        for (int x = 18; x < 61; x++) rail(h, x, 2, 8, iron());
        var engine = engine(h, 14.5, 8.5, true);
        engine.setDeltaMovement(10.0 / 20, 0, 0);
        h.onEachTick(
                () ->
                        h.assertTrue(
                                ((MinecartMotionVisuals) engine).spelunkersCharm$braking() == 0,
                                "Routine rail speed change triggered sparks"));
        h.runAfterDelay(
                50,
                () -> {
                    h.assertTrue(
                            engine.getX() > h.absoluteVec(new Vec3(25, 2, 8)).x,
                            "Engine failed to cross slower rail");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 260)
    public static void directionalReversalRemainsContinuousThroughFollowingCurves(
            GameTestHelper h) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        for (int x = 6; x <= 45; x++) {
            rail(h, x, 2, 8, powered());
            rail(h, x, 2, 13, powered());
        }
        for (int z = 9; z <= 12; z++) rail(h, 5, 2, z, Blocks.RAIL.defaultBlockState());
        rail(h, 5, 2, 8, iron().setValue(RailBlock.SHAPE, RailShape.SOUTH_EAST));
        rail(h, 5, 2, 13, iron().setValue(RailBlock.SHAPE, RailShape.NORTH_EAST));
        rail(h, 36, 2, 8, directional(true, Direction.WEST));
        var engine = engine(h, 34.5, 8.5, true);
        engine.setDeltaMovement(0.45, 0, 0);
        Vec3[] previous = {engine.position()};
        boolean[] reversed = {false};
        h.onEachTick(
                () -> {
                    Vec3 now = engine.position();
                    h.assertTrue(
                            now.distanceTo(previous[0]) < 0.62,
                            "Engine teleported after directional reversal: "
                                    + now.subtract(previous[0]));
                    previous[0] = now;
                    if (engine.getDeltaMovement().x < -0.1) reversed[0] = true;
                    if (now.z > h.absoluteVec(new Vec3(0, 2, 13.4)).z
                            && engine.getDeltaMovement().x > 0.1) {
                        h.assertTrue(reversed[0], "Fixture did not reverse engine first");
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "rail_large")
    public static void networkInterpolationFollowsCurveInBothDirectionsAndAfterReversal(
            GameTestHelper h) {
        for (int x = 7; x <= 10; x++) h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
        for (int z = 9; z <= 12; z++) h.setBlock(new BlockPos(10, 1, z), Blocks.STONE);
        for (int x = 7; x < 10; x++) rail(h, x, 2, 8, iron());
        for (int z = 9; z <= 12; z++) rail(h, 10, 2, z, Blocks.RAIL.defaultBlockState());
        rail(h, 10, 2, 8, iron().setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST));
        Vec3 a = h.absoluteVec(new Vec3(9.4, 2.0625, 8.5)),
                b = h.absoluteVec(new Vec3(10.5, 2.0625, 9.6));
        var cart = cart(h, 9.4, 8.5);
        cart.setDeltaMovement(0.4, 0, 0);
        for (int direction = 0; direction < 2; direction++) {
            Vec3 start = direction == 0 ? a : b, target = direction == 0 ? b : a;
            cart.setPos(start.x, start.y, start.z);
            ((MinecartMotionVisuals) cart).spelunkersCharm$motion().railPosition = null;
            ((MinecartMotionVisuals) cart).spelunkersCharm$motion().clientStepSpeed = Double.NaN;
            for (int steps = 5; steps > 0; steps--) {
                Vec3 next = MinecartInterpolation.next(cart, target, steps);
                h.assertTrue(
                        next != null && next.distanceTo(cart.position()) < 0.39,
                        "Interpolation jumped or lost connected rail path");
                cart.setPos(next.x, next.y, next.z);
                var track = MinecartTrack.current(cart);
                h.assertTrue(
                        track != null && track.point(track.offset(next)).distanceTo(next) < 0.001,
                        "Interpolation cut across empty space inside corner");
                if (steps == 3) {
                    double savedSpeed =
                            ((MinecartMotionVisuals) cart).spelunkersCharm$motion().clientStepSpeed;
                    Vec3 reversed = MinecartInterpolation.next(cart, start, 4);
                    h.assertTrue(
                            reversed != null && reversed.distanceTo(cart.position()) < 0.4,
                            "Network target reversal jumped off curve");
                    ((MinecartMotionVisuals) cart).spelunkersCharm$motion().clientStepSpeed =
                            savedSpeed;
                }
            }
            h.assertTrue(
                    cart.position().distanceTo(target) < 0.001,
                    "Interpolation did not reach network target");
        }
        h.assertTrue(
                MinecartInterpolation.next(cart, a.add(20, 0, 0), 5) == null,
                "Intentional teleport was projected onto railway");
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

    private static BlockState directional(boolean power, Direction facing) {
        return ModBlocks.DIRECTIONAL_RAIL
                .get()
                .defaultBlockState()
                .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST)
                .setValue(DirectionalRailBlock.FACING, facing)
                .setValue(DirectionalRailBlock.POWERED, power);
    }

    private static void lane(GameTestHelper h, int z, BlockState rail) {
        for (int x = 1; x < 62; x++) rail(h, x, 2, z, rail);
    }

    private static void rail(GameTestHelper h, int x, int y, int z, BlockState state) {
        boolean directional = state.getBlock() instanceof DirectionalRailBlock;
        boolean powered =
                state.hasProperty(PoweredRailBlock.POWERED)
                        && state.getValue(PoweredRailBlock.POWERED);
        if (directional)
            h.setBlock(new BlockPos(x, y, z + 1), powered ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        h.setBlock(
                new BlockPos(x, y - 1, z),
                !directional && powered ? Blocks.REDSTONE_BLOCK : Blocks.STONE);
        h.getLevel().setBlock(h.absolutePos(new BlockPos(x, y, z)), state, Block.UPDATE_CLIENTS);
    }

    private static Minecart cart(GameTestHelper h, double x, double z) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, z));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static LocomotiveMinecart engine(GameTestHelper h, double x, double z, boolean fuel) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, z));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        if (fuel)
            for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 4));
        h.getLevel().addFreshEntity(engine);
        return engine;
    }
}
