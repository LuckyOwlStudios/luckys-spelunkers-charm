package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private void rockRecipe(Item input, Item output, RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, output)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', input)
                .unlockedBy("has_rock", has(input))
                .save(recipeOutput);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        rockRecipe(ModBlocks.ROCK.asItem(), Blocks.STONE.asItem(), recipeOutput);
        rockRecipe(ModBlocks.DEEPSLATE_ROCK.asItem(), Blocks.DEEPSLATE.asItem(), recipeOutput);
        rockRecipe(ModBlocks.DRIPSTONE_ROCK.asItem(), Blocks.DRIPSTONE_BLOCK.asItem(), recipeOutput);
        rockRecipe(ModBlocks.BASALT_ROCK.asItem(), Blocks.BASALT.asItem(), recipeOutput);
    }
}
