// Train passenger tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainPassengerTests {
    @GameTest(template = "rail_large")
    public static void coupledCartPassesSeatedPlayerHitbox(GameTestHelper h) {
        var train = pair(h);
        var player = rider(h, train[1]);
        h.assertFalse(
                train[0].canCollideWith(player), "Coupled cart collides with its train passenger");
        double before = train[0].getX();
        train[0].move(MoverType.SELF, new Vec3(.9, 0, 0));
        h.assertTrue(
                Math.abs(train[0].getX() - before - .9) < .001,
                "Seated passenger physically blocked cart movement");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void seatedPlayerCannotPushOtherCartInTrain(GameTestHelper h) {
        var train = pair(h);
        var player = rider(h, train[1]);
        train[0].setDeltaMovement(Vec3.ZERO);
        player.setDeltaMovement(Vec3.ZERO);
        player.push(train[0]);
        h.assertTrue(
                train[0].getDeltaMovement().lengthSqr() < 1e-12
                        && player.getDeltaMovement().lengthSqr() < 1e-12,
                "Passenger push changed train velocity");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void cartCannotPushPassengerOfCoupledCart(GameTestHelper h) {
        var train = pair(h);
        var player = rider(h, train[1]);
        train[0].setDeltaMovement(Vec3.ZERO);
        player.setDeltaMovement(Vec3.ZERO);
        train[0].push(player);
        h.assertTrue(
                train[0].getDeltaMovement().lengthSqr() < 1e-12
                        && player.getDeltaMovement().lengthSqr() < 1e-12,
                "Cart pushed a passenger in its own train");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void dismountAndUnlinkRestorePassengerCollision(GameTestHelper h) {
        var train = pair(h);
        var player = rider(h, train[1]);
        h.assertFalse(train[0].canCollideWith(player), "Mounted passenger was not excluded");
        player.stopRiding();
        h.assertTrue(train[0].canCollideWith(player), "Dismounted player lost collision");
        player.startRiding(train[1], true);
        train[1].positionRider(player);
        MinecartCouplings.get(h.getLevel()).unlink(train[0].getUUID(), train[1].getUUID());
        h.assertTrue(train[0].canCollideWith(player), "Unlinked passenger lost collision");
        train[0].setDeltaMovement(Vec3.ZERO);
        player.push(train[0]);
        h.assertTrue(
                train[0].getDeltaMovement().lengthSqr() > 1e-8,
                "Unlinked passenger can no longer push another cart");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void nestedBoatPassengersIgnoreOnlyTheirTrain(GameTestHelper h) {
        var train = pair(h);
        var p = train[1].position();
        var boat = new Boat(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(boat);
        h.assertTrue(boat.startRiding(train[1], true), "Boat mount failed");
        var player = player(h);
        h.assertTrue(player.startRiding(boat, true), "Nested passenger mount failed");
        train[1].positionRider(boat);
        boat.positionRider(player);
        h.assertFalse(
                train[0].canCollideWith(boat)
                        || boat.canCollideWith(train[0])
                        || train[0].canCollideWith(player),
                "Nested passenger still blocks train");
        var outside = cart(h, 13.5);
        h.assertTrue(
                outside.canCollideWith(boat) && boat.canCollideWith(outside),
                "Nested collision exemption reached another train");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void passengerExemptionKeepsWallsAndBystanders(GameTestHelper h) {
        var train = pair(h);
        rider(h, train[1]);
        var outside = player(h);
        h.assertTrue(train[0].canCollideWith(outside), "Standing player lost collision");
        h.setBlock(new BlockPos(7, 2, 8), Blocks.STONE);
        double before = train[0].getX();
        train[0].move(MoverType.SELF, new Vec3(-1, 0, 0));
        h.assertTrue(train[0].getX() > before - .02, "Passenger exemption disabled wall collision");
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 1300)
    public static void playersRidePulledTrainAcrossTightFolds(GameTestHelper h) {
        loop(h, false, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 1300)
    public static void playersRidePushedTrainAcrossTightFolds(GameTestHelper h) {
        loop(h, true, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 1300)
    public static void playersRideFurnaceTrainAcrossTightFolds(GameTestHelper h) {
        loop(h, false, true);
    }

    private static void loop(GameTestHelper h, boolean pushing, boolean furnace) {
        h.getLevel().getServer().tickRateManager().setTickRate(100);
        var route = new PassengerTrainRoute(h.getLevel(), h.absolutePos(BlockPos.ZERO));
        route.build();
        Vec3 p = route.point(10);
        AbstractMinecart root;
        if (furnace) {
            var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
            cart.fuel = 6000;
            cart.zPush = -1;
            root = cart;
        } else {
            var cart = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
            cart.setDriveDirection(pushing ? Direction.SOUTH : Direction.NORTH);
            if (pushing) cart.switchDirection();
            for (int s = 0; s < 3; s++) cart.setItem(s, new ItemStack(Items.COAL, 4));
            root = cart;
        }
        h.getLevel().addFreshEntity(root);
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(root);
        var riders = new ArrayList<ServerPlayer>();
        var seats = new ArrayList<AbstractMinecart>();
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 1; i <= 20; i++) {
            Vec3 at = route.point(10 + (pushing ? 1 : -1) * i * 1.55);
            AbstractMinecart cart =
                    i % 3 == 0
                            ? new MinecartChest(h.getLevel(), at.x, at.y, at.z)
                            : new Minecart(h.getLevel(), at.x, at.y, at.z);
            h.getLevel().addFreshEntity(cart);
            links.link(carts.getLast(), cart);
            carts.add(cart);
            if (i == 1 || i == 4 || i == 8) {
                riders.add(rider(h, cart));
                seats.add(cart);
            }
        }
        double[] before = carts.stream().mapToDouble(c -> route.progress(c.position())).toArray(),
                total = new double[carts.size()],
                window = new double[carts.size()];
        var report = new StringBuilder("tick,cart,progress,speed\n");
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    if (tick > 1200) return;
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        double now = route.progress(cart.position());
                        total[i] += route.delta(before[i], now);
                        before[i] = now;
                        report.append(tick)
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(total[i])
                                .append(',')
                                .append(cart.getDeltaMovement().horizontalDistance())
                                .append('\n');
                        if (tick > 100 && tick % 160 == 0) {
                            h.assertTrue(
                                    total[i] - window[i] > 12,
                                    "Mounted train jammed on a fold: wagon="
                                            + i
                                            + ", tick="
                                            + tick);
                            window[i] = total[i];
                        }
                        if (i > 0)
                            h.assertTrue(
                                    links.linked(carts.get(i - 1).getUUID(), cart.getUUID()),
                                    "Mounted train broke a coupling");
                    }
                    for (int i = 0; i < riders.size(); i++)
                        h.assertTrue(
                                riders.get(i).isAlive()
                                        && riders.get(i).getVehicle() == seats.get(i),
                                "Fold dismounted or hurt a passenger");
                    if (tick == 1200) {
                        for (double distance : total)
                            h.assertTrue(
                                    distance > route.length * (furnace ? 1.4 : 1.7),
                                    "Mounted train lost speed on folds: " + distance);
                        try {
                            Files.writeString(
                                    Path.of("passenger-" + pushing + "-" + furnace + ".csv"),
                                    report);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        h.succeed();
                    }
                });
    }

    private static Minecart[] pair(GameTestHelper h) {
        var first = cart(h, 8.5);
        var second = cart(h, 9.5);
        MinecartCouplings.get(h.getLevel()).link(first, second);
        return new Minecart[] {first, second};
    }

    private static Minecart cart(GameTestHelper h, double x) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static ServerPlayer player(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static ServerPlayer rider(GameTestHelper h, AbstractMinecart cart) {
        var player = player(h);
        h.assertTrue(player.startRiding(cart, true), "Player mount failed");
        cart.positionRider(player);
        return player;
    }
}
