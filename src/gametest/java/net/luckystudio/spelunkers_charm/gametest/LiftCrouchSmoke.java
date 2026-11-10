package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class LiftCrouchSmoke {
    private static boolean requested, setup, finished;
    private static volatile int liftId = -1;
    private static int tick, upChecks, downChecks, standingChecks;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientCrouch") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!requested && mc.getOverlay() == null && mc.screen != null) {
                requested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.options.framerateLimit().set(60);
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                var settings =
                        new LevelSettings(
                                "Lift crouch regression",
                                net.minecraft.world.level.GameType.CREATIVE,
                                false,
                                net.minecraft.world.Difficulty.PEACEFUL,
                                true,
                                new GameRules(),
                                WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows()
                        .createFreshLevel(
                                "lift-crouch-" + System.currentTimeMillis(),
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
                setup = true;
                mc.getSingleplayerServer()
                        .execute(() -> setupWorld(mc.getSingleplayerServer().overworld()));
                return;
            }
            if (!(mc.level.getEntity(liftId) instanceof AbstractLift lift) || mc.player.getY() < 70)
                return;
            ++tick;
            boolean holding = tick < 95 || tick >= 110 && tick < 160;
            mc.options.keyShift.setDown(holding);
            if (tick == 40 || tick == 90 || tick == 110 || tick == 160) {
                MovementType movement =
                        tick == 40
                                ? MovementType.UP
                                : tick == 110 ? MovementType.DOWN : MovementType.STATIONARY;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var serverLift =
                                            mc.getSingleplayerServer()
                                                    .overworld()
                                                    .getEntity(liftId);
                                    if (serverLift instanceof AbstractLift platform)
                                        platform.setMovementType(movement);
                                });
            }
            if (tick >= 50 && tick <= 85 || tick >= 120 && tick <= 155) {
                check(
                        mc.player.isCrouching(),
                        "Lost crouching while input held at tick "
                                + tick
                                + "; pose="
                                + mc.player.getPose()
                                + "; feet="
                                + mc.player.getY()
                                + "; deck="
                                + lift.getBoundingBox().maxY);
                check(
                        mc.player.getPose() == Pose.CROUCHING,
                        "Pose flickered at tick " + tick + ": " + mc.player.getPose());
                check(
                        Math.abs(mc.player.getY() - lift.getBoundingBox().maxY) < 0.4,
                        "Player left moving deck");
                if (tick < 95) ++upChecks;
                else ++downChecks;
            }
            if (tick >= 103 && tick <= 108 || tick >= 170 && tick <= 174) {
                check(
                        !mc.player.isCrouching() && mc.player.getPose() == Pose.STANDING,
                        "Release failed to restore standing");
                ++standingChecks;
            }
            if (tick == 175) {
                check(
                        upChecks >= 30 && downChecks >= 30 && standingChecks >= 8,
                        "Incomplete crouch coverage");
                finish(
                        "PASS actual client crouch; up_checks="
                                + upChecks
                                + "; down_checks="
                                + downChecks
                                + "; standing_checks="
                                + standingChecks
                                + "; keyboard_input=true; pose_flicker=0\n");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL " + error + "\n");
        }
    }

    private static void setupWorld(ServerLevel level) {
        try {
            level.setDayTime(6000);
            AbstractLift lift =
                    AbstractLift.createLift(
                            level, 0.5, 74, 0.5, AbstractLift.Type.SMALL, ItemStack.EMPTY, null);
            lift.setYRot(180);
            for (BlockPos guide : lift.getGuidePositions()) {
                Direction inward = guide.getX() < 0 ? Direction.EAST : Direction.WEST;
                for (int y = 68; y <= 90; ++y) {
                    level.setBlockAndUpdate(
                            guide.atY(y),
                            ModBlocks.POWERED_LIFT_TRACK
                                    .get()
                                    .defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, inward));
                    level.setBlockAndUpdate(
                            guide.atY(y).relative(inward.getOpposite()),
                            Blocks.REDSTONE_BLOCK.defaultBlockState());
                }
            }
            level.addFreshEntity(lift);
            var player = level.players().getFirst();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(level, 0.5, 75, 0.5, 0, 15);
            liftId = lift.getId();
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> finish("FAIL setup " + error + "\n"));
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void finish(String result) {
        finished = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyShift.setDown(false);
        try {
            Files.writeString(Path.of("crouch-checks.txt"), result);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("SPELUNKERS_CROUCH " + result);
        mc.stop();
    }
}
