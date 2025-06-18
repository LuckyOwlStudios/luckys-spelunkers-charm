package net.luckystudio.spelunkers_charm.datagen;

import com.google.common.collect.ImmutableList;
import net.luckystudio.spelunkers_charm.datagen.itemTags.ModItemTags;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public static final ImmutableList<ItemLike> COAL_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_COAL_ORE,
            ModBlocks.WILDSTONE_COAL_ORE,
            ModBlocks.PERMAFROST_COAL_ORE);
    public static final ImmutableList<ItemLike> IRON_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_IRON_ORE,
            ModBlocks.WILDSTONE_IRON_ORE,
            ModBlocks.PERMAFROST_IRON_ORE
    );
    public static final ImmutableList<ItemLike> COPPER_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_COPPER_ORE,
            ModBlocks.WILDSTONE_COPPER_ORE,
            ModBlocks.PERMAFROST_COPPER_ORE
    );
    public static final ImmutableList<ItemLike> GOLD_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_GOLD_ORE,
            ModBlocks.WILDSTONE_GOLD_ORE,
            ModBlocks.PERMAFROST_GOLD_ORE
    );
    public static final ImmutableList<ItemLike> REDSTONE_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_REDSTONE_ORE,
            ModBlocks.WILDSTONE_REDSTONE_ORE,
            ModBlocks.PERMAFROST_REDSTONE_ORE
    );
    public static final ImmutableList<ItemLike> EMERALD_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_EMERALD_ORE,
            ModBlocks.WILDSTONE_EMERALD_ORE,
            ModBlocks.PERMAFROST_EMERALD_ORE
    );
    public static final ImmutableList<ItemLike> LAPIS_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_LAPIS_ORE,
            ModBlocks.WILDSTONE_LAPIS_ORE,
            ModBlocks.PERMAFROST_LAPIS_ORE
    );
    public static final ImmutableList<ItemLike> DIAMOND_SMELTABLES = ImmutableList.of(
            ModBlocks.DUNESTONE_DIAMOND_ORE,
            ModBlocks.WILDSTONE_DIAMOND_ORE,
            ModBlocks.PERMAFROST_DIAMOND_ORE
    );

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

    private void stoneBrickRecipe(Item input, Item output, RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, output, 4)
                .pattern("##")
                .pattern("##")
                .define('#', input)
                .unlockedBy("has_stone", has(input))
                .save(recipeOutput);
    }

    private void blaster(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BLASTER.asItem(), 1)
                .pattern("%#%")
                .pattern("#@#")
                .pattern("###")
                .define('%', Items.REDSTONE)
                .define('#', Items.IRON_INGOT)
                .define('@', ModItemTags.GEYSERS)
                .unlockedBy("has_geyser", has(ModItemTags.GEYSERS))
                .save(recipeOutput);
    }

    private void candleHelmet(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.CANDLE_HELMET, 1)
                .pattern(" % ")
                .pattern("@#@")
                .pattern("# #")
                .define('%', Items.CANDLE)
                .define('#', Items.IRON_INGOT)
                .define('@', ItemTags.PLANKS)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(recipeOutput);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        stairBuilder(ModBlocks.DUNESTONE_STAIRS.get(), Ingredient.of(ModBlocks.DUNESTONE.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DUNESTONE_SLAB.get(), Ingredient.of(ModBlocks.DUNESTONE.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DUNESTONE_WALL.get(), Ingredient.of(ModBlocks.DUNESTONE.asItem()));
        stoneBrickRecipe(ModBlocks.DUNESTONE.asItem(), ModBlocks.DUNESTONE_BRICKS.asItem(), recipeOutput);
        stairBuilder(ModBlocks.DUNESTONE_BRICK_STAIRS.get(), Ingredient.of(ModBlocks.DUNESTONE_BRICKS.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DUNESTONE_BRICK_SLAB.get(), Ingredient.of(ModBlocks.DUNESTONE_BRICKS.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DUNESTONE_BRICK_WALL.get(), Ingredient.of(ModBlocks.DUNESTONE_BRICKS.asItem()));
        chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_DUNESTONE_BRICKS.get(), Ingredient.of(ModBlocks.DUNESTONE_BRICKS.asItem()));
        stairBuilder(ModBlocks.COBBLED_DUNESTONE_STAIRS.get(), Ingredient.of(ModBlocks.COBBLED_DUNESTONE.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_DUNESTONE_SLAB.get(), Ingredient.of(ModBlocks.COBBLED_DUNESTONE.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_DUNESTONE_WALL.get(), Ingredient.of(ModBlocks.COBBLED_DUNESTONE.asItem()));

        stairBuilder(ModBlocks.WILDSTONE_STAIRS.get(), Ingredient.of(ModBlocks.WILDSTONE.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WILDSTONE_SLAB.get(), Ingredient.of(ModBlocks.WILDSTONE.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WILDSTONE_WALL.get(), Ingredient.of(ModBlocks.WILDSTONE.asItem()));
        stoneBrickRecipe(ModBlocks.WILDSTONE.asItem(), ModBlocks.WILDSTONE_BRICKS.asItem(), recipeOutput);
        stairBuilder(ModBlocks.WILDSTONE_BRICK_STAIRS.get(), Ingredient.of(ModBlocks.WILDSTONE_BRICKS.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WILDSTONE_BRICK_SLAB.get(), Ingredient.of(ModBlocks.WILDSTONE_BRICKS.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WILDSTONE_BRICK_WALL.get(), Ingredient.of(ModBlocks.WILDSTONE_BRICKS.asItem()));
        chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_WILDSTONE_BRICKS.get(), Ingredient.of(ModBlocks.WILDSTONE_BRICKS.asItem()));
        stairBuilder(ModBlocks.COBBLED_WILDSTONE_STAIRS.get(), Ingredient.of(ModBlocks.COBBLED_WILDSTONE.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_WILDSTONE_SLAB.get(), Ingredient.of(ModBlocks.COBBLED_WILDSTONE.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_WILDSTONE_WALL.get(), Ingredient.of(ModBlocks.COBBLED_WILDSTONE.asItem()));

        stairBuilder(ModBlocks.PERMAFROST_STAIRS.get(), Ingredient.of(ModBlocks.PERMAFROST.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PERMAFROST_SLAB.get(), Ingredient.of(ModBlocks.PERMAFROST.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PERMAFROST_WALL.get(), Ingredient.of(ModBlocks.PERMAFROST.asItem()));
        stoneBrickRecipe(ModBlocks.PERMAFROST.asItem(), ModBlocks.PERMAFROST_BRICKS.asItem(), recipeOutput);
        stairBuilder(ModBlocks.PERMAFROST_BRICK_STAIRS.get(), Ingredient.of(ModBlocks.PERMAFROST_BRICKS.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PERMAFROST_BRICK_SLAB.get(), Ingredient.of(ModBlocks.PERMAFROST_BRICKS.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PERMAFROST_BRICK_WALL.get(), Ingredient.of(ModBlocks.PERMAFROST_BRICKS.asItem()));
        chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_PERMAFROST_BRICKS.get(), Ingredient.of(ModBlocks.PERMAFROST_BRICKS.asItem()));
        stairBuilder(ModBlocks.COBBLED_PERMAFROST_STAIRS.get(), Ingredient.of(ModBlocks.COBBLED_PERMAFROST.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_PERMAFROST_SLAB.get(), Ingredient.of(ModBlocks.COBBLED_PERMAFROST.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_PERMAFROST_WALL.get(), Ingredient.of(ModBlocks.COBBLED_PERMAFROST.asItem()));

        stoneBrickRecipe(Blocks.AMETHYST_BLOCK.asItem(), ModBlocks.AMETHYST_BRICKS.asItem(), recipeOutput);
        stairBuilder(ModBlocks.AMETHYST_BRICK_STAIRS.get(), Ingredient.of(ModBlocks.AMETHYST_BRICKS.asItem()));
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.AMETHYST_BRICK_SLAB.get(), Ingredient.of(ModBlocks.AMETHYST_BRICKS.asItem()));
        wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.AMETHYST_BRICK_WALL.get(), Ingredient.of(ModBlocks.AMETHYST_BRICKS.asItem()));
        chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_AMETHYST_BLOCK.get(), Ingredient.of(ModBlocks.AMETHYST_BRICKS.asItem()));

        oreSmelting(recipeOutput, COAL_SMELTABLES, RecipeCategory.MISC, Items.COAL, 0.1F, 200, "coal");
        oreSmelting(recipeOutput, IRON_SMELTABLES, RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 200, "iron_ingot");
        oreSmelting(recipeOutput, COPPER_SMELTABLES, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 200, "copper_ingot");
        oreSmelting(recipeOutput, GOLD_SMELTABLES, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0F, 200, "gold_ingot");
        oreSmelting(recipeOutput, DIAMOND_SMELTABLES, RecipeCategory.MISC, Items.DIAMOND, 1.0F, 200, "diamond");
        oreSmelting(recipeOutput, LAPIS_SMELTABLES, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 200, "lapis_lazuli");
        oreSmelting(recipeOutput, REDSTONE_SMELTABLES, RecipeCategory.REDSTONE, Items.REDSTONE, 0.7F, 200, "redstone");
        oreSmelting(recipeOutput, EMERALD_SMELTABLES, RecipeCategory.MISC, Items.EMERALD, 1.0F, 200, "emerald");

        oreBlasting(recipeOutput, COAL_SMELTABLES, RecipeCategory.MISC, Items.COAL, 0.1F, 100, "coal");
        oreBlasting(recipeOutput, IRON_SMELTABLES, RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 100, "iron_ingot");
        oreBlasting(recipeOutput, COPPER_SMELTABLES, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 100, "copper_ingot");
        oreBlasting(recipeOutput, GOLD_SMELTABLES, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0F, 100, "gold_ingot");
        oreBlasting(recipeOutput, DIAMOND_SMELTABLES, RecipeCategory.MISC, Items.DIAMOND, 1.0F, 100, "diamond");
        oreBlasting(recipeOutput, LAPIS_SMELTABLES, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 100, "lapis_lazuli");
        oreBlasting(recipeOutput, REDSTONE_SMELTABLES, RecipeCategory.REDSTONE, Items.REDSTONE, 0.7F, 100, "redstone");
        oreBlasting(recipeOutput, EMERALD_SMELTABLES, RecipeCategory.MISC, Items.EMERALD, 1.0F, 100, "emerald");

        nineBlockStorageRecipes(recipeOutput, RecipeCategory.MISC, Items.STRING, RecipeCategory.BUILDING_BLOCKS, ModBlocks.PACKED_WEB);

        rockRecipe(ModBlocks.ROCK.asItem(), Blocks.COBBLESTONE.asItem(), recipeOutput);
        rockRecipe(ModBlocks.DEEPSLATE_ROCK.asItem(), Blocks.COBBLED_DEEPSLATE.asItem(), recipeOutput);
        rockRecipe(ModBlocks.DRIPSTONE_ROCK.asItem(), Blocks.DRIPSTONE_BLOCK.asItem(), recipeOutput);
        rockRecipe(ModBlocks.BASALT_ROCK.asItem(), Blocks.BASALT.asItem(), recipeOutput);

        candleHelmet(recipeOutput);

        blaster(recipeOutput);
    }
}
