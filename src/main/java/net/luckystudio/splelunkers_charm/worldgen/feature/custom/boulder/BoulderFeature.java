package net.luckystudio.splelunkers_charm.worldgen.feature.custom.boulder;

import com.mojang.serialization.Codec;
import net.luckystudio.splelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderType;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderUtils;
import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderBlock;
import net.luckystudio.splelunkers_charm.block.util.enums.BlockPart;
import net.luckystudio.splelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

public class BoulderFeature extends Feature<NoneFeatureConfiguration> {

    public BoulderFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!SpelunkersCharmConfig.BOULDERS.get()) return false;
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin().above();

        Random random = new Random();
        boolean isLushCave = world.getBiome(origin).is(ModBiomeTags.IS_JUNGLE_CAVE) || world.getBiome(origin).getKey() == Biomes.LUSH_CAVES;
        boolean isFrozenCave = world.getBiome(origin).is(ModBiomeTags.IS_COLD_CAVE) && world.getBiome(origin).getKey() != Biomes.DRIPSTONE_CAVES;
        BlockState surroundingBlockState;

        boolean isBelowZero = origin.getY() < 0;

        float goldWeight = isBelowZero ? 0.25F : 0.1F;
        float ironWeight = 0.3F;
        float copperWeight = 0.3F;
        float regularWeight = 1.0F;

        float total = goldWeight + ironWeight + copperWeight + regularWeight;
        float random2 = random.nextFloat() * total;

        BoulderType boulderType;

        if (random2 < goldWeight) {
            boulderType = isLushCave ? BoulderType.LUSH_GOLD : BoulderType.GOLD;
        } else if ((random2 -= goldWeight) < ironWeight) {
            boulderType = isLushCave ? BoulderType.LUSH_IRON : BoulderType.IRON;
        } else if (random2 - ironWeight < copperWeight) {
            boulderType = isLushCave ? BoulderType.LUSH_COPPER : BoulderType.COPPER;
        } else {
            boulderType = isLushCave ? BoulderType.LUSH : BoulderType.STONE;
        }

        BlockState boulderProviderState = BoulderUtils.getBoulderFromType(boulderType);

        // Handle the outer blocks around the boulder
        if (isBelowZero) {
            surroundingBlockState = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        } else if (isLushCave) {
            surroundingBlockState = Blocks.MOSS_BLOCK.defaultBlockState();
        } else if (isFrozenCave) {
            surroundingBlockState = Blocks.BLUE_ICE.defaultBlockState();
        } else {
            surroundingBlockState = Blocks.COBBLESTONE.defaultBlockState();
        }

        Direction direction = Direction.NORTH;

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // First Layer
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        this.setBlock(world, origin, boulderProviderState
                .setValue(BlockStateProperties.FACING, Direction.NORTH)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_MIDDLE));
        // Front
        this.setBlock(world, origin.relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_SIDE));
        // Front Right
        this.setBlock(world, origin.relative(direction).relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_CORNER));
        // Right
        this.setBlock(world, origin.relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_SIDE));
        // Back Right
        this.setBlock(world, origin.relative(direction.getClockWise()).relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_CORNER));
        // Back
        this.setBlock(world, origin.relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_SIDE));
        // Back Left
        this.setBlock(world, origin.relative(direction.getOpposite()).relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_CORNER));
        // Left
        this.setBlock(world, origin.relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_SIDE));
        // Front Left
        this.setBlock(world, origin.relative(direction.getCounterClockWise()).relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.BOTTOM_CORNER));

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Second Layer
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        this.setBlock(world, origin.above(), BoulderUtils.getRawCore(boulderType).defaultBlockState());
        // Front
        this.setBlock(world, origin.above().relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_SIDE));
        // Front Right
        this.setBlock(world, origin.above().relative(direction).relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_CORNER));
        // Right
        this.setBlock(world, origin.above().relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_SIDE));
        // Back Right
        this.setBlock(world, origin.above().relative(direction.getClockWise()).relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_CORNER));
        // Back
        this.setBlock(world, origin.above().relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_SIDE));
        // Back Left
        this.setBlock(world, origin.above().relative(direction.getOpposite()).relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_CORNER));
        // Left
        this.setBlock(world, origin.above().relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_SIDE));
        // Front Left
        this.setBlock(world, origin.above().relative(direction.getCounterClockWise()).relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.MIDDLE_CORNER));

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // Third Layer
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        this.setBlock(world, origin.above(2), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_MIDDLE));
        // Front
        this.setBlock(world, origin.above(2).relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_SIDE));
        // Front Right
        this.setBlock(world, origin.above(2).relative(direction).relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction)
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_CORNER));
        // Right
        this.setBlock(world, origin.above(2).relative(direction.getClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_SIDE));
        // Back Right
        this.setBlock(world, origin.above(2).relative(direction.getClockWise()).relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_CORNER));
        // Back
        this.setBlock(world, origin.above(2).relative(direction.getOpposite()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_SIDE));
        // Back Left
        this.setBlock(world, origin.above(2).relative(direction.getOpposite()).relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getOpposite())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_CORNER));
        // Left
        this.setBlock(world, origin.above(2).relative(direction.getCounterClockWise()), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_SIDE));
        // Front Left
        this.setBlock(world, origin.above(2).relative(direction.getCounterClockWise()).relative(direction), boulderProviderState
                .setValue(BlockStateProperties.FACING, direction.getCounterClockWise())
                .setValue(BoulderBlock.BLOCK_PART, BlockPart.TOP_CORNER));

        // Set surrounding blocks in a 5x5 under the origin
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = origin.offset(x, -1, z);
                if (world.getBlockState(pos).canBeReplaced() || world.getBlockState(pos).is(BlockTags.AZALEA_ROOT_REPLACEABLE)) {
                    this.setBlock(world, pos, surroundingBlockState);
                }
            }
        }
        return true;
    }

    private BlockState getCore(BlockState boulderProviderState) {
        if (boulderProviderState.getBlock() == ModBlocks.COPPER_BOULDER.get()) {
            return Blocks.RAW_COPPER_BLOCK.defaultBlockState();
        } else if (boulderProviderState.getBlock() == ModBlocks.GOLD_BOULDER.get()) {
            return Blocks.RAW_GOLD_BLOCK.defaultBlockState();
        } else {
            return Blocks.RAW_IRON_BLOCK.defaultBlockState();
        }
    }
}
