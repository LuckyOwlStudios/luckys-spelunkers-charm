package luckyowlstudios.mods.luckysspelunkerscharm.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import luckyowlstudios.mods.luckysspelunkerscharm.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {
    protected ModBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> provider) {
        super(dataOutput, provider);
    }

    @Override
    public void generate() {
        otherWhenSilkTouch(Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_GEYSER);
    }
}
