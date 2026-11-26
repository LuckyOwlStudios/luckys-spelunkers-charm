// Landing transition client smoke.
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
public final class LandingTransitionClientSmoke {
    private static boolean markerDone;
    private static int scene, ticks, captures, lastCapture = -1, samples, landings, launches;
    private static int captureFrames = -1;
    private static double captureHorizontal, minimumCaptureAverage = 10;
    private static boolean wasAir;
    private static double maximumStep,
            maximumHorizontal,
            maximumTurn,
            minimumFacing = 1,
            minimumRailSpeed = 10;
    private static RepeatedLandingTests.Course course;
    private static ArmorStand camera;
    private static Vec3 previous, previousAxis;
    private static int previousTick;
    private static CompletableFuture<Void> pending;
    private static Throwable renderError;

    static boolean tick(Minecraft mc) throws Exception {
        if (renderError != null) throw new AssertionError(renderError);
        if (!markerDone) {
            if (!TrainPolishClientSmoke.tick(mc)) return false;
            markerDone = true;
            Files.writeString(
                    Path.of("train-polish-checks.txt"), "PASS " + TrainPolishClientSmoke.report());
        }
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
                                        player.teleportTo(62, 105, 108);
                                        for (int x = 0; x <= 6; x++)
                                            for (int z = 5; z <= 6; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                        course =
                                                new RepeatedLandingTests.Course(
                                                        level,
                                                        new BlockPos(0, 80, 80),
                                                        scene == 1 ? 67 : 65,
                                                        false,
                                                        scene == 1,
                                                        scene == 3 ? 0 : 6,
                                                        scene == 2 ? 1 : 2);
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
        camera.moveTo(
                (first.getX() + last.getX()) * .5,
                (first.getY() + last.getY()) * .5 + 22,
                120,
                180,
                35);
        camera.yHeadRot = camera.yHeadRotO = 180;
        var engine = course.engine;
        if (!(mc.level.getEntity(engine.getId()) instanceof AbstractMinecart visible)) {
            check(ticks < 45, "Engine missing from client");
            return false;
        }
        boolean air = ((MinecartMotionVisuals) engine).spelunkersCharm$airborne();
        if (air && !wasAir) launches++;
        if (!air && wasAir) {
            landings++;
            captureFrames = 0;
            captureHorizontal = 0;
        }
        wasAir = air;
        Vec3 axis = MinecartBodyPose.axis(visible, 1);
        if (ticks > 15) {
            int elapsed = Math.max(1, visible.tickCount - previousTick);
            if (previous != null) {
                double step = visible.position().distanceTo(previous) / elapsed,
                        horizontal =
                                visible.position().subtract(previous).horizontalDistance()
                                        / elapsed;
                maximumStep = Math.max(maximumStep, step);
                maximumHorizontal = Math.max(maximumHorizontal, horizontal);
                check(
                        step < 2.5 && horizontal < 1.35,
                        "Landing position jumped: " + step + ", " + horizontal);
                if (captureFrames >= 0) {
                    captureHorizontal += horizontal;
                    if (++captureFrames == 5) {
                        double average = captureHorizontal / 5;
                        minimumCaptureAverage = Math.min(minimumCaptureAverage, average);
                        check(average > .12, "Rendered capture paused: " + average);
                        captureFrames = -1;
                    }
                }
            }
            if (previousAxis != null) {
                double turn =
                        Math.toDegrees(Math.acos(Math.clamp(axis.dot(previousAxis), -1, 1)))
                                / elapsed;
                maximumTurn = Math.max(maximumTurn, turn);
                check(turn < 95, "Landing model flipped: " + turn);
            }
            if (MinecartTrainBrakes.onRail(engine)) {
                double facing = axis.multiply(1, 0, 1).normalize().dot(engine.bodyDirection());
                minimumFacing = Math.min(minimumFacing, facing);
                check(facing > -.15, "Rendered engine changed its physical front: " + facing);
                if (landings > 0) {
                    double speed = engine.getDeltaMovement().horizontalDistance();
                    minimumRailSpeed = Math.min(minimumRailSpeed, speed);
                    check(speed > .10, "Captured engine stalled: " + speed);
                }
            }
            check(!engine.emergencyBrake(), "Landing enabled the brake");
            samples++;
        }
        previous = visible.position();
        previousAxis = axis;
        previousTick = visible.tickCount;
        if (ticks % 10 == 0)
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            TrainRegressionFixture.intact(
                                                    mc.getSingleplayerServer().overworld(),
                                                    course.carts));
        check(ticks < 430, "Landing scene timed out: " + scene);
        if (!course.finished()) return false;
        check(
                launches >= 2 && landings >= 2,
                "Scene missed consecutive captures: " + launches + ", " + landings);
        if (pending != null) {
            pending.join();
            pending = null;
        }
        var old = course;
        pending = mc.getSingleplayerServer().submit(() -> old.carts.forEach(Entity::discard));
        if (scene == 3) {
            mc.setCameraEntity(mc.player);
            return true;
        }
        scene++;
        course = null;
        ticks = landings = launches = 0;
        wasAir = false;
        previous = previousAxis = null;
        camera = null;
        captureFrames = -1;
        return false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.landingTransitionOnly")
                || course == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        int key = scene * 1000 + ticks;
        if (lastCapture == key || ticks != 35 && ticks != 65 && ticks != 105 && ticks != 145)
            return;
        try (var frame = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            lastCapture = key;
            frame.writeToFile(Path.of(String.format("landing-%02d-%03d.png", scene, ticks)));
            captures++;
        } catch (Throwable e) {
            renderError = e;
        }
    }

    static String report() {
        return "scenes=4 samples="
                + samples
                + " captures="
                + captures
                + " maximum_step="
                + maximumStep
                + " maximum_horizontal="
                + maximumHorizontal
                + " maximum_turn="
                + maximumTurn
                + " minimum_facing="
                + minimumFacing
                + " minimum_rail_speed="
                + minimumRailSpeed
                + " minimum_capture_horizontal="
                + minimumCaptureAverage;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
