// Train flight client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
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
public final class TrainFlightClientSmoke {
    private static int phase, ticks, samples, wave, lastCapture = -1, captures, landingSounds;
    private static ArrayList<AbstractMinecart> carts;
    private static CompletableFuture<Void> pending;
    private static ArmorStand camera;
    private static boolean cargoLoaded;
    private static final boolean[] flew = new boolean[9], landed = new boolean[9];
    private static final Vec3[] previous = new Vec3[9];
    private static final Vec3[] previousAxis = new Vec3[9];
    private static final int[] previousTick = new int[9];
    private static final long[] previousWorld = new long[9];
    private static double maximumHitch,
            maximumStep,
            maximumPitch,
            maximumPoseStep,
            minimumGap = Double.POSITIVE_INFINITY;
    private static Throwable renderError;
    private static final StringBuilder trace =
            new StringBuilder("tick,cart,x,y,z,air,pitch,hitch\n");

    static boolean tick(Minecraft mc) throws Exception {
        if (phase == 3) return LocomotiveControlsClientSmoke.tick(mc);
        if (phase == 2) {
            if (TrainModelShowcase.tick(mc)) phase = 3;
            return false;
        }
        if (renderError != null) throw new RuntimeException(renderError);
        if (phase == 0) {
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
                                        player.setGameMode(GameType.SURVIVAL);
                                        player.getAbilities().flying = false;
                                        player.onUpdateAbilities();
                                        player.addEffect(
                                                new net.minecraft.world.effect.MobEffectInstance(
                                                        net.minecraft.world.effect.MobEffects
                                                                .NIGHT_VISION,
                                                        1000,
                                                        0,
                                                        false,
                                                        false));
                                        for (var cart :
                                                level.getEntitiesOfClass(
                                                        AbstractMinecart.class,
                                                        new AABB(-5, 80, 17, 95, 95, 115)))
                                            cart.discard();
                                        var fixture =
                                                TrainFlightOrderTests.ramp(
                                                        level, new BlockPos(0, 82, 72));
                                        carts = fixture.train(24.5, 8);
                                        var seat = carts.get(4);
                                        player.teleportTo(
                                                seat.getX(), seat.getY() + .5, seat.getZ());
                                        check(
                                                player.startRiding(seat, true),
                                                "Player could not board flight train");
                                    });
            phase = 1;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            camera.moveTo(44, 104, 112, 180, 20);
            camera.yHeadRot = 180;
            camera.yHeadRotO = 180;
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        ticks++;
        camera.moveTo((carts.getFirst().getX() + carts.getLast().getX()) * .5, 104, 112, 180, 20);
        if (ticks == 20 && !cargoLoaded) {
            var clientOld = mc.level.getEntity(carts.get(6).getId());
            Files.writeString(
                    Path.of("cargo-conversion-debug.txt"),
                    "client_before="
                            + clientOld
                            + ", tick="
                            + clientOld.tickCount
                            + ", server="
                            + carts.get(6)
                            + ", server_tick="
                            + carts.get(6).tickCount
                            + "\n");
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var old = carts.get(6);
                                        check(
                                                MinecartBlockLoading.load(
                                                        old, new ItemStack(Items.STONE), null),
                                                "Flight cargo load failed");
                                        var replacement =
                                                old.level()
                                                        .getEntitiesOfClass(
                                                                BlockMinecart.class,
                                                                old.getBoundingBox().inflate(1))
                                                        .getFirst();
                                        try {
                                            Files.writeString(
                                                    Path.of("cargo-conversion-debug.txt"),
                                                    "server_old="
                                                            + old
                                                            + ", tick="
                                                            + old.tickCount
                                                            + ", replacement="
                                                            + replacement
                                                            + ", source="
                                                            + ((MinecartMotionVisuals) replacement)
                                                                    .spelunkersCharm$replacedCartId()
                                                            + "\n",
                                                    java.nio.file.StandardOpenOption.APPEND);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        carts.set(6, replacement);
                                        cargoLoaded = true;
                                    });
            return false;
        }
        if (ticks > 30)
            check(
                    mc.player.isAlive()
                            && mc.player.getVehicle() != null
                            && mc.player.getVehicle().getId() == carts.get(4).getId(),
                    "Player lost flying seat");
        if (ticks > 360) throw new AssertionError("Client flight scene timed out");
        int airborne = 0, grounded = 0;
        for (int i = 0; i < carts.size(); i++) {
            if (!(mc.level.getEntity(carts.get(i).getId()) instanceof AbstractMinecart cart)) {
                if (ticks < 30) return false;
                throw new AssertionError("Flying cart disappeared: " + i);
            }
            var visuals = (MinecartMotionVisuals) cart;
            boolean air = visuals.spelunkersCharm$airborne();
            Vec3 axis = MinecartCouplerAnchors.axis(cart, 1);
            if (previousAxis[i] != null) {
                double turn =
                        Math.toDegrees(Math.acos(Math.clamp(axis.dot(previousAxis[i]), -1, 1)))
                                / Math.max(1, cart.tickCount - previousTick[i]);
                maximumPoseStep = Math.max(maximumPoseStep, turn);
                check(turn < 14.2, "Body articulation jumped: " + i + ", turn=" + turn);
            }
            previousAxis[i] = axis;
            if (previous[i] != null) {
                double step =
                        cart.position().distanceTo(previous[i])
                                / Math.max(1, cart.tickCount - previousTick[i]);
                maximumStep = Math.max(maximumStep, step);
                check(
                        step < 1.65,
                        "Client flight teleported: cart="
                                + i
                                + ", step="
                                + step
                                + ", tick_delta="
                                + (cart.tickCount - previousTick[i])
                                + ", world_delta="
                                + (mc.level.getGameTime() - previousWorld[i])
                                + ", previous="
                                + previous[i]
                                + ", current="
                                + cart.position()
                                + ", replacement_handled="
                                + visuals.spelunkersCharm$motion().replacementHandled);
            }
            previous[i] = cart.position();
            previousTick[i] = cart.tickCount;
            previousWorld[i] = mc.level.getGameTime();
            if (air) {
                flew[i] = true;
                airborne++;
                check(
                        cart.getPos(cart.getX(), cart.getY(), cart.getZ()) == null,
                        "Flight rendering snapped onto rail below");
                maximumPitch =
                        Math.max(
                                maximumPitch,
                                Math.abs(
                                        MinecartBodyPose.pitch(
                                                cart, MinecartBodyPose.yaw(cart, 1), 1)));
            }
            if (flew[i] && !air && cart.getY() < 84.25 && MinecartTrack.current(cart) != null)
                landed[i] = true;
            if (landed[i]) {
                grounded++;
                check(axis.x > .01, "Rail landing flipped a wagon's physical front: " + i);
            }
            double hitch = 0;
            if (i > 0
                    && mc.level.getEntity(carts.get(i - 1).getId())
                            instanceof AbstractMinecart parent) {
                double gap = parent.distanceTo(cart);
                minimumGap = Math.min(minimumGap, gap);
                check(gap > .98, "Client flight overlapped wagon bodies: " + i + ", gap=" + gap);
                Vec3 first = MinecartCouplerAnchors.center(parent, 1, parent.position()),
                        second = MinecartCouplerAnchors.center(cart, 1, cart.position());
                Vec3 start = MinecartCouplerAnchors.endpoint(parent, cart, 1, first);
                Vec3 end = MinecartCouplerAnchors.endpoint(cart, parent, 1, second);
                hitch = start.distanceTo(end);
                maximumHitch = Math.max(maximumHitch, hitch);
                check(hitch < 2.5, "Flying coupler detached from wagon ends: " + hitch);
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
                    .append(air)
                    .append(',')
                    .append(cart.getXRot())
                    .append(',')
                    .append(hitch)
                    .append('\n');
        }
        Files.writeString(Path.of("client-train-flight-trace.csv"), trace);
        samples++;
        if (airborne > 0 && airborne < carts.size()) wave++;
        if (ticks < 220 || grounded < carts.size()) return false;
        for (boolean air : flew) check(air, "A wagon never became airborne");
        check(
                wave > 10 && maximumPitch > 8 && captures >= 3,
                "Missing articulated flight or render evidence");
        check(cargoLoaded, "Missing flight cargo conversion");
        for (int i = 1; i < carts.size(); i++)
            check(
                    MinecartCouplings.get(
                                    (net.minecraft.server.level.ServerLevel) carts.get(i).level())
                            .linked(carts.get(i - 1).getUUID(), carts.get(i).getUUID()),
                    "Normal flight broke a coupling: " + i);
        mc.setCameraEntity(mc.player);
        check(
                landingSounds >= carts.size() && landingSounds <= carts.size() * 2,
                "Missing or repeated landing sounds: " + landingSounds);
        phase = 2;
        return false;
    }

    @SubscribeEvent
    public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()
                || !RailwayMotionSmoke.enabled()
                || phase != 1
                || !(event.getEntity() instanceof AbstractMinecart cart)) return;
        try {
            Files.writeString(
                    Path.of("cargo-conversion-events.txt"),
                    "leave="
                            + cart
                            + ", tick="
                            + cart.tickCount
                            + ", time="
                            + cart.level().getGameTime()
                            + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (RailwayMotionSmoke.enabled()
                && phase == 1
                && event.getSound() != null
                && event.getSound()
                        .getLocation()
                        .equals(net.luckystudio.spelunkers_charm.SpelunkersCharm.id("cart_land")))
            landingSounds++;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!RailwayMotionSmoke.enabled()
                || phase != 1
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ticks < 35
                || ticks > 135
                || ticks % 5 != 0
                || lastCapture == ticks) return;
        try {
            lastCapture = ticks;
            Minecraft mc = Minecraft.getInstance();
            try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                frame.writeToFile(Path.of(String.format("train-flight-frame-%03d.png", ticks)));
                captures++;
            }
        } catch (Throwable error) {
            renderError = error;
        }
    }

    static String report() {
        return "flight_rider=true flight_cargo="
                + cargoLoaded
                + " flight_samples="
                + samples
                + " flight_wave_frames="
                + wave
                + " flight_max_hitch="
                + maximumHitch
                + " flight_max_step="
                + maximumStep
                + " flight_min_gap="
                + minimumGap
                + " flight_pitch="
                + maximumPitch
                + " flight_pose_step="
                + maximumPoseStep
                + " flight_landings="
                + landingSounds
                + " flight_captures="
                + captures
                + " "
                + TrainModelShowcase.report()
                + " "
                + LocomotiveControlsClientSmoke.report()
                + "\n";
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
