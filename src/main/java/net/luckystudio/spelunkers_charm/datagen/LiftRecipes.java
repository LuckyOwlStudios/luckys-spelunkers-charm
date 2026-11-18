package net.luckystudio.spelunkers_charm.datagen;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/** All elevator crafting in one place, including the recipe-book unlocks. */
public final class LiftRecipes {
    private LiftRecipes() {}

    public static void generate(RecipeOutput output) {
        generateTracks(output);
        generatePlatforms(output);
    }

    private static void generateTracks(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.BASIC_LIFT_TRACK, 8)
                .pattern("ISI")
                .pattern("SLS")
                .pattern("ISI")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .define('L', ItemTags.LOGS)
                .unlockedBy("has_log", has(ItemTags.LOGS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.POWERED_LIFT_TRACK, 8)
                .pattern("IRI")
                .pattern("DDD")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('D', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DIRECTIONAL_LIFT_TRACK)
                .pattern(" R ")
                .pattern("ANT")
                .pattern(" R ")
                .define('R', Items.REDSTONE)
                .define('A', Items.ARROW)
                .define('N', ModBlocks.POWERED_LIFT_TRACK)
                .define('T', Items.REPEATER)
                .unlockedBy("has_motor_track", has(ModBlocks.POWERED_LIFT_TRACK))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.DETECTOR_LIFT_TRACK, 8)
                .pattern("IRI")
                .pattern("DTD")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('D', ItemTags.PLANKS)
                .define('T', Items.DETECTOR_RAIL)
                .unlockedBy("has_detector_rail", has(Items.DETECTOR_RAIL))
                .save(output);
    }

    private static void generatePlatforms(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMALL_LIFT)
                .pattern("ICI")
                .pattern("DLD")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHAIN)
                .define('D', ItemTags.PLANKS)
                .define('L', Items.LEVER)
                .unlockedBy("has_chain", has(Items.CHAIN))
                .save(output);
        platformUpgrade(output, ModItems.SMALL_LIFT, ModItems.MEDIUM_LIFT);
        platformUpgrade(output, ModItems.MEDIUM_LIFT, ModItems.LARGE_LIFT);
    }

    private static void platformUpgrade(RecipeOutput output, ItemLike previous, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("ICI")
                .pattern("DPD")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHAIN)
                .define('D', ItemTags.PLANKS)
                .define('P', previous)
                .unlockedBy("has_previous_lift", has(previous))
                .save(output);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return has(ItemPredicate.Builder.item().of(item));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return has(ItemPredicate.Builder.item().of(tag));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(
            ItemPredicate.Builder predicate) {
        return CriteriaTriggers.INVENTORY_CHANGED.createCriterion(
                new InventoryChangeTrigger.TriggerInstance(
                        Optional.empty(),
                        InventoryChangeTrigger.TriggerInstance.Slots.ANY,
                        List.of(predicate.build())));
    }
}
