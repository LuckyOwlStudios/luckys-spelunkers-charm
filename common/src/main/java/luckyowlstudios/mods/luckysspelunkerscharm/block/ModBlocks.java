package luckyowlstudios.mods.luckysspelunkerscharm.block;

import luckyowlstudios.mods.luckysspelunkerscharm.block.custom.geyser.GeyserBlock;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.blay09.mods.balm.api.item.BalmItems;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import static net.blay09.mods.balm.api.block.BalmBlocks.blockProperties;
import static luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm.id;

public class ModBlocks {

    public static Block DEEPSLATE_GEYSER;

    public static void initialize(BalmBlocks blocks) {

        // Copied from the deepslate block
        blocks.register(
                (identifier) -> DEEPSLATE_GEYSER = new GeyserBlock(blockProperties(identifier).mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE)),
                BalmItems::blockItem,
                id("geyser_block"));
    }
}
