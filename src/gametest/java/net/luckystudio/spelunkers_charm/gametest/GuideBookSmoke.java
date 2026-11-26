// Guide book smoke.
package net.luckystudio.spelunkers_charm.gametest;

import com.google.gson.JsonParser;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.client.guide.GuideBookScreen;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import org.lwjgl.glfw.GLFW;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class GuideBookSmoke {
    private static final Path REPORT = Path.of("client-checks.txt");
    private static final String KEY = "guide.spelunkers_charm.";
    private static final int PAGE_COUNT = 24;
    private static final Set<String> COMPARISONS =
            Set.of(
                    "lift_build",
                    "lift_controls",
                    "cart_coupling",
                    "block_loader",
                    "block_crate",
                    "block_tank",
                    "block_blaster");
    private static Stage stage = Stage.START;
    private static GuideBookScreen book;
    private static int attempts;
    private static int capturedPage;
    private static int openingSounds;
    private static String recipeResult;
    private static CompletableFuture<Void> pickupCheck;

    private GuideBookSmoke() {}

    static boolean enabled() {
        return Boolean.getBoolean("spelunkers_guide.clientSmoke");
    }

    static void tick(Minecraft mc) {
        if (stage == Stage.DONE) return;
        try {
            switch (stage) {
                case START -> {
                    validateTextAndArt(mc);
                    var server = mc.getSingleplayerServer();
                    pickupCheck = server.submit(() -> checkGuidePickup(server));
                    stage = Stage.HOLDING;
                }
                case HOLDING -> {
                    if (!pickupCheck.isDone()) return;
                    pickupCheck.join();
                    if (!mc.player.getMainHandItem().is(ModItems.SPELUNKERS_GUIDE.asItem())) return;
                    check(
                            mc.player.getMainHandItem().getRarity() == Rarity.RARE,
                            "Guide is not rare");
                    check(
                            mc.gameMode
                                    .useItem(mc.player, InteractionHand.MAIN_HAND)
                                    .consumesAction(),
                            "Using the guide item did not open the book");
                    check(
                            mc.screen instanceof GuideBookScreen,
                            "Guide item opened the wrong screen");
                    check(openingSounds == 1, "Using the guide should play its opening sound once");
                    book = (GuideBookScreen) mc.screen;
                    stage = Stage.INTRO;
                }
                case INTRO -> {
                    if (!Files.isRegularFile(Path.of("guide-intro.png"))) return;
                    check(click(book, 8, 65), "Shifted lifts tab was not clickable");
                    check(click(book, 304, 180), "New next arrow was not clickable");
                    check(click(book, 17, 180), "New previous arrow was not clickable");
                    check(
                            book.mouseScrolled(0, 0, 0, -1),
                            "Mouse wheel did not turn the guide page");
                    stage = Stage.SMALL;
                }
                case SMALL -> {
                    if (!Files.isRegularFile(Path.of("guide-small.png"))) return;
                    check(
                            book.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0),
                            "Right key did not turn the page");
                    check(
                            book.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0),
                            "Right key did not reach the large lift");
                    stage = Stage.LARGE;
                }
                case LARGE -> {
                    if (!Files.isRegularFile(Path.of("guide-large.png"))) return;
                    stage = Stage.RECIPE;
                }
                case RECIPE -> testRecipeLink(mc);
                case ARTWORK -> captureArtwork(mc);
                case DONE -> {}
            }
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static void checkGuidePickup(MinecraftServer server) {
        var level = server.overworld();
        var player = level.players().getFirst();
        var advancement = server.getAdvancements().get(SpelunkersCharm.id("obtain_guide"));
        check(advancement != null, "Guide advancement is missing");
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        check(!progress.isDone(), "Guide advancement completed before the pickup");

        var guide =
                new ItemEntity(
                        level,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModItems.SPELUNKERS_GUIDE.toStack());
        guide.setNoPickUpDelay();
        level.addFreshEntity(guide);
        guide.playerTouch(player);
        player.containerMenu.broadcastChanges();

        check(guide.isRemoved(), "The player did not pick up the guide");
        check(progress.isDone(), "Picking up the guide did not complete its advancement");
    }

    private static void testRecipeLink(Minecraft mc) throws Exception {
        boolean hasJei = ModList.get().isLoaded("jei");
        if (mc.screen == book && attempts++ % 10 == 0) {
            check(click(book, 293, 31), "Recipe icon was not clickable");
            check(
                    mc.player.getMainHandItem().is(ModItems.SPELUNKERS_GUIDE.asItem()),
                    "Recipe icon changed the player's held item");
            if (!hasJei) {
                check(mc.screen == book, "Recipe icon should leave the guide open without JEI");
                startArtworkCheck("PASS guide navigation and optional JEI fallback");
                return;
            }
            check(attempts < 601, "JEI recipe GUI did not open");
        }
        if (Files.isRegularFile(Path.of("guide-jei.png"))) {
            check(
                    mc.screen != null && mc.screen.getClass().getName().startsWith("mezz.jei."),
                    "The recipe did not open in JEI");
            mc.screen.onClose();
            check(mc.screen == book, "Closing JEI did not return to the guide page");
            startArtworkCheck("PASS guide navigation and recipe opened in JEI");
        }
    }

    private static void startArtworkCheck(String result) {
        recipeResult = result;
        check(book.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0), "Home did not return to the cover page");
        capturedPage = 1;
        stage = Stage.ARTWORK;
    }

    private static void captureArtwork(Minecraft mc) throws Exception {
        if (!Files.isRegularFile(Path.of(artworkFile()))) return;
        if (capturedPage == PAGE_COUNT) {
            check(openingSounds == 1, "Opening sound replayed while navigating the guide");
            finish(
                    mc,
                    recipeResult
                            + "\nPASS rendered all "
                            + PAGE_COUNT
                            + " guide pages"
                            + "\nPASS shifted tabs and arrows, rare item, and one opening sound"
                            + "\nPASS guide advancement unlocked on item pickup");
            return;
        }
        check(book.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0), "Could not advance guide artwork check");
        capturedPage++;
    }

    private static String artworkFile() {
        return "guide-page-%02d.png".formatted(capturedPage);
    }

    private static boolean click(Screen screen, int localX, int localY) {
        float scale =
                Math.min(
                        1.0F,
                        Math.min((screen.width - 8.0F) / 344.0F, (screen.height - 8.0F) / 198.0F));
        int left = Math.round((screen.width - 296 * scale) / 2);
        int top = Math.round((screen.height - 198 * scale) / 2);
        return screen.mouseClicked(
                left + localX * scale, top + localY * scale, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        if (enabled()
                && event.getSound() != null
                && event.getSound().getLocation().equals(SoundEvents.BOOK_PUT.getLocation())) {
            openingSounds++;
        }
    }

    @SubscribeEvent
    public static void screenshot(ScreenEvent.Render.Post event) {
        if (!enabled()) return;
        String file =
                switch (stage) {
                    case INTRO -> "guide-intro.png";
                    case SMALL -> "guide-small.png";
                    case LARGE -> "guide-large.png";
                    case RECIPE ->
                            event.getScreen().getClass().getName().startsWith("mezz.jei.")
                                    ? "guide-jei.png"
                                    : null;
                    case ARTWORK -> artworkFile();
                    default -> null;
                };
        if (file == null || Files.isRegularFile(Path.of(file))) return;
        var mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(Path.of(file));
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static void validateTextAndArt(Minecraft mc) throws Exception {
        for (String locale : new String[] {"en_us", "pl_pl"}) {
            var resource =
                    mc.getResourceManager()
                            .getResourceOrThrow(SpelunkersCharm.id("lang/" + locale + ".json"));
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                var translations = JsonParser.parseReader(reader).getAsJsonObject();
                int pages = 0;
                for (String key : translations.keySet()) {
                    if (!key.startsWith(KEY) || !key.endsWith(".body")) continue;
                    String page = key.substring(KEY.length(), key.length() - 5);
                    var body = Component.literal(translations.get(key).getAsString());
                    check(
                            mc.font.split(body, 125).size() <= 12,
                            "Guide body overflows " + locale + " " + page);
                    var caption =
                            Component.literal(
                                    translations.get(KEY + page + ".caption").getAsString());
                    check(
                            mc.font.split(caption, 124).size() <= 2,
                            "Guide caption overflows " + locale + " " + page);
                    if (COMPARISONS.contains(page)) {
                        requireIllustration(mc, page + "0");
                        requireIllustration(mc, page + "1");
                    } else {
                        requireIllustration(mc, page);
                    }
                    pages++;
                }
                check(pages == PAGE_COUNT, "Missing localized guide pages for " + locale);
            }
        }
    }

    private static void requireIllustration(Minecraft mc, String image) {
        var texture = SpelunkersCharm.id("textures/gui/guide/plates/" + image + ".png");
        check(
                mc.getResourceManager().getResource(texture).isPresent(),
                "Missing guide art " + image);
    }

    private static void check(boolean passed, String problem) {
        if (!passed) throw new AssertionError(problem);
    }

    private static void finish(Minecraft mc, String message) throws Exception {
        stage = Stage.DONE;
        Files.writeString(REPORT, message + "\n");
        mc.stop();
    }

    private static void fail(Minecraft mc, Throwable error) {
        stage = Stage.DONE;
        error.printStackTrace();
        try {
            Files.writeString(REPORT, "FAIL " + error + "\n");
        } catch (Exception reportError) {
            error.addSuppressed(reportError);
        }
        mc.stop();
    }

    /** Provides stage data for this gameplay test. */
    private enum Stage {
        START,
        HOLDING,
        INTRO,
        SMALL,
        LARGE,
        RECIPE,
        ARTWORK,
        DONE
    }
}
