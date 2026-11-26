// Guide jei plugin.
package net.luckystudio.spelunkers_charm.client.guide;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class GuideJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return SpelunkersCharm.id("spelunkers_guide");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        GuideRecipeLink.setOpener(
                stack -> {
                    var focus =
                            runtime.getJeiHelpers()
                                    .getFocusFactory()
                                    .createFocus(
                                            RecipeIngredientRole.OUTPUT,
                                            VanillaTypes.ITEM_STACK,
                                            stack);
                    runtime.getRecipesGui().show(focus);
                });
    }

    @Override
    public void onRuntimeUnavailable() {
        GuideRecipeLink.setOpener(null);
    }
}
