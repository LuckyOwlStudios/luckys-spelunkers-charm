// Train polish client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.client.CartHookSelection;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class TrainPolishClientSmoke {
    private static int stage, ticks, normalSounds, heavySounds;
    private static AbstractMinecart first, second;
    private static ItemStack hook;
    private static ArmorStand camera;
    private static CompletableFuture<Void> pending;
    private static boolean markedCapture, clearCapture;
    private static Throwable renderError;

    private static void trace(String text) {
        try {
            Files.writeString(
                    Path.of("placement-sounds.txt"),
                    text + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void levelSound(net.neoforged.neoforge.event.PlayLevelSoundEvent event) {
        if (Boolean.getBoolean("spelunkers_railways.polishOnly") && event.getSound() != null)
            trace(
                    (event.getLevel().isClientSide() ? "client-level " : "server-level ")
                            + event.getSound().value().getLocation()
                            + " pitch="
                            + event.getOriginalPitch());
    }

    @SubscribeEvent
    public static void rawSound(PlaySoundEvent event) {
        if (Boolean.getBoolean("spelunkers_railways.polishOnly") && event.getSound() != null)
            trace("client-engine " + event.getSound().getLocation());
    }

    @SubscribeEvent
    public static void joins(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.polishOnly")
                || !(event.getEntity() instanceof AbstractMinecart c)) return;
        try {
            Files.writeString(
                    Path.of("placement-joins.txt"),
                    (event.getLevel().isClientSide() ? "client" : "server")
                            + " pos="
                            + c.position()
                            + " tick="
                            + c.tickCount
                            + " disk="
                            + event.loadedFromDisk()
                            + " rail="
                            + MinecartTrack.current(c)
                            + " source="
                            + ((MinecartMotionVisuals) c).spelunkersCharm$replacedCartId()
                            + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean tick(Minecraft mc) throws Exception {
        if (renderError != null) throw new AssertionError(renderError);
        if (pending != null && !pending.isDone()) return false;
        if (pending != null) {
            pending.join();
            pending = null;
        }
        ticks++;
        if (stage == 0) {
            mc.getSoundManager()
                    .addListener(
                            (sound, eventInfo, range) -> {
                                if (!sound.getLocation().getPath().equals("cart_place")) return;
                                if (sound.getPitch() < .9) heavySounds++;
                                else normalSounds++;
                            });
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
                                        player.setGameMode(GameType.CREATIVE);
                                        player.getInventory().clearContent();
                                        player.teleportTo(170, 84.2, 94);
                                        level.getChunk(10, 5);
                                        level.setChunkForced(10, 5, true);
                                        for (int x = 160; x < 184; x++)
                                            for (int z = 86; z <= 96; z++) {
                                                var base = new BlockPos(x, 83, z);
                                                level.setBlock(
                                                        base, Blocks.STONE.defaultBlockState(), 2);
                                                if (z == 88 || z == 90)
                                                    level.setBlock(
                                                            base.above(),
                                                            Blocks.RAIL
                                                                    .defaultBlockState()
                                                                    .setValue(
                                                                            RailBlock.SHAPE,
                                                                            RailShape.EAST_WEST),
                                                            2);
                                            }
                                    });
            next(1);
            return false;
        }
        if (stage == 1) {
            if (ticks < 25) return false;
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
                                        for (var p :
                                                List.of(
                                                        new BlockPos(168, 84, 88),
                                                        new BlockPos(171, 84, 88),
                                                        new BlockPos(174, 84, 90))) {
                                            var item =
                                                    p.getZ() == 90
                                                            ? ModItems.LOCOMOTIVE.get()
                                                            : Items.MINECART;
                                            player.setItemInHand(
                                                    InteractionHand.MAIN_HAND, new ItemStack(item));
                                            var hit =
                                                    new BlockHitResult(
                                                            Vec3.atCenterOf(p),
                                                            Direction.UP,
                                                            p,
                                                            false);
                                            check(
                                                    item.useOn(
                                                                    new UseOnContext(
                                                                            player,
                                                                            InteractionHand
                                                                                    .MAIN_HAND,
                                                                            hit))
                                                            .consumesAction(),
                                                    "Cart placement failed");
                                        }
                                        first =
                                                level.getEntitiesOfClass(
                                                                AbstractMinecart.class,
                                                                new AABB(168, 84, 88, 169, 85, 89))
                                                        .getFirst();
                                        second =
                                                level.getEntitiesOfClass(
                                                                AbstractMinecart.class,
                                                                new AABB(171, 84, 88, 172, 85, 89))
                                                        .getFirst();
                                        second.setPos(
                                                first.getX() + 3.3, first.getY(), first.getZ());
                                        for (var c : List.of(first, second))
                                            TrainRegressionFixture.facing(c, new Vec3(1, 0, 0));
                                        trace("server-player " + player.position());
                                        hook = ModItems.CART_COUPLING_HOOK.toStack();
                                        player.setItemInHand(InteractionHand.MAIN_HAND, hook);
                                        ((CartCouplingHook) hook.getItem())
                                                .useOnCart(hook, player, first);
                                    });
            next(2);
            return false;
        }
        if (camera == null) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            mc.setCameraEntity(camera);
            mc.options.hideGui = true;
        }
        camera.moveTo(170, 89, 94, 180, 50);
        camera.yHeadRot = camera.yHeadRotO = 180;
        if (stage == 2) {
            if (ticks < 15) return false;
            check(
                    CartHookSelection.selected(mc) != null
                            && CartHookSelection.selected(mc).getId() == first.getId(),
                    "Main-hand marker missing");
            if (ticks == 15)
                trace(
                        "client-player "
                                + mc.player.position()
                                + " camera="
                                + mc.gameRenderer.getMainCamera().getPosition());
            if (ticks < 25 || !markedCapture) return false;
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            mc.getSingleplayerServer()
                                                    .getPlayerList()
                                                    .getPlayer(id)
                                                    .setItemInHand(
                                                            InteractionHand.MAIN_HAND,
                                                            ItemStack.EMPTY));
            next(3);
            return false;
        }
        if (stage == 3) {
            if (ticks < 10) return false;
            check(CartHookSelection.selected(mc) == null, "Marker persisted without held hook");
            if (!clearCapture) return false;
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () ->
                                            mc.getSingleplayerServer()
                                                    .getPlayerList()
                                                    .getPlayer(id)
                                                    .setItemInHand(InteractionHand.OFF_HAND, hook));
            next(4);
            return false;
        }
        if (stage == 4) {
            if (ticks < 10) return false;
            check(CartHookSelection.selected(mc) != null, "Off-hand marker missing");
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var player =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(id);
                                        var tool = (CartCouplingHook) hook.getItem();
                                        tool.useOnCart(hook, player, first);
                                        player.setItemInHand(
                                                InteractionHand.OFF_HAND, ItemStack.EMPTY);
                                        player.setItemInHand(InteractionHand.MAIN_HAND, hook);
                                    });
            next(5);
            return false;
        }
        if (stage == 5) {
            if (ticks < 10) return false;
            check(CartHookSelection.selected(mc) == null, "Cleared selection retained marker");
            var id = mc.player.getUUID();
            pending =
                    mc.getSingleplayerServer()
                            .submit(
                                    () -> {
                                        var player =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(id);
                                        var tool = (CartCouplingHook) hook.getItem();
                                        tool.useOnCart(hook, player, first);
                                        tool.useOnCart(hook, player, second);
                                        check(
                                                MinecartCouplings.get(
                                                                mc.getSingleplayerServer()
                                                                        .overworld())
                                                        .linked(first.getUUID(), second.getUUID()),
                                                "Creative link without chains failed");
                                    });
            next(6);
            return false;
        }
        if (stage == 6) {
            if (ticks < 35) return false;
            check(CartHookSelection.selected(mc) == null, "Finished coupling retained marker");
            check(
                    MinecartCouplings.get(mc.getSingleplayerServer().overworld())
                            .linked(first.getUUID(), second.getUUID()),
                    "Distant link did not survive settling");
            check(
                    normalSounds >= 2 && heavySounds >= 1,
                    "Placement sound missing: " + normalSounds + ", " + heavySounds);
            var text = new ArrayList<net.minecraft.network.chat.Component>();
            var lift = ModItems.SMALL_LIFT.toStack();
            lift.getItem()
                    .appendHoverText(
                            lift, Item.TooltipContext.of(mc.level), text, TooltipFlag.NORMAL);
            check(
                    text.size() == 3
                            && text.stream()
                                    .noneMatch(
                                            c ->
                                                    c.getString().contains("BETA")
                                                            || c.getString().contains("1.21.6")),
                    "Lift tooltip was not simplified");
            mc.setCameraEntity(mc.player);
            return true;
        }
        check(ticks < 150, "Client polish stage timed out: " + stage);
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_railways.polishOnly")
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        String name =
                stage == 2 && ticks >= 16 && !markedCapture
                        ? "hook-selected.png"
                        : stage == 3 && ticks >= 10 && !clearCapture ? "hook-hidden.png" : null;
        if (name == null) return;
        try (var frame = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            frame.writeToFile(Path.of(name));
            if (stage == 2) markedCapture = true;
            else clearCapture = true;
        } catch (Throwable e) {
            renderError = e;
        }
    }

    static String report() {
        return "marker_main=true marker_offhand=true marker_hidden=true marker_cleared=true creative_free=true distant_link=true normal_place_sounds="
                + normalSounds
                + " heavy_place_sounds="
                + heavySounds
                + " lift_tooltip_lines=3";
    }

    private static void next(int value) {
        stage = value;
        ticks = 0;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
