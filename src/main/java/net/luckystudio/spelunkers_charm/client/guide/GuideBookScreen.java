// Guide book pages and tabs.
package net.luckystudio.spelunkers_charm.client.guide;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.client.guide.GuidePages.Chapter;
import net.luckystudio.spelunkers_charm.client.guide.GuidePages.Illustration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class GuideBookScreen extends Screen {
    private static final int BOOK_W = 320;
    private static final int BOOK_H = 198;
    private static final int TAB_W = 24;
    private static final int TAB_H = 24;
    private static final int TAB_TOP = 25;
    private static final int TAB_STEP = 28;
    private static final int TAB_SHIFT = 8;
    private static final int ARROW_W = 16;
    private static final int ARROW_H = 14;
    private static final int ARROW_Y = 173;
    private static final int PREVIOUS_X = 9;
    private static final int NEXT_X = BOOK_W - PREVIOUS_X - ARROW_W;
    private static final int ART_CENTER = 82;
    private static final int ART_TOP = 44;
    private static final int ART_WIDTH = 124;
    private static final int ART_HEIGHT = 112;
    private static final int ART_GAP = 6;
    private static final int RECIPE_X = 281;
    private static final int RECIPE_Y = 19;
    private static final int INK = 0x432F22;
    private static final int FADED_INK = 0x73553D;
    private static final ResourceLocation BOOK = SpelunkersCharm.id("textures/gui/guide/book.png");
    private static final ResourceLocation WIDGETS =
            SpelunkersCharm.id("textures/gui/guide/widgets.png");
    private static final Chapter[] CHAPTERS = Chapter.values();

    private static int lastPage;
    private int pageIndex = lastPage;
    private ItemStack recipe = page().recipeIcon();
    private float scale;
    private float left;
    private float top;

    private GuideBookScreen() {
        super(Component.translatable("item.spelunkers_charm.spelunkers_guide"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GuideBookScreen());
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PUT, 1.0F));
    }

    @Override
    protected void init() {
        scale =
                Math.min(
                        1.0F,
                        Math.min((width - 8.0F) / (BOOK_W + TAB_W), (height - 8.0F) / BOOK_H));
        left = Math.round((width - (BOOK_W - TAB_W) * scale) / 2);
        top = Math.round((height - BOOK_H * scale) / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0x900F0804, 0xB0100A07);
        var page = page();
        var poses = graphics.pose();
        poses.pushPose();
        poses.translate(left, top, 0);
        poses.scale(scale, scale, 1);

        graphics.blit(BOOK, 0, 0, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);
        for (Chapter chapter : CHAPTERS) {
            int row = chapter.ordinal();
            int x = tabX(chapter);
            int spriteX =
                    chapter == page.chapter()
                                    || over(mouseX, mouseY, x, tabY(chapter), TAB_W, TAB_H)
                            ? TAB_W
                            : 0;
            widget(graphics, x, tabY(chapter), spriteX, row * TAB_H, TAB_W, TAB_H);
        }

        centered(
                graphics,
                page.chapter().title().copy().withStyle(ChatFormatting.BOLD),
                ART_CENTER,
                25,
                FADED_INK);
        drawIllustrations(graphics, page.illustrations());
        int captionY = 161;
        for (var line : font.split(page.text("caption"), 124)) {
            graphics.drawString(font, line, 82 - font.width(line) / 2, captionY, FADED_INK, false);
            captionY += 9;
        }

        drawLines(
                graphics,
                page.text("title").copy().withStyle(ChatFormatting.BOLD),
                174,
                23,
                101,
                INK,
                10);
        drawLines(graphics, page.text("body"), 174, 53, 125, INK, 10);
        if (!recipe.isEmpty()) {
            boolean hovered = over(mouseX, mouseY, RECIPE_X, RECIPE_Y, 24, 24);
            widget(graphics, RECIPE_X, RECIPE_Y, 80, hovered ? 24 : 0, 24, 24);
            graphics.renderItem(recipe, RECIPE_X + 4, RECIPE_Y + 4);
        }

        if (pageIndex > 0) widget(graphics, PREVIOUS_X, ARROW_Y, 48, 0, ARROW_W, ARROW_H);
        if (pageIndex < GuidePages.ALL.size() - 1) {
            widget(graphics, NEXT_X, ARROW_Y, 64, 0, ARROW_W, ARROW_H);
        }
        centered(
                graphics,
                Component.translatable(
                        GuidePages.TEXT + "page", pageIndex + 1, GuidePages.ALL.size()),
                240,
                178,
                FADED_INK);
        poses.popPose();

        renderGuideTooltip(graphics, mouseX, mouseY);
    }

    private static void drawIllustrations(GuiGraphics graphics, List<Illustration> illustrations) {
        if (illustrations.size() == 1) {
            drawIllustration(graphics, illustrations.getFirst(), ART_TOP, ART_HEIGHT);
            return;
        }

        int frameHeight = (ART_HEIGHT - ART_GAP) / 2;
        int arrowY = ART_TOP + frameHeight;
        drawIllustration(graphics, illustrations.getFirst(), ART_TOP, frameHeight);
        drawIllustration(graphics, illustrations.getLast(), arrowY + ART_GAP, frameHeight);

        int arrowColor = 0xFF73553D;
        graphics.fill(ART_CENTER - 1, arrowY + 1, ART_CENTER + 2, arrowY + 3, arrowColor);
        graphics.fill(ART_CENTER - 2, arrowY + 3, ART_CENTER + 3, arrowY + 4, arrowColor);
        graphics.fill(ART_CENTER - 1, arrowY + 4, ART_CENTER + 2, arrowY + 5, arrowColor);
        graphics.fill(ART_CENTER, arrowY + 5, ART_CENTER + 1, arrowY + 6, arrowColor);
    }

    private static void drawIllustration(
            GuiGraphics graphics, Illustration illustration, int top, int maxHeight) {
        int sourceWidth = illustration.width();
        int sourceHeight = illustration.height();
        float fit = Math.min((float) ART_WIDTH / sourceWidth, (float) maxHeight / sourceHeight);
        int width = Math.round(sourceWidth * fit);
        int height = Math.round(sourceHeight * fit);
        graphics.blit(
                illustration.texture(),
                ART_CENTER - width / 2,
                top + (maxHeight - height) / 2,
                width,
                height,
                0,
                0,
                sourceWidth,
                sourceHeight,
                sourceWidth,
                sourceHeight);
    }

    private void drawLines(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int lineWidth,
            int color,
            int spacing) {
        for (var line : font.split(text, lineWidth)) {
            graphics.drawString(font, line, x, y, color, false);
            y += spacing;
        }
    }

    private void centered(GuiGraphics graphics, Component text, int center, int y, int color) {
        graphics.drawString(font, text, center - font.width(text) / 2, y, color, false);
    }

    private static void widget(
            GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(WIDGETS, x, y, u, v, width, height, 128, 128);
    }

    private void renderGuideTooltip(GuiGraphics graphics, int x, int y) {
        for (Chapter chapter : CHAPTERS) {
            if (over(x, y, tabX(chapter), tabY(chapter), TAB_W, TAB_H)) {
                graphics.renderTooltip(font, chapter.title(), x, y);
                return;
            }
        }
        if (!recipe.isEmpty() && over(x, y, RECIPE_X, RECIPE_Y, 24, 24)) {
            var hint =
                    Component.translatable(
                                    GuidePages.TEXT
                                            + (GuideRecipeLink.available()
                                                    ? "jei.open"
                                                    : "jei.missing"))
                            .withStyle(ChatFormatting.GRAY);
            graphics.renderComponentTooltip(font, List.of(recipe.getHoverName(), hint), x, y);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(x, y, button);
        for (Chapter chapter : CHAPTERS) {
            if (over(x, y, tabX(chapter), tabY(chapter), TAB_W, TAB_H)) {
                turnTo(GuidePages.firstPage(chapter));
                return true;
            }
        }
        if (!recipe.isEmpty() && over(x, y, RECIPE_X, RECIPE_Y, 24, 24)) {
            GuideRecipeLink.open(recipe.copy());
            return true;
        }
        if (over(x, y, PREVIOUS_X, ARROW_Y, ARROW_W, ARROW_H)) return turnTo(pageIndex - 1);
        if (over(x, y, NEXT_X, ARROW_Y, ARROW_W, ARROW_H)) return turnTo(pageIndex + 1);
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical != 0) return turnTo(pageIndex + (vertical < 0 ? 1 : -1));
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> turnTo(pageIndex - 1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> turnTo(pageIndex + 1);
            case GLFW.GLFW_KEY_HOME -> turnTo(0);
            case GLFW.GLFW_KEY_END -> turnTo(GuidePages.ALL.size() - 1);
            case GLFW.GLFW_KEY_TAB -> {
                int shift = hasShiftDown() ? -1 : 1;
                var chapter =
                        CHAPTERS[
                                Math.floorMod(page().chapter().ordinal() + shift, CHAPTERS.length)];
                yield turnTo(GuidePages.firstPage(chapter));
            }
            default -> super.keyPressed(key, scan, modifiers);
        };
    }

    private boolean turnTo(int target) {
        int next = Mth.clamp(target, 0, GuidePages.ALL.size() - 1);
        if (next == pageIndex) return false;
        pageIndex = lastPage = next;
        recipe = page().recipeIcon();
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        return true;
    }

    private GuidePages.Page page() {
        return GuidePages.ALL.get(pageIndex);
    }

    private int tabX(Chapter chapter) {
        return TAB_SHIFT + (chapter == page().chapter() ? -23 : -20);
    }

    private static int tabY(Chapter chapter) {
        return TAB_TOP + chapter.ordinal() * TAB_STEP;
    }

    private boolean over(double x, double y, int bx, int by, int w, int h) {
        double localX = (x - left) / scale;
        double localY = (y - top) / scale;
        return localX >= bx && localX < bx + w && localY >= by && localY < by + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
