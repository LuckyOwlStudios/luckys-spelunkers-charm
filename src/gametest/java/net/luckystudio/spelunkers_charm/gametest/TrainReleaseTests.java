// Train release tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("spelunkers_release_tests")
@PrefixGameTestTemplate(false)
public final class TrainReleaseTests {
    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large")
    public static void connectedBodiesEaseIntoDescentInsteadOfDivingImmediately(GameTestHelper h) {
        var first = drop(h, 8.5, 18);
        var middle = drop(h, 10.05, 18);
        var last = drop(h, 11.6, 18);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(first, middle);
        links.link(middle, last);
        for (var cart : new Minecart[] {first, middle, last})
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
        MinecartBodyPose.axis(middle, 1);
        float previous = 0;
        for (int tick = 0; tick < 36; tick++) {
            first.setPos(first.getX(), middle.getY() + 1, first.getZ());
            last.setPos(last.getX(), middle.getY() - 1, last.getZ());
            middle.tickCount++;
            float pitch = MinecartBodyPose.pitch(middle, 0, 1);
            h.assertTrue(
                    Math.abs(pitch - previous) < 6.2,
                    "Wagon pitched into a sudden dive: " + previous + " -> " + pitch);
            if (tick == 0)
                h.assertTrue(pitch > -7, "Wagon immediately copied the full descent angle");
            previous = pitch;
        }
        h.assertTrue(previous < -20, "Gentler interpolation never followed the descending train");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 130)
    public static void survivingPlayerReceivesNormalFallDamageFromCrashedCart(GameTestHelper h) {
        floor(h, false);
        var cart = drop(h, 22.5, 14);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for (int tick = 0; tick < 65; tick++) player.tick();
        player.setPos(cart.position());
        player.startRiding(cart, true);
        float health = player.getHealth();
        h.runAfterDelay(
                90,
                () -> {
                    h.assertTrue(
                            cart.isRemoved() && player.isAlive() && !player.isPassenger(),
                            "Player or cart crash state is incorrect");
                    h.assertTrue(
                            player.getHealth() < health,
                            "Crashed cart did not apply player fall damage");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large")
    public static void exactEngineSpeedTableUsesAbsoluteBlocksPerSecond(GameTestHelper h) {
        var wood = ModBlocks.WOODEN_RAIL.get().defaultBlockState();
        var normal = Blocks.RAIL.defaultBlockState();
        var powered =
                Blocks.POWERED_RAIL.defaultBlockState().setValue(PoweredRailBlock.POWERED, true);
        var off = powered.setValue(PoweredRailBlock.POWERED, false);
        for (int power = 1; power <= 3; power++) {
            var engine = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
            fuel(engine, power);
            near(h, MinecartSpeedRules.limit(engine, wood, false) * 20, 4 + power);
            near(h, MinecartSpeedRules.limit(engine, normal, false) * 20, 5 + power);
            near(h, MinecartSpeedRules.limit(engine, off, false) * 20, 5 + power);
            near(h, MinecartSpeedRules.limit(engine, powered, true) * 20, 6.5 + power);
        }
        var furnace = new MinecartFurnace(h.getLevel(), 0, 0, 0);
        near(h, MinecartSpeedRules.limit(furnace, wood, false) * 20, 5);
        near(h, MinecartSpeedRules.limit(furnace, normal, false) * 20, 6);
        near(h, MinecartSpeedRules.limit(furnace, powered, true) * 20, 7.5);
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large")
    public static void oldFuelMigratesOnceToHalfDurationAndNewCoalBurnsNormally(GameTestHelper h) {
        var engine = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        fuel(engine, 3);
        var tag = engine.saveWithoutId(new CompoundTag());
        tag.remove("FuelFormat");
        engine.load(tag);
        h.assertTrue(engine.fuelRemaining(0) == 800, "Old fuel did not become twice as short");
        tag = engine.saveWithoutId(new CompoundTag());
        engine.load(tag);
        h.assertTrue(engine.fuelRemaining(0) == 800, "Fuel was halved twice after saving");
        var fresh = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        fresh.setNoGravity(true);
        fresh.setItem(0, new ItemStack(Items.COAL));
        fresh.tick();
        h.assertTrue(
                fresh.fuelRemaining(0) == 1599,
                "New coal did not last half of the previous 3200 ticks");
        h.succeed();
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 130)
    public static void hardGroundLandingBreaksCartAndHurtsPassenger(GameTestHelper h) {
        floor(h, false);
        var cart = drop(h, 8.5, 14);
        var pig = EntityType.PIG.create(h.getLevel());
        pig.setNoAi(true);
        pig.setPos(cart.position());
        h.getLevel().addFreshEntity(pig);
        h.assertTrue(pig.startRiding(cart, true), "Crash passenger could not board");
        float health = pig.getHealth();
        h.runAfterDelay(
                90,
                () -> {
                    h.assertTrue(cart.isRemoved(), "Cart survived a twelve-block ground impact");
                    h.assertTrue(
                            pig.isAlive() && !pig.isPassenger() && pig.getHealth() < health,
                            "Passenger did not receive fall damage");
                    h.assertTrue(
                            items(h, cart, Items.MINECART) == 1,
                            "Crash lost or duplicated cart item");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 130)
    public static void railLandingProtectsPassengerAndCartEvenWithSidewaysMomentum(
            GameTestHelper h) {
        floor(h, true);
        var cart = drop(h, 16.5, 14);
        cart.setDeltaMovement(0, 0, .015);
        var pig = EntityType.PIG.create(h.getLevel());
        pig.setNoAi(true);
        pig.setPos(cart.position());
        h.getLevel().addFreshEntity(pig);
        pig.startRiding(cart, true);
        float health = pig.getHealth();
        h.runAfterDelay(
                90,
                () -> {
                    h.assertTrue(
                            cart.isAlive() && cart.isOnRails(),
                            "Rail landing destroyed or missed cart");
                    h.assertTrue(
                            pig.getVehicle() == cart && pig.getHealth() == health,
                            "Safe rail landing hurt passenger");
                    h.assertTrue(
                            Double.isNaN(
                                    ((MinecartMotionVisuals) cart)
                                            .spelunkersCharm$motion()
                                            .fallPeak),
                            "Rail landing retained stale crash distance");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 100)
    public static void smallGroundDropDoesNotBreakCart(GameTestHelper h) {
        floor(h, false);
        var cart = drop(h, 8.5, 7);
        h.runAfterDelay(
                65,
                () -> {
                    h.assertTrue(cart.isAlive() && cart.onGround(), "Small drop broke cart");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 150)
    public static void coupledCargoCrashesOnImpactAndDropsInventoryExactlyOnce(GameTestHelper h) {
        floor(h, false);
        var a = drop(h, 8.5, 15);
        Vec3 p = h.absoluteVec(new Vec3(10.05, 15, 8.5));
        var b = new MinecartChest(h.getLevel(), p.x, p.y, p.z);
        b.setItem(0, new ItemStack(Items.DIAMOND, 3));
        h.getLevel().addFreshEntity(b);
        var links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(
                links.link(a, b) == MinecartCouplings.Result.LINKED,
                "Cargo fixture failed to link");
        h.runAfterDelay(
                3,
                () ->
                        h.assertTrue(
                                items(h, b, Items.DIAMOND) == 0,
                                "Cargo dropped before ground contact"));
        h.runAfterDelay(
                100,
                () -> {
                    h.assertTrue(a.isRemoved() && b.isRemoved(), "Crashed train retained carts");
                    h.assertTrue(
                            items(h, b, Items.DIAMOND) == 3, "Crash lost or duplicated inventory");
                    h.assertTrue(
                            links.neighbors(a.getUUID()).isEmpty()
                                    && links.neighbors(b.getUUID()).isEmpty(),
                            "Crash left ghost train links");
                    h.succeed();
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 60)
    public static void angryEndermanLeavesCartButCalmHostilesKeepRiding(GameTestHelper h) {
        var cart = drop(h, 8.5, 4);
        cart.setNoGravity(true);
        var mob = EntityType.ENDERMAN.create(h.getLevel());
        mob.setNoAi(true);
        mob.setPos(cart.position());
        h.getLevel().addFreshEntity(mob);
        h.assertTrue(mob.startRiding(cart, true), "Calm enderman could not board");
        h.runAfterDelay(
                4,
                () -> {
                    h.assertTrue(mob.getVehicle() == cart, "Calm enderman was ejected");
                    var player = h.makeMockServerPlayerInLevel();
                    mob.setTarget(player);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        !mob.isPassenger() && cart.isAlive(),
                                        "Angry enderman did not leave intact cart");
                                h.succeed();
                            });
                });
    }

    @GameTest(templateNamespace = LiftTestMod.ID, template = "rail_large", timeoutTicks = 50)
    public static void creeperPassengerStaysUntilDetachedNormally(GameTestHelper h) {
        var cart = drop(h, 8.5, 4);
        cart.setNoGravity(true);
        var mob = EntityType.CREEPER.create(h.getLevel());
        mob.setNoAi(true);
        mob.setPos(cart.position());
        h.getLevel().addFreshEntity(mob);
        h.assertTrue(mob.startRiding(cart, true), "Creeper could not board");
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(
                            mob.getVehicle() == cart,
                            "Creeper passenger was ejected by old hostile rule");
                    h.succeed();
                });
    }

    private static void fuel(LocomotiveMinecart cart, int slots) {
        var tag = cart.saveWithoutId(new CompoundTag());
        var burners = new ListTag();
        for (int i = 0; i < 3; i++) {
            var entry = new CompoundTag();
            entry.putInt("Remaining", i < slots ? 1600 : 0);
            entry.putInt("Duration", 1600);
            burners.add(entry);
        }
        tag.put("Burners", burners);
        cart.load(tag);
    }

    private static void floor(GameTestHelper h, boolean rails) {
        for (int x = 2; x < 40; x++)
            for (int z = 5; z <= 12; z++)
                h.getLevel()
                        .setBlock(
                                h.absolutePos(new BlockPos(x, 1, z)),
                                Blocks.STONE.defaultBlockState(),
                                2);
        if (rails)
            for (int x = 2; x < 40; x++)
                for (int z = 7; z <= 10; z++)
                    h.getLevel()
                            .setBlock(
                                    h.absolutePos(new BlockPos(x, 2, z)),
                                    Blocks.RAIL
                                            .defaultBlockState()
                                            .setValue(RailBlock.SHAPE, RailShape.EAST_WEST),
                                    2);
    }

    private static Minecart drop(GameTestHelper h, double x, double y) {
        Vec3 p = h.absoluteVec(new Vec3(x, y, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static int items(
            GameTestHelper h, AbstractMinecart cart, net.minecraft.world.item.Item item) {
        return h
                .getLevel()
                .getEntitiesOfClass(ItemEntity.class, cart.getBoundingBox().inflate(14))
                .stream()
                .filter(e -> e.getItem().is(item))
                .mapToInt(e -> e.getItem().getCount())
                .sum();
    }

    private static void near(GameTestHelper h, double actual, double expected) {
        h.assertTrue(
                Math.abs(actual - expected) < 1e-6,
                "Wrong speed: " + actual + ", expected=" + expected);
    }
}
