// Railway motion smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class RailwayMotionSmoke {
    private static int stage, ticks, total, pistons, lightBrakes, heavyBrakes, sparks, lockedFrames;
    private static LocomotiveMinecart engine;
    private static net.minecraft.world.phys.Vec3 previousCurvePosition;
    private static boolean reversedOnClient;
    private static double maximumCurveStep;
    private static int curveSamples;
    private static int smoothCompatibilityTicks;
    private static LocomotiveMinecart gapEngine;
    private static net.minecraft.world.phys.Vec3 previousWood, previousGap;
    private static int contactSamples;
    private static double maximumContactStep;
    private static float previousBodyYaw = Float.NaN;
    private static int previousCurveTick, previousWoodTick, previousGapTick;
    private static Minecart rollingWood, rollingIron;
    private static final ArrayList<net.minecraft.client.resources.sounds.SoundInstance>
            rollingSounds = new ArrayList<>();
    private static float woodVolume, ironVolume, riderVolume;
    private static net.minecraft.world.entity.vehicle.AbstractMinecart conversionCart;
    private static LocomotiveMinecart conversionEngine;
    private static net.minecraft.world.phys.Vec3 previousCargo;
    private static long previousCargoTime;
    private static int conversionSource, conversionSamples;
    private static double maximumConversionStep, conversionLag;
    private static final StringBuilder conversionTrace =
            new StringBuilder("stage,tick,time,cart,x,server_x,step,elapsed\n");
    private static final ArrayList<net.minecraft.world.entity.vehicle.AbstractMinecart> syncTrain =
            new ArrayList<>();
    private static final Set<BlockPos> detected = new java.util.HashSet<>();
    private static final Map<Integer, net.minecraft.world.phys.Vec3> syncPrevious =
            new java.util.HashMap<>();
    private static final Map<Integer, Integer> syncTicks = new java.util.HashMap<>();
    private static double maximumPositionError, maximumStoppedGapError;
    private static final StringBuilder syncTrace =
            new StringBuilder("tick,cart,server_x,client_x,error\n");
    private static final StringBuilder brakeTrace =
            new StringBuilder(
                    "tick,server_braking,client_braking,emergency,server_x,client_x,player_x\n");
    private static PassengerTrainRoute riderRoute;
    private static final ArrayList<net.minecraft.world.entity.vehicle.AbstractMinecart> riderTrain =
            new ArrayList<>();
    private static LocomotiveMinecart riderEngine;
    private static double riderBefore, riderDistance, riderWindow;
    private static int riderSamples, riderContacts;
    private static final StringBuilder riderTrace =
            new StringBuilder("tick,progress,speed,vehicle,contacts\n");
    private static final StringBuilder contactTrace =
            new StringBuilder("tick,wood_x,wood_y,gap_x,gap_y\n");
    private static final StringBuilder curveTrace =
            new StringBuilder(
                    "tick,client_x,client_y,client_z,target_x,target_z,server_x,server_z,entity_tick,world_time\n");
    private static net.minecraft.world.entity.vehicle.MinecartFurnace testFurnace;
    private static CompletableFuture<Void> pending;
    private static final ArrayList<Integer> pistonTimes = new ArrayList<>();
    private static final Set<Particle> seen = Collections.newSetFromMap(new IdentityHashMap<>());

    static boolean enabled() {
        return Boolean.getBoolean("spelunkers_railways.clientSmoke");
    }

    static void tick(Minecraft mc) {
        if (stage == 99) return;
        try {
            if (Boolean.getBoolean("spelunkers_railways.railExitOnly")) {
                if (RailExitClientSmoke.tick(mc)) {
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.curvePlacementOnly")) {
                if (CurvePlacementClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("curve-placement-checks.txt"),
                            "PASS " + CurvePlacementClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.trenchDescentOnly")) {
                if (TrenchDescentClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("trench-descent-checks.txt"),
                            "PASS " + TrenchDescentClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.landingTransitionOnly")) {
                if (LandingTransitionClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("landing-transition-checks.txt"),
                            "PASS " + LandingTransitionClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.polishOnly")) {
                if (TrainPolishClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("train-polish-checks.txt"),
                            "PASS " + TrainPolishClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.trainRegressionOnly")) {
                if (TrainRegressionClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("train-regression-checks.txt"),
                            "PASS " + TrainRegressionClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.couplingFacingOnly")) {
                if (CouplingFacingClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("coupling-facing-checks.txt"),
                            "PASS " + CouplingFacingClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.continuityOnly")) {
                if (TrainContinuityClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("continuity-checks.txt"),
                            "PASS " + TrainContinuityClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.stoppedRampOnly")) {
                if (TrainStabilityClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("stopped-ramp-client-checks.txt"),
                            "PASS " + TrainStabilityClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.highFallOnly")) {
                if (TrainHighFallClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("high-fall-checks.txt"),
                            "PASS " + TrainHighFallClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (Boolean.getBoolean("spelunkers_railways.flightOnly")) {
                if (TrainFlightClientSmoke.tick(mc)) {
                    Files.writeString(
                            Path.of("flight-checks.txt"),
                            "PASS " + TrainFlightClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
                return;
            }
            if (++total > 3000) throw new AssertionError("Client stage timed out: " + stage);
            ticks++;
            if (pending != null && !pending.isDone()) return;
            if (pending != null) {
                pending.join();
                pending = null;
            }
            switch (stage) {
                case 0 -> {
                    var id = mc.player.getUUID();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var level = mc.getSingleplayerServer().overworld();
                                                for (int x = 0; x <= 90; x++)
                                                    for (int z = 17; z <= 32; z++)
                                                        level.setBlockAndUpdate(
                                                                new BlockPos(x, 83, z),
                                                                Blocks.STONE.defaultBlockState());
                                                for (int x = 0; x <= 90; x++)
                                                    for (int z : new int[] {24, 29}) {
                                                        level.setBlockAndUpdate(
                                                                new BlockPos(x, 83, z),
                                                                Blocks.STONE.defaultBlockState());
                                                        level.setBlockAndUpdate(
                                                                new BlockPos(x, 84, z),
                                                                Blocks.RAIL
                                                                        .defaultBlockState()
                                                                        .setValue(
                                                                                RailBlock.SHAPE,
                                                                                RailShape
                                                                                        .EAST_WEST));
                                                    }
                                                engine =
                                                        new LocomotiveMinecart(
                                                                level, 12.5, 84.0625, 24.5);
                                                engine.setDriveDirection(Direction.EAST);
                                                for (int slot = 0; slot < 3; slot++)
                                                    engine.setItem(
                                                            slot, new ItemStack(Items.COAL, 4));
                                                engine.tick();
                                                engine.toggleEmergencyBrake();
                                                engine.setDeltaMovement(0, 0, 0);
                                                level.addFreshEntity(engine);
                                                var player =
                                                        mc.getSingleplayerServer()
                                                                .getPlayerList()
                                                                .getPlayer(id);
                                                player.teleportTo(11, 84.1, 22);
                                                player.openMenu(engine);
                                            });
                    next(1);
                }
                case 1 -> {
                    if (!(mc.screen instanceof LocomotiveScreen)
                            || !(mc.player.containerMenu instanceof LocomotiveMenu menu)
                            || ticks < 15) return;
                    check(
                            menu.slots.size() == 39,
                            "Expected three fuel slots and 36 inventory slots");
                    for (int slot = 0; slot < 3; slot++)
                        check(
                                menu.getFuelPercent(slot) == 0
                                        && menu.getSlot(slot).getItem().is(Items.COAL),
                                "Braked firebox did not stay dark with synchronized fuel: " + slot);
                    check(menu.emergencyBrake(), "Emergency brake state did not synchronize");
                    if (!Files.isRegularFile(Path.of("locomotive-three-fireboxes.png"))) return;
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                    next(2);
                }
                case 2 -> {
                    if (!(mc.player.containerMenu instanceof LocomotiveMenu menu)
                            || menu.burnerEnabled(2)
                            || ticks < 8) return;
                    if (++lockedFrames < 3) return;
                    check(
                            !menu.getSlot(2).mayPlace(new ItemStack(Items.COAL)),
                            "Locked client slot accepts fuel");
                    if (!Files.isRegularFile(Path.of("locomotive-locked-firebox.png"))) return;
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, 2);
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, 3);
                    next(3);
                }
                case 3 -> {
                    if (!(mc.player.containerMenu instanceof LocomotiveMenu menu)
                            || !menu.burnerEnabled(2)
                            || menu.emergencyBrake()) return;
                    mc.player.closeContainer();
                    var id = mc.player.getUUID();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                engine.releaseEmergencyBrake();
                                                engine.setPos(8.5, 84.0625, 24.5);
                                                engine.setDeltaMovement(0, 0, 0);
                                                engine.setDriveDirection(Direction.EAST);
                                                engine.toggleEmergencyBrake();
                                                var player =
                                                        mc.getSingleplayerServer()
                                                                .getPlayerList()
                                                                .getPlayer(id);
                                                player.teleportTo(22.5, 85.5, 18.5);
                                                player.setYRot(0);
                                                player.setXRot(18);
                                            });
                    next(30);
                }
                case 30 -> {
                    if (ticks < 20
                            || mc.level.getEntity(engine.getId()) == null
                            || mc.level
                                            .getEntity(engine.getId())
                                            .position()
                                            .distanceTo(engine.position())
                                    > .1) return;
                    pistonTimes.clear();
                    pending =
                            mc.getSingleplayerServer().submit(() -> engine.releaseEmergencyBrake());
                    next(4);
                }
                case 4 -> {
                    if (ticks < 90) return;
                    check(pistons >= 6, "Steam piston never sounded: " + pistons);
                    check(pistonTimes.size() >= 4, "Missing piston timing samples");
                    int firstGap = pistonTimes.get(1) - pistonTimes.get(0);
                    int lastGap =
                            pistonTimes.get(pistonTimes.size() - 1)
                                    - pistonTimes.get(pistonTimes.size() - 2);
                    check(
                            lastGap < firstGap,
                            "Piston cadence did not accelerate: " + firstGap + " -> " + lastGap);
                    Files.writeString(
                            Path.of("locomotive-sound-checks.txt"),
                            "PASS piston_acceleration=true first_interval="
                                    + firstGap
                                    + " last_interval="
                                    + lastGap
                                    + " piston_sounds="
                                    + pistons
                                    + "\n");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var player =
                                                        mc.getSingleplayerServer()
                                                                .getPlayerList()
                                                                .getPlayer(mc.player.getUUID());
                                                player.teleportTo(19.5, 85.5, 22.5);
                                                engine.releaseEmergencyBrake();
                                                engine.setPos(18.5, 84.0625, 24.5);
                                                engine.setDeltaMovement(0, 0, 0);
                                                engine.toggleEmergencyBrake();
                                                var level = engine.level();
                                                level.setBlockAndUpdate(
                                                        new BlockPos(20, 84, 29),
                                                        net.luckystudio.spelunkers_charm.init
                                                                .ModBlocks.DIRECTIONAL_RAIL
                                                                .get()
                                                                .defaultBlockState()
                                                                .setValue(
                                                                        PoweredRailBlock.SHAPE,
                                                                        RailShape.EAST_WEST)
                                                                .setValue(
                                                                        PoweredRailBlock.POWERED,
                                                                        false));
                                                var cart =
                                                        new net.minecraft.world.entity.vehicle
                                                                .MinecartFurnace(
                                                                level, 18.5, 84.0625, 29.5);
                                                cart.setDeltaMovement(0.36, 0, 0);
                                                level.addFreshEntity(cart);
                                            });
                    next(40);
                }
                case 40 -> {
                    if (ticks < 15
                            || mc.level.getEntity(engine.getId()) == null
                            || mc.level
                                            .getEntity(engine.getId())
                                            .position()
                                            .distanceTo(engine.position())
                                    > .1) return;
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                engine.releaseEmergencyBrake();
                                                engine.setDeltaMovement(.65, 0, 0);
                                                engine.toggleEmergencyBrake();
                                            });
                    next(5);
                }
                case 5 -> {
                    var visible = mc.level.getEntity(engine.getId());
                    brakeTrace
                            .append(ticks)
                            .append(',')
                            .append(((MinecartMotionVisuals) engine).spelunkersCharm$braking())
                            .append(',')
                            .append(
                                    visible == null
                                            ? -1
                                            : ((MinecartMotionVisuals) visible)
                                                    .spelunkersCharm$braking())
                            .append(',')
                            .append(engine.emergencyBrake())
                            .append(',')
                            .append(engine.getX())
                            .append(',')
                            .append(visible == null ? 0 : visible.getX())
                            .append(',')
                            .append(mc.player.getX())
                            .append('\n');
                    Files.writeString(Path.of("railway-brake-trace.csv"), brakeTrace);
                    mc.player.setYRot(25);
                    mc.player.setXRot(13);
                    if (ticks < 45) return;
                    check(
                            lightBrakes > 0 && heavyBrakes > 0,
                            "Missing brake sounds: light=" + lightBrakes + " heavy=" + heavyBrakes);
                    check(sparks >= 8, "Missing golden wheel sparks: " + sparks);
                    Files.writeString(
                            Path.of("locomotive-sound-checks.txt"),
                            "PASS light_brake_sounds="
                                    + lightBrakes
                                    + " heavy_brake_sounds="
                                    + heavyBrakes
                                    + " sparks="
                                    + sparks
                                    + "\n",
                            java.nio.file.StandardOpenOption.APPEND);
                    check(
                            Files.isRegularFile(Path.of("railway-braking.png")),
                            "Missing braking screenshot");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var level = mc.getSingleplayerServer().overworld();
                                                for (int x = 7; x <= 36; x++)
                                                    for (int z : new int[] {24, 29})
                                                        level.setBlockAndUpdate(
                                                                new BlockPos(x, 83, z),
                                                                Blocks.REDSTONE_BLOCK
                                                                        .defaultBlockState());
                                                for (int x = 8; x <= 36; x++)
                                                    for (int z : new int[] {24, 29})
                                                        level.setBlock(
                                                                new BlockPos(x, 84, z),
                                                                Blocks.POWERED_RAIL
                                                                        .defaultBlockState()
                                                                        .setValue(
                                                                                PoweredRailBlock
                                                                                        .SHAPE,
                                                                                RailShape.EAST_WEST)
                                                                        .setValue(
                                                                                PoweredRailBlock
                                                                                        .POWERED,
                                                                                true),
                                                                2);
                                                for (int z = 25; z <= 28; z++)
                                                    level.setBlock(
                                                            new BlockPos(7, 84, z),
                                                            Blocks.RAIL.defaultBlockState(),
                                                            2);
                                                level.setBlock(
                                                        new BlockPos(7, 84, 24),
                                                        Blocks.RAIL
                                                                .defaultBlockState()
                                                                .setValue(
                                                                        RailBlock.SHAPE,
                                                                        RailShape.SOUTH_EAST),
                                                        2);
                                                level.setBlock(
                                                        new BlockPos(7, 84, 29),
                                                        Blocks.RAIL
                                                                .defaultBlockState()
                                                                .setValue(
                                                                        RailBlock.SHAPE,
                                                                        RailShape.NORTH_EAST),
                                                        2);
                                                level.setBlock(
                                                        new BlockPos(25, 84, 24),
                                                        net.luckystudio.spelunkers_charm.init
                                                                .ModBlocks.DIRECTIONAL_RAIL
                                                                .get()
                                                                .defaultBlockState()
                                                                .setValue(
                                                                        PoweredRailBlock.SHAPE,
                                                                        RailShape.EAST_WEST)
                                                                .setValue(
                                                                        PoweredRailBlock.POWERED,
                                                                        true)
                                                                .setValue(
                                                                        net.luckystudio
                                                                                .spelunkers_charm
                                                                                .block.custom.rail
                                                                                .DirectionalRailBlock
                                                                                .FACING,
                                                                        Direction.WEST),
                                                        2);
                                                level.setBlockAndUpdate(
                                                        new BlockPos(25, 84, 25),
                                                        Blocks.REDSTONE_BLOCK.defaultBlockState());
                                                engine.releaseEmergencyBrake();
                                                engine.setPos(23.5, 84.0625, 24.5);
                                                engine.setDriveDirection(Direction.EAST);
                                                engine.setDeltaMovement(0.45, 0, 0);
                                            });
                    next(6);
                }
                case 6 -> {
                    try {
                        Field slowness = mc.level.getClass().getDeclaredField("slownessFactor");
                        slowness.setAccessible(true);
                        slowness.setDouble(mc.level, 1.05);
                        smoothCompatibilityTicks++;
                    } catch (NoSuchFieldException ignored) {
                    }
                    if (!(mc.level.getEntity(engine.getId()) instanceof LocomotiveMinecart visible)
                            || ticks < 15) return;
                    var position = visible.position();
                    var body = ((MinecartMotionVisuals) visible).spelunkersCharm$motion();
                    if (body.visualYawReady) {
                        if (Float.isFinite(previousBodyYaw))
                            check(
                                    Math.abs(body.visualYaw - previousBodyYaw) <= 100,
                                    "Locomotive model flipped during reversal or curve");
                        previousBodyYaw = body.visualYaw;
                    }
                    curveTrace
                            .append(ticks)
                            .append(',')
                            .append(position.x)
                            .append(',')
                            .append(position.y)
                            .append(',')
                            .append(position.z)
                            .append(',')
                            .append(visible.lerpTargetX())
                            .append(',')
                            .append(visible.lerpTargetZ())
                            .append(',')
                            .append(engine.getX())
                            .append(',')
                            .append(engine.getZ())
                            .append(',')
                            .append(visible.tickCount)
                            .append(',')
                            .append(mc.level.getGameTime())
                            .append('\n');
                    Files.writeString(Path.of("railway-curve-trace.csv"), curveTrace);
                    if (previousCurvePosition != null) {
                        double step = position.distanceTo(previousCurvePosition);
                        maximumCurveStep = Math.max(maximumCurveStep, step);
                        check(
                                step < 0.70 * Math.max(1, visible.tickCount - previousCurveTick),
                                "Client locomotive jumped after reversal: "
                                        + step
                                        + ", elapsed_entity_ticks="
                                        + (visible.tickCount - previousCurveTick)
                                        + ", tick="
                                        + ticks
                                        + ", from="
                                        + previousCurvePosition
                                        + ", to="
                                        + position);
                        if (position.x < previousCurvePosition.x - 0.05) reversedOnClient = true;
                    }
                    previousCurvePosition = position;
                    previousCurveTick = visible.tickCount;
                    var track = MinecartTrack.current(visible);
                    check(
                            track != null
                                    && track.point(track.offset(position)).distanceTo(position)
                                            < 0.04,
                            "Client interpolation left railway: " + position);
                    curveSamples++;
                    if (position.z > 29.4 && position.x > 9 && reversedOnClient) {
                        check(body.visualYawReady, "Stable locomotive rendering hook never ran");
                        pending =
                                mc.getSingleplayerServer()
                                        .submit(
                                                () -> {
                                                    var level =
                                                            mc.getSingleplayerServer().overworld();
                                                    for (var cart :
                                                            level.getEntitiesOfClass(
                                                                    net.minecraft.world.entity
                                                                            .vehicle
                                                                            .AbstractMinecart.class,
                                                                    new net.minecraft
                                                                            .world
                                                                            .phys
                                                                            .AABB(
                                                                            0, 80, 17, 90, 88, 33)))
                                                        if (cart != engine) cart.discard();
                                                    for (int x = 0; x <= 90; x++)
                                                        for (int z : new int[] {24, 29}) {
                                                            level.setBlockAndUpdate(
                                                                    new BlockPos(x, 83, z),
                                                                    Blocks.REDSTONE_BLOCK
                                                                            .defaultBlockState());
                                                            var rail =
                                                                    z == 24 && x >= 8 && x <= 45
                                                                            ? net.luckystudio
                                                                                    .spelunkers_charm
                                                                                    .init.ModBlocks
                                                                                    .WOODEN_RAIL
                                                                                    .get()
                                                                                    .defaultBlockState()
                                                                                    .setValue(
                                                                                            RailBlock
                                                                                                    .SHAPE,
                                                                                            RailShape
                                                                                                    .EAST_WEST)
                                                                            : Blocks.POWERED_RAIL
                                                                                    .defaultBlockState()
                                                                                    .setValue(
                                                                                            PoweredRailBlock
                                                                                                    .SHAPE,
                                                                                            RailShape
                                                                                                    .EAST_WEST)
                                                                                    .setValue(
                                                                                            PoweredRailBlock
                                                                                                    .POWERED,
                                                                                            true);
                                                            level.setBlock(
                                                                    new BlockPos(x, 84, z),
                                                                    rail,
                                                                    2);
                                                        }
                                                    level.setBlockAndUpdate(
                                                            new BlockPos(17, 84, 29),
                                                            Blocks.AIR.defaultBlockState());
                                                    engine.releaseEmergencyBrake();
                                                    engine.setPos(5.5, 84.0625, 24.5);
                                                    engine.setDriveDirection(Direction.EAST);
                                                    engine.setDeltaMovement(.5, 0, 0);
                                                    gapEngine =
                                                            new LocomotiveMinecart(
                                                                    level, 5.5, 84.0625, 29.5);
                                                    gapEngine.setDriveDirection(Direction.EAST);
                                                    for (int slot = 0; slot < 3; slot++)
                                                        gapEngine.setItem(
                                                                slot, new ItemStack(Items.COAL, 4));
                                                    gapEngine.setDeltaMovement(.5, 0, 0);
                                                    level.addFreshEntity(gapEngine);
                                                });
                        next(7);
                    }
                }
                case 7 -> {
                    if (!(mc.level.getEntity(engine.getId()) instanceof LocomotiveMinecart wood)
                            || gapEngine == null
                            || !(mc.level.getEntity(gapEngine.getId())
                                    instanceof LocomotiveMinecart gap)
                            || ticks < 15) return;
                    var a = wood.position();
                    var b = gap.position();
                    if (previousWood != null) {
                        double first = a.distanceTo(previousWood),
                                second = b.distanceTo(previousGap);
                        maximumContactStep = Math.max(maximumContactStep, Math.max(first, second));
                        check(
                                first < .75 * Math.max(1, wood.tickCount - previousWoodTick)
                                        && second
                                                < .75
                                                        * Math.max(
                                                                1, gap.tickCount - previousGapTick),
                                "Client jumped at wooden track or rail gap: "
                                        + first
                                        + ", "
                                        + second);
                        check(
                                a.x >= previousWood.x - .01 && b.x >= previousGap.x - .01,
                                "Track loss reversed client movement");
                    }
                    previousWood = a;
                    previousGap = b;
                    previousWoodTick = wood.tickCount;
                    previousGapTick = gap.tickCount;
                    contactSamples++;
                    check(
                            ((MinecartMotionVisuals) wood).spelunkersCharm$braking() == 0
                                    && ((MinecartMotionVisuals) gap).spelunkersCharm$braking() == 0,
                            "Track loss generated brake effects");
                    contactTrace
                            .append(ticks)
                            .append(',')
                            .append(a.x)
                            .append(',')
                            .append(a.y)
                            .append(',')
                            .append(b.x)
                            .append(',')
                            .append(b.y)
                            .append('\n');
                    Files.writeString(Path.of("railway-contact-trace.csv"), contactTrace);
                    if (ticks >= 85) {
                        check(
                                a.x > 8
                                        && engine.onGround()
                                        && !engine.isOnRails()
                                        && engine.getDeltaMovement().horizontalDistanceSqr()
                                                < .0001,
                                "Locomotive did not stop after breaking its wooden support rail");
                        check(b.x > 30, "Single rail gap stalled locomotive");
                        var id = mc.player.getUUID();
                        pending =
                                mc.getSingleplayerServer()
                                        .submit(
                                                () -> {
                                                    var level =
                                                            mc.getSingleplayerServer().overworld();
                                                    for (int x = 4; x <= 28; x++)
                                                        level.setBlockAndUpdate(
                                                                new BlockPos(x, 84, 31),
                                                                Blocks.RAIL
                                                                        .defaultBlockState()
                                                                        .setValue(
                                                                                RailBlock.SHAPE,
                                                                                RailShape
                                                                                        .EAST_WEST));
                                                    var furnace =
                                                            new net.minecraft.world.entity.vehicle
                                                                    .MinecartFurnace(
                                                                    level, 8.5, 84.0625, 31.5);
                                                    testFurnace = furnace;
                                                    furnace.fuel = 6000;
                                                    furnace.xPush = 1;
                                                    level.addFreshEntity(furnace);
                                                    MinecartHandbrake.engage(furnace);
                                                    var player =
                                                            mc.getSingleplayerServer()
                                                                    .getPlayerList()
                                                                    .getPlayer(id);
                                                    player.teleportTo(7, 84.1, 31.5);
                                                    furnace.interact(
                                                            player,
                                                            net.minecraft.world.InteractionHand
                                                                    .MAIN_HAND);
                                                });
                        next(8);
                    }
                }
                case 8 -> {
                    if (ticks < 15) return;
                    check(mc.screen == null, "Furnace cart still opened a custom screen");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(() -> MinecartHandbrake.release(testFurnace));
                    next(9);
                }
                case 9 -> {
                    if (ticks < 8) return;
                    check(
                            testFurnace.getDeltaMovement().horizontalDistance() > .01,
                            "Vanilla furnace cart did not resume");
                    var id = mc.player.getUUID();
                    mc.player.closeContainer();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var level = mc.getSingleplayerServer().overworld();
                                                for (int x = 1; x <= 60; x++) {
                                                    level.setBlockAndUpdate(
                                                            new BlockPos(x, 84, 24),
                                                            net.luckystudio.spelunkers_charm.init
                                                                    .ModBlocks.WOODEN_RAIL
                                                                    .get()
                                                                    .defaultBlockState()
                                                                    .setValue(
                                                                            RailBlock.SHAPE,
                                                                            RailShape.EAST_WEST));
                                                    level.setBlockAndUpdate(
                                                            new BlockPos(x, 84, 29),
                                                            Blocks.RAIL
                                                                    .defaultBlockState()
                                                                    .setValue(
                                                                            RailBlock.SHAPE,
                                                                            RailShape.EAST_WEST));
                                                }
                                                rollingWood =
                                                        new Minecart(level, 8.5, 84.0625, 24.5);
                                                rollingIron =
                                                        new Minecart(level, 8.5, 84.0625, 29.5);
                                                rollingWood.setDeltaMovement(.168, 0, 0);
                                                rollingIron.setDeltaMovement(.22, 0, 0);
                                                level.addFreshEntity(rollingWood);
                                                level.addFreshEntity(rollingIron);
                                                var player =
                                                        mc.getSingleplayerServer()
                                                                .getPlayerList()
                                                                .getPlayer(id);
                                                player.teleportTo(8.5, 84.1, 24.5);
                                                player.startRiding(rollingWood, true);
                                            });
                    next(10);
                }
                case 10 -> {
                    zeroClientRollingVelocities(mc);
                    if (ticks < 35) return;
                    woodVolume = rollingVolume(mc, rollingWood.getId(), false);
                    ironVolume = rollingVolume(mc, rollingIron.getId(), false);
                    riderVolume = rollingVolume(mc, rollingWood.getId(), true);
                    check(
                            woodVolume > .03 && ironVolume > .03 && riderVolume > .03,
                            "Missing active rolling sound: wood="
                                    + woodVolume
                                    + ", iron="
                                    + ironVolume
                                    + ", rider="
                                    + riderVolume);
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                rollingWood.setDeltaMovement(0, 0, 0);
                                                rollingIron.setDeltaMovement(0, 0, 0);
                                            });
                    next(11);
                }
                case 11 -> {
                    zeroClientRollingVelocities(mc);
                    if (ticks < 30) return;
                    check(
                            rollingVolume(mc, rollingWood.getId(), false) < .005
                                    && rollingVolume(mc, rollingIron.getId(), false) < .005
                                    && rollingVolume(mc, rollingWood.getId(), true) < .005,
                            "Stopped carts kept playing rolling sound");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                rollingWood.setDeltaMovement(.168, 0, 0);
                                                rollingIron.setDeltaMovement(.20, 0, 0);
                                            });
                    next(12);
                }
                case 12 -> {
                    zeroClientRollingVelocities(mc);
                    if (ticks < 20) return;
                    check(
                            rollingVolume(mc, rollingWood.getId(), false) > .03
                                    && rollingVolume(mc, rollingIron.getId(), false) > .03
                                    && rollingVolume(mc, rollingWood.getId(), true) > .03,
                            "Rolling sound did not resume after restart");
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
                                                for (var cart :
                                                        level.getEntitiesOfClass(
                                                                net.minecraft.world.entity.vehicle
                                                                        .AbstractMinecart.class,
                                                                new net.minecraft.world.phys.AABB(
                                                                        -5, 80, 17, 95, 88, 33)))
                                                    cart.discard();
                                                for (int x = 0; x <= 90; x++)
                                                    level.setBlockAndUpdate(
                                                            new BlockPos(x, 84, 29),
                                                            Blocks.POWERED_RAIL
                                                                    .defaultBlockState()
                                                                    .setValue(
                                                                            PoweredRailBlock.SHAPE,
                                                                            RailShape.EAST_WEST)
                                                                    .setValue(
                                                                            PoweredRailBlock
                                                                                    .POWERED,
                                                                            true));
                                                conversionEngine =
                                                        new LocomotiveMinecart(
                                                                level, 12.5, 84.0625, 29.5);
                                                conversionEngine.setDriveDirection(Direction.EAST);
                                                for (int slot = 0; slot < 3; slot++)
                                                    conversionEngine.setItem(
                                                            slot, new ItemStack(Items.COAL, 4));
                                                conversionCart =
                                                        new Minecart(level, 10.95, 84.0625, 29.5);
                                                level.addFreshEntity(conversionEngine);
                                                level.addFreshEntity(conversionCart);
                                                MinecartCouplings.get(level)
                                                        .link(conversionEngine, conversionCart);
                                                player.teleportTo(20, 84.1, 27);
                                            });
                    next(13);
                }
                case 13 -> {
                    if (ticks < 30
                            || !(mc.level.getEntity(conversionCart.getId())
                                    instanceof
                                    net.minecraft.world.entity.vehicle.AbstractMinecart visible))
                        return;
                    previousCargo = visible.position();
                    previousCargoTime = mc.level.getGameTime();
                    conversionLag = conversionCart.getX() - visible.getX();
                    check(
                            Math.abs(conversionLag) < .85,
                            "Moving wagon is displayed behind its real position: " + conversionLag);
                    visible.setPos(visible.getX() - 1.5, visible.getY(), visible.getZ());
                    previousCargo = visible.position();
                    conversionSource = visible.getId();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var old = conversionCart;
                                                check(
                                                        MinecartBlockLoading.load(
                                                                old,
                                                                new ItemStack(Items.STONE, 3),
                                                                null),
                                                        "Moving block load failed");
                                                conversionCart =
                                                        old.level()
                                                                .getEntitiesOfClass(
                                                                        BlockMinecart.class,
                                                                        old.getBoundingBox()
                                                                                .inflate(1))
                                                                .getFirst();
                                            });
                    next(14);
                }
                case 14, 16 -> {
                    if (!(mc.level.getEntity(conversionCart.getId())
                            instanceof net.minecraft.world.entity.vehicle.AbstractMinecart visible))
                        return;
                    check(
                            ((MinecartMotionVisuals) visible)
                                    .spelunkersCharm$motion()
                                    .replacementHandled,
                            "Client skipped replacement interpolation handoff");
                    check(
                            ((MinecartMotionVisuals) visible).spelunkersCharm$replacedCartId()
                                    == conversionSource,
                            "Replacement source changed before client handoff");
                    check(
                            ((MinecartCouplingVisuals) visible).spelunkersCharm$firstCouplingId()
                                            == conversionEngine.getId()
                                    || ((MinecartCouplingVisuals) visible)
                                                    .spelunkersCharm$secondCouplingId()
                                            == conversionEngine.getId(),
                            "Loaded client wagon lost its coupling: source="
                                    + conversionSource
                                    + ", client="
                                    + visible.getId()
                                    + ", engine="
                                    + conversionEngine.getId()
                                    + ", first="
                                    + ((MinecartCouplingVisuals) visible)
                                            .spelunkersCharm$firstCouplingId()
                                    + ", second="
                                    + ((MinecartCouplingVisuals) visible)
                                            .spelunkersCharm$secondCouplingId()
                                    + ", server_link="
                                    + MinecartCouplings.get(mc.getSingleplayerServer().overworld())
                                            .linked(
                                                    conversionEngine.getUUID(),
                                                    conversionCart.getUUID())
                                    + ", client_pos="
                                    + visible.position()
                                    + ", server_pos="
                                    + conversionCart.position());
                    if (stage == 14)
                        check(
                                visible.getDisplayBlockState().is(Blocks.STONE),
                                "Loaded block did not synchronize");
                    long elapsed = mc.level.getGameTime() - previousCargoTime;
                    double distance = visible.position().distanceTo(previousCargo);
                    maximumConversionStep =
                            Math.max(maximumConversionStep, distance / Math.max(1, elapsed));
                    check(
                            distance < .70 * Math.max(1, elapsed),
                            "Client wagon jumped during loading: "
                                    + distance
                                    + ", elapsed="
                                    + elapsed);
                    check(
                            visible.getX() >= previousCargo.x - .01,
                            "Client wagon reversed during loading");
                    conversionTrace
                            .append(stage)
                            .append(',')
                            .append(ticks)
                            .append(',')
                            .append(mc.level.getGameTime())
                            .append(',')
                            .append(visible.getId())
                            .append(',')
                            .append(visible.getX())
                            .append(',')
                            .append(conversionCart.getX())
                            .append(',')
                            .append(distance)
                            .append(',')
                            .append(elapsed)
                            .append('\n');
                    Files.writeString(Path.of("railway-conversion-trace.csv"), conversionTrace);
                    previousCargo = visible.position();
                    previousCargoTime = mc.level.getGameTime();
                    conversionSamples++;
                    if (ticks < 25) return;
                    if (stage == 14) {
                        conversionSource = visible.getId();
                        pending =
                                mc.getSingleplayerServer()
                                        .submit(
                                                () -> {
                                                    var old = conversionCart;
                                                    var empty = new Minecart(old.level(), 0, 0, 0);
                                                    check(
                                                            MinecartCouplings.replaceEntity(
                                                                    old, empty),
                                                            "Moving unload failed");
                                                    check(
                                                            MinecartBlockLoading.load(
                                                                    empty,
                                                                    new ItemStack(Items.CHEST),
                                                                    null),
                                                            "Moving chest load failed");
                                                    conversionCart =
                                                            empty.level()
                                                                    .getEntitiesOfClass(
                                                                            net.minecraft.world
                                                                                    .entity.vehicle
                                                                                    .MinecartChest
                                                                                    .class,
                                                                            empty.getBoundingBox()
                                                                                    .inflate(1))
                                                                    .getFirst();
                                                });
                        next(16);
                        return;
                    }
                    var id = mc.player.getUUID();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var level = mc.getSingleplayerServer().overworld();
                                                for (var cart :
                                                        level.getEntitiesOfClass(
                                                                net.minecraft.world.entity.vehicle
                                                                        .AbstractMinecart.class,
                                                                new net.minecraft.world.phys.AABB(
                                                                        -5, 80, 17, 95, 88, 33)))
                                                    cart.discard();
                                                for (int x = 0; x <= 90; x++) {
                                                    level.setBlockAndUpdate(
                                                            new BlockPos(x, 83, 29),
                                                            Blocks.REDSTONE_BLOCK
                                                                    .defaultBlockState());
                                                    var rail =
                                                            x >= 18 && x <= 43 && (x - 18) % 5 == 0
                                                                    ? Blocks.DETECTOR_RAIL
                                                                            .defaultBlockState()
                                                                            .setValue(
                                                                                    net.minecraft
                                                                                            .world
                                                                                            .level
                                                                                            .block
                                                                                            .DetectorRailBlock
                                                                                            .SHAPE,
                                                                                    RailShape
                                                                                            .EAST_WEST)
                                                                    : Blocks.POWERED_RAIL
                                                                            .defaultBlockState()
                                                                            .setValue(
                                                                                    PoweredRailBlock
                                                                                            .SHAPE,
                                                                                    RailShape
                                                                                            .EAST_WEST)
                                                                            .setValue(
                                                                                    PoweredRailBlock
                                                                                            .POWERED,
                                                                                    true);
                                                    level.setBlock(
                                                            new BlockPos(x, 84, 29), rail, 2);
                                                }
                                                conversionEngine =
                                                        new LocomotiveMinecart(
                                                                level, 14.5, 84.0625, 29.5);
                                                conversionEngine.setDriveDirection(Direction.EAST);
                                                for (int slot = 0; slot < 3; slot++)
                                                    conversionEngine.setItem(
                                                            slot, new ItemStack(Items.COAL, 4));
                                                level.addFreshEntity(conversionEngine);
                                                syncTrain.add(conversionEngine);
                                                var links = MinecartCouplings.get(level);
                                                for (int i = 1; i <= 6; i++) {
                                                    var cart =
                                                            new Minecart(
                                                                    level,
                                                                    14.5 - i * 1.55,
                                                                    84.0625,
                                                                    29.5);
                                                    level.addFreshEntity(cart);
                                                    links.link(syncTrain.getLast(), cart);
                                                    syncTrain.add(cart);
                                                }
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(id)
                                                        .teleportTo(30, 84.1, 27);
                                            });
                    next(17);
                }
                case 17 -> {
                    var serverPositions =
                            syncTrain.stream()
                                    .map(net.minecraft.world.entity.Entity::position)
                                    .toList();
                    for (int syncIndex = 0; syncIndex < syncTrain.size(); syncIndex++) {
                        var cart = syncTrain.get(syncIndex);
                        if (!(mc.level.getEntity(cart.getId())
                                instanceof
                                net.minecraft.world.entity.vehicle.AbstractMinecart visible))
                            return;
                        var before = syncPrevious.put(cart.getId(), visible.position());
                        var beforeTick = syncTicks.put(cart.getId(), visible.tickCount);
                        if (before != null)
                            check(
                                    visible.position().distanceTo(before)
                                            < .70 * Math.max(1, visible.tickCount - beforeTick),
                                    "Synchronized train jumped");
                        if (ticks > 20) {
                            double error =
                                    serverPositions.get(syncIndex).distanceTo(visible.position());
                            maximumPositionError = Math.max(maximumPositionError, error);
                            syncTrace
                                    .append(ticks)
                                    .append(',')
                                    .append(cart.getId())
                                    .append(',')
                                    .append(serverPositions.get(syncIndex).x)
                                    .append(',')
                                    .append(visible.getX())
                                    .append(',')
                                    .append(error)
                                    .append('\n');
                            check(
                                    error < .85,
                                    "Train visual position is delayed from the detector position: "
                                            + error);
                        }
                    }
                    if (ticks > 20) Files.writeString(Path.of("railway-sync-trace.csv"), syncTrace);
                    for (int x = 18; x <= 43; x += 5) {
                        BlockPos pos = new BlockPos(x, 84, 29);
                        var detector = mc.level.getBlockState(pos);
                        if (detector.getBlock()
                                        instanceof net.minecraft.world.level.block.DetectorRailBlock
                                && detector.getValue(
                                        net.minecraft.world.level.block.DetectorRailBlock.POWERED)
                                && detected.add(pos)) {
                            var box =
                                    new net.minecraft.world.phys.AABB(
                                                    x + .2, 84, 29.2, x + .8, 84.8, 29.8)
                                            .inflate(.5, 0, .5);
                            check(
                                    syncTrain.stream()
                                            .map(cart -> mc.level.getEntity(cart.getId()))
                                            .anyMatch(
                                                    cart ->
                                                            cart != null
                                                                    && cart.getBoundingBox()
                                                                            .intersects(box)),
                                    "Detector pressed before visible train arrived: " + pos);
                        }
                    }
                    if (ticks < 80) return;
                    check(detected.size() >= 4, "Missing detector synchronization samples");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(() -> MinecartHandbrake.engage(conversionEngine));
                    next(18);
                }
                case 18 -> {
                    if (ticks < 20) return;
                    for (int i = 1; i < syncTrain.size(); i++) {
                        var first =
                                (net.minecraft.world.entity.vehicle.AbstractMinecart)
                                        mc.level.getEntity(syncTrain.get(i - 1).getId());
                        var second =
                                (net.minecraft.world.entity.vehicle.AbstractMinecart)
                                        mc.level.getEntity(syncTrain.get(i).getId());
                        var gap = MinecartRailPath.between(first, second);
                        check(gap != null, "Stopped client train left rails");
                        maximumStoppedGapError =
                                Math.max(maximumStoppedGapError, Math.abs(gap.distance() - 1.55));
                        check(
                                Math.abs(gap.distance() - 1.55) < .04,
                                "Visible stopped coupling stretched: " + gap.distance());
                    }
                    var id = mc.player.getUUID();
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(
                                            () -> {
                                                var level = mc.getSingleplayerServer().overworld();
                                                for (var cart :
                                                        level.getEntitiesOfClass(
                                                                net.minecraft.world.entity.vehicle
                                                                        .AbstractMinecart.class,
                                                                new net.minecraft.world.phys.AABB(
                                                                        -5, 80, 17, 95, 88, 85)))
                                                    cart.discard();
                                                riderRoute =
                                                        new PassengerTrainRoute(
                                                                level, new BlockPos(5, 82, 40));
                                                riderRoute.build();
                                                var p = riderRoute.point(10);
                                                riderEngine =
                                                        new LocomotiveMinecart(
                                                                level, p.x, p.y, p.z);
                                                riderEngine.setDriveDirection(Direction.SOUTH);
                                                riderEngine.switchDirection();
                                                for (int slot = 0; slot < 3; slot++)
                                                    riderEngine.setItem(
                                                            slot, new ItemStack(Items.COAL, 4));
                                                level.addFreshEntity(riderEngine);
                                                riderTrain.add(riderEngine);
                                                var links = MinecartCouplings.get(level);
                                                for (int i = 1; i <= 10; i++) {
                                                    var at = riderRoute.point(10 + i * 1.55);
                                                    net.minecraft.world.entity.vehicle
                                                                    .AbstractMinecart
                                                            cart =
                                                                    i % 3 == 0
                                                                            ? new net.minecraft
                                                                                    .world.entity
                                                                                    .vehicle
                                                                                    .MinecartChest(
                                                                                    level, at.x,
                                                                                    at.y, at.z)
                                                                            : new Minecart(
                                                                                    level, at.x,
                                                                                    at.y, at.z);
                                                    level.addFreshEntity(cart);
                                                    check(
                                                            links.link(riderTrain.getLast(), cart)
                                                                    == MinecartCouplings.Result
                                                                            .LINKED,
                                                            "Passenger fixture failed to attach rear coupling");
                                                    riderTrain.add(cart);
                                                }
                                                var player =
                                                        mc.getSingleplayerServer()
                                                                .getPlayerList()
                                                                .getPlayer(id);
                                                player.setGameMode(
                                                        net.minecraft.world.level.GameType
                                                                .SURVIVAL);
                                                player.getAbilities().flying = false;
                                                player.onUpdateAbilities();
                                                var seat = riderTrain.get(1);
                                                player.teleportTo(
                                                        seat.getX(), seat.getY(), seat.getZ());
                                                check(
                                                        player.startRiding(seat, true),
                                                        "Real player could not mount test train");
                                                var engineVelocity = riderEngine.getDeltaMovement();
                                                var riderVelocity = player.getDeltaMovement();
                                                player.setPos(
                                                        riderEngine.getX(),
                                                        riderEngine.getY() + .1,
                                                        riderEngine.getZ());
                                                check(
                                                        player.getBoundingBox()
                                                                .intersects(
                                                                        riderEngine
                                                                                .getBoundingBox()),
                                                        "Passenger contact probe did not overlap");
                                                riderEngine.push(player);
                                                player.push(riderEngine);
                                                check(
                                                        riderEngine
                                                                                .getDeltaMovement()
                                                                                .distanceToSqr(
                                                                                        engineVelocity)
                                                                        < 1e-10
                                                                && player.getDeltaMovement()
                                                                                .distanceToSqr(
                                                                                        riderVelocity)
                                                                        < 1e-10,
                                                        "Mounted passenger pushed another cart in the train");
                                                riderContacts++;
                                                seat.positionRider(player);
                                            });
                    next(19);
                }
                case 19 -> {
                    if (!(mc.player.getVehicle()
                                    instanceof
                                    net.minecraft.world.entity.vehicle.AbstractMinecart seat)
                            || seat.getId() != riderTrain.get(1).getId()) {
                        check(ticks < 70, "Real passenger was dismounted on fold");
                        return;
                    }
                    double now = riderRoute.progress(seat.position());
                    if (riderSamples == 0) riderBefore = now;
                    riderDistance += riderRoute.delta(riderBefore, now);
                    riderBefore = now;
                    riderSamples++;
                    check(
                            mc.player.isAlive() && mc.player.getHealth() > 0,
                            "Real passenger was hurt by train overlap");
                    if (riderSamples > 100 && riderSamples % 120 == 0) {
                        check(
                                riderDistance - riderWindow > 25,
                                "Real passenger blocked pushed train at hairpin");
                        riderWindow = riderDistance;
                    }
                    for (var cart : riderTrain) {
                        var visible = mc.level.getEntity(cart.getId());
                        if (visible != null
                                && cart.getId() != seat.getId()
                                && visible.getBoundingBox().intersects(mc.player.getBoundingBox()))
                            riderContacts++;
                    }
                    riderTrace
                            .append(ticks)
                            .append(',')
                            .append(riderDistance)
                            .append(',')
                            .append(seat.getDeltaMovement().horizontalDistance())
                            .append(',')
                            .append(seat.getId())
                            .append(',')
                            .append(riderContacts)
                            .append('\n');
                    Files.writeString(Path.of("railway-passenger-trace.csv"), riderTrace);
                    if (riderSamples < 620) return;
                    check(
                            riderDistance > riderRoute.length * 1.05,
                            "Real mounted train did not complete the fold course: "
                                    + riderDistance);
                    check(
                            riderContacts > 0,
                            "Mounted test never exercised overlapping passenger hitboxes");
                    pending =
                            mc.getSingleplayerServer()
                                    .submit(() -> MinecartHandbrake.engage(riderEngine));
                    next(20);
                }
                case 20 -> {
                    if (ticks < 20) return;
                    if (ticks == 20)
                        check(
                                mc.player.getVehicle() != null
                                        && mc.player.getVehicle().getId()
                                                == riderTrain.get(1).getId(),
                                "Stopping train dismounted real passenger");
                    if (!TrainFlightClientSmoke.tick(mc)) return;
                    Files.writeString(
                            Path.of("railway-checks.txt"),
                            "PASS railway GUI, fuel locks, emergency brakes, steam cadence, brake sounds and sparks, curves, stable model, wooden rail destruction, track gaps, furnace speed; ambient and ridden rolling sounds on wood and iron with zero client velocity, silence at stop and restart\n"
                                    + "pistons="
                                    + pistons
                                    + " light_brakes="
                                    + lightBrakes
                                    + " heavy_brakes="
                                    + heavyBrakes
                                    + " sparks="
                                    + sparks
                                    + "\ncurve_samples="
                                    + curveSamples
                                    + " maximum_curve_step="
                                    + maximumCurveStep
                                    + " smooth_compatibility_ticks="
                                    + smoothCompatibilityTicks
                                    + " contact_samples="
                                    + contactSamples
                                    + " maximum_contact_step="
                                    + maximumContactStep
                                    + "\nrolling_wood_volume="
                                    + woodVolume
                                    + " rolling_iron_volume="
                                    + ironVolume
                                    + " rolling_rider_volume="
                                    + riderVolume
                                    + "\nconversion_samples="
                                    + conversionSamples
                                    + " maximum_conversion_step="
                                    + maximumConversionStep
                                    + " interpolation_lag="
                                    + conversionLag
                                    + "\ndetectors="
                                    + detected.size()
                                    + " maximum_position_error="
                                    + maximumPositionError
                                    + " stopped_gap_error="
                                    + maximumStoppedGapError
                                    + "\nmounted_samples="
                                    + riderSamples
                                    + " mounted_distance="
                                    + riderDistance
                                    + " passenger_contacts="
                                    + riderContacts
                                    + "\n"
                                    + TrainFlightClientSmoke.report());
                    stage = 99;
                    mc.stop();
                }
            }
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    @SubscribeEvent
    public static void gui(ScreenEvent.Render.Post event) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof LocomotiveScreen screen)) return;
        try {
            var menu = screen.getMenu();
            var buttons =
                    screen.children().stream()
                            .filter(
                                    net.minecraft.client.gui.components.AbstractButton.class
                                            ::isInstance)
                            .map(net.minecraft.client.gui.components.AbstractButton.class::cast)
                            .toList();
            if (stage == 1
                    && ticks > 10
                    && menu.emergencyBrake()
                    && menu.getFuelPercent(2) == 0
                    && !Files.exists(Path.of("locomotive-three-fireboxes.png")))
                capture(mc, "locomotive-three-fireboxes.png");
            if (stage == 2
                    && !menu.burnerEnabled(2)
                    && buttons.size() >= 3
                    && buttons.get(2)
                            .getMessage()
                            .getString()
                            .equals(
                                    net.minecraft.network.chat.Component.translatable(
                                                    "gui.spelunkers_charm.locomotive.enable_burner",
                                                    3)
                                            .getString())
                    && !Files.exists(Path.of("locomotive-locked-firebox.png")))
                capture(mc, "locomotive-locked-firebox.png");
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (!enabled() || event.getSound() == null) return;
        var id = event.getSound().getLocation();
        if (event.getSound() instanceof net.minecraft.client.resources.sounds.MinecartSoundInstance
                || event.getSound()
                        instanceof
                        net.minecraft.client.resources.sounds.RidingMinecartSoundInstance)
            rollingSounds.add(event.getSound());
        if (id.equals(SpelunkersCharm.id("locomotive_piston"))) {
            pistons++;
            if (stage == 4) pistonTimes.add(total);
        }
        if (id.equals(SpelunkersCharm.id("cart_brake"))) lightBrakes++;
        if (id.equals(SpelunkersCharm.id("locomotive_brake"))) heavyBrakes++;
    }

    private static void zeroClientRollingVelocities(Minecraft mc) {
        for (Minecart cart : new Minecart[] {rollingWood, rollingIron}) {
            var visible = mc.level.getEntity(cart.getId());
            if (visible != null) visible.setDeltaMovement(0, 0, 0);
        }
    }

    private static float rollingVolume(Minecraft mc, int entity, boolean inside) throws Exception {
        float volume = 0;
        for (var sound : rollingSounds) {
            boolean ridden =
                    sound
                            instanceof
                            net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
            if (ridden != inside) continue;
            Field field = sound.getClass().getDeclaredField("minecart");
            field.setAccessible(true);
            if (field.get(sound) instanceof net.minecraft.world.entity.vehicle.AbstractMinecart cart
                    && cart.getId() == entity
                    && mc.getSoundManager().isActive(sound))
                volume = Math.max(volume, sound.getVolume());
        }
        return volume;
    }

    @SubscribeEvent
    public static void particles(RenderLevelStageEvent event) {
        if (!enabled()
                || stage != 5
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            Field field = mc.particleEngine.getClass().getDeclaredField("particles");
            field.setAccessible(true);
            Map<?, ?> particles = (Map<?, ?>) field.get(mc.particleEngine);
            for (Object group : particles.values())
                for (Object entry : (Iterable<?>) group) {
                    if (!(entry instanceof GlowParticle spark) || !seen.add(spark)) continue;
                    if (number(spark, "rCol") > 0.99
                            && Math.abs(number(spark, "bCol") - 0.15) < 0.001) sparks++;
                }
            if (sparks > 8 && ticks >= 7 && !Files.exists(Path.of("railway-braking.png")))
                capture(mc, "railway-braking.png");
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static double number(Particle particle, String name) throws Exception {
        Field field = Particle.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(particle);
    }

    private static void next(int value) {
        stage = value;
        ticks = 0;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void capture(Minecraft mc, String file) throws Exception {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(Path.of(file));
        }
    }

    private static void fail(Minecraft mc, Throwable error) {
        if (stage == 99) return;
        error.printStackTrace();
        try {
            Files.writeString(Path.of("railway-checks.txt"), "FAIL " + error);
        } catch (Exception ignored) {
        }
        stage = 99;
        mc.stop();
    }
}
