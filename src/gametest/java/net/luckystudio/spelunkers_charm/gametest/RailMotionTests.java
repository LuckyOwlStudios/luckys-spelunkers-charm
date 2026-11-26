// Rail motion tests.
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
public final class RailMotionTests {
    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void playerCartWoodenSpeedMatchesSprint(GameTestHelper h) {
        lane(
                h,
                8,
                ModBlocks.WOODEN_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        Minecart cart = cart(h, 5.5, 8.5);
        var player = h.makeMockServerPlayerInLevel();
        player.setYRot(-90);
        player.zza = 1;
        player.startRiding(cart, true);
        for (int time = 1; time <= 65; time++)
            h.runAfterDelay(
                    time,
                    () -> {
                        player.zza = 1;
                        player.setYRot(-90);
                    });
        h.runAfterDelay(
                60,
                () -> {
                    double speed = cart.getDeltaMovement().horizontalDistance() * 20;
                    h.assertTrue(
                            Math.abs(
                                            speed
                                                    - MinecartSpeedRules.SPRINT_BLOCKS_PER_SECOND
                                                            * MinecartSpeedRules.SPEED_SCALE)
                                    < 0.08,
                            "Wood baseline differs from sprint: " + speed);
                    h.assertTrue(
                            ((MinecartMotionVisuals) cart).spelunkersCharm$braking() == 0,
                            "Cruising continuously triggers brakes");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large")
    public static void speedCapsUseVehicleRailAndDownhill(GameTestHelper h) {
        var cart = cart(h, 5.5, 8.5);
        var furnace = new MinecartFurnace(h.getLevel(), 0, 0, 0);
        var wood = ModBlocks.WOODEN_RAIL.get().defaultBlockState();
        var iron = Blocks.RAIL.defaultBlockState();
        var powered =
                Blocks.POWERED_RAIL.defaultBlockState().setValue(PoweredRailBlock.POWERED, true);
        h.assertTrue(
                close(
                        MinecartSpeedRules.limit(cart, wood, false),
                        MinecartSpeedRules.SPRINT_BLOCKS_PER_SECOND
                                * MinecartSpeedRules.SPEED_SCALE
                                / 20),
                "Wood cap incorrect");
        h.assertTrue(
                close(
                        MinecartSpeedRules.limit(cart, iron, false),
                        (MinecartSpeedRules.SPRINT_BLOCKS_PER_SECOND
                                                * MinecartSpeedRules.SPEED_SCALE
                                        + 1)
                                / 20),
                "Iron cap incorrect");
        h.assertTrue(
                close(
                        MinecartSpeedRules.limit(cart, powered, true),
                        (MinecartSpeedRules.SPRINT_BLOCKS_PER_SECOND
                                                * MinecartSpeedRules.SPEED_SCALE
                                        + 2.5)
                                / 20),
                "Powered downhill cap incorrect");
        h.assertTrue(
                close(MinecartSpeedRules.limit(furnace, iron, true), 6.5 / 20),
                "Furnace base incorrect");
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void threeIndependentBurnersLockPauseAndPersist(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 5.5, 8.5, 3);
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(
                            engine.activeBurners() == 3,
                            "Three fuel slots did not produce full power");
                    h.assertTrue(
                            MinecartSpeedRules.vehicleBase(engine) == 7,
                            "Full engine base incorrect");
                    h.assertTrue(
                            close(MinecartSpeedRules.limit(engine, iron(), false), 8.0 / 20),
                            "Full engine speed is incorrect");
                    int first = engine.fuelRemaining(0);
                    int second = engine.fuelRemaining(1);
                    engine.toggleBurner(1);
                    engine.toggleBurner(2);
                    h.runAfterDelay(
                            10,
                            () -> {
                                h.assertTrue(
                                        engine.activeBurners() == 1
                                                && MinecartSpeedRules.vehicleBase(engine) == 5,
                                        "Locks did not reduce power");
                                h.assertTrue(
                                        close(
                                                MinecartSpeedRules.limit(engine, iron(), false),
                                                6.0 / 20),
                                        "First power level changed");
                                h.assertTrue(
                                        engine.fuelRemaining(0) == first - 10,
                                        "One firebox did not burn at normal rate");
                                h.assertTrue(
                                        engine.fuelRemaining(1) == second,
                                        "Locked firebox continued consuming fuel");
                                h.assertFalse(
                                        engine.canPlaceItem(1, new ItemStack(Items.COAL)),
                                        "Locked firebox accepts hopper fuel");
                                var saved = engine.saveWithoutId(new CompoundTag());
                                var restored = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
                                restored.load(saved);
                                h.assertTrue(
                                        !restored.burnerEnabled(1)
                                                && restored.fuelRemaining(1) == second,
                                        "Save/load lost paused firebox");
                                h.assertTrue(
                                        restored.getItem(2).getCount() == 3,
                                        "Save/load lost third fuel stack");
                                engine.toggleBurner(1);
                                h.assertTrue(
                                        engine.activeBurners() == 2
                                                && MinecartSpeedRules.vehicleBase(engine) == 6,
                                        "Two fireboxes did not produce medium power");
                                h.assertTrue(
                                        close(
                                                MinecartSpeedRules.limit(engine, iron(), false),
                                                7.0 / 20),
                                        "Medium engine speed is incorrect");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large")
    public static void legacySingleFireboxMigratesAndLavaKeepsBucket(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 5.5, 8.5, 0);
        CompoundTag tag = engine.saveWithoutId(new CompoundTag());
        tag.remove("Burners");
        tag.remove("EnabledBurners");
        tag.putInt("BurnTime", 1234);
        tag.putInt("BurnDuration", 3200);
        engine.load(tag);
        h.assertTrue(
                engine.fuelRemaining(0) == 1234 && engine.fuelRemaining(1) == 0,
                "Legacy firebox migration failed");
        engine.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(
                            engine.getItem(1).is(Items.BUCKET) && engine.fuelRemaining(1) > 0,
                            "Lava firebox did not retain empty bucket");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void locomotiveAcceleratesAndEmergencyBrakeHolds(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 5.5, 8.5, 3);
        h.runAfterDelay(
                5,
                () -> {
                    double early = engine.getDeltaMovement().horizontalDistance();
                    h.assertTrue(early > 0 && early < 0.12, "Engine jumps to full speed: " + early);
                    h.runAfterDelay(
                            35,
                            () -> {
                                h.assertTrue(
                                        engine.getDeltaMovement().horizontalDistance()
                                                > early + 0.15,
                                        "Engine failed to gradually accelerate");
                                double x = engine.getX();
                                engine.toggleEmergencyBrake();
                                h.runAfterDelay(
                                        4,
                                        () -> {
                                            h.assertTrue(
                                                    engine.getDeltaMovement().horizontalDistance()
                                                            < 0.005,
                                                    "Emergency brake did not stop quickly");
                                            h.assertTrue(
                                                    engine.getX() - x < 0.6,
                                                    "Emergency stopping distance too long");
                                            double stopped = engine.getX();
                                            int remaining = engine.fuelRemaining(0);
                                            h.runAfterDelay(
                                                    6,
                                                    () -> {
                                                        h.assertTrue(
                                                                Math.abs(engine.getX() - stopped)
                                                                        < 0.005,
                                                                "Emergency brake did not hold engine");
                                                        h.assertTrue(
                                                                engine.fuelRemaining(0)
                                                                        == remaining,
                                                                "Emergency brake kept burning fuel");
                                                        engine.toggleEmergencyBrake();
                                                        h.runAfterDelay(
                                                                5,
                                                                () -> {
                                                                    h.assertTrue(
                                                                            engine.getX()
                                                                                    > stopped
                                                                                            + 0.05,
                                                                            "Releasing brake did not restart engine");
                                                                    h.succeed();
                                                                });
                                                    });
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void fastCartStopsNearDirectionalRailExit(GameTestHelper h) {
        lane(h, 8, iron());
        BlockPos stop = new BlockPos(8, 2, 8);
        h.setBlock(
                stop,
                ModBlocks.DIRECTIONAL_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST)
                        .setValue(DirectionalRailBlock.FACING, Direction.EAST));
        var cart = engine(h, 7.8, 8.5, 0);
        cart.setDeltaMovement(0.78, 0, 0);
        double boundary = h.absolutePos(stop).getX();
        h.runAfterDelay(
                1,
                () ->
                        h.assertTrue(
                                cart.getDeltaMovement().horizontalDistance() > 0.02,
                                "Directional rail instantly stopped cart"));
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(
                            cart.getX() >= boundary + 0.70 && cart.getX() <= boundary + 0.90,
                            "Cart stopped outside end of rail: " + (cart.getX() - boundary));
                    h.assertTrue(
                            cart.getDeltaMovement().horizontalDistance() < 0.003,
                            "Cart still moving at station");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void riderReversalBrakesBeforeChangingDirection(GameTestHelper h) {
        lane(h, 8, iron());
        var cart = cart(h, 9.5, 8.5);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(cart, true);
        cart.setDeltaMovement(0.35, 0, 0);
        for (int time = 1; time <= 24; time++)
            h.runAfterDelay(
                    time,
                    () -> {
                        player.zza = 1;
                        player.setYRot(90);
                    });
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(
                            cart.getDeltaMovement().x >= 0 && cart.getDeltaMovement().x < 0.25,
                            "Reversal did not start with braking");
                    h.assertTrue(
                            ((MinecartMotionVisuals) cart).spelunkersCharm$braking() > 0,
                            "Reversal did not trigger effects");
                });
        h.runAfterDelay(
                20,
                () -> {
                    h.assertTrue(
                            cart.getDeltaMovement().x < -0.1,
                            "Rider did not reverse after braking");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void riderCannotClimbFromRestButMomentumClimbs(GameTestHelper h) {
        for (int z : new int[] {8, 11}) {
            lane(h, z, iron());
            h.setBlock(
                    new BlockPos(8, 2, z),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(RailBlock.SHAPE, RailShape.ASCENDING_EAST));
            for (int x = 9; x < 35; x++) {
                h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                h.setBlock(new BlockPos(x, 3, z), iron());
            }
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(8, 2, z)),
                            Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(RailBlock.SHAPE, RailShape.ASCENDING_EAST),
                            Block.UPDATE_CLIENTS);
        }
        var slow = cart(h, 8.1, 8.5);
        var fast = cart(h, 7.8, 11.5);
        fast.setDeltaMovement(0.39, 0, 0);
        var player = h.makeMockServerPlayerInLevel();
        player.startRiding(slow, true);
        for (int time = 1; time <= 24; time++)
            h.runAfterDelay(
                    time,
                    () -> {
                        player.zza = 1;
                        player.setYRot(-90);
                    });
        double crest = h.absolutePos(new BlockPos(9, 3, 8)).getY();
        h.runAfterDelay(
                20,
                () -> {
                    h.assertTrue(slow.getY() < crest - 0.4, "Rider powered uphill from rest");
                    h.assertTrue(
                            fast.getY() > crest
                                    && fast.getX() > h.absolutePos(new BlockPos(9, 3, 11)).getX(),
                            "Momentum failed to climb one-block ramp");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void furnaceAndOneFireboxHaveSameSpeed(GameTestHelper h) {
        lane(h, 8, iron());
        lane(h, 11, iron());
        var engine = engine(h, 5.5, 8.5, 1);
        Vec3 position = h.absoluteVec(new Vec3(5.5, 2.08, 11.5));
        var furnace = new MinecartFurnace(h.getLevel(), position.x, position.y, position.z);
        furnace.fuel = 10000;
        furnace.xPush = 1;
        h.getLevel().addFreshEntity(furnace);
        h.runAfterDelay(
                65,
                () -> {
                    h.assertTrue(
                            Math.abs(
                                            engine.getDeltaMovement().horizontalDistance()
                                                    - furnace.getDeltaMovement()
                                                            .horizontalDistance())
                                    < 0.005,
                            "Single firebox and furnace speeds differ");
                    h.assertTrue(
                            engine.getDeltaMovement().horizontalDistance()
                                    > 0.45 * MinecartSpeedRules.SPEED_SCALE,
                            "Engine never reached requested normal-rail speed");
                    h.succeed();
                });
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 0.0001;
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void hopperBalancesAllThreeEnabledFireboxes(GameTestHelper h) {
        lane(h, 8, iron());
        h.setBlock(
                new BlockPos(5, 2, 8),
                ModBlocks.DIRECTIONAL_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST));
        var engine = engine(h, 5.5, 8.5, 0);
        var pos = new BlockPos(5, 3, 8);
        h.setBlock(pos, Blocks.HOPPER);
        var hopper =
                (net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(pos);
        hopper.setItem(0, new ItemStack(Items.COAL, 3));
        h.runAfterDelay(
                35,
                () -> {
                    for (int slot = 0; slot < 3; slot++)
                        h.assertTrue(
                                engine.getItem(slot).is(Items.COAL),
                                "Hopper left a firebox empty while handbrake was engaged");
                    h.assertTrue(hopper.isEmpty(), "Hopper failed to transfer three fuel items");
                    h.assertTrue(
                            engine.emergencyBrake() && engine.activeBurners() == 0,
                            "Station failed to pause engine fuel");
                    h.setBlock(new BlockPos(5, 2, 8), iron());
                    engine.releaseEmergencyBrake();
                    h.runAfterDelay(
                            4,
                            () -> {
                                h.assertTrue(
                                        engine.activeBurners() == 3,
                                        "Releasing station brake did not light all three fireboxes");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 110)
    public static void coupledWagonsShareEngineSpeedWithoutExtraFuelMultiplier(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 8.5, 8.5, 3);
        var first = cart(h, 6.95, 8.5);
        var second = cart(h, 5.40, 8.5);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(engine.getUUID(), first.getUUID());
        links.link(first.getUUID(), second.getUUID());
        h.succeedWhen(
                () -> {
                    double expected = 8.0 / 20;
                    h.assertTrue(
                            engine.getDeltaMovement().horizontalDistance() > expected - 0.07,
                            "Loaded train never reached engine speed: "
                                    + engine.getDeltaMovement());
                    h.assertTrue(
                            second.getDeltaMovement().horizontalDistance() > expected - 0.07,
                            "Trailing wagon retained slow independent speed cap: "
                                    + second.getDeltaMovement());
                    h.assertTrue(
                            engine.fuelRemaining(0) == engine.fuelRemaining(1)
                                    && engine.fuelRemaining(1) == engine.fuelRemaining(2),
                            "Fireboxes consumed unevenly under load");
                    h.assertTrue(
                            engine.fuelRemaining(0) > 1300,
                            "Coupled wagons added an unintended fuel multiplier");
                    h.assertTrue(
                            links.linked(engine.getUUID(), first.getUUID())
                                    && links.linked(first.getUUID(), second.getUUID()),
                            "Train uncoupled at top speed");
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void hopperSkipsLockedFireboxes(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 5.5, 8.5, 0);
        engine.toggleBurner(1);
        engine.toggleBurner(2);
        engine.toggleEmergencyBrake();
        var hopperPos = new BlockPos(5, 3, 8);
        h.setBlock(hopperPos, Blocks.HOPPER);
        var hopper =
                (net.minecraft.world.level.block.entity.HopperBlockEntity)
                        h.getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.COAL, 4));
        h.runAfterDelay(
                40,
                () -> {
                    h.assertTrue(
                            engine.getItem(0).getCount() == 4,
                            "Hopper failed to refuel enabled firebox: " + engine.getItem(0));
                    h.assertTrue(
                            engine.getItem(1).isEmpty() && engine.getItem(2).isEmpty(),
                            "Hopper filled locked firebox");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large")
    public static void menuControlsRejectStaleAndDistantRequests(GameTestHelper h) {
        lane(h, 8, iron());
        var engine = engine(h, 5.5, 8.5, 0);
        var player = h.makeMockServerPlayerInLevel();
        player.moveTo(engine.getX(), engine.getY(), engine.getZ(), 0, 0);
        var menu = new LocomotiveMenu(1, player.getInventory(), engine, engine.menuData());
        h.assertFalse(menu.clickMenuButton(player, 0), "Unopened menu accepted control packet");
        player.containerMenu = menu;
        h.assertTrue(
                menu.clickMenuButton(player, 0) && !engine.burnerEnabled(0),
                "Menu failed to lock first slot");
        h.assertTrue(
                menu.clickMenuButton(player, 3) && engine.emergencyBrake(),
                "Menu failed to engage brake");
        player.setPos(engine.getX() + 30, engine.getY(), engine.getZ());
        h.assertFalse(menu.clickMenuButton(player, 3), "Distant player released brake");
        h.assertFalse(menu.clickMenuButton(player, 99), "Invalid control ID accepted");
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 80)
    public static void highSpeedCartFollowsCurveWithoutDerailing(GameTestHelper h) {
        lane(h, 8, iron());
        for (int z = 9; z < 30; z++) {
            h.setBlock(new BlockPos(15, 1, z), Blocks.STONE);
            h.setBlock(
                    new BlockPos(15, 2, z),
                    Blocks.RAIL
                            .defaultBlockState()
                            .setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        }
        for (int x = 16; x < 55; x++) h.setBlock(new BlockPos(x, 2, 8), Blocks.AIR);
        h.getLevel()
                .setBlock(
                        h.absolutePos(new BlockPos(15, 2, 8)),
                        Blocks.RAIL
                                .defaultBlockState()
                                .setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST),
                        Block.UPDATE_CLIENTS);
        var cart = cart(h, 13.9, 8.5);
        ((MinecartMotionVisuals) cart).spelunkersCharm$motion().externalSpeedCap = 1;
        cart.setDeltaMovement(0.85, 0, 0);
        h.runAfterDelay(
                10,
                () -> {
                    double x = h.absoluteVec(new Vec3(15.5, 0, 0)).x;
                    h.assertTrue(
                            Math.abs(cart.getX() - x) < 0.03
                                    && cart.getZ() > h.absolutePos(new BlockPos(15, 2, 10)).getZ(),
                            "High-speed cart left curve: " + cart.position());
                    h.assertTrue(cart.isOnRails(), "High-speed cart derailed");
                    h.succeed();
                });
    }

    private static BlockState iron() {
        return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static void lane(GameTestHelper h, int z, BlockState rail) {
        for (int x = 2; x < 55; x++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            h.setBlock(new BlockPos(x, 2, z), rail);
        }
    }

    private static Minecart cart(GameTestHelper h, double x, double z) {
        Vec3 pos = h.absoluteVec(new Vec3(x, 2.08, z));
        var cart = new Minecart(h.getLevel(), pos.x, pos.y, pos.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static LocomotiveMinecart engine(GameTestHelper h, double x, double z, int burners) {
        Vec3 pos = h.absoluteVec(new Vec3(x, 2.08, z));
        var engine = new LocomotiveMinecart(h.getLevel(), pos.x, pos.y, pos.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < burners; slot++)
            engine.setItem(slot, new ItemStack(Items.COAL, 4));
        h.getLevel().addFreshEntity(engine);
        return engine;
    }
}
