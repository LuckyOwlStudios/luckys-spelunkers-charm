package net.luckystudio.spelunkers_charm.worldgen.feature.custom;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

// Copy this class from the HugeBrownMushroomFeature class in the Minecraft source code
public class HugeCaveMushroomFeature extends AbstractHugeMushroomFeature {

    public HugeCaveMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected int getTreeHeight(RandomSource random) {
        return 7;
    }

    protected boolean isValidPosition(
            LevelAccessor level, BlockPos pos, int maxHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
    ) {
        int i = pos.getY();
        if (i >= level.getMinBuildHeight() + 1 && i + maxHeight + 1 < level.getMaxBuildHeight()) {
            BlockState blockstate = level.getBlockState(pos.below());
            if (!isDirt(blockstate) && !blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
                return false;
            } else {
                for (int j = 0; j <= maxHeight; j++) {
                    int k = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius, j);

                    for (int l = -k; l <= k; l++) {
                        for (int i1 = -k; i1 <= k; i1++) {
                            BlockState blockstate1 = level.getBlockState(mutablePos.setWithOffset(pos, l, j, i1));
                            if (!blockstate1.isAir() && !blockstate1.is(BlockTags.LEAVES)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    protected void makeCap(LevelAccessor level, RandomSource random, BlockPos pos, int treeHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config) {
        BlockState capState = config.capProvider.getState(random, pos);
        BlockState lightState = Blocks.OCHRE_FROGLIGHT.defaultBlockState();

        int[][][] capShape = {
                { // Top Layer (treeHeight)
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 1, 1, 1, 0, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 0, 1, 1, 1, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0}
                },
                { // Second Layer (1 block below)
                        {0, 0, 1, 1, 1, 0, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {1, 1, 2, 2, 2, 1, 1},
                        {1, 1, 2, 2, 2, 1, 1},
                        {1, 1, 2, 2, 2, 1, 1},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 0, 1, 1, 1, 0, 0}
                },
                { // Third Layer (2 blocks below)
                        {0, 0, 1, 1, 1, 0, 0},
                        {0, 1, 0, 0, 0, 1, 0},
                        {1, 0, 0, 0, 0, 0, 1},
                        {1, 0, 0, 0, 0, 0, 1},
                        {1, 0, 0, 0, 0, 0, 1},
                        {0, 1, 0, 0, 0, 1, 0},
                        {0, 0, 1, 1, 1, 0, 0}
                }
        };

        for (int y = 0; y < capShape.length; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int blockType = capShape[y][dx + 3][dz + 3];

                    if (blockType == 1 || blockType == 2) {
                        mutablePos.set(pos).move(dx, treeHeight - 1 - y, dz);

                        if (!level.getBlockState(mutablePos).isSolidRender(level, mutablePos)) {
                            BlockState stateToPlace = (blockType == 2) ? lightState : capState;
                            boolean isInnerSecondLayer = (y == 1 && dx >= -2 && dx <= 2 && dz >= -2 && dz <= 2 && blockType != 2);
                            boolean isInnerThirdLayer = (y == 2 && dx > -3 && dx < 3 && dz > -3 && dz < 3);

                            if (stateToPlace == capState) {
                                stateToPlace = stateToPlace
                                        .setValue(HugeMushroomBlock.DOWN, y != 1)
                                        .setValue(HugeMushroomBlock.NORTH, !(dz == 3 || dz == 2 && Math.abs(dx) == 2))
                                        .setValue(HugeMushroomBlock.EAST, !(dx == -3 || dx == -2 && Math.abs(dz) == 2))
                                        .setValue(HugeMushroomBlock.SOUTH, !(dz == -3 || dz == -2 && Math.abs(dx) == 2))
                                        .setValue(HugeMushroomBlock.WEST, !(dx == 3 || dx == 2 && Math.abs(dz) == 2));
                            }

                            this.setBlock(level, mutablePos, stateToPlace);
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void placeTrunk(LevelAccessor level, RandomSource random, BlockPos pos, HugeMushroomFeatureConfiguration config, int maxHeight, BlockPos.MutableBlockPos mutablePos) {
        System.out.println("Placing trunk");
        // Define the layers of the mushroom stem shape
        int[][][] shape = {
                { // Bottom Layer (0)
                        {0, 2, 1, 2, 0},
                        {0, 1, 1, 1, 2},
                        {1, 1, 1, 1, 1},
                        {0, 1, 1, 1, 2},
                        {0, 2, 1, 2, 0}
                },
                { // Layer 1
                        {0, 0, 0, 0, 0},
                        {0, 1, 1, 1, 0},
                        {0, 1, 1, 1, 0},
                        {0, 1, 1, 1, 0},
                        {0, 0, 0, 0, 0}
                },
                { // Layer 2
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                { // Layer 3
                        {0, 0, 0, 0, 0},
                        {0, 0, 2, 0, 0},
                        {0, 2, 1, 2, 0},
                        {0, 0, 2, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                { // Top Layer (4)
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0}
                }
        };

        // Loop through each layer and place blocks
        for (int y = 0; y < shape.length; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int blockType = shape[y][dx + 2][dz + 2]; // Convert -2 to index 0

                    if (blockType == 1 || (blockType == 2 && random.nextBoolean())) {
                        mutablePos.set(pos).move(dx, y, dz);
                        BlockState stem = config.stemProvider.getState(random, pos);
                        if (!level.getBlockState(mutablePos).isSolidRender(level, mutablePos)) {
                            this.setBlock(level, mutablePos, stem
                                    .setValue(HugeMushroomBlock.UP, true)
                                    .setValue(HugeMushroomBlock.DOWN, true));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected int getTreeRadiusForHeight(int p_65094_, int height, int foliageRadius, int y) {
        return y <= 3 ? 0 : foliageRadius;
    }
}
