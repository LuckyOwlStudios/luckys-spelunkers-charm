package net.luckystudio.splelunkers_charm.block.custom;


import com.mojang.serialization.MapCodec;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class GeyserBlock extends Block {
    public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);
    public static final EnumProperty<GeyserState> GEYSER_STATE = ModBlockStateProperties.GEYSER_STATE;

    public GeyserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(GEYSER_STATE, GeyserState.INACTIVE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GEYSER_STATE);
    }
}
