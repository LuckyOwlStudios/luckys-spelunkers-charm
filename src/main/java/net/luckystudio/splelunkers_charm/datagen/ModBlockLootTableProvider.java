package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
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
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    private static final float[] CHANCES = new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {

        // Cave Blocks
        otherWhenSilkTouch(ModBlocks.DEEPSLATE_GEYSER.get(), Blocks.DEEPSLATE);
        otherWhenSilkTouch(ModBlocks.BASALT_GEYSER.get(), Blocks.BASALT);
        dropOther(ModBlocks.CLAY_BALL.get(), Items.CLAY_BALL);
        rockDrops(ModBlocks.ROCK.get());
        rockDrops(ModBlocks.DEEPSLATE_ROCK.get());
        rockDrops(ModBlocks.DRIPSTONE_ROCK.get());
        rockDrops(ModBlocks.BASALT_ROCK.get());
        dropSelf(ModBlocks.CAVE_MUSHROOM.get());
        this.add(ModBlocks.CAVE_MUSHROOM_BLOCK.get(), createMushroomBlockDrop(ModBlocks.CAVE_MUSHROOM_BLOCK.get(), ModBlocks.CAVE_MUSHROOM));
        this.add(ModBlocks.CAVE_MUSHROOM.get(), createSilkTouchOrShearsDispatchTable(ModBlocks.CAVE_MUSHROOM.get(), LootItem.lootTableItem(ModBlocks.CAVE_MUSHROOM)));
        this.add(ModBlocks.SHORT_UNDER_BRUSH.get(), createShearsOnlyDrop(ModBlocks.SHORT_UNDER_BRUSH));
        this.add(ModBlocks.TALL_UNDER_BRUSH.get(), createDoublePlantShearsDrop(ModBlocks.TALL_UNDER_BRUSH.get()));

        // Ice Cave Blocks
        dropSelf(ModBlocks.COLD_STONE.get());
        this.add(ModBlocks.SILT.get(), this.createSiltDrops(ModBlocks.SILT.get()));
        dropSelf(ModBlocks.ICICLE.get());
        dropSelf(ModBlocks.AQUAMARINE_CLUSTER.get());
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
        if (block == ModBlocks.CLAY_BALL.get()) {
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
