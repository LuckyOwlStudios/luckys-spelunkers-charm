// Train high fall client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrainHighFallClientSmoke {
    private static int phase, ticks, captures, lastCapture = -1, samples, completed, warmup;
    private static ArrayList<AbstractMinecart> carts;
    private static CompletableFuture<Void> pending;
    private static ArmorStand camera;
    private static Throwable error;
    private static final boolean[] flew = new boolean[25];
    private static double maximumGap, maximumAngle, maximumServerError;
    private static long clientStarted;
    private static final StringBuilder trace = new StringBuilder("phase,tick,cart,x,y,z,air,vy\n");

    static boolean tick(Minecraft mc) throws Exception {
        if (error != null) throw new AssertionError(error);
        if (phase == 0) {
            mc.options.renderDistance().set(8);
            mc.options.simulationDistance().set(12);
            mc.options.entityDistanceScaling().set(2.0);
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
                                        player.teleportTo(54, 69, 144);
                                        player.addEffect(
                                                new net.minecraft.world.effect.MobEffectInstance(
                                                        net.minecraft.world.effect.MobEffects
                                                                .NIGHT_VISION,
                                                        2400,
                                                        0,
                                                        false,
                                                        false));
                                        var fixture =
                                                new TrainHighFallFixture(
                                                        level, new BlockPos(0, 20, 120));
                                        fixture.build();
                                        for (int x = 0; x < 12; x++)
                                            for (int z = 7; z <= 8; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                    });
            phase = 1;
            ticks = 0;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (carts == null) {
            if (++warmup == 40)
                pending =
                        mc.getSingleplayerServer()
                                .submit(
                                        () -> {
                                            carts =
                                                    new TrainHighFallFixture(
                                                                    mc.getSingleplayerServer()
                                                                            .overworld(),
                                                                    new BlockPos(0, 20, 120))
                                                            .train(24);
                                        });
            return false;
        }
        if (camera == null) {
            clientStarted = mc.level.getGameTime();
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            camera.moveTo(112, 69, 180, 125, 3);
            camera.yHeadRot = 125;
            camera.yHeadRotO = 125;
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        camera.moveTo(112, 69, 180, 132, 8);
        camera.yHeadRot = 132;
        camera.yHeadRotO = 132;
        ticks++;
        int landed = 0, visible = 0;
        if (ticks % 20 == 0) {
            var id = mc.player.getUUID();
            double center = (carts.getFirst().getX() + carts.getLast().getX()) * .5;
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            mc.getSingleplayerServer()
                                                    .getPlayerList()
                                                    .getPlayer(id)
                                                    .teleportTo(center, 69, 144));
        }
        for (int i = 0; i < carts.size(); i++) {
            if (!(mc.level.getEntity(carts.get(i).getId()) instanceof AbstractMinecart cart)) {
                if (ticks < 35) return false;
                throw new AssertionError("High-drop cart disappeared: " + i);
            }
            visible++;
            boolean air = ((MinecartMotionVisuals) cart).spelunkersCharm$airborne();
            flew[i] |= air;
            maximumServerError =
                    Math.max(
                            maximumServerError,
                            cart.position().distanceTo(carts.get(i).position()));
            if (flew[i] && cart.getY() < 24) landed++;
            maximumAngle =
                    Math.max(
                            maximumAngle,
                            Math.abs(
                                    MinecartBodyPose.pitch(
                                            cart, MinecartBodyPose.yaw(cart, 1), 1)));
            if (i > 0
                    && mc.level.getEntity(carts.get(i - 1).getId())
                            instanceof AbstractMinecart parent) {
                double gap = cart.distanceTo(parent);
                maximumGap = Math.max(maximumGap, gap);
                check(
                        gap < 4,
                        "High-drop drawbar stretched: "
                                + gap
                                + ", cart="
                                + i
                                + ", server_gap="
                                + carts.get(i).distanceTo(carts.get(i - 1)));
            }
            trace.append(phase)
                    .append(',')
                    .append(ticks)
                    .append(',')
                    .append(i)
                    .append(',')
                    .append(cart.getX())
                    .append(',')
                    .append(cart.getY())
                    .append(',')
                    .append(cart.getZ())
                    .append(',')
                    .append(air)
                    .append(',')
                    .append(cart.getDeltaMovement().y)
                    .append('\n');
        }
        samples++;
        if (ticks % 10 == 0) Files.writeString(Path.of("client-high-fall-trace.csv"), trace);
        check(
                ticks < 650,
                "Long train remained suspended over cliff: landed="
                        + landed
                        + ", head="
                        + carts.getFirst().position()
                        + ", tail="
                        + carts.getLast().position());
        if (landed != carts.size() || ticks < 60) return false;
        completed++;
        check(visible == 25 && captures >= 3, "Missing long-drop render evidence");
        mc.setCameraEntity(mc.player);
        return true;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.highFallOnly")
                || phase != 1
                || ticks < 10
                || ticks > 350
                || ticks % 10 != 0
                || lastCapture == ticks
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        try {
            lastCapture = ticks;
            try (var image =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Path.of(String.format("high-fall-%03d.png", ticks)));
                captures++;
            }
        } catch (Throwable failure) {
            error = failure;
        }
    }

    static String report() {
        return "wagons=24 completed="
                + completed
                + " samples="
                + samples
                + " captures="
                + captures
                + " maximum_gap="
                + maximumGap
                + " maximum_angle="
                + maximumAngle
                + " maximum_server_error="
                + maximumServerError
                + " client_ticks="
                + (Minecraft.getInstance().level.getGameTime() - clientStarted)
                + " server_cart_ticks="
                + carts.getFirst().tickCount
                + "\n";
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
