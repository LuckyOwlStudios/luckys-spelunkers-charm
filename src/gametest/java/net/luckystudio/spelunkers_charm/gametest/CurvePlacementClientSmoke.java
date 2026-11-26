// Curve placement client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class CurvePlacementClientSmoke {
    private static int scene,
            ticks,
            samples,
            curveSamples,
            placementSamples,
            captures,
            lastCapture = -1,
            stalled;
    private static double maximumBoundary, maximumQuarter, maximumStep, minimumFront = 1, distance;
    private static double minimumMovingFront = 1;
    private static boolean ready;
    private static boolean preloaded;
    private static int preloadTicks;
    private static boolean visibleReady;
    private static int visibilityWait;
    private static final ArrayList<AbstractMinecart> carts = new ArrayList<>();
    private static final Map<Integer, Vec3> firstPositions = new HashMap<>();
    private static final Map<Integer, Float> previousYaw = new HashMap<>();
    private static final Map<Integer, Integer> previousSampleTick = new HashMap<>();
    private static PassengerTrainRoute route;
    private static LocomotiveMinecart engine;
    private static ArmorStand camera;
    private static CompletableFuture<Void> pending;
    private static Throwable renderError;
    private static double previousProgress;

    static boolean tick(Minecraft mc) throws Exception {
        if (renderError != null) throw new AssertionError(renderError);
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (!preloaded) {
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var level = mc.getSingleplayerServer().overworld();
                                        for (int x = 0; x < 12; x++)
                                            for (int z = 4; z < 8; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                    });
            preloaded = true;
            return false;
        }
        if (preloadTicks++ < 25) return false;
        if (!ready) {
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
                                        player.teleportTo(scene == 0 ? 173 : 16, 96, 98);
                                        for (int x = 0; x < 12; x++)
                                            for (int z = 4; z < 8; z++) {
                                                level.getChunk(x, z);
                                                level.setChunkForced(x, z, true);
                                            }
                                        if (scene == 0) {
                                            var shapes =
                                                    new RailShape[] {
                                                        RailShape.SOUTH_EAST,
                                                        RailShape.SOUTH_WEST,
                                                        RailShape.NORTH_WEST,
                                                        RailShape.NORTH_EAST
                                                    };
                                            var directions =
                                                    new Direction[] {
                                                        Direction.NORTH,
                                                        Direction.EAST,
                                                        Direction.SOUTH,
                                                        Direction.WEST
                                                    };
                                            for (int shape = 0; shape < 4; shape++)
                                                for (int look = 0; look < 4; look++)
                                                    for (int kind = 0; kind < 2; kind++) {
                                                        var pos =
                                                                new BlockPos(
                                                                        162 + shape * 7,
                                                                        84,
                                                                        80 + look * 4 + kind * 2);
                                                        for (int x = -1; x <= 1; x++)
                                                            for (int z = -1; z <= 1; z++)
                                                                level.setBlock(
                                                                        pos.offset(x, -1, z),
                                                                        Blocks.STONE
                                                                                .defaultBlockState(),
                                                                        2);
                                                        for (int pass = 0; pass < 2; pass++)
                                                            level.setBlock(
                                                                    pos,
                                                                    Blocks.RAIL
                                                                            .defaultBlockState()
                                                                            .setValue(
                                                                                    RailBlock.SHAPE,
                                                                                    shapes[shape]),
                                                                    2);
                                                        var cart =
                                                                CurvePlacementTests.place(
                                                                        level,
                                                                        player,
                                                                        pos,
                                                                        kind == 0
                                                                                ? ModItems
                                                                                        .LOCOMOTIVE
                                                                                        .get()
                                                                                : Items.MINECART,
                                                                        directions[look]);
                                                        CurvePlacementTests.aligned(cart);
                                                        carts.add(cart);
                                                    }
                                        } else {
                                            route =
                                                    new PassengerTrainRoute(
                                                            level, new BlockPos(0, 82, 80));
                                            route.build();
                                            BlockPos pos = null;
                                            double at = 0;
                                            Vec3 along = null;
                                            for (double d = .25; d < route.length; d += .25) {
                                                var p = route.point(d);
                                                if (level.getBlockState(BlockPos.containing(p))
                                                                .getBlock()
                                                        == Blocks.RAIL) {
                                                    pos = BlockPos.containing(p);
                                                    at = d;
                                                    along =
                                                            route.point(d + .01)
                                                                    .subtract(route.point(d - .01))
                                                                    .normalize();
                                                    break;
                                                }
                                            }
                                            if (pos == null)
                                                throw new AssertionError("Native curve missing");
                                            engine =
                                                    (LocomotiveMinecart)
                                                            CurvePlacementTests.place(
                                                                    level,
                                                                    player,
                                                                    pos,
                                                                    ModItems.LOCOMOTIVE.get(),
                                                                    Direction.getNearest(
                                                                            along.x, 0, along.z));
                                            CurvePlacementTests.aligned(engine);
                                            carts.add(engine);
                                            at = route.progress(engine.position());
                                            if (scene == 2)
                                                for (int i = 1; i <= 6; i++) {
                                                    double s = at - i * 1.55;
                                                    var p = route.point(s);
                                                    var c = new Minecart(level, p.x, p.y, p.z);
                                                    TrainRegressionFixture.facing(
                                                            c,
                                                            route.point(s + .02)
                                                                    .subtract(route.point(s - .02))
                                                                    .normalize());
                                                    level.addFreshEntity(c);
                                                    if (MinecartCouplings.get(level)
                                                                    .link(carts.getLast(), c)
                                                            != MinecartCouplings.Result.LINKED)
                                                        throw new AssertionError(
                                                                "Native curve train did not link");
                                                    carts.add(c);
                                                }
                                            for (int slot = 0; slot < 3; slot++)
                                                engine.setItem(slot, new ItemStack(Items.COAL, 16));
                                        }
                                        ready = true;
                                    });
            return false;
        }
        if (!visibleReady) {
            boolean visible =
                    carts.stream()
                            .allMatch(
                                    c -> mc.level.getEntity(c.getId()) instanceof AbstractMinecart);
            if (!visible) {
                check(++visibilityWait < 200, "Placed carts did not reach the client");
                return false;
            }
            visibleReady = true;
            ticks = 0;
        }
        ticks++;
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        camera.moveTo(
                scene == 0 ? 173 : 16, scene == 0 ? 104 : 112, scene == 0 ? 109 : 113, 180, 55);
        camera.yHeadRot = camera.yHeadRotO = 180;
        for (var server : carts)
            if (mc.level.getEntity(server.getId()) instanceof AbstractMinecart visible) {
                if (ticks > 8 && scene == 0) {
                    var start =
                            firstPositions.computeIfAbsent(
                                    visible.getId(), ignored -> visible.position());
                    check(
                            start.distanceTo(visible.position()) < .005,
                            "Placed curve cart drifted or snapped");
                    var track = MinecartTrack.current(visible);
                    check(
                            track != null
                                    && track.point(track.offset(visible.position()))
                                                    .distanceToSqr(visible.position())
                                            < 1e-6,
                            "Curve spawn was beside the rail");
                    double expected =
                            server instanceof LocomotiveMinecart e
                                    ? e.bodyYaw()
                                    : ((MinecartMotionVisuals) server)
                                            .spelunkersCharm$motion()
                                            .visualYaw;
                    double dot =
                            MinecartHingeGeometry.axis(Math.toRadians(expected), 0)
                                    .dot(MinecartBodyPose.axis(visible, 1));
                    minimumFront = Math.min(minimumFront, dot);
                    check(dot > .99, "Placed model disagreed with its rail front");
                    placementSamples++;
                }
                if (scene > 0 && ticks > 10 && MinecartTrainBrakes.onRail(server)) {
                    double progress = route.progress(visible.position());
                    Vec3 expected =
                            route.point(progress + .02)
                                    .subtract(route.point(progress - .02))
                                    .normalize();
                    double front = MinecartBodyPose.axis(visible, 1).dot(expected);
                    minimumMovingFront = Math.min(minimumMovingFront, front);
                    check(front > .1, "Moving curve model reversed its fixed front: " + front);
                    float yaw = MinecartBodyPose.yaw(visible, 0);
                    Float previous = previousYaw.get(visible.getId());
                    Integer sampledTick = previousSampleTick.get(visible.getId());
                    if (previous != null
                            && sampledTick != null
                            && visible.tickCount == sampledTick + 1) {
                        double jump = Math.abs(Mth.wrapDegrees(yaw - previous));
                        maximumBoundary = Math.max(maximumBoundary, jump);
                        check(
                                jump < 8,
                                "Curve render jumped at tick boundary: "
                                        + jump
                                        + ", scene="
                                        + scene
                                        + ", tick="
                                        + ticks
                                        + ", cart="
                                        + visible.getId()
                                        + ", previous="
                                        + previous
                                        + ", yaw="
                                        + yaw
                                        + ", from="
                                        + visible.getPosition(0)
                                        + ", target="
                                        + visible.position());
                    }
                    for (int part = 1; part <= 4; part++) {
                        float next = MinecartBodyPose.yaw(visible, part * .25f);
                        double turn = Math.abs(Mth.wrapDegrees(next - yaw));
                        maximumQuarter = Math.max(maximumQuarter, turn);
                        check(
                                turn < 25,
                                "Curve rotated too abruptly: scene="
                                        + scene
                                        + ", tick="
                                        + ticks
                                        + ", part="
                                        + part
                                        + ", turn="
                                        + turn
                                        + ", yaw="
                                        + yaw
                                        + ", next="
                                        + next
                                        + ", old="
                                        + visible.getPosition(0)
                                        + ", pos="
                                        + visible.position());
                        yaw = next;
                    }
                    previousYaw.put(visible.getId(), yaw);
                    previousSampleTick.put(visible.getId(), visible.tickCount);
                    samples++;
                    var track = MinecartTrack.current(server);
                    if (track.first().direction().getAxis() != track.second().direction().getAxis())
                        curveSamples++;
                }
            }
        if (scene > 0 && mc.level.getEntity(engine.getId()) instanceof AbstractMinecart visible) {
            double progress = route.progress(visible.position());
            if (ticks > 15) {
                double step = route.delta(previousProgress, progress);
                maximumStep = Math.max(maximumStep, Math.abs(step));
                distance += step;
                check(
                        step > -.015 && step < 1,
                        "Visible curve movement reversed or jumped: scene="
                                + scene
                                + ", tick="
                                + ticks
                                + ", step="
                                + step
                                + ", pos="
                                + visible.position()
                                + ", server="
                                + engine.position()
                                + ", packet="
                                + ((MinecartMotionVisuals) visible)
                                        .spelunkersCharm$motion()
                                        .clientPacketVelocity
                                + ", rail="
                                + ((MinecartMotionVisuals) visible)
                                        .spelunkersCharm$motion()
                                        .clientPacketRailVelocity);
                var track = MinecartTrack.current(visible);
                if (track != null
                        && track.first().direction().getAxis()
                                != track.second().direction().getAxis()
                        && engine.getDeltaMovement().horizontalDistance() > .15
                        && step < .03) stalled++;
            }
            previousProgress = progress;
            check(
                    !engine.emergencyBrake() && !engine.isReversed(),
                    "Native curve changed selected mode");
        }
        if (scene > 0 && ticks % 10 == 0)
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            TrainRegressionFixture.intact(
                                                    mc.getSingleplayerServer().overworld(), carts));
        if (ticks < (scene == 0 ? 40 : 280)) return false;
        if (scene > 0) check(distance > 65, "Native curve engine stalled: " + distance);
        if (pending != null) {
            pending.join();
            pending = null;
        }
        var old = new ArrayList<>(carts);
        pending = mc.getSingleplayerServer().submit(() -> old.forEach(Entity::discard));
        if (scene == 2) {
            check(stalled <= 1, "Curved rails paused visible carts: " + stalled);
            mc.setCameraEntity(mc.player);
            return true;
        }
        scene++;
        ticks = 0;
        ready = visibleReady = false;
        visibilityWait = 0;
        carts.clear();
        previousYaw.clear();
        previousSampleTick.clear();
        firstPositions.clear();
        camera = null;
        distance = 0;
        return false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.curvePlacementOnly")
                || !ready
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        int key = scene * 1000 + ticks;
        if (key == lastCapture
                || scene == 0 && ticks != 25
                || scene > 0 && ticks != 65 && ticks != 125 && ticks != 185 && ticks != 245) return;
        try (var frame = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            lastCapture = key;
            frame.writeToFile(Path.of(String.format("curve-%02d-%03d.png", scene, ticks)));
            captures++;
        } catch (Throwable e) {
            renderError = e;
        }
    }

    static String report() {
        return "scenes=3 placement_samples="
                + placementSamples
                + " samples="
                + samples
                + " curve_samples="
                + curveSamples
                + " captures="
                + captures
                + " maximum_boundary="
                + maximumBoundary
                + " maximum_quarter="
                + maximumQuarter
                + " maximum_step="
                + maximumStep
                + " minimum_front="
                + minimumFront
                + " minimum_moving_front="
                + minimumMovingFront
                + " stalled_curve_ticks="
                + stalled;
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
