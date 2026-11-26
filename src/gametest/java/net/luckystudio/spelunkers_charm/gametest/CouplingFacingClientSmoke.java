// Coupling facing client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CouplingFacingClientSmoke {
    private static int phase, ticks, samples, hooks;
    private static CompletableFuture<Void> pending;
    private static final List<AbstractMinecart> server = new ArrayList<>();
    private static PassengerTrainRoute route;
    private static LocomotiveMinecart engine;
    private static final int[] fronts = new int[9];
    private static final StringBuilder trace =
            new StringBuilder("tick,cart,x,z,yaw,expected,dot\n");

    private CouplingFacingClientSmoke() {}

    static boolean tick(Minecraft mc) throws Exception {
        if (phase == 0) {
            var player = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var level = mc.getSingleplayerServer().overworld();
                                        route =
                                                new PassengerTrainRoute(
                                                        level, new BlockPos(0, 82, 160));
                                        route.build();
                                        for (int i = 0; i < 9; i++) {
                                            double at = 18 - i * 1.55;
                                            Vec3 p = route.point(at),
                                                    heading =
                                                            route.point(at + .05)
                                                                    .subtract(route.point(at - .05))
                                                                    .normalize();
                                            AbstractMinecart cart;
                                            if (i == 0) {
                                                engine =
                                                        new LocomotiveMinecart(
                                                                level, p.x, p.y, p.z);
                                                engine.setDriveDirection(
                                                        Direction.getNearest(
                                                                heading.x, 0, heading.z));
                                                engine.setItem(0, new ItemStack(Items.COAL, 12));
                                                cart = engine;
                                            } else cart = new Minecart(level, p.x, p.y, p.z);
                                            cart.setYRot(
                                                    (float)
                                                            Math.toDegrees(
                                                                    Math.atan2(
                                                                            heading.z, heading.x)));
                                            level.addFreshEntity(cart);
                                            if (i > 0
                                                    && MinecartCouplings.get(level)
                                                                    .link(server.getLast(), cart)
                                                            != MinecartCouplings.Result.LINKED)
                                                throw new AssertionError(
                                                        "Facing fixture failed to link");
                                            server.add(cart);
                                        }
                                        MinecartHandbrake.engage(engine);
                                        var viewer =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(player);
                                        viewer.setGameMode(
                                                net.minecraft.world.level.GameType.SPECTATOR);
                                        viewer.teleportTo(15, 94, 175);
                                        viewer.setXRot(-85);
                                    });
            phase = 1;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (++ticks > 800) throw new AssertionError("Facing client timed out");
        if (phase == 1) {
            for (var cart : server) if (mc.level.getEntity(cart.getId()) == null) return false;
            if (ticks < 35) return false;
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        for (int i = 0; i < server.size(); i++) {
                                            var cart = server.get(i);
                                            double at = route.progress(cart.position());
                                            Vec3 heading =
                                                    route.point(at + .05)
                                                            .subtract(route.point(at - .05));
                                            fronts[i] =
                                                    MinecartHingeGeometry.supportedAxis(cart)
                                                                            .dot(heading)
                                                                    < 0
                                                            ? -1
                                                            : 1;
                                        }
                                        engine.releaseEmergencyBrake();
                                    });
            phase = 2;
            ticks = 0;
            return false;
        }
        if (ticks < 300) {
            mc.player.setXRot(-85);
            mc.player.setYRot(0);
            return false;
        }
        mc.player.setXRot(55);
        mc.player.setYRot(0);
        if (ticks == 420)
            pending = mc.getSingleplayerServer().submit(() -> engine.switchDirection());
        for (int i = 0; i < server.size(); i++) {
            if (!(mc.level.getEntity(server.get(i).getId()) instanceof AbstractMinecart cart))
                throw new AssertionError("Facing cart unloaded");
            double at = route.progress(cart.position());
            Vec3 expected = route.point(at + .08).subtract(route.point(at - .08)).normalize();
            Vec3 axis = MinecartBodyPose.axis(cart, 1);
            double dot = axis.dot(expected) * fronts[i];
            samples++;
            trace.append(ticks)
                    .append(',')
                    .append(i)
                    .append(',')
                    .append(cart.getX())
                    .append(',')
                    .append(cart.getZ())
                    .append(',')
                    .append(MinecartBodyPose.yaw(cart, 1))
                    .append(',')
                    .append(Math.toDegrees(Math.atan2(expected.z, expected.x)))
                    .append(',')
                    .append(dot)
                    .append('\n');
            if (dot < .20) {
                Files.writeString(Path.of("coupling-facing-trace.csv"), trace);
                throw new AssertionError(
                        "Offscreen turn swapped physical front: tick="
                                + ticks
                                + ", cart="
                                + i
                                + ", dot="
                                + dot);
            }
            var peers = (MinecartCouplingVisuals) cart;
            for (int id :
                    new int[] {
                        peers.spelunkersCharm$firstCouplingId(),
                        peers.spelunkersCharm$secondCouplingId()
                    }) {
                if (!(mc.level.getEntity(id) instanceof AbstractMinecart peer)) continue;
                var path = MinecartRailPath.between(cart, peer);
                if (path == null || path.distance() > 2) continue;
                double outward =
                        axis.scale(MinecartHingeGeometry.side(cart, peer)).dot(path.firstToward());
                hooks++;
                if (outward < .10) {
                    Files.writeString(Path.of("coupling-facing-trace.csv"), trace);
                    throw new AssertionError(
                            "Hook crossed through cart: " + i + ", dot=" + outward);
                }
            }
        }
        if (ticks % 20 == 0) Files.writeString(Path.of("coupling-facing-trace.csv"), trace);
        return ticks >= 620;
    }

    static String report() {
        return "offscreen_ticks=300 commanded_reversals=1 facing_samples="
                + samples
                + " hook_samples="
                + hooks
                + "\n";
    }
}
