// Train continuity client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class TrainContinuityClientSmoke {
    private static int phase, ticks, samples, stalls;
    private static CompletableFuture<Void> pending;
    private static final ArrayList<AbstractMinecart> carts = new ArrayList<>();
    private static double[] previous;
    private static int[] previousTicks;
    private static double sum, squared, maximumStep, maximumLag;
    private static final StringBuilder trace =
            new StringBuilder("tick,cart,server_x,client_x,step,lag\n");

    static boolean tick(Minecraft mc) throws Exception {
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
                                        player.teleportTo(55, 88, 112);
                                        var fixture =
                                                new TrainFlightFixture(
                                                        level,
                                                        new BlockPos(0, 82, 92),
                                                        180,
                                                        182,
                                                        195);
                                        fixture.build();
                                        for (int x = 0; x < 200; x++) {
                                            level.setBlock(
                                                    new BlockPos(x, 83, 100),
                                                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                                    2);
                                            level.setBlock(
                                                    new BlockPos(x, 84, 100),
                                                    Blocks.POWERED_RAIL
                                                            .defaultBlockState()
                                                            .setValue(
                                                                    PoweredRailBlock.SHAPE,
                                                                    RailShape.EAST_WEST)
                                                            .setValue(
                                                                    PoweredRailBlock.POWERED, true),
                                                    2);
                                        }
                                        carts.addAll(fixture.train(24.5, 6));
                                    });
            phase = 1;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (phase == 2) return TrainFlightClientSmoke.tick(mc);
        if (carts.getFirst().tickCount < 30) return false;
        ticks++;
        if (previous == null) {
            previous = new double[carts.size()];
            previousTicks = new int[carts.size()];
            java.util.Arrays.fill(previous, Double.NaN);
        }
        for (int i = 0; i < carts.size(); i++) {
            if (!(mc.level.getEntity(carts.get(i).getId()) instanceof AbstractMinecart cart)) {
                if (ticks < 80) return false;
                throw new AssertionError("Continuity cart missing " + i);
            }
            double current = cart.getX(),
                    step = Double.isNaN(previous[i]) ? 0 : current - previous[i];
            previous[i] = current;
            int elapsed = cart.tickCount - previousTicks[i];
            previousTicks[i] = cart.tickCount;
            if (ticks < 35 || ticks > 150 || elapsed != 1) continue;
            sum += step;
            squared += step * step;
            samples++;
            if (step < .005) stalls++;
            maximumStep = Math.max(maximumStep, step);
            maximumLag = Math.max(maximumLag, Math.abs(carts.get(i).getX() - current));
            trace.append(ticks)
                    .append(',')
                    .append(i)
                    .append(',')
                    .append(carts.get(i).getX())
                    .append(',')
                    .append(current)
                    .append(',')
                    .append(step)
                    .append(',')
                    .append(carts.get(i).getX() - current)
                    .append('\n');
        }
        if (ticks % 10 == 0) Files.writeString(Path.of("continuity-trace.csv"), trace);
        if (ticks < 155) return false;
        Files.writeString(Path.of("continuity-motion.txt"), report());
        if (Boolean.getBoolean("spelunkers_railways.measureOnly")) return true;
        double mean = sum / samples,
                variation = Math.sqrt(Math.max(0, squared / samples - mean * mean)) / mean;
        if (stalls > samples * .015 || variation > .12)
            throw new AssertionError(
                    "Cart motion still pulses: variation="
                            + variation
                            + ", stalls="
                            + stalls
                            + "/"
                            + samples);
        phase = 2;
        return false;
    }

    static String report() {
        double mean = samples == 0 ? 0 : sum / samples,
                variation =
                        mean == 0
                                ? 0
                                : Math.sqrt(Math.max(0, squared / samples - mean * mean)) / mean;
        return "samples="
                + samples
                + " stalls="
                + stalls
                + " mean_step="
                + mean
                + " variation="
                + variation
                + " maximum_step="
                + maximumStep
                + " maximum_lag="
                + maximumLag
                + "\n"
                + TrainFlightClientSmoke.report();
    }
}
