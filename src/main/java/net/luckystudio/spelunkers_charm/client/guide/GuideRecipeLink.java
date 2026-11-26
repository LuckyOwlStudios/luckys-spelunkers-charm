// Guide recipe link.
package net.luckystudio.spelunkers_charm.client.guide;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class GuideRecipeLink {
    @Nullable private static Consumer<ItemStack> opener;

    private GuideRecipeLink() {}

    public static void setOpener(@Nullable Consumer<ItemStack> handler) {
        opener = handler;
    }

    static boolean available() {
        return opener != null;
    }

    static void open(ItemStack stack) {
        if (opener != null) opener.accept(stack);
    }
}
