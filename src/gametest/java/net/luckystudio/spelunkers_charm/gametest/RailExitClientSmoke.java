// Client rail exit checks.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class RailExitClientSmoke {
    private static final int[][] CASES = {{1, 0}, {1, 7}, {2, 7}, {2, 0}, {0, 7}};
    private static final List<String> results = new ArrayList<>();
    private static volatile List<AbstractMinecart> carts = List.of();
    private static volatile ServerLevel level;
    private static volatile boolean done;
    private static volatile Throwable error;
    private static int scene, age, landed = -1, settle;
    private static Vec3 contact, stopped, clientStart;
    private static CompletableFuture<Void> pending;
    private static ArmorStand camera;

    static boolean tick(Minecraft mc) throws Exception {
        if (error != null) throw new AssertionError(error);
        if (pending == null) {
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        level = mc.getSingleplayerServer().overworld();
                                        var player =
                                                level.getServer().getPlayerList().getPlayer(id);
                                        player.setGameMode(GameType.SPECTATOR);
                                        player.teleportTo(30, 100, 100);
                                        for (int x = -1; x < 7; x++)
                                            for (int z = 4; z < 8; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                        carts =
                                                RailExitStopTests.spawn(
                                                        level,
                                                        new BlockPos(0, 82, 80),
                                                        CASES[scene][0],
                                                        CASES[scene][1]);
                                        age = 0;
                                        landed = -1;
                                        done = false;
                                        settle = 0;
                                        clientStart = null;
                                    });
            return false;
        }
        if (!pending.isDone()) return false;
        pending.join();
        if (carts.stream()
                .anyMatch(c -> !(mc.level.getEntity(c.getId()) instanceof AbstractMinecart)))
            return false;
        var head = carts.getFirst();
        var visible = (AbstractMinecart) mc.level.getEntity(head.getId());
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        camera.moveTo(head.getX() - 4, 97, 104, 180, 42);
        camera.yHeadRot = camera.yHeadRotO = 180;
        if (!done) return false;
        if (++settle == 15) clientStart = visible.position();
        if (settle < 45) return false;
        double drift = visible.position().distanceTo(clientStart),
                errorToServer = visible.position().distanceTo(head.position());
        check(
                drift < .04 && errorToServer < .1,
                "Visible engine continued after server stop: drift="
                        + drift
                        + ", error="
                        + errorToServer);
        try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            shot.writeToFile(Path.of("rail-exit-" + scene + ".png"));
        }
        results.add(
                "scene="
                        + scene
                        + " engine="
                        + CASES[scene][0]
                        + " wagons="
                        + CASES[scene][1]
                        + " ground_distance="
                        + head.position().distanceTo(contact)
                        + " client_drift="
                        + drift
                        + " server_error="
                        + errorToServer);
        level = null;
        pending =
                mc.getSingleplayerServer()
                        .submit(
                                () -> {
                                    for (var cart : carts) cart.discard();
                                });
        pending.join();
        pending = null;
        if (++scene == CASES.length) {
            Files.writeString(
                    Path.of("rail-exit-checks.txt"),
                    "PASS actual rail exits and client stop\n" + String.join("\n", results) + "\n");
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void serverTick(LevelTickEvent.Post event) {
        if (level == null || event.getLevel() != level || carts.isEmpty() || done || error != null)
            return;
        try {
            age++;
            var head = carts.getFirst();
            if (landed < 0 && head.onGround() && !MinecartTrainBrakes.onRail(head)) {
                landed = age;
                contact = head.position();
            }
            if (landed >= 0 && age == landed + 20) {
                check(
                        head.getDeltaMovement().horizontalDistanceSqr() == 0,
                        "Engine continued on ground: " + head.getDeltaMovement());
                check(
                        head.position().distanceTo(contact) < 1.5,
                        "Engine slid too far: " + head.position().distanceTo(contact));
                stopped = head.position();
            }
            if (landed >= 0 && age == landed + 70) {
                TrainRegressionFixture.intact(level, carts);
                check(
                        head.getDeltaMovement().horizontalDistanceSqr() == 0
                                && head.position().distanceTo(stopped) < .05,
                        "Ground engine restarted");
                done = true;
            }
            check(age < 190, "Engine did not exit onto ground");
        } catch (Throwable failure) {
            error = failure;
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
