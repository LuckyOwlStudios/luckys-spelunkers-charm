// Trench descent client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrenchDescentClientSmoke {
    private static int scene, ticks, captures, lastCapture = -1, samples, landings, launches;
    private static boolean wasAir, perpendicular;
    private static double maximumStep, maximumTurn, minimumFacing = 1, minimumRailSpeed = 10;
    private static PerpendicularTrenchTests.Course course;
    private static ArmorStand camera;
    private static Vec3 previous, previousAxis;
    private static int previousTick;
    private static CompletableFuture<Void> pending;
    private static Throwable renderError;
    private static final java.util.Map<Integer, Vec3> cartAxes = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Integer> cartTicks = new java.util.HashMap<>();

    static boolean tick(Minecraft mc) throws Exception {
        if (renderError != null) throw new AssertionError(renderError);
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (course == null) {
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var level = mc.getSingleplayerServer().overworld();
                                        var player =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(id);
                                        player.stopRiding();
                                        player.setGameMode(GameType.SPECTATOR);
                                        player.teleportTo(64, 105, 144);
                                        for (int x = 0; x <= 8; x++)
                                            for (int z = 5; z <= 13; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                        course =
                                                new PerpendicularTrenchTests.Course(
                                                        level,
                                                        new BlockPos(0, 80, 80),
                                                        scene == 0 ? 0 : 2,
                                                        true,
                                                        scene == 2,
                                                        6);
                                    });
            return false;
        }
        ticks++;
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            mc.options.hideGui = true;
            mc.setCameraEntity(camera);
        }
        var first = course.carts.getFirst();
        var last = course.carts.getLast();
        double x = (first.getX() + last.getX()) * .5, z = (first.getZ() + last.getZ()) * .5;
        camera.moveTo(x, (first.getY() + last.getY()) * .5 + 22, z + 22, 180, 45);
        camera.yHeadRot = camera.yHeadRotO = 180;
        var engine = course.engine;
        if (!(mc.level.getEntity(engine.getId()) instanceof AbstractMinecart visible)) {
            check(ticks < 45, "Engine missing from client");
            return false;
        }
        boolean air = ((MinecartMotionVisuals) engine).spelunkersCharm$airborne();
        if (air && !wasAir) launches++;
        if (!air && wasAir) landings++;
        wasAir = air;
        Vec3 axis = MinecartBodyPose.axis(visible, 1);
        var local = course.local(engine.position());
        if (local.y < 3.4 && local.x > 61 && MinecartTrainBrakes.onRail(engine))
            perpendicular = true;
        if (ticks > 15) {
            int elapsed = Math.max(1, visible.tickCount - previousTick);
            if (previous != null) {
                double step = visible.position().distanceTo(previous) / elapsed;
                maximumStep = Math.max(maximumStep, step);
                check(step < 2.5, "Trench position jumped: " + step);
            }
            if (previousAxis != null) {
                double turn =
                        Math.toDegrees(Math.acos(Math.clamp(axis.dot(previousAxis), -1, 1)))
                                / elapsed;
                maximumTurn = Math.max(maximumTurn, turn);
                check(turn < 100, "Trench model flipped: " + turn);
            }
            if (MinecartTrainBrakes.onRail(engine)) {
                double facing = axis.multiply(1, 0, 1).normalize().dot(engine.bodyDirection());
                minimumFacing = Math.min(minimumFacing, facing);
                check(facing > -.15, "Rendered engine changed its front: " + facing);
                if (perpendicular) {
                    double speed = engine.getDeltaMovement().horizontalDistance();
                    minimumRailSpeed = Math.min(minimumRailSpeed, speed);
                    check(speed > .10, "Captured engine stalled: " + speed);
                }
            }
            check(
                    !engine.emergencyBrake() && engine.isReversed() == (scene == 2),
                    "Trench changed the drive mode");
            samples++;
        }
        previous = visible.position();
        previousAxis = axis;
        previousTick = visible.tickCount;
        for (var serverCart : course.carts)
            if (mc.level.getEntity(serverCart.getId()) instanceof AbstractMinecart cart) {
                Vec3 direction = MinecartBodyPose.axis(cart, 1),
                        old = cartAxes.put(cart.getId(), direction);
                Integer oldTick = cartTicks.put(cart.getId(), cart.tickCount);
                if (ticks > 15 && old != null && oldTick != null) {
                    double turn =
                            Math.toDegrees(Math.acos(Math.clamp(direction.dot(old), -1, 1)))
                                    / Math.max(1, cart.tickCount - oldTick);
                    maximumTurn = Math.max(maximumTurn, turn);
                    check(turn < 100, "Rendered wagon flipped: " + turn);
                }
                var track = MinecartTrack.current(cart);
                if (track != null
                        && track.point(track.offset(cart.position())).distanceToSqr(cart.position())
                                < .015) {
                    Vec3 expected = course.headings.get(track.pos());
                    if (expected != null && direction.horizontalDistanceSqr() > .5) {
                        double dot =
                                direction.multiply(1, 0, 1).normalize().dot(expected)
                                        * (serverCart == engine && scene == 2 ? -1 : 1);
                        check(dot > -.15, "Rendered wagon reversed its front on rails: " + dot);
                    }
                }
            }
        if (ticks % 10 == 0)
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            TrainRegressionFixture.intact(
                                                    mc.getSingleplayerServer().overworld(),
                                                    course.carts));
        check(ticks < 600, "Trench scene timed out: " + scene);
        if (!course.finished()) return false;
        check(
                perpendicular && launches >= 2 && landings >= 2,
                "Scene missed consecutive captures: " + launches + ", " + landings);
        if (pending != null) {
            pending.join();
            pending = null;
        }
        var old = course;
        pending = mc.getSingleplayerServer().submit(() -> old.carts.forEach(Entity::discard));
        if (scene == 2) {
            mc.setCameraEntity(mc.player);
            return true;
        }
        scene++;
        course = null;
        ticks = landings = launches = 0;
        wasAir = perpendicular = false;
        previous = previousAxis = null;
        camera = null;
        return false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.trenchDescentOnly")
                || course == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        int key = scene * 1000 + ticks;
        if (lastCapture == key
                || ticks != 35
                        && ticks != 65
                        && ticks != 100
                        && ticks != 125
                        && ticks != 160
                        && ticks != 190) return;
        try (var frame = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            lastCapture = key;
            frame.writeToFile(Path.of(String.format("trench-%02d-%03d.png", scene, ticks)));
            captures++;
        } catch (Throwable e) {
            renderError = e;
        }
    }

    static String report() {
        return "scenes=3 samples="
                + samples
                + " captures="
                + captures
                + " maximum_step="
                + maximumStep
                + " maximum_turn="
                + maximumTurn
                + " minimum_facing="
                + minimumFacing
                + " minimum_rail_speed="
                + minimumRailSpeed;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
