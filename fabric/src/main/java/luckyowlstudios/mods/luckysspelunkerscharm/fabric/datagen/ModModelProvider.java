package luckyowlstudios.mods.luckysspelunkerscharm.fabric.datagen;

import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.*;
import luckyowlstudios.mods.luckysspelunkerscharm.block.ModBlocks;
import luckyowlstudios.mods.luckysspelunkerscharm.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        createGeyser(blockStateModelGenerator, ModBlocks.DEEPSLATE_GEYSER);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ModItems.yourItem, ModelTemplates.FLAT_ITEM);
    }

    public final void createGeyser(BlockModelGenerators blockStateModelGenerator, Block block) {
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.END, ResourceLocation.fromNamespaceAndPath(LuckysSpelunkersCharm.MOD_ID, "block/deepslate_geyser_top"))
                .put(TextureSlot.SIDE, ResourceLocation.fromNamespaceAndPath(LuckysSpelunkersCharm.MOD_ID, "block/deepslate_geyser_side"));
        ResourceLocation model = ModelTemplates.CUBE_COLUMN_MIRRORED.create(block, textureMapping, blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, model));
    }
}
