// Guide recipe tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class GuideRecipeTests {
    @GameTest(template = "empty")
    public static void craftsTheGuideWithoutJei(GameTestHelper h) {
        var level = h.getLevel();
        var entry =
                level.getRecipeManager()
                        .byKey(SpelunkersCharm.id("spelunkers_guide"))
                        .orElseThrow();
        var recipe = (CraftingRecipe) entry.value();
        var input =
                CraftingInput.of(
                        2,
                        2,
                        List.of(
                                new ItemStack(Items.BOOK),
                                new ItemStack(Items.COPPER_INGOT),
                                new ItemStack(Items.ORANGE_DYE),
                                ItemStack.EMPTY));
        h.assertTrue(
                recipe.matches(input, level), "Guide recipe did not match book, dye and copper");
        h.assertTrue(
                recipe.assemble(input, level.registryAccess())
                        .is(ModItems.SPELUNKERS_GUIDE.asItem()),
                "Guide recipe did not craft the guide item");
        h.succeed();
    }
}
