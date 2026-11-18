package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

@GameTestHolder("spelunkers_lift_tests")
@PrefixGameTestTemplate(false)
public final class LiftPacketTests {
    private static AbstractLift lift(GameTestHelper h) {
        BlockPos center = h.absolutePos(new BlockPos(12, 5, 12));
        AbstractLift lift =
                AbstractLift.createLift(
                        h.getLevel(),
                        center.getX() + 0.5,
                        center.getY(),
                        center.getZ() + 0.5,
                        AbstractLift.Type.SMALL,
                        ItemStack.EMPTY,
                        null);
        h.getLevel().addFreshEntity(lift);
        return lift;
    }

    private static ServerPlayer player(GameTestHelper h, AbstractLift lift) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();

        acknowledgeTeleport(player);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().flying = false;
        player.setPos(lift.getX(), lift.getBoundingBox().maxY, lift.getZ());
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        player.connection.resetPosition();

        double originalX = player.getX();
        player.connection.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(
                        originalX + 0.125, player.getY(), player.getZ(), true));
        near(
                h,
                player.getX(),
                originalX + 0.125,
                "Positive control: clean movement packet was ignored or rejected");
        player.setPos(originalX, player.getY(), player.getZ());
        player.connection.resetPosition();
        return player;
    }

    private static void acknowledgeTeleport(ServerPlayer player) {
        try {
            Field awaiting = player.connection.getClass().getDeclaredField("awaitingTeleport");
            awaiting.setAccessible(true);
            player.connection.handleAcceptTeleportPacket(
                    new ServerboundAcceptTeleportationPacket(awaiting.getInt(player.connection)));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot acknowledge mock player's teleport", exception);
        }
    }

    private static ServerboundMoveVehiclePacket boatPacket(
            GameTestHelper h, Boat boat, double x, double y, double z) {
        Boat snapshot = EntityType.BOAT.create(h.getLevel());
        snapshot.setPos(x, y, z);
        snapshot.setYRot(boat.getYRot());
        snapshot.setXRot(boat.getXRot());
        return new ServerboundMoveVehiclePacket(snapshot);
    }

    private static void moveDeck(AbstractLift lift, double dy) {
        AABB previous = lift.getBoundingBox();
        lift.setPos(lift.getX(), lift.getY() + dy, lift.getZ());
        LiftCollision.carry(lift, previous, lift.getBoundingBox());
    }

    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(
                Math.abs(actual - expected) < 0.003,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void interpolatedPlayer(GameTestHelper h, double lag) {
        AbstractLift lift = lift(h);
        ServerPlayer player = player(h, lift);
        double clientFeet = player.getY();
        moveDeck(lift, lag);
        near(
                h,
                player.getY(),
                lift.getBoundingBox().maxY,
                "Server carry did not establish contact");
        player.connection.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(
                        player.getX(), clientFeet, player.getZ(), true));
        near(
                h,
                player.getY(),
                clientFeet,
                "Valid client interpolation caused a correction teleport");

        moveDeck(lift, 0.15);
        near(
                h,
                player.getY(),
                lift.getBoundingBox().maxY,
                "Packet lag lost validated deck support");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void risingDeckAcceptsOneTickDelayedPlayer(GameTestHelper h) {
        interpolatedPlayer(h, 0.15);
    }

    @GameTest(template = "empty")
    public static void risingDeckAcceptsThreeTickInterpolatedPlayer(GameTestHelper h) {
        interpolatedPlayer(h, 0.45);
    }

    @GameTest(template = "empty")
    public static void risingDeckAcceptsFiveTickInterpolatedPlayer(GameTestHelper h) {
        interpolatedPlayer(h, 0.75);
    }

    @GameTest(template = "empty")
    public static void descendingDeckRetainsInterpolatedPlayerSupport(GameTestHelper h) {
        interpolatedPlayer(h, -0.45);
    }

    @GameTest(template = "empty")
    public static void supportedPlayerStillCannotWalkThroughStone(GameTestHelper h) {
        AbstractLift lift = lift(h);
        ServerPlayer player = player(h, lift);
        double clientFeet = player.getY(), oldX = player.getX();
        moveDeck(lift, 0.45);
        h.getLevel()
                .setBlockAndUpdate(
                        BlockPos.containing(oldX + 1.0, clientFeet, player.getZ()),
                        Blocks.STONE.defaultBlockState());
        player.connection.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(oldX + 1.0, clientFeet, player.getZ(), true));
        near(h, player.getX(), oldX, "Lift packet guard allowed movement through a stone block");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void supportedPlayerStillCollidesWithOtherVehicles(GameTestHelper h) {
        AbstractLift lift = lift(h);
        ServerPlayer player = player(h, lift);
        double clientFeet = player.getY(), oldX = player.getX();
        moveDeck(lift, 0.45);
        Boat obstacle = EntityType.BOAT.create(h.getLevel());
        obstacle.setPos(oldX + 1.0, clientFeet, player.getZ());
        h.getLevel().addFreshEntity(obstacle);
        player.connection.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(oldX + 1.0, clientFeet, player.getZ(), true));
        near(h, player.getX(), oldX, "Lift packet guard ignored an unrelated boat collider");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unvalidatedPlayerCannotEnterDeckBody(GameTestHelper h) {
        AbstractLift lift = lift(h);
        ServerPlayer player = player(h, lift);
        player.setPos(player.getX() + 3.0, lift.getBoundingBox().maxY - 0.45, player.getZ());
        player.connection.resetPosition();
        double oldX = player.getX();
        player.connection.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(
                        lift.getX(), player.getY(), player.getZ(), true));
        near(h, player.getX(), oldX, "Unvalidated player entered the lift body");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void risingDeckAcceptsInterpolatedMountedBoat(GameTestHelper h) {
        AbstractLift lift = lift(h);
        ServerPlayer player = player(h, lift);
        Boat boat = EntityType.BOAT.create(h.getLevel());
        boat.setPos(lift.getX(), lift.getBoundingBox().maxY, lift.getZ());
        boat.setDeltaMovement(Vec3.ZERO);
        boat.setOnGround(true);
        h.getLevel().addFreshEntity(boat);
        player.startRiding(boat, true);

        acknowledgeTeleport(player);
        boat.positionRider(player);
        player.connection.tick();
        double originalX = boat.getX();
        player.connection.handleMoveVehicle(
                boatPacket(h, boat, originalX + 0.125, boat.getY(), boat.getZ()));
        near(
                h,
                boat.getX(),
                originalX + 0.125,
                "Positive control: clear-space boat packet was ignored");
        boat.setPos(originalX, boat.getY(), boat.getZ());
        boat.positionRider(player);
        player.connection.tick();
        ServerboundMoveVehiclePacket delayed = new ServerboundMoveVehiclePacket(boat);
        double clientFeet = boat.getY();
        moveDeck(lift, 0.45);
        near(h, boat.getY(), lift.getBoundingBox().maxY, "Boat server carry failed");
        player.connection.handleMoveVehicle(delayed);
        near(h, boat.getY(), clientFeet, "Valid boat interpolation caused vehicle correction");
        h.succeed();
    }
}
