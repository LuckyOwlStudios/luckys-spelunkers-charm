// Performance tests.
package net.luckystudio.spelunkers_charm.gametest;

import com.sun.management.ThreadMXBean;

import jdk.jfr.Recording;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@GameTestHolder("spelunkers_performance_tests")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = LiftTestMod.ID)
public final class PerformanceTests {
    private static final ThreadMXBean METER = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final int WARMUP = 160, SAMPLES = 400;
    private static final String[] SCENES = {
        "empty", "idle_64", "train_8", "train_24", "lifts_8", "flight_16"
    };
    private static final List<Entity> entities = new ArrayList<>();
    private static final List<String> rows = new ArrayList<>();
    private static final List<Long> wall = new ArrayList<>(),
            cpu = new ArrayList<>(),
            allocated = new ArrayList<>();
    private static ServerLevel level;
    private static GameTestHelper helper;
    private static TrainRegressionFixture.Loop train;
    private static int scene = -1, repeat, age;
    private static long startWall, startCpu, startBytes;
    private static boolean measure;
    private static Recording recording;

    @GameTest(template = "course", timeoutTicks = 10000)
    public static void representativeLoads(GameTestHelper h) throws Exception {
        helper = h;
        level = h.getLevel();
        level.getServer().tickRateManager().setTickRate(100);
        METER.setThreadAllocatedMemoryEnabled(true);
        METER.setThreadCpuTimeEnabled(true);
        var origin = h.absolutePos(BlockPos.ZERO);
        for (int x = -1; x <= 8; x++)
            for (int z = -1; z <= 8; z++) {
                var p = origin.offset(x * 16, 0, z * 16);
                level.getChunkAt(p);
                level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, true);
            }
        recording = new Recording();
        recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(5));
        recording.enable("jdk.ObjectAllocationSample");
        recording.start();
        nextScene();
        h.onEachTick(
                () -> {
                    if (level == null) return;
                    if (SCENES[scene].equals("lifts_8"))
                        for (var entity : entities) {
                            var lift = (AbstractLift) entity;
                            if (lift.getY() > origin.getY() + 85)
                                lift.setMovementType(MovementType.DOWN);
                            if (lift.getY() < origin.getY() + 5)
                                lift.setMovementType(MovementType.UP);
                        }
                });
    }

    private static void clearEntities() {
        for (var entity : entities) entity.discard();
        entities.clear();
        train = null;
    }

    private static void nextScene() throws Exception {
        clearEntities();
        if (++scene == SCENES.length) {
            Files.write(Path.of("performance.csv"), rows);
            recording.stop();
            recording.dump(Path.of("performance.jfr"));
            recording.close();
            level = null;
            helper.succeed();
            return;
        }
        age = repeat = 0;
        var origin = helper.absolutePos(BlockPos.ZERO);
        switch (SCENES[scene]) {
            case "flight_16" -> spawnFlight();
            case "idle_64" -> {
                for (int i = 0; i < 64; i++) {
                    var p = origin.offset(5 + i % 8 * 4, 2, 5 + i / 8 * 4);
                    level.setBlock(p.below(), Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(p, Blocks.RAIL.defaultBlockState(), 2);
                    var cart = new Minecart(level, p.getX() + .5, p.getY() + .0625, p.getZ() + .5);
                    level.addFreshEntity(cart);
                    entities.add(cart);
                }
            }
            case "train_8", "train_24" -> {
                train =
                        TrainRegressionFixture.loop(
                                level, origin.offset(48, 0, 0), scene == 2 ? 7 : 23);
                entities.addAll(train.carts());
            }
            case "lifts_8" -> {
                for (int i = 0; i < 8; i++) {
                    var p = origin.offset(5 + i % 4 * 8, 5, 50 + i / 4 * 8);
                    var lift =
                            new net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift(
                                    level, p.getX() + .5, p.getY(), p.getZ() + .5);
                    lift.setYRot(0);
                    for (var guide : lift.getGuidePositions())
                        for (int y = origin.getY() + 2; y < origin.getY() + 90; y++) {
                            var rail = guide.atY(y);
                            Direction inward =
                                    rail.getX() < p.getX() ? Direction.EAST : Direction.WEST;
                            level.setBlock(
                                    rail.relative(inward.getOpposite()),
                                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                    2);
                            level.setBlock(
                                    rail,
                                    ModBlocks.POWERED_LIFT_TRACK
                                            .get()
                                            .defaultBlockState()
                                            .setValue(HorizontalDirectionalBlock.FACING, inward)
                                            .setValue(
                                                    net.minecraft.world.level.block.state.properties
                                                            .BlockStateProperties.POWERED,
                                                    true),
                                    2);
                        }
                    lift.setMovementType(MovementType.UP);
                    level.addFreshEntity(lift);
                    entities.add(lift);
                }
            }
        }
    }

    private static void spawnFlight() {
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (int i = 0; i < 16; i++) {
            AbstractMinecart cart =
                    i == 0
                            ? new LocomotiveMinecart(
                                    level,
                                    origin.getX() + 80,
                                    origin.getY() + 120,
                                    origin.getZ() + 90)
                            : new Minecart(
                                    level,
                                    origin.getX() + 80 - i * 1.55,
                                    origin.getY() + 120,
                                    origin.getZ() + 90);
            TrainRegressionFixture.facing(cart, new net.minecraft.world.phys.Vec3(1, 0, 0));
            cart.setDeltaMovement(.35, .12, 0);
            level.addFreshEntity(cart);
            if (!entities.isEmpty()
                    && MinecartCouplings.get(level)
                                    .link((AbstractMinecart) entities.getLast(), cart)
                            != MinecartCouplings.Result.LINKED)
                throw new AssertionError("Flight performance train did not link");
            ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
            MinecartFlightPhysics.launch(cart);
            entities.add(cart);
        }
    }

    private static boolean flying() {
        return SCENES[scene].equals("flight_16");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(LevelTickEvent.Pre event) {
        if (level == null || event.getLevel() != level) return;
        measure = age >= (flying() ? 10 : WARMUP);
        if (measure) {
            startBytes = METER.getThreadAllocatedBytes(Thread.currentThread().threadId());
            startCpu = METER.getCurrentThreadCpuTime();
            startWall = System.nanoTime();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(LevelTickEvent.Post event) throws Exception {
        if (level == null || event.getLevel() != level) return;
        if (measure) {
            wall.add(System.nanoTime() - startWall);
            cpu.add(METER.getCurrentThreadCpuTime() - startCpu);
            allocated.add(
                    METER.getThreadAllocatedBytes(Thread.currentThread().threadId()) - startBytes);
        }
        if (++age < (flying() ? 60 : WARMUP + SAMPLES)) return;
        if (train != null) TrainRegressionFixture.intact(level, train.carts());
        if (flying())
            TrainRegressionFixture.intact(
                    level, entities.stream().map(e -> (AbstractMinecart) e).toList());
        if (rows.isEmpty())
            rows.add(
                    "scene,repeat,ticks,wall_median_ms,wall_p95_ms,cpu_mean_ms,allocated_mean_bytes");
        String row =
                String.format(
                        Locale.ROOT,
                        "%s,%d,%d,%.6f,%.6f,%.6f,%.1f",
                        SCENES[scene],
                        repeat + 1,
                        wall.size(),
                        percentile(wall, .5) / 1e6,
                        percentile(wall, .95) / 1e6,
                        cpu.stream().mapToLong(Long::longValue).average().orElse(0) / 1e6,
                        allocated.stream().mapToLong(Long::longValue).average().orElse(0));
        rows.add(row);
        System.out.println("PERFORMANCE " + row);
        wall.clear();
        cpu.clear();
        allocated.clear();
        age = 0;
        if (++repeat == 3) nextScene();
        else if (flying()) {
            clearEntities();
            spawnFlight();
        }
    }

    private static long percentile(List<Long> values, double fraction) {
        var sorted = values.stream().mapToLong(Long::longValue).sorted().toArray();
        return sorted[Math.min(sorted.length - 1, (int) (sorted.length * fraction))];
    }
}
