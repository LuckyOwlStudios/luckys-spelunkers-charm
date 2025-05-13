package luckyowlstudios.mods.luckysspelunkerscharm.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import static luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm.id;

public class ModBlockTags {
    public static final TagKey<Block> GEYSERS = TagKey.create(Registries.BLOCK, id("geysers"));
}
