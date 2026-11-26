// Locomotive controls client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class LocomotiveControlsClientSmoke {
    private static int phase, ticks, captures, lastCapture = -1, lockedFuel;
    private static CompletableFuture<Void> pending;
    private static LocomotiveMinecart engine;
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
                                        for (int x = 42; x <= 48; x++)
                                            for (int z = 138; z <= 144; z++)
                                                level.setBlockAndUpdate(
                                                        new net.minecraft.core.BlockPos(x, 83, z),
                                                        net.minecraft.world.level.block.Blocks.STONE
                                                                .defaultBlockState());
                                        engine = new LocomotiveMinecart(level, 45, 84.0625, 140.5);
                                        engine.setNoGravity(true);
                                        engine.setDriveDirection(net.minecraft.core.Direction.EAST);
                                        for (int i = 0; i < 3; i++)
                                            engine.setItem(
                                                    i,
                                                    new net.minecraft.world.item.ItemStack(
                                                            net.minecraft.world.item.Items.COAL,
                                                            8));
                                        var tag = engine.saveWithoutId(new CompoundTag());
                                        var fuel = new net.minecraft.nbt.ListTag();
                                        for (int remaining : new int[] {10000, 5500, 1500}) {
                                            var burner = new CompoundTag();
                                            burner.putInt("Remaining", remaining);
                                            burner.putInt("Duration", 10000);
                                            fuel.add(burner);
                                        }
                                        tag.put("Burners", fuel);
                                        engine.load(tag);
                                        level.addFreshEntity(engine);
                                        player.setGameMode(GameType.SURVIVAL);
                                        player.teleportTo(45, 84.0625, 142);
                                        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                                        player.resetFallDistance();
                                        player.getInventory()
                                                .setItem(
                                                        0,
                                                        net.luckystudio.spelunkers_charm.init
                                                                .ModItems.LOCOMOTIVE
                                                                .toStack());
                                        player.openMenu(engine);
                                    });
            mc.options.hideGui = false;
            mc.setCameraEntity(mc.player);
            phase = 1;
            ticks = 0;
            return false;
        }
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        if (!(mc.screen instanceof LocomotiveScreen screen)) {
            if (++ticks > 120) throw new AssertionError("New locomotive GUI did not open");
            return false;
        }
        ticks++;
        var menu = screen.getMenu();
        if (ticks < 20) return false;
        if (phase == 1) {
            check(
                    ((LocomotiveMinecart) mc.level.getEntity(engine.getId())).activeBurners() == 3,
                    "Initial fuel power did not synchronize");
            check(
                    menu.getFuelPercent(0) > menu.getFuelPercent(1)
                            && menu.getFuelPercent(1) > menu.getFuelPercent(2),
                    "Independent flame levels lost");
            var buttons =
                    screen.children().stream()
                            .filter(AbstractButton.class::isInstance)
                            .map(AbstractButton.class::cast)
                            .toList();
            check(buttons.size() == 6, "GUI lost a direction or lock button");
            int x = (screen.width - 176) / 2, y = (screen.height - 166) / 2;
            for (int i = 0; i < 3; i++) {
                var slot = menu.slots.get(i);
                check(slot.x == 55 + 25 * i && slot.y == 34, "Client fuel slot bounds wrong");
                var button = buttons.get(3 + i);
                check(
                        button.getX() == x + 42 + i * 31
                                && button.getY() == y + 60
                                && button.getWidth() == 29
                                && button.getHeight() == 13,
                        "Direction button did not match supplied GUI");
            }
            if (captures < 1) return false;
            click(screen, 88, 23);
            phase = 2;
            ticks = 0;
            return false;
        }
        if (phase == 2) {
            check(
                    !menu.burnerEnabled(1)
                            && ((LocomotiveMinecart) mc.level.getEntity(engine.getId()))
                                            .activeBurners()
                                    == 2,
                    "Clicking flame failed to lock firebox");
            lockedFuel = engine.fuelRemaining(1);
            if (captures < 2) return false;
            click(screen, 87, 66);
            phase = 3;
            ticks = 0;
            return false;
        }
        if (phase == 3) {
            check(
                    menu.emergencyBrake()
                            && ((LocomotiveMinecart) mc.level.getEntity(engine.getId()))
                                            .activeBurners()
                                    == 0,
                    "Stop failed to synchronize brake and paused flames");
            check(engine.fuelRemaining(1) == lockedFuel, "Locked burner continued to consume fuel");
            if (captures < 3) return false;
            click(screen, 87, 66);
            phase = 4;
            ticks = 0;
            return false;
        }
        if (phase == 4) {
            check(menu.emergencyBrake(), "Repeated GUI stop released brake");
            click(screen, 56, 66);
            phase = 5;
            ticks = 0;
            return false;
        }
        if (phase == 5) {
            check(
                    !menu.emergencyBrake() && menu.isReversed(),
                    "Reverse button did not select reverse and release brake");
            if (captures < 4) return false;
            click(screen, 118, 66);
            phase = 6;
            ticks = 0;
            return false;
        }
        check(
                !menu.emergencyBrake() && !menu.isReversed(),
                "Forward button did not restore forward");
        check(captures >= 4, "Missing GUI render evidence");
        mc.player.closeContainer();
        return true;
    }

    private static void click(LocomotiveScreen screen, int x, int y) {
        check(
                screen.mouseClicked(
                        (screen.width - 176) / 2.0 + x, (screen.height - 166) / 2.0 + y, 0),
                "Mouse click missed GUI control");
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof LocomotiveScreen)
                || ticks < 10
                || lastCapture == phase
                || !(phase == 1 || phase == 2 || phase == 3 || phase == 5)) return;
        try {
            lastCapture = phase;
            try (var image =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(Path.of("locomotive-gui-" + phase + ".png"));
                captures++;
            }
        } catch (Throwable failure) {
            error = failure;
        }
    }

    static String report() {
        return "gui_captures="
                + captures
                + " gui_mouse_controls=true gui_fuel_levels=true gui_locks=true gui_modes=true";
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
