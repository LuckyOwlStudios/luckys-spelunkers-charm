package net.luckystudio.splelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UnderBrushBlock extends TallGrassBlock {
    public static final MapCodec<TallGrassBlock> CODEC = simpleCodec(TallGrassBlock::new);

    @Override
    public MapCodec<TallGrassBlock> codec() {
        return CODEC;
    }

    public UnderBrushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        DoublePlantBlock doubleplantblock = (DoublePlantBlock) ModBlocks.TALL_UNDER_BRUSH.get();
        if (doubleplantblock.defaultBlockState().canSurvive(level, pos) && level.isEmptyBlock(pos.above())) {
            DoublePlantBlock.placeAt(level, doubleplantblock.defaultBlockState(), pos, 2);
        }
    }
}
