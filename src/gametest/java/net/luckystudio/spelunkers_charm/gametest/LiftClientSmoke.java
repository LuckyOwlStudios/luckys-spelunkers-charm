package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSupport;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class LiftClientSmoke {
    private static boolean requested, setup, finished;
    private static volatile int[] lifts, riders;
    private static int tick,
            frames,
            samples,
            upSparks,
            downSparks,
            stoppedChecks,
            loopChecks,
            silentChecks,
            pitchMask,
            creativeGroups;
    private static double maxError, maxUncorrected;
    private static final Set<Particle> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Path REPORT = Path.of("client-checks.txt");

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!requested && mc.getOverlay() == null && mc.screen != null) {
                requested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.options.framerateLimit().set(60);
                mc.options
                        .getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER)
                        .set(0.02D);
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                verifyResources(mc);
                String migration = System.getProperty("spelunkers_lifts.migrationWorld", "");
                if (!migration.isEmpty()) {
                    mc.createWorldOpenFlows().openWorld(migration, mc::stop);
                    return;
                }
                var settings =
                        new LevelSettings(
                                "Lift regression",
                                net.minecraft.world.level.GameType.CREATIVE,
                                false,
                                net.minecraft.world.Difficulty.PEACEFUL,
                                true,
                                new GameRules(),
                                WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows()
                        .createFreshLevel(
                                "lift-client-" + System.currentTimeMillis(),
                                settings,
                                new WorldOptions(42L, false, false),
                                registries ->
                                        registries
                                                .registryOrThrow(Registries.WORLD_PRESET)
                                                .getHolderOrThrow(WorldPresets.FLAT)
                                                .value()
                                                .createWorldDimensions(),
                                mc.screen);
                return;
            }
            if (mc.level == null
                    || mc.player == null
                    || mc.getOverlay() != null
                    || mc.screen != null) return;
            if (!setup) {
                verifyCreativeOrder(mc);
                setup = true;
                if (!System.getProperty("spelunkers_lifts.migrationWorld", "").isEmpty()) {
                    mc.getSingleplayerServer()
                            .execute(() -> verifyMigration(mc.getSingleplayerServer().overworld()));
                    return;
                }
                mc.getSingleplayerServer()
                        .execute(() -> setupWorld(mc.getSingleplayerServer().overworld()));
                return;
            }
            if (lifts == null || mc.level.getEntity(lifts[2]) == null) return;
            ++tick;
            if (tick == 20 || tick == 90 || tick == 65 || tick == 135) {
                MovementType movement =
                        tick == 20
                                ? MovementType.UP
                                : tick == 90 ? MovementType.DOWN : MovementType.STATIONARY;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    ServerLevel level = mc.getSingleplayerServer().overworld();
                                    for (int id : lifts)
                                        if (level.getEntity(id) instanceof AbstractLift lift) {
                                            if (movement == MovementType.DOWN)
                                                for (BlockPos guide : lift.getGuidePositions()) {
                                                    double dx = lift.getX() - guide.getX() - .5,
                                                            dz = lift.getZ() - guide.getZ() - .5;
                                                    Direction inward =
                                                            Math.abs(dx) > Math.abs(dz)
                                                                    ? (dx > 0
                                                                            ? Direction.EAST
                                                                            : Direction.WEST)
                                                                    : (dz > 0
                                                                            ? Direction.SOUTH
                                                                            : Direction.NORTH);
                                                    for (int y = 68; y <= 89; y++)
                                                        level.setBlockAndUpdate(
                                                                guide.atY(y)
                                                                        .relative(
                                                                                inward
                                                                                        .getOpposite()),
                                                                Blocks.AIR.defaultBlockState());
                                                }
                                            lift.setMovementType(movement);
                                        }
                                });
            }
            if (tick == 155) {
                check(
                        frames > 30 && samples > 300,
                        "Client did not render enough moving frames: " + frames + "/" + samples);
                check(maxError < 0.035, "Remote riders visibly left deck: " + maxError);
                check(
                        upSparks >= 30 && downSparks >= 30,
                        "Missing visible sparks: up=" + upSparks + " down=" + downSparks);
                check(stoppedChecks > 5, "No stationary particle check");
                check(
                        loopChecks > 10 && silentChecks > 5,
                        "Movement loop was not verified playing/stopped");
                check(pitchMask == 7, "Pitch not verified for all lift sizes");
                finished = true;
                String result =
                        "PASS actual Minecraft client; frames="
                                + frames
                                + "; samples="
                                + samples
                                + "; maximum_displayed_feet_error="
                                + maxError
                                + "; maximum_unadjusted_error="
                                + maxUncorrected
                                + "; up_sparks="
                                + upSparks
                                + "; down_sparks="
                                + downSparks
                                + "; stopped_checks="
                                + stoppedChecks
                                + "; loop_checks="
                                + loopChecks
                                + "; silent_checks="
                                + silentChecks
                                + "; textures=12; station_models=128; new_track_models=32;"
                                + " half_power_up=true; zero_power_down=true; pitch_mask="
                                + pitchMask
                                + "; creative_groups="
                                + creativeGroups
                                + "\n";
                Files.writeString(REPORT, result);
                System.out.println("SPELUNKERS_LIFT_CLIENT_PASS " + result);
                mc.stop();
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static void verifyResources(Minecraft mc) throws Exception {
        String[] files = {
            "up_powered_lift_track_front",
            "up_powered_lift_track_front_powered",
            "up_powered_lift_track_side",
            "down_powered_lift_track_front",
            "down_powered_lift_track_front_powered",
            "down_powered_lift_track_side",
            "directional_powered_lift_track_end",
            "directional_powered_lift_track_end_powered",
            "detector_lift_track_front",
            "powered_detector_lift_track_front",
            "up_powered_lift_track_side_powered",
            "down_powered_lift_track_side_powered"
        };
        for (String file : files) {
            var id =
                    ResourceLocation.fromNamespaceAndPath(
                            "spelunkers_charm", "textures/block/" + file + ".png");
            try (var stream = mc.getResourceManager().getResourceOrThrow(id).open();
                    var image = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                check(
                        image.getWidth() == 16 && image.getHeight() == 16,
                        "Incorrect texture " + file);
            }
        }
        for (var state :
                ModBlocks.DIRECTIONAL_LIFT_TRACK.get().getStateDefinition().getPossibleStates())
            check(
                    mc.getBlockRenderer().getBlockModel(state)
                            != mc.getModelManager().getMissingModel(),
                    "Missing station model " + state);
        for (var state :
                ModBlocks.POWERED_LIFT_TRACK.get().getStateDefinition().getPossibleStates())
            check(
                    mc.getBlockRenderer().getBlockModel(state)
                            != mc.getModelManager().getMissingModel(),
                    "Missing motor model " + state);
        for (var block :
                new net.minecraft.world.level.block.Block[] {
                    ModBlocks.BASIC_LIFT_TRACK.get(), ModBlocks.DETECTOR_LIFT_TRACK.get()
                })
            for (var state : block.getStateDefinition().getPossibleStates())
                check(
                        mc.getBlockRenderer().getBlockModel(state)
                                != mc.getModelManager().getMissingModel(),
                        "Missing new guide model " + state);
    }

    private static void verifyCreativeOrder(Minecraft mc) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
                mc.level.enabledFeatures(), true, mc.level.registryAccess());
        net.minecraft.world.item.Item[] group = {
            ModBlocks.BASIC_LIFT_TRACK.get().asItem(),
            ModBlocks.POWERED_LIFT_TRACK.get().asItem(),
            ModBlocks.DIRECTIONAL_LIFT_TRACK.get().asItem(),
            ModBlocks.DETECTOR_LIFT_TRACK.get().asItem(),
            net.luckystudio.spelunkers_charm.init.ModItems.SMALL_LIFT.get(),
            net.luckystudio.spelunkers_charm.init.ModItems.MEDIUM_LIFT.get(),
            net.luckystudio.spelunkers_charm.init.ModItems.LARGE_LIFT.get()
        };
        for (var key :
                java.util.List.of(
                        net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS,
                        net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES,
                        net.minecraft.world.item.CreativeModeTabs.SEARCH)) {
            var tab =
                    net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(
                            key);
            var entries = new java.util.ArrayList<>(tab.getDisplayItems());
            int start = -1;
            for (int i = 0; i < entries.size(); i++)
                if (entries.get(i).is(group[0])) {
                    start = i;
                    break;
                }
            check(
                    start >= 0 && start + group.length <= entries.size(),
                    "Missing lift group in creative " + key.location());
            for (int i = 0; i < group.length; i++)
                check(
                        entries.get(start + i).is(group[i]),
                        "Separated lift items in creative " + key.location() + " at " + i);
            creativeGroups++;
        }
    }

    private static void verifyMigration(ServerLevel level) {
        try {
            BlockPos oldBasic = new BlockPos(2, 75, 0), oldDetector = new BlockPos(2, 76, 0);
            level.getChunkAt(oldBasic);
            check(
                    level.getBlockState(oldBasic).is(ModBlocks.BASIC_LIFT_TRACK.get()),
                    "Legacy world lost basic rack at "
                            + oldBasic
                            + ": "
                            + level.getBlockState(oldBasic));
            check(
                    level.getBlockState(oldDetector).is(ModBlocks.DETECTOR_LIFT_TRACK.get()),
                    "Legacy world lost detector rack");
            check(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK
                            .getKey(level.getBlockState(oldBasic).getBlock())
                            .getPath()
                            .equals("basic_lift_track"),
                    "Old world kept wrong canonical block ID");
            Minecraft.getInstance()
                    .execute(
                            () -> {
                                try {
                                    Files.writeString(
                                            Path.of("migration-checks.txt"),
                                            "PASS loaded lifts.4 world with lifts.5; old basic"
                                                + " palette remapped, detector preserved, registry"
                                                + " snapshot accepted\n");
                                    finished = true;
                                    Minecraft.getInstance().stop();
                                } catch (Throwable error) {
                                    fail(error);
                                }
                            });
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    private static void setupWorld(ServerLevel level) {
        try {
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            var player = level.players().getFirst();
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 14.5, 85, 31, 180, 21);
            int[] liftIds = new int[3], riderIds = new int[12];
            int index = 0;
            for (AbstractLift.Type type : AbstractLift.Type.values()) {
                double x = index * 14 + 0.5;
                AbstractLift lift =
                        AbstractLift.createLift(level, x, 74, 0.5, type, ItemStack.EMPTY, null);
                lift.setYRot(180);
                int column = 0;
                for (BlockPos guide : lift.getGuidePositions()) {
                    boolean motor = column++ < lift.getGuidePositions().size() / 2;
                    double dx = x - guide.getX() - 0.5, dz = 0.5 - guide.getZ() - 0.5;
                    Direction inward =
                            Math.abs(dx) > Math.abs(dz)
                                    ? (dx > 0 ? Direction.EAST : Direction.WEST)
                                    : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                    for (int y = 68; y <= 89; ++y) {
                        BlockPos rail = guide.atY(y);
                        var block =
                                motor
                                        ? ModBlocks.POWERED_LIFT_TRACK.get()
                                        : (y == 76
                                                ? ModBlocks.DETECTOR_LIFT_TRACK.get()
                                                : ModBlocks.BASIC_LIFT_TRACK.get());
                        level.setBlockAndUpdate(
                                rail,
                                block.defaultBlockState()
                                        .setValue(HorizontalDirectionalBlock.FACING, inward));
                        level.setBlockAndUpdate(
                                rail.relative(inward.getOpposite()),
                                (motor ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
                    }
                }
                level.addFreshEntity(lift);
                liftIds[index] = lift.getId();
                Entity[] entities = {
                    EntityType.ARMOR_STAND.create(level),
                    EntityType.MINECART.create(level),
                    EntityType.SHEEP.create(level),
                    new net.minecraft.world.entity.item.ItemEntity(
                            level, x + 0.65, 75, -0.15, new ItemStack(Items.DIAMOND))
                };
                double[][] positions = {{-.65, -.65}, {.65, .65}, {-.65, .65}, {.65, -.65}};
                for (int n = 0; n < entities.length; ++n) {
                    Entity entity = entities[n];
                    entity.setPos(x + positions[n][0], 75, 0.5 + positions[n][1]);
                    if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.setOnGround(true);
                    level.addFreshEntity(entity);
                    riderIds[index * 4 + n] = entity.getId();
                }
                ++index;
            }
            riders = riderIds;
            lifts = liftIds;
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    @SubscribeEvent
    public static void rendered(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke")
                || finished
                || lifts == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!((tick >= 32 && tick <= 60) || (tick >= 102 && tick <= 130))) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            ++frames;
            for (int i = 0; i < 3; ++i) {
                Entity lift = mc.level.getEntity(lifts[i]);
                if (lift == null) continue;
                double deck = Mth.lerp(partial, lift.yOld, lift.getY()) + lift.getBbHeight();
                for (int n = 0; n < 4; ++n) {
                    Entity rider = mc.level.getEntity(riders[i * 4 + n]);
                    if (rider == null) continue;
                    double feet =
                            Mth.lerp(partial, rider.yOld, rider.getY())
                                    + rider.getBoundingBox().minY
                                    - rider.getY();
                    double offset = LiftClientSupport.getRenderOffset(rider, partial);
                    maxUncorrected = Math.max(maxUncorrected, Math.abs(feet - deck));
                    maxError = Math.max(maxError, Math.abs(feet + offset - deck));
                    ++samples;
                }
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public static void pixels(RenderGuiEvent.Post event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke") || finished || lifts == null)
            return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (tick == 45 || tick == 115) {
                Path screenshot = Path.of(tick == 45 ? "lift-up.png" : "lift-down.png");
                if (!Files.exists(screenshot))
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(screenshot);
                    }
            }
            if (tick < 30) return;
            Field loopsField =
                    net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSounds.class
                            .getDeclaredField("SOUNDS");
            loopsField.setAccessible(true);
            Map<?, ?> loops = (Map<?, ?>) loopsField.get(null);
            int activeLoops = 0;
            for (Object sound : loops.values()) {
                var instance = (net.minecraft.client.resources.sounds.SoundInstance) sound;
                if (mc.getSoundManager().isActive(instance)) activeLoops++;
                Field owner = sound.getClass().getDeclaredField("lift");
                owner.setAccessible(true);
                var lift = (AbstractLift) owner.get(sound);
                int flag = lift.getBbWidth() < 4 ? 1 : lift.getBbWidth() < 6 ? 2 : 4;
                float expected = flag == 1 ? 1.2F : flag == 2 ? 1.0F : .8F;
                check(
                        Math.abs(instance.getPitch() - expected) < .00001,
                        "Wrong engine pitch for " + lift.getBbWidth());
                pitchMask |= flag;
            }
            if ((tick >= 32 && tick <= 60) || (tick >= 102 && tick <= 130)) {
                check(activeLoops > 0, "No active running sound while travelling");
                check(loops.size() <= 3, "Duplicate loops for one lift");
                loopChecks++;
            }
            if ((tick >= 80 && tick <= 85) || tick >= 150) {
                check(activeLoops == 0, "Loop kept playing after stop");
                silentChecks++;
            }
            int warm = 0;
            Object engine = mc.particleEngine;
            Field field = engine.getClass().getDeclaredField("particles");
            field.setAccessible(true);
            Map<?, ?> particles = (Map<?, ?>) field.get(engine);
            for (Object value : particles.values())
                for (Object entry : (Iterable<?>) value) {
                    if (!(entry instanceof GlowParticle particle)
                            || number(particle, "rCol") < .99
                            || Math.abs(number(particle, "bCol") - .15) > .001) continue;
                    ++warm;
                    if (seen.add(particle)) {
                        double vy = number(particle, "yd");
                        if (tick >= 30 && tick <= 64) {
                            check(vy < 0, "Ascending lift spark did not travel down: " + vy);
                            ++upSparks;
                        }
                        if (tick >= 100 && tick <= 134) {
                            check(vy > 0, "Descending lift spark did not travel up: " + vy);
                            ++downSparks;
                        }
                    }
                }
            if ((tick >= 80 && tick <= 85) || tick >= 150) {
                check(warm == 0, "Stopped lift kept emitting sparks: " + warm);
                ++stoppedChecks;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static double number(Particle particle, String name) throws Exception {
        Field field = Particle.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(particle)).doubleValue();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void fail(Throwable error) {
        if (finished) return;
        finished = true;
        error.printStackTrace();
        try {
            Files.writeString(REPORT, "FAIL " + error + "\n");
        } catch (Exception ignored) {
        }
        Minecraft.getInstance().stop();
    }
}
