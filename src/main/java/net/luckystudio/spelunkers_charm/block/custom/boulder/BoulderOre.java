package net.luckystudio.spelunkers_charm.block.custom.boulder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The ore a boulder carries in its core.
 * <p>
 * Vanilla ores point straight at their raw block. Modded ores are found through the common {@code c:} tags
 * ({@code c:storage_blocks/raw_<name>} for the core block and {@code c:raw_materials/<name>} for the drops),
 * so this mod never depends on the mod that adds them. If nothing fills the tag, the boulder never generates
 * and is hidden from the creative tab.
 * <p>
 * To add a new modded ore boulder: add an entry here with {@link #modded}, add matching entries to
 * {@link BoulderType}, then add textures in {@code textures/block/boulder/} and lang entries.
 */
public enum BoulderOre {
    NONE("none", () -> Blocks.STONE, () -> Blocks.COBBLESTONE, 1.0F, 1.0F),
    IRON("iron", () -> Blocks.RAW_IRON_BLOCK, () -> Blocks.RAW_IRON_BLOCK, 0.3F, 0.3F),
    COPPER("copper", () -> Blocks.RAW_COPPER_BLOCK, () -> Blocks.RAW_COPPER_BLOCK, 0.3F, 0.3F),
    GOLD("gold", () -> Blocks.RAW_GOLD_BLOCK, () -> Blocks.RAW_GOLD_BLOCK, 0.1F, 0.25F),
    // Create
    ZINC("zinc", 0.2F, 0.2F),
    // Mekanism
    OSMIUM("osmium", 0.2F, 0.2F),
    // Mekanism, Thermal, Immersive Engineering, Oritech, Modern Industrialization...
    TIN("tin", 0.2F, 0.2F),
    LEAD("lead", 0.1F, 0.2F);

    private final String name;
    private final Supplier<Block> propertiesSource;
    @Nullable
    private final Supplier<Block> fixedCore;
    @Nullable
    private final TagKey<Block> coreTag;
    @Nullable
    private final TagKey<Item> rawBlockItemTag;
    @Nullable
    private final TagKey<Item> rawMaterialTag;
    private final float weightAboveZero;
    private final float weightBelowZero;

    BoulderOre(String name, Supplier<Block> propertiesSource, Supplier<Block> fixedCore, float weightAboveZero, float weightBelowZero) {
        this.name = name;
        this.propertiesSource = propertiesSource;
        this.fixedCore = fixedCore;
        this.coreTag = null;
        this.rawBlockItemTag = null;
        this.rawMaterialTag = null;
        this.weightAboveZero = weightAboveZero;
        this.weightBelowZero = weightBelowZero;
    }

    /** A modded ore, resolved entirely through {@code c:} tags. */
    BoulderOre(String name, float weightAboveZero, float weightBelowZero) {
        this.name = name;
        this.propertiesSource = () -> Blocks.RAW_IRON_BLOCK;
        this.fixedCore = null;
        this.coreTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_" + name));
        this.rawBlockItemTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_" + name));
        this.rawMaterialTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/" + name));
        this.weightAboveZero = weightAboveZero;
        this.weightBelowZero = weightBelowZero;
    }

    public String getName() {
        return name;
    }

    public boolean isModded() {
        return coreTag != null;
    }

    /** The block whose properties (hardness, sound...) the boulder copies. */
    public Block getPropertiesSource() {
        return propertiesSource.get();
    }

    @Nullable
    public TagKey<Block> getCoreTag() {
        return coreTag;
    }

    @Nullable
    public TagKey<Item> getRawBlockItemTag() {
        return rawBlockItemTag;
    }

    @Nullable
    public TagKey<Item> getRawMaterialTag() {
        return rawMaterialTag;
    }

    public float getWeight(boolean belowZero) {
        return belowZero ? weightBelowZero : weightAboveZero;
    }

    /**
     * The block placed in the middle of the boulder, or null when no installed mod provides it.
     * Uses the tags bound to the block registry, so only call this once tags are loaded (worldgen, gameplay).
     */
    @Nullable
    public Block getCore() {
        if (fixedCore != null) return fixedCore.get();
        Optional<HolderSet.Named<Block>> tag = BuiltInRegistries.BLOCK.getTag(coreTag);
        return tag.flatMap(set -> set.stream().findFirst()).map(Holder::value).orElse(null);
    }

    /** Whether this ore exists in the current game (always true for vanilla ores). */
    public boolean isAvailable() {
        return getCore() != null;
    }

    /** Same as {@link #isAvailable()}, but looks the tag up in the given registries (e.g. the creative tab's). */
    public boolean isAvailable(HolderLookup.Provider registries) {
        if (fixedCore != null) return true;
        return registries.lookup(Registries.BLOCK)
                .flatMap(lookup -> lookup.get(coreTag))
                .map(set -> set.size() > 0)
                .orElse(false);
    }
}
