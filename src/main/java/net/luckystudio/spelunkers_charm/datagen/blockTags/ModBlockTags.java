package net.luckystudio.spelunkers_charm.datagen.blockTags;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    // Worldgen tags
    public static final TagKey<Block> OVERWORLD_REPLACEABLE = createTag("overworld_replaceable");
    public static final TagKey<Block> DUNESTONE_REPLACEABLE = createTag("dunestone_replaceable");
    public static final TagKey<Block> WILDSTONE_REPLACEABLE = createTag("wildstone_replaceable");
    public static final TagKey<Block> PERMAFROST_REPLACEABLE = createTag("permafrost_replaceable");

    private static TagKey<Block> createTag(String name) {
        return BlockTags.create(SpelunkersCharm.id(name));
    }
}
