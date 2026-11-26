// Guide pages.
package net.luckystudio.spelunkers_charm.client.guide;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.Nullable;

import java.util.List;

final class GuidePages {
    static final String TEXT = "guide.spelunkers_charm.";
    static final List<Page> ALL =
            List.of(
                    new Page(Chapter.START, "intro", ModItems.SPELUNKERS_GUIDE),
                    comparison(Chapter.LIFTS, "lift_build", ModItems.SMALL_LIFT),
                    wideScreenshot(Chapter.LIFTS, "lift_small", ModItems.SMALL_LIFT),
                    wideScreenshot(Chapter.LIFTS, "lift_medium", ModItems.MEDIUM_LIFT),
                    wideScreenshot(Chapter.LIFTS, "lift_large", ModItems.LARGE_LIFT),
                    comparison(Chapter.LIFTS, "lift_controls", ModItems.SMALL_LIFT),
                    screenshot(Chapter.LIFTS, "track_basic", ModBlocks.BASIC_LIFT_TRACK),
                    screenshot(Chapter.LIFTS, "track_motor", ModBlocks.POWERED_LIFT_TRACK),
                    screenshot(Chapter.LIFTS, "track_direction", ModBlocks.DIRECTIONAL_LIFT_TRACK),
                    screenshot(Chapter.LIFTS, "track_detector", ModBlocks.DETECTOR_LIFT_TRACK),
                    comparison(Chapter.CARTS, "cart_coupling", ModItems.CART_COUPLING_HOOK),
                    screenshot(Chapter.CARTS, "cart_block", Items.MINECART),
                    screenshot(Chapter.CARTS, "cart_crossing", Items.RAIL),
                    screenshot(Chapter.CARTS, "cart_wooden", ModBlocks.WOODEN_RAIL),
                    screenshot(Chapter.CARTS, "cart_ejector", ModBlocks.EJECTOR_RAIL),
                    screenshot(Chapter.CARTS, "cart_engine", Items.BLAST_FURNACE),
                    screenshot(Chapter.CARTS, "cart_jukebox", Items.JUKEBOX),
                    comparison(Chapter.BLOCKS, "block_loader", ModBlocks.CART_LOADER),
                    comparison(Chapter.BLOCKS, "block_crate", ModBlocks.MINE_CRATE),
                    comparison(Chapter.BLOCKS, "block_tank", ModBlocks.TANK),
                    comparison(Chapter.BLOCKS, "block_blaster", ModBlocks.BLASTER),
                    new Page(Chapter.TREMORS, "tremor_natural", null),
                    new Page(Chapter.TREMORS, "tremor_explosion", Items.TNT),
                    new Page(Chapter.TREMORS, "tremor_falling", null));

    private GuidePages() {}

    private static Page screenshot(Chapter chapter, String key, ItemLike recipe) {
        return new Page(chapter, key, recipe, List.of(new Illustration(key, 512, 384)));
    }

    private static Page wideScreenshot(Chapter chapter, String key, ItemLike recipe) {
        return new Page(chapter, key, recipe, List.of(new Illustration(key, 512, 288)));
    }

    private static Page comparison(Chapter chapter, String key, ItemLike recipe) {
        return new Page(
                chapter,
                key,
                recipe,
                List.of(
                        new Illustration(key + "0", 512, 384),
                        new Illustration(key + "1", 512, 384)));
    }

    static int firstPage(Chapter chapter) {
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).chapter() == chapter) return i;
        }
        throw new IllegalArgumentException("Empty guide chapter " + chapter);
    }

    /** Lists the guide chapters. */
    enum Chapter {
        START("start"),
        LIFTS("lifts"),
        CARTS("carts"),
        BLOCKS("blocks"),
        TREMORS("tremors");

        private final String key;

        Chapter(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(TEXT + "chapter." + key);
        }
    }

    /** Describes one guide image and its dimensions. */
    record Illustration(ResourceLocation texture, int width, int height) {
        Illustration(String key, int width, int height) {
            this(SpelunkersCharm.id("textures/gui/guide/plates/" + key + ".png"), width, height);
        }
    }

    /** Describes one guide page and its linked recipe. */
    record Page(
            Chapter chapter,
            String key,
            @Nullable ItemLike recipe,
            List<Illustration> illustrations) {
        Page(Chapter chapter, String key, @Nullable ItemLike recipe) {
            this(chapter, key, recipe, List.of(new Illustration(key, 124, 112)));
        }

        Component text(String part) {
            return Component.translatable(TEXT + key + "." + part);
        }

        ItemStack recipeIcon() {
            return recipe == null ? ItemStack.EMPTY : new ItemStack(recipe);
        }
    }
}
