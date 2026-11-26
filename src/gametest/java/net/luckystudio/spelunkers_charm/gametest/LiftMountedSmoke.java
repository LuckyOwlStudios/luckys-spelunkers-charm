// Lift mounted smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class LiftMountedSmoke {
    private static final List<String> RESULTS = new ArrayList<>();
    private static final StringBuilder SAMPLES =
            new StringBuilder(
                    "scenario,tick,partial,deck,camera,player,player_old,vehicle,vehicle_old\n");
    private static CompletableFuture<Void> setup;
    private static volatile int liftId = -1;
    private static volatile int vehicleId = -1;
    private static int scenario;
    private static int tick;
    private static int frames;
    private static final java.util.Set<Integer> upTicks = new java.util.HashSet<>();
    private static final java.util.Set<Integer> downTicks = new java.util.HashSet<>();
    private static double minimumUp = Double.POSITIVE_INFINITY;
    private static double maximumUp = Double.NEGATIVE_INFINITY;
    private static double minimumDown = Double.POSITIVE_INFINITY;
    private static double maximumDown = Double.NEGATIVE_INFINITY;
    private static double worstRange;
    private static boolean finished;

    private LiftMountedSmoke() {}

    static boolean enabled() {
        return Boolean.getBoolean("spelunkers_lifts.clientMounted");
    }

    static void tick(Minecraft mc) {
        if (finished) return;
        try {
            if (setup == null) {
                setup =
                        mc.getSingleplayerServer()
                                .submit(() -> setupWorld(mc.getSingleplayerServer().overworld()));
                return;
            }
            if (!setup.isDone()) return;
            setup.join();
            if (!(mc.level.getEntity(liftId) instanceof AbstractLift)
                    || mc.level.getEntity(vehicleId) == null
                    || tick < 140 && mc.player.getRootVehicle().getId() != vehicleId) return;
            tick++;
            if (tick == 20 || tick == 65 || tick == 85 || tick == 130) {
                MovementType movement =
                        tick == 20
                                ? MovementType.UP
                                : tick == 85 ? MovementType.DOWN : MovementType.STATIONARY;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var lift =
                                            (AbstractLift)
                                                    mc.getSingleplayerServer()
                                                            .overworld()
                                                            .getEntity(liftId);
                                    lift.setMovementType(movement);
                                });
            }
            if (tick == 140) {
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var server = mc.getSingleplayerServer();
                                    var player = server.overworld().players().getFirst();
                                    var lift = (AbstractLift) server.overworld().getEntity(liftId);
                                    player.stopRiding();
                                    player.teleportTo(
                                            server.overworld(),
                                            lift.getX(),
                                            lift.getBoundingBox().maxY,
                                            lift.getZ(),
                                            0,
                                            0);
                                });
            }
            if (tick == 150) {
                check(!mc.player.isPassenger(), "Could not dismount from the lift vehicle");
                var lift = (AbstractLift) mc.level.getEntity(liftId);
                check(
                        Math.abs(mc.player.getY() - lift.getBoundingBox().maxY) < .2,
                        "Dismount left the player anchored to the vehicle");
                check(
                        upTicks.size() >= 20 && downTicks.size() >= 20,
                        "Too few distinct mounted camera ticks");
                double upRange = maximumUp - minimumUp;
                double downRange = maximumDown - minimumDown;
                worstRange = Math.max(worstRange, Math.max(upRange, downRange));
                RESULTS.add(
                        label()
                                + " up_range="
                                + upRange
                                + " down_range="
                                + downRange
                                + " frames="
                                + frames
                                + " up_ticks="
                                + upTicks.size()
                                + " down_ticks="
                                + downTicks.size());
                if (++scenario == 6) {
                    Files.writeString(Path.of("mounted-camera.csv"), SAMPLES);
                    check(
                            worstRange < .02,
                            "Mounted camera jumps relative to the platform, range=" + worstRange);
                    finish(
                            mc,
                            "PASS mounted camera, up/down, boat/horse/minecart, both entity tick orders, dismount\n"
                                    + String.join("\n", RESULTS));
                    return;
                }
                tick = frames = 0;
                upTicks.clear();
                downTicks.clear();
                minimumUp = minimumDown = Double.POSITIVE_INFINITY;
                maximumUp = maximumDown = Double.NEGATIVE_INFINITY;
                setup = null;
            }
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static void setupWorld(ServerLevel level) {
        level.setDayTime(6000);
        var player = level.players().getFirst();
        player.stopRiding();
        Entity oldVehicle = level.getEntity(vehicleId);
        Entity oldLift = level.getEntity(liftId);
        if (oldVehicle != null) oldVehicle.discard();
        if (oldLift != null) oldLift.discard();
        double x = scenario * 14 + .5;
        AbstractLift lift =
                AbstractLift.createLift(
                        level, x, 74, .5, AbstractLift.Type.SMALL, ItemStack.EMPTY, null);
        lift.setYRot(180);
        for (BlockPos guide : lift.getGuidePositions()) {
            Direction inward = guide.getX() < x ? Direction.EAST : Direction.WEST;
            for (int y = 68; y <= 92; y++) {
                BlockPos rail = guide.atY(y);
                level.setBlockAndUpdate(
                        rail,
                        ModBlocks.POWERED_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, inward));
                level.setBlockAndUpdate(
                        rail.relative(inward.getOpposite()),
                        Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
        }
        Entity vehicle;
        if (scenario < 2) {
            vehicle = new Boat(level, x, 75, .5);
        } else if (scenario < 4) {
            Horse horse = EntityType.HORSE.create(level);
            horse.setTamed(true);
            horse.setNoAi(true);
            horse.equipSaddle(new ItemStack(Items.SADDLE), SoundSource.NEUTRAL);
            horse.setPos(x, 75, .5);
            vehicle = horse;
        } else {
            vehicle = new Minecart(level, x, 75, .5);
        }
        vehicle.setOnGround(true);
        if (scenario % 2 == 0) {
            level.addFreshEntity(lift);
            level.addFreshEntity(vehicle);
        } else {
            level.addFreshEntity(vehicle);
            level.addFreshEntity(lift);
        }
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.teleportTo(level, x, 76, .5, 0, 0);
        check(player.startRiding(vehicle, true), "Could not mount " + label());
        vehicle.positionRider(player);
        liftId = lift.getId();
        vehicleId = vehicle.getId();
    }

    @SubscribeEvent
    public static void sampleCamera(RenderLevelStageEvent event) {
        if (!enabled()
                || finished
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        boolean up = tick >= 35 && tick <= 60;
        boolean down = tick >= 100 && tick <= 125;
        if (!up && !down) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(liftId) instanceof AbstractLift lift)) return;
        try {
            var vehicle = mc.player.getRootVehicle();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            double deck = Mth.lerp(partial, lift.yOld, lift.getY()) + lift.getBbHeight();
            double camera = event.getCamera().getPosition().y;
            double relative = camera - deck;
            if (up) {
                upTicks.add(tick);
                minimumUp = Math.min(minimumUp, relative);
                maximumUp = Math.max(maximumUp, relative);
            } else {
                downTicks.add(tick);
                minimumDown = Math.min(minimumDown, relative);
                maximumDown = Math.max(maximumDown, relative);
            }
            check(
                    Math.abs(vehicle.getBoundingBox().minY - lift.getBoundingBox().maxY) < .4,
                    "Vehicle left the lift");
            SAMPLES.append(scenario)
                    .append(',')
                    .append(tick)
                    .append(',')
                    .append(partial)
                    .append(',')
                    .append(deck)
                    .append(',')
                    .append(camera)
                    .append(',')
                    .append(mc.player.getY())
                    .append(',')
                    .append(mc.player.yo)
                    .append(',')
                    .append(vehicle.getY())
                    .append(',')
                    .append(vehicle.yOld)
                    .append('\n');
            frames++;
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static String label() {
        return (scenario < 2 ? "boat" : scenario < 4 ? "horse" : "minecart")
                + (scenario % 2 == 0 ? "_lift_first" : "_vehicle_first");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void fail(Minecraft mc, Throwable error) {
        error.printStackTrace();
        try {
            Files.writeString(Path.of("mounted-camera.csv"), SAMPLES);
        } catch (Exception ignored) {
        }
        finish(mc, "FAIL " + error + "\n" + String.join("\n", RESULTS));
    }

    private static void finish(Minecraft mc, String result) {
        finished = true;
        try {
            Files.writeString(Path.of("mounted-checks.txt"), result + "\n");
        } catch (Exception error) {
            error.printStackTrace();
        }
        mc.stop();
    }
}
