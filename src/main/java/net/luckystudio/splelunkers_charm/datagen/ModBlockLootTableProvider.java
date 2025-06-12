package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

// Look at this class for help VanillaBlockLoot
public class ModBlockLootTableProvider extends BlockLootSubProvider {

    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {

        // Building Blocks
        this.add(ModBlocks.DUNESTONE.get(), block -> this.createSingleItemTableWithSilkTouch(block, ModBlocks.COBBLED_DUNESTONE));
        dropSelf(ModBlocks.DUNESTONE_STAIRS.get());
        dropSelf(ModBlocks.DUNESTONE_SLAB.get());
        dropSelf(ModBlocks.DUNESTONE_WALL.get());
        dropSelf(ModBlocks.DUNESTONE_BRICKS.get());
        dropSelf(ModBlocks.DUNESTONE_BRICK_STAIRS.get());
        dropSelf(ModBlocks.DUNESTONE_BRICK_SLAB.get());
        dropSelf(ModBlocks.DUNESTONE_BRICK_WALL.get());
        dropSelf(ModBlocks.CHISELED_DUNESTONE_BRICKS.get());
        dropSelf(ModBlocks.COBBLED_DUNESTONE.get());
        dropSelf(ModBlocks.COBBLED_DUNESTONE_STAIRS.get());
        dropSelf(ModBlocks.COBBLED_DUNESTONE_SLAB.get());
        dropSelf(ModBlocks.COBBLED_DUNESTONE_WALL.get());

        this.add(ModBlocks.PERMAFROST.get(), block -> this.createSingleItemTableWithSilkTouch(block, ModBlocks.COBBLED_PERMAFROST));
        dropSelf(ModBlocks.PERMAFROST_STAIRS.get());
        dropSelf(ModBlocks.PERMAFROST_SLAB.get());
        dropSelf(ModBlocks.PERMAFROST_WALL.get());
        dropSelf(ModBlocks.PERMAFROST_BRICKS.get());
        dropSelf(ModBlocks.PERMAFROST_BRICK_STAIRS.get());
        dropSelf(ModBlocks.PERMAFROST_BRICK_SLAB.get());
        dropSelf(ModBlocks.PERMAFROST_BRICK_WALL.get());
        dropSelf(ModBlocks.CHISELED_PERMAFROST_BRICKS.get());
        dropSelf(ModBlocks.COBBLED_PERMAFROST.get());
        dropSelf(ModBlocks.COBBLED_PERMAFROST_STAIRS.get());
        dropSelf(ModBlocks.COBBLED_PERMAFROST_SLAB.get());
        dropSelf(ModBlocks.COBBLED_PERMAFROST_WALL.get());

        this.add(ModBlocks.WILDSTONE.get(), block -> this.createSingleItemTableWithSilkTouch(block, ModBlocks.COBBLED_WILDSTONE));
        dropSelf(ModBlocks.WILDSTONE_STAIRS.get());
        dropSelf(ModBlocks.WILDSTONE_SLAB.get());
        dropSelf(ModBlocks.WILDSTONE_WALL.get());
        dropSelf(ModBlocks.WILDSTONE_BRICKS.get());
        dropSelf(ModBlocks.WILDSTONE_BRICK_STAIRS.get());
        dropSelf(ModBlocks.WILDSTONE_BRICK_SLAB.get());
        dropSelf(ModBlocks.WILDSTONE_BRICK_WALL.get());
        dropSelf(ModBlocks.CHISELED_WILDSTONE_BRICKS.get());
        dropSelf(ModBlocks.COBBLED_WILDSTONE.get());
        dropSelf(ModBlocks.COBBLED_WILDSTONE_STAIRS.get());
        dropSelf(ModBlocks.COBBLED_WILDSTONE_SLAB.get());
        dropSelf(ModBlocks.COBBLED_WILDSTONE_WALL.get());

        dropSelf(ModBlocks.AMETHYST_BRICKS.get());
        dropSelf(ModBlocks.AMETHYST_BRICK_STAIRS.get());
        dropSelf(ModBlocks.AMETHYST_BRICK_SLAB.get());
        dropSelf(ModBlocks.AMETHYST_BRICK_WALL.get());
        dropSelf(ModBlocks.CHISELED_AMETHYST_BLOCK.get());

        // Natural Blocks
        this.add(ModBlocks.SILT.get(), this.createSiltDrops(ModBlocks.SILT.get()));
        dropSelf(ModBlocks.ICICLE.get());
        this.add(ModBlocks.DUNESTONE_COAL_ORE.get(), createOreDrop(ModBlocks.PERMAFROST_COAL_ORE.get(), Items.COAL));
        this.add(ModBlocks.DUNESTONE_IRON_ORE.get(), createOreDrop(ModBlocks.PERMAFROST_IRON_ORE.get(), Items.RAW_IRON));
        this.add(ModBlocks.DUNESTONE_COPPER_ORE.get(), createCopperOreDrops(ModBlocks.PERMAFROST_COPPER_ORE.get()));
        this.add(ModBlocks.DUNESTONE_GOLD_ORE.get(), createOreDrop(ModBlocks.PERMAFROST_GOLD_ORE.get(), Items.RAW_GOLD));
        this.add(ModBlocks.DUNESTONE_REDSTONE_ORE.get(), createRedstoneOreDrops(ModBlocks.PERMAFROST_REDSTONE_ORE.get()));
        this.add(ModBlocks.DUNESTONE_EMERALD_ORE.get(), createOreDrop(ModBlocks.PERMAFROST_EMERALD_ORE.get(), Items.EMERALD));
        this.add(ModBlocks.DUNESTONE_LAPIS_ORE.get(), createLapisOreDrops(ModBlocks.PERMAFROST_LAPIS_ORE.get()));
        this.add(ModBlocks.DUNESTONE_DIAMOND_ORE.get(), createOreDrop(ModBlocks.PERMAFROST_DIAMOND_ORE.get(), Items.DIAMOND));
        this.add(ModBlocks.PERMAFROST_COAL_ORE.get(), this.createOreDrop(ModBlocks.PERMAFROST_COAL_ORE.get(), Items.COAL));
        this.add(ModBlocks.PERMAFROST_IRON_ORE.get(), this.createOreDrop(ModBlocks.PERMAFROST_IRON_ORE.get(), Items.RAW_IRON));
        this.add(ModBlocks.PERMAFROST_COPPER_ORE.get(), this.createCopperOreDrops(ModBlocks.PERMAFROST_COPPER_ORE.get()));
        this.add(ModBlocks.PERMAFROST_GOLD_ORE.get(), this.createOreDrop(ModBlocks.PERMAFROST_GOLD_ORE.get(), Items.RAW_GOLD));
        this.add(ModBlocks.PERMAFROST_REDSTONE_ORE.get(), this.createRedstoneOreDrops(ModBlocks.PERMAFROST_REDSTONE_ORE.get()));
        this.add(ModBlocks.PERMAFROST_EMERALD_ORE.get(), this.createOreDrop(ModBlocks.PERMAFROST_EMERALD_ORE.get(), Items.EMERALD));
        this.add(ModBlocks.PERMAFROST_LAPIS_ORE.get(), this.createLapisOreDrops(ModBlocks.PERMAFROST_LAPIS_ORE.get()));
        this.add(ModBlocks.PERMAFROST_DIAMOND_ORE.get(), this.createOreDrop(ModBlocks.PERMAFROST_DIAMOND_ORE.get(), Items.DIAMOND));
        this.add(ModBlocks.WILDSTONE_COAL_ORE.get(), createOreDrop(ModBlocks.WILDSTONE_COAL_ORE.get(), Items.COAL));
        this.add(ModBlocks.WILDSTONE_IRON_ORE.get(), createOreDrop(ModBlocks.WILDSTONE_IRON_ORE.get(), Items.RAW_IRON));
        this.add(ModBlocks.WILDSTONE_COPPER_ORE.get(), createCopperOreDrops(ModBlocks.WILDSTONE_COPPER_ORE.get()));
        this.add(ModBlocks.WILDSTONE_GOLD_ORE.get(), createOreDrop(ModBlocks.WILDSTONE_GOLD_ORE.get(), Items.RAW_GOLD));
        this.add(ModBlocks.WILDSTONE_REDSTONE_ORE.get(), createRedstoneOreDrops(ModBlocks.WILDSTONE_REDSTONE_ORE.get()));
        this.add(ModBlocks.WILDSTONE_EMERALD_ORE.get(), createOreDrop(ModBlocks.WILDSTONE_EMERALD_ORE.get(), Items.EMERALD));
        this.add(ModBlocks.WILDSTONE_LAPIS_ORE.get(), createLapisOreDrops(ModBlocks.WILDSTONE_LAPIS_ORE.get()));
        this.add(ModBlocks.WILDSTONE_DIAMOND_ORE.get(), createOreDrop(ModBlocks.WILDSTONE_DIAMOND_ORE.get(), Items.DIAMOND));
        this.add(ModBlocks.BOULDER.get(), createOreDrop(Blocks.STONE, Items.COBBLESTONE));
        this.add(ModBlocks.IRON_BOULDER.get(), createBoulderDrops(Blocks.RAW_IRON_BLOCK, Items.RAW_IRON));
        this.add(ModBlocks.COPPER_BOULDER.get(), createBoulderDrops(Blocks.RAW_COPPER_BLOCK, Items.RAW_COPPER));
        this.add(ModBlocks.GOLD_BOULDER.get(), createBoulderDrops(Blocks.RAW_GOLD_BLOCK, Items.RAW_GOLD));
        this.add(ModBlocks.LUSH_BOULDER.get(), createOreDrop(Blocks.STONE, Items.COBBLESTONE));
        this.add(ModBlocks.LUSH_IRON_BOULDER.get(), createBoulderDrops(Blocks.RAW_IRON_BLOCK, Items.RAW_IRON));
        this.add(ModBlocks.LUSH_COPPER_BOULDER.get(), createBoulderDrops(Blocks.RAW_COPPER_BLOCK, Items.RAW_COPPER));
        this.add(ModBlocks.LUSH_GOLD_BOULDER.get(), createBoulderDrops(Blocks.RAW_GOLD_BLOCK, Items.RAW_GOLD));
        this.add(ModBlocks.CAVE_MUSHROOM_BLOCK.get(), createMushroomBlockDrop(
                ModBlocks.CAVE_MUSHROOM_BLOCK.get(), ModBlocks.CAVE_MUSHROOM
        ));
        dropSelf(ModBlocks.CAVE_MUSHROOM.get());
        dropOther(ModBlocks.CLAY_PILE.get(), Items.CLAY_BALL);
        rockDrops(ModBlocks.ROCK.get());
        rockDrops(ModBlocks.ICE_BALL.get());
        rockDrops(ModBlocks.DEEPSLATE_ROCK.get());
        rockDrops(ModBlocks.DRIPSTONE_ROCK.get());
        rockDrops(ModBlocks.BASALT_ROCK.get());
        this.add(ModBlocks.SHORT_UNDER_BRUSH.get(), createShearsOnlyDrop(ModBlocks.SHORT_UNDER_BRUSH));
        this.add(ModBlocks.TALL_UNDER_BRUSH.get(), createDoublePlantShearsDrop(ModBlocks.TALL_UNDER_BRUSH.get()));
        this.add(ModBlocks.PACKED_WEB.get(), createOreDrop(ModBlocks.PACKED_WEB.get(), Items.STRING));
        this.add(ModBlocks.WEB_VEIN.get(), createOreDrop(ModBlocks.WEB_VEIN.get(), Items.STRING));
        this.add(ModBlocks.HANGING_WEB.get(), createOreDrop(ModBlocks.HANGING_WEB.get(), Items.STRING));
        dropWhenSilkTouch(ModBlocks.SPIDER_EGG.get());
        this.add(ModBlocks.DEEPSLATE_GEYSER.get(), block -> this.createSingleItemTableWithSilkTouch(block, Blocks.COBBLED_DEEPSLATE));
        this.add(ModBlocks.BASALT_GEYSER.get(), block -> this.createSingleItemTableWithSilkTouch(block, Blocks.BASALT));

        // Redstone Blocks
        dropSelf(ModBlocks.BLASTER.get());
        dropSelf(ModBlocks.WOODEN_LIFT_TRACK.get());
        dropSelf(ModBlocks.POWERED_LIFT_TRACK.get());
    }

    protected LootTable.Builder createBoulderDrops(Block block, ItemLike drop) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(
                block,
                this.applyExplosionDecay(
                        block,
                        LootItem.lootTableItem(drop)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 8.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )
        );
    }

    private LootTable.Builder createSiltDrops(Block block) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        // LootPool for no silk touch drops: either block with 85% chance, or nuggets with 15% chance
        LootPool.Builder noSilkTouchPool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(this.doesNotHaveSilkTouch())
                .add(applyExplosionDecay(block,
                        LootItem.lootTableItem(block)
                                .when(LootItemRandomChanceCondition.randomChance(0.85f))
                ))
                .add(applyExplosionDecay(block,
                        LootItem.lootTableItem(Items.FLINT)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                                .when(LootItemRandomChanceCondition.randomChance(0.15f))
                ))
                .add(applyExplosionDecay(block,
                        LootItem.lootTableItem(Items.BONE)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                                .when(LootItemRandomChanceCondition.randomChance(0.15f))
                ))
                .add(applyExplosionDecay(block,
                        LootItem.lootTableItem(Items.IRON_NUGGET)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                                .when(LootItemRandomChanceCondition.randomChance(0.10f))
                ))
                .add(applyExplosionDecay(block,
                        LootItem.lootTableItem(Items.GOLD_NUGGET)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                                .when(LootItemRandomChanceCondition.randomChance(0.05f))
                ));

        // Silk touch always drops block
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(this.hasSilkTouch())
                        .add(LootItem.lootTableItem(block))
                )
                .withPool(noSilkTouchPool);
    }


    private void rockDrops(Block block) {
        if (block == ModBlocks.CLAY_PILE.get()) {
            this.add(block, this.createRockDrops(block, Items.CLAY_BALL));
        } else {
            this.add(block, this.createRockDrops(block, block));
        }
    }

    private LootTable.Builder createRockDrops(Block block, ItemLike drop) {
        return LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(
                                        this.applyExplosionDecay(
                                                drop,
                                                LootItem.lootTableItem(block)
                                                        .apply(
                                                                List.of(2, 3, 4),
                                                                amount -> SetItemCountFunction.setCount(ConstantValue.exactly((float)amount.intValue()))
                                                                        .when(
                                                                                LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                                                                        .setProperties(
                                                                                                StatePropertiesPredicate.Builder.properties().hasProperty(ModBlockStateProperties.ROCKS, amount.intValue())
                                                                                        )
                                                                        )
                                                        )
                                        )
                                )
                );
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
