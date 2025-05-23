package luckyowlstudios.mods.luckysspelunkerscharm.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import luckyowlstudios.mods.luckysspelunkerscharm.block.ModBlocks;
import luckyowlstudios.mods.luckysspelunkerscharm.item.ModItems;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.data.recipes.ShapedRecipeBuilder.shaped;
import static net.minecraft.data.recipes.ShapelessRecipeBuilder.shapeless;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider);
    }

    @Override
    public void buildRecipes(RecipeOutput exporter) {
        shaped(RecipeCategory.DECORATIONS, ModBlocks.DEEPSLATE_GEYSER)
                .pattern("DDD")
                .pattern("SSS")
                .pattern("DDD")
                .define('D', Items.DIAMOND)
                .define('S', Items.STICK)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(exporter);

        shapeless(RecipeCategory.DECORATIONS, ModItems.yourItem)
                .requires(Items.DIAMOND)
                .requires(Items.BONE_MEAL)
                .unlockedBy("has_bone_meal", has(Items.BONE_MEAL))
                .save(exporter);
    }

    @Override
    public String getName() {
        return LuckysSpelunkersCharm.MOD_ID;
    }
}
