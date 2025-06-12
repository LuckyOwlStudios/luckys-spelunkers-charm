package net.luckystudio.splelunkers_charm.datagen.itemTags;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModItemTags {
    public static final TagKey<Item> MINERALS = createTag("minerals");
    public static final  TagKey<Item> GEYSERS = createTag("geysers");

    private static TagKey<Item> createTag(String name) {
        return ItemTags.create(SpelunkersCharm.id(name));
    }
}
