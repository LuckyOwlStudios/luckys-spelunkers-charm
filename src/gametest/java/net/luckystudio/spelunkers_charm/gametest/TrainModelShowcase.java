// Train model showcase.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.NewLocomotiveModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrainModelShowcase {
    private static final Map<AbstractMinecart, Vec3> FIXED = new ConcurrentHashMap<>();
    private static final ArrayList<LocomotiveMinecart> engines = new ArrayList<>();
    private static CompletableFuture<Void> pending;
    private static int phase, ticks, captures, last = -1;
    private static ArmorStand camera;
    private static Throwable error;

    static boolean tick(Minecraft mc) {
        if (error != null) throw new AssertionError(error);
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
                                        player.setGameMode(GameType.SPECTATOR);
                                        player.teleportTo(24, 88, 136);
                                        for (int i = 0; i < 6; i++) {
                                            double x = 12 + i * 5;
                                            var engine =
                                                    new LocomotiveMinecart(
                                                            level, x, 84.0625, 140.5);
                                            engine.setDriveDirection(
                                                    net.minecraft.core.Direction.EAST);
                                            engine.setNoGravity(true);
                                            for (int slot = 0; slot < Math.min(i, 3); slot++)
                                                engine.setItem(slot, new ItemStack(Items.COAL, 8));
                                            level.addFreshEntity(engine);
                                            FIXED.put(engine, engine.position());
                                            engines.add(engine);
                                            if (i > 0) {
                                                var cart =
                                                        new Minecart(
                                                                level, x - 1.55, 84.0625, 140.5);
                                                cart.setNoGravity(true);
                                                level.addFreshEntity(cart);
                                                FIXED.put(cart, cart.position());
                                                MinecartCouplings.get(level).link(engine, cart);
                                            }
                                            if (i == 4) MinecartHandbrake.engage(engine);
                                            if (i == 5) engine.switchDirection();
                                        }
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
            mc.setCameraEntity(camera);
        }
        ticks++;
        if (ticks < 25) return false;
        int index = Math.min(5, (ticks - 25) / 20);
        var server = engines.get(index);
        if (!(mc.level.getEntity(server.getId()) instanceof LocomotiveMinecart cart)) return false;
        var renderer = mc.getEntityRenderDispatcher().getRenderer(cart);
        check(renderer instanceof LocomotiveRenderer, "Locomotive uses old placeholder renderer");
        check(
                ((net.minecraft.client.renderer.entity.MinecartRenderer<?>) renderer).model
                        instanceof NewLocomotiveModel,
                "Wrong locomotive mesh");
        int power = index == 4 ? 0 : Math.min(index, 3);
        check(
                cart.activeBurners() == power,
                "Power did not synchronize: " + index + ", actual=" + cart.activeBurners());
        String texture = renderer.getTextureLocation(cart).getPath();
        check(
                texture.endsWith(power == 0 ? "/locomotive.png" : "/locomotive_" + power + ".png"),
                "Power indicator texture mismatch: " + texture);
        check(
                cart.leverAngle() == (index == 4 ? 0 : index == 5 ? -30 : 30),
                "Lever did not synchronize");
        check(
                MinecartCouplerSides.mask(cart, 1) == (index == 0 ? 0 : 1),
                "Locomotive hook is not rear-only");
        boolean controls = index >= 4;
        camera.moveTo(
                cart.getX() - 1.6,
                cart.getY() + .15,
                cart.getZ() + (controls ? 3.2 : -3.2),
                controls ? -153.435f : -26.565f,
                19);
        camera.yHeadRot = camera.getYRot();
        camera.yHeadRotO = camera.getYRot();
        if (ticks < 145) return false;
        check(captures >= 6, "Missing model showcase captures");
        FIXED.clear();
        mc.setCameraEntity(mc.player);
        return true;
    }

    @SubscribeEvent
    public static void hold(EntityTickEvent.Post event) {
        var position = FIXED.get(event.getEntity());
        if (position != null) {
            event.getEntity().setPos(position);
            event.getEntity().setDeltaMovement(Vec3.ZERO);
            event.getEntity().hasImpulse = true;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (phase != 1
                || ticks < 35
                || (ticks - 35) % 20 != 0
                || ticks > 135
                || ticks == last
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        try {
            last = ticks;
            try (var image =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Path.of("model-state-" + ((ticks - 35) / 20) + ".png"));
                captures++;
            }
        } catch (Throwable failure) {
            error = failure;
        }
    }

    static String report() {
        return "model_states="
                + captures
                + " rear_hooks=true lever_states=true power_textures=true";
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
