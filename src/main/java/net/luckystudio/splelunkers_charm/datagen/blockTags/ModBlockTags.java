package net.luckystudio.splelunkers_charm.datagen.blockTags;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {



    private ModBlockTags() {
    }

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, SpelunkersCharm.id(name));
    }

    public static TagKey<Block> create(ResourceLocation name) {
        return TagKey.create(Registries.BLOCK, name);
    }
}
