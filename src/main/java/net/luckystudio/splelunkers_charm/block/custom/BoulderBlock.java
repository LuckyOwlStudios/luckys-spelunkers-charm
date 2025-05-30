package net.luckystudio.splelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.BlockPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class BoulderBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<BoulderBlock> CODEC = simpleCodec(BoulderBlock::new);
    public static final EnumProperty<BlockPart> BLOCK_PART = ModBlockStateProperties.BLOCK_PART;

    public BoulderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BLOCK_PART, BlockPart.TOP));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
