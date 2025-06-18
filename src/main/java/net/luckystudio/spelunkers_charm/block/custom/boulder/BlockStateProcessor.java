package net.luckystudio.spelunkers_charm.block.custom.boulder;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckystudio.spelunkers_charm.init.ModStructureProcessorType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BlockStateProcessor extends StructureProcessor {

    public static final MapCodec<BlockStateProcessor> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    HangingType.CODEC.fieldOf("hanging_type").forGetter(p -> p.hangingType)
            ).apply(instance, BlockStateProcessor::new)
    );

    private final HangingType hangingType;

    public BlockStateProcessor(HangingType hangingType) {
        this.hangingType = hangingType;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos pos,
            BlockPos pivot,
            StructureTemplate.StructureBlockInfo originalBlockInfo,
            StructureTemplate.StructureBlockInfo currentBlockInfo,
            StructurePlaceSettings settings
    ) {
        BlockState state = currentBlockInfo.state();
        if (state.getBlock() instanceof BoulderBlock) {
            state = state.setValue(BoulderBlock.HANGING_TYPE, hangingType);
            return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos(), state, currentBlockInfo.nbt());
        }
        return currentBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessorType.BOULDER_STATE.get();
    }
}