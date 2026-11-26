// Train regression client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrainRegressionClientSmoke {
    private static int scene = -1,
            ticks,
            warmup,
            landed,
            captures,
            lastCapture = -1,
            samples,
            topViews,
            completedJumps;
    private static CompletableFuture<Void> pending;
    private static ArrayList<AbstractMinecart> carts;
    private static TrainRegressionFixture.Loop loop;
    private static ArmorStand camera;
    private static Throwable renderError;
    private static boolean[] flew;
    private static Vec3[] previous, previousAxis;
    private static int[] previousTick;
    private static double maximumGap,
            maximumStep,
            maximumHorizontalStep,
            maximumPoseStep,
            minimumFacing = 1;
    private static double downhillStart;
    private static final int[] heights = {0, 0, 24, 24, 80, 80};

    static boolean tick(Minecraft mc) throws Exception {
        if (renderError != null) throw new AssertionError(renderError);
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (scene < 0 || carts == null) {
            if (scene < 0) {
                scene = 0;
                mc.options.renderDistance().set(8);
                mc.options.simulationDistance().set(12);
                mc.options.entityDistanceScaling().set(2.0);
            }
            if (warmup++ == 0) {
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
                                            player.teleportTo(60, 75, 125);
                                            player.addEffect(
                                                    new net.minecraft.world.effect
                                                            .MobEffectInstance(
                                                            net.minecraft.world.effect.MobEffects
                                                                    .NIGHT_VISION,
                                                            20000,
                                                            0,
                                                            false,
                                                            false));
                                            for (int x = 0; x <= 11; x++)
                                                for (int z = 5; z <= 8; z++) {
                                                    level.getChunk(x, z);
                                                    level.setChunkForced(x, z, true);
                                                }
                                        });
                return false;
            }
            if (warmup < 35) return false;
            var id = mc.player.getUUID();
            int selected = scene;
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var level = mc.getSingleplayerServer().overworld();
                                        var player =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(id);
                                        if (selected < 4)
                                            carts =
                                                    TrainRegressionFixture.curve(
                                                            level,
                                                            new BlockPos(selected * 40, 70, 80),
                                                            selected,
                                                            player);
                                        else if (selected < 10)
                                            carts =
                                                    TrainRegressionFixture.jump(
                                                            level,
                                                            new BlockPos(0, 20, 120),
                                                            heights[selected - 4],
                                                            (selected - 4) % 2 == 1,
                                                            8);
                                        else if (selected == 10) {
                                            carts =
                                                    TrainRegressionFixture.downhill(
                                                            level,
                                                            new BlockPos(0, 70, 80),
                                                            0,
                                                            true,
                                                            8);
                                            downhillStart = carts.getFirst().getX();
                                        } else {
                                            loop =
                                                    TrainRegressionFixture.loop(
                                                            level, new BlockPos(0, 70, 80), 12);
                                            carts = loop.carts();
                                        }
                                        player.teleportTo(
                                                selected < 4
                                                        ? selected * 40 + 16
                                                        : selected < 10 ? 60 : 28,
                                                75,
                                                selected >= 4 && selected < 10 ? 140 : 102);
                                    });
            return false;
        }
        ticks++;
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        if (scene < 4) camera.moveTo(scene * 40 + 16, 82, 96, 180, 90);
        else if (scene < 10) {
            double center = (carts.getFirst().getX() + carts.getLast().getX()) * .5,
                    y = (carts.getFirst().getY() + carts.getLast().getY()) * .5;
            camera.moveTo(center, y + 12, 177, 180, 14);
        } else if (scene == 10) camera.moveTo(44, 112, 143, 180, 31);
        else camera.moveTo(15, 114, 95, 180, 90);
        camera.yHeadRot = camera.getYRot();
        camera.yHeadRotO = camera.getYRot();
        if (flew == null) {
            flew = new boolean[carts.size()];
            previous = new Vec3[carts.size()];
            previousAxis = new Vec3[carts.size()];
            previousTick = new int[carts.size()];
        }
        int down = 0;
        for (int i = 0; i < carts.size(); i++) {
            var server = carts.get(i);
            if (!(mc.level.getEntity(server.getId()) instanceof AbstractMinecart cart)) {
                if (ticks < 45) return false;
                throw new AssertionError(
                        "Rendered regression cart missing: scene="
                                + scene
                                + ", cart="
                                + i
                                + ", server="
                                + server);
            }
            var visual = (MinecartMotionVisuals) cart;
            Vec3 axis = MinecartBodyPose.axis(cart, 1);
            if (ticks > 15) {
                Vec3 expected;
                if (scene < 4) expected = MinecartHingeGeometry.supportedAxis(server);
                else if (scene < 10)
                    expected =
                            new Vec3((scene - 4) % 2 == 1 && i == carts.size() - 1 ? -1 : 1, 0, 0);
                else if (scene == 10) expected = new Vec3(1, 0, 0);
                else {
                    double at = loop.route().progress(cart.position());
                    expected =
                            loop.route()
                                    .point(at + .03)
                                    .subtract(loop.route().point(at - .03))
                                    .normalize();
                }
                if (axis.horizontalDistanceSqr() > .003) {
                    double facing = axis.multiply(1, 0, 1).normalize().dot(expected);
                    minimumFacing = Math.min(minimumFacing, facing);
                    check(
                            facing > -.15,
                            "Rendered cart flipped its front: scene="
                                    + scene
                                    + ", cart="
                                    + i
                                    + ", facing="
                                    + facing);
                }
                if (previous[i] != null) {
                    double step =
                            cart.position().distanceTo(previous[i])
                                    / Math.max(1, cart.tickCount - previousTick[i]);
                    maximumStep = Math.max(maximumStep, step);
                    check(step < 4.6, "Rendered landing teleported: " + step + ", scene=" + scene);
                    if (scene >= 4 && scene < 10) {
                        double horizontal =
                                cart.position().subtract(previous[i]).horizontalDistance()
                                        / Math.max(1, cart.tickCount - previousTick[i]);
                        maximumHorizontalStep = Math.max(maximumHorizontalStep, horizontal);
                        check(
                                horizontal < 1.35,
                                "Rail capture snapped horizontally: " + horizontal);
                        if (previousAxis[i] != null) {
                            double turn =
                                    Math.toDegrees(
                                                    Math.acos(
                                                            Math.clamp(
                                                                    axis.dot(previousAxis[i]),
                                                                    -1,
                                                                    1)))
                                            / Math.max(1, cart.tickCount - previousTick[i]);
                            maximumPoseStep = Math.max(maximumPoseStep, turn);
                            check(turn < 16.2, "Capture pose snapped: " + turn);
                        }
                    }
                }
                if (i > 0
                        && mc.level.getEntity(carts.get(i - 1).getId())
                                instanceof AbstractMinecart parent) {
                    double gap = cart.distanceTo(parent);
                    maximumGap = Math.max(maximumGap, gap);
                    check(
                            gap > .95 && gap < 3.3,
                            "Rendered train spacing broke: "
                                    + gap
                                    + ", scene="
                                    + scene
                                    + ", server_gap="
                                    + server.distanceTo(carts.get(i - 1)));
                    if (scene < 4) {
                        check(
                                axis.scale(MinecartHingeGeometry.side(cart, parent))
                                                .dot(parent.position().subtract(cart.position()))
                                        > -.08,
                                "Top-view rear hook crossed a cart");
                        check(
                                MinecartBodyPose.axis(parent, 1)
                                                .scale(MinecartHingeGeometry.side(parent, cart))
                                                .dot(cart.position().subtract(parent.position()))
                                        > -.08,
                                "Top-view front hook crossed a cart");
                    }
                }
            }
            previous[i] = cart.position();
            previousAxis[i] = axis;
            previousTick[i] = cart.tickCount;
            flew[i] |= visual.spelunkersCharm$airborne();
            if (flew[i] && cart.getY() < 24 && !visual.spelunkersCharm$airborne()) down++;
        }
        samples++;
        if (ticks % 10 == 0) {
            var snapshot = carts;
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            TrainRegressionFixture.intact(
                                                    mc.getSingleplayerServer().overworld(),
                                                    snapshot));
        }
        if (scene == 11 && (ticks == 100 || ticks == 145)) {
            int at = ticks;
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        if (at == 100) loop.engine().toggleEmergencyBrake();
                                        else loop.engine().selectDirection(true);
                                    });
        }
        boolean done =
                scene < 4
                        ? ticks >= 45
                        : scene < 10
                                ? (landed = down == carts.size() ? landed + 1 : 0) >= 15
                                : scene == 10
                                        ? ticks > 80 && carts.getFirst().getX() < downhillStart - 34
                                        : ticks >= 420;
        check(ticks < 650, "Regression scene timed out: " + scene);
        if (!done) return false;
        if (scene < 4) topViews++;
        else if (scene < 10) {
            for (boolean air : flew) check(air, "A regression cart never jumped");
            completedJumps++;
        } else if (scene == 10)
            check(
                    ((LocomotiveMinecart) carts.getFirst()).isReversed(),
                    "Downhill changed selected reverse mode");
        else check(loop.engine().isReversed(), "Hairpin changed selected reverse mode");
        if (pending != null) {
            pending.join();
            pending = null;
        }
        var old = carts;
        pending =
                mc.getSingleplayerServer()
                        .submit(
                                () -> {
                                    for (var c : old) c.discard();
                                });
        if (scene == 11) {
            check(
                    topViews == 4 && completedJumps == 6 && captures >= 12,
                    "Missing rendered regression evidence");
            mc.setCameraEntity(mc.player);
            return true;
        }
        scene++;
        carts = null;
        flew = null;
        previous = null;
        previousAxis = null;
        previousTick = null;
        ticks = warmup = landed = 0;
        return false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.trainRegressionOnly")
                || carts == null
                || ticks < 20
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        int key = scene * 1000 + ticks;
        boolean capture =
                scene < 4
                        ? ticks == 25
                        : scene < 10
                                ? ticks == 35 || ticks == 65 || ticks == 95 || ticks == 125
                                : scene == 10
                                        ? ticks == 35 || ticks == 85
                                        : ticks == 85 || ticks == 200 || ticks == 350;
        if (!capture || lastCapture == key) return;
        try {
            lastCapture = key;
            try (var frame =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                frame.writeToFile(
                        Path.of(String.format("train-regression-%02d-%03d.png", scene, ticks)));
                captures++;
            }
        } catch (Throwable failure) {
            renderError = failure;
        }
    }

    static String report() {
        return "top_views="
                + topViews
                + " jump_scenes="
                + completedJumps
                + " reverse_downhill=true reverse_hairpins=true samples="
                + samples
                + " captures="
                + captures
                + " maximum_gap="
                + maximumGap
                + " maximum_step="
                + maximumStep
                + " maximum_horizontal_step="
                + maximumHorizontalStep
                + " maximum_pose_step="
                + maximumPoseStep
                + " minimum_facing="
                + minimumFacing;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
