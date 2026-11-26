// Train stability client smoke.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrainStabilityClientSmoke {
    private static int phase, ticks, captures, lastCapture = -1, samples, loadingTicks;
    private static ArrayList<AbstractMinecart> carts;
    private static CompletableFuture<Void> pending;
    private static ArmorStand camera;
    private static Throwable error;
    private static final Vec3[] previous = new Vec3[13];
    private static double maximumGap, maximumTurn, settledTurn;
    private static final StringBuilder trace =
            new StringBuilder("tick,cart,x,y,z,yaw,pitch,gap,turn\n");

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
                                        player.teleportTo(12, 69, 148);
                                        for (int x = 0; x <= 1; x++)
                                            for (int z = 7; z <= 8; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                        player.addEffect(
                                                new net.minecraft.world.effect.MobEffectInstance(
                                                        net.minecraft.world.effect.MobEffects
                                                                .NIGHT_VISION,
                                                        2400,
                                                        0,
                                                        false,
                                                        false));
                                        var fixture =
                                                new TrainStabilityFixture(
                                                        level, new BlockPos(0, 64, 120));
                                        fixture.build();
                                        carts = fixture.train();
                                    });
            phase = 1;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        for (var cart : carts)
            if (mc.level.getEntity(cart.getId()) == null) {
                if (cart.isAlive() && ++loadingTicks < 400) return false;
                throw new AssertionError(
                        "Stopped-ramp cart did not load: "
                                + cart
                                + ", removal="
                                + cart.getRemovalReason());
            }
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            camera.moveTo(12, 70, 146, 180, 4);
            camera.yHeadRot = 180;
            camera.yHeadRotO = 180;
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        ticks++;
        for (int i = 0; i < carts.size(); i++) {
            if (!(mc.level.getEntity(carts.get(i).getId()) instanceof AbstractMinecart cart)) {
                if (ticks < 80 && carts.get(i).isAlive()) return false;
                throw new AssertionError(
                        "Stopped-ramp wagon disappeared: "
                                + i
                                + ", serverAlive="
                                + carts.get(i).isAlive()
                                + ", serverPosition="
                                + carts.get(i).position()
                                + ", removal="
                                + carts.get(i).getRemovalReason());
            }
            float yaw = MinecartBodyPose.yaw(cart, 1), pitch = MinecartBodyPose.pitch(cart, yaw, 1);
            Vec3 axis = MinecartBodyPose.axis(cart, 1);
            double turn =
                    previous[i] == null
                            ? 0
                            : Math.toDegrees(Math.acos(Math.clamp(previous[i].dot(axis), -1, 1)));
            previous[i] = axis;
            if (ticks > 40) {
                maximumTurn = Math.max(maximumTurn, turn);
                check(turn < 26, "Stopped-ramp body snapped: " + i + ", turn=" + turn);
            }
            if (ticks > 250) settledTurn = Math.max(settledTurn, turn);
            double gap = 0;
            if (i > 0
                    && mc.level.getEntity(carts.get(i - 1).getId())
                            instanceof AbstractMinecart parent) {
                gap = cart.distanceTo(parent);
                maximumGap = Math.max(maximumGap, gap);
                check(
                        ticks < 40 || (gap > .95 && gap < 3),
                        "Stopped-ramp drawbar distorted: " + i + ", gap=" + gap);
            }
            trace.append(ticks)
                    .append(',')
                    .append(i)
                    .append(',')
                    .append(cart.getX())
                    .append(',')
                    .append(cart.getY())
                    .append(',')
                    .append(cart.getZ())
                    .append(',')
                    .append(yaw)
                    .append(',')
                    .append(pitch)
                    .append(',')
                    .append(gap)
                    .append(',')
                    .append(turn)
                    .append('\n');
        }
        samples++;
        if (ticks % 20 == 0) Files.writeString(Path.of("stopped-ramp-client-trace.csv"), trace);
        if (ticks < 440) return false;
        check(captures >= 4, "Stopped-ramp render evidence missing");
        mc.setCameraEntity(mc.player);
        return true;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.stoppedRampOnly")
                || phase != 1
                || ticks < 20
                || ticks % 50 != 0
                || lastCapture == ticks
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        try {
            lastCapture = ticks;
            try (var image =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Path.of(String.format("stopped-ramp-%03d.png", ticks)));
                captures++;
            }
        } catch (Throwable failure) {
            error = failure;
        }
    }

    static String report() {
        return "wagons=12 samples="
                + samples
                + " captures="
                + captures
                + " maximum_gap="
                + maximumGap
                + " maximum_turn="
                + maximumTurn
                + " settled_turn="
                + settledTurn
                + "\n";
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
