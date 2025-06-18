package net.luckystudio.spelunkers_charm.worldgen.feature.custom.large_icicle;

import com.mojang.serialization.Codec;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.icicle.IcicleBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

// Copied straight from the LargeIcicleFeature class in Minecraft, with modifications for icicles
public class LargeIcicleFeature extends Feature<LargeIcicleFeatureConfiguration> {

    public LargeIcicleFeature(Codec<LargeIcicleFeatureConfiguration> codec) {
        super(codec);
    }

    /**
     * Places the given feature at the given location.
     * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated, that they can safely generate into.
     *
     * @param context A context object with a reference to the level and the position
     *                the feature is being placed at
     */
    @Override
    public boolean place(FeaturePlaceContext<LargeIcicleFeatureConfiguration> context) {
        WorldGenLevel worldgenlevel = context.level();
        BlockPos blockpos = context.origin();
        LargeIcicleFeatureConfiguration config = context.config();
        RandomSource randomsource = context.random();
        if (!IcicleBlockUtil.isEmptyOrWater(worldgenlevel, blockpos)) {
            return false;
        } else {
            Optional<Column> optional = Column.scan(
                    worldgenlevel,
                    blockpos,
                    config.floorToCeilingSearchRange,
                    IcicleBlockUtil::isEmptyOrWater,
                    IcicleBlockUtil::isIcicleBaseOrLava
            );
            if (!optional.isEmpty() && optional.get() instanceof Column.Range) {
                Column.Range column$range = (Column.Range)optional.get();
                if (column$range.height() < 4) {
                    return false;
                } else {
                    int i = (int)((float)column$range.height() * config.maxColumnRadiusToCaveHeightRatio);
                    int j = Mth.clamp(i, config.columnRadius.getMinValue(), config.columnRadius.getMaxValue());
                    int k = Mth.randomBetweenInclusive(randomsource, config.columnRadius.getMinValue(), j);
                    LargeIcicleFeature.LargeDripstone largedripstonefeature$largedripstone = makeDripstone(
                            blockpos.atY(column$range.ceiling() - 1),
                            false,
                            randomsource,
                            k,
                            config.stalactiteBluntness,
                            config.heightScale
                    );
                    LargeIcicleFeature.LargeDripstone largedripstonefeature$largedripstone1 = makeDripstone(
                            blockpos.atY(column$range.floor() + 1),
                            true,
                            randomsource,
                            k,
                            config.stalagmiteBluntness,
                            config.heightScale
                    );
                    LargeIcicleFeature.WindOffsetter largedripstonefeature$windoffsetter;
                    if (largedripstonefeature$largedripstone.isSuitableForWind(config)
                            && largedripstonefeature$largedripstone1.isSuitableForWind(config)) {
                        largedripstonefeature$windoffsetter = new LargeIcicleFeature.WindOffsetter(
                                blockpos.getY(), randomsource, config.windSpeed
                        );
                    } else {
                        largedripstonefeature$windoffsetter = LargeIcicleFeature.WindOffsetter.noWind();
                    }

                    boolean flag = largedripstonefeature$largedripstone.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(
                            worldgenlevel, largedripstonefeature$windoffsetter
                    );
                    boolean flag1 = largedripstonefeature$largedripstone1.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(
                            worldgenlevel, largedripstonefeature$windoffsetter
                    );
                    if (flag) {
                        largedripstonefeature$largedripstone.placeBlocks(worldgenlevel, randomsource, largedripstonefeature$windoffsetter);
                    }

                    if (flag1) {
                        largedripstonefeature$largedripstone1.placeBlocks(worldgenlevel, randomsource, largedripstonefeature$windoffsetter);
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static LargeIcicleFeature.LargeDripstone makeDripstone(
            BlockPos root, boolean pointingUp, RandomSource random, int radius, FloatProvider bluntnessBase, FloatProvider scaleBase
    ) {
        return new LargeIcicleFeature.LargeDripstone(
                root, pointingUp, radius, (double)bluntnessBase.sample(random), (double)scaleBase.sample(random)
        );
    }

    static final class LargeDripstone {
        private BlockPos root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        LargeDripstone(BlockPos root, boolean pointingUp, int radius, double bluntness, double scale) {
            this.root = root;
            this.pointingUp = pointingUp;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
        }

        private int getHeight() {
            return this.getHeightAtRadius(0.0F);
        }

        boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel level, LargeIcicleFeature.WindOffsetter windOffsetter) {
            while (this.radius > 1) {
                BlockPos.MutableBlockPos blockpos$mutableblockpos = this.root.mutable();
                int i = Math.min(10, this.getHeight());

                for (int j = 0; j < i; j++) {
                    if (level.getBlockState(blockpos$mutableblockpos).is(Blocks.LAVA)) {
                        return false;
                    }

                    if (IcicleBlockUtil.isCircleMostlyEmbeddedInStone(level, windOffsetter.offset(blockpos$mutableblockpos), this.radius)) {
                        this.root = blockpos$mutableblockpos;
                        return true;
                    }

                    blockpos$mutableblockpos.move(this.pointingUp ? Direction.DOWN : Direction.UP);
                }

                this.radius /= 2;
            }

            return false;
        }

        private int getHeightAtRadius(float radius) {
            return (int) IcicleBlockUtil.getIcicleHeight((double)radius, (double)this.radius, this.scale, this.bluntness);
        }

        void placeBlocks(WorldGenLevel level, RandomSource random, LargeIcicleFeature.WindOffsetter windOffsetter) {
            for (int i = -this.radius; i <= this.radius; i++) {
                for (int j = -this.radius; j <= this.radius; j++) {
                    float f = Mth.sqrt((float)(i * i + j * j));
                    if (!(f > (float)this.radius)) {
                        int k = this.getHeightAtRadius(f);
                        if (k > 0) {
                            if ((double)random.nextFloat() < 0.2) {
                                k = (int)((float)k * Mth.randomBetween(random, 0.8F, 1.0F));
                            }

                            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.root.offset(i, 0, j).mutable();
                            boolean flag = false;
                            int l = this.pointingUp
                                    ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ())
                                    : Integer.MAX_VALUE;

                            for (int i1 = 0; i1 < k && blockpos$mutableblockpos.getY() < l; i1++) {
                                BlockPos blockpos = windOffsetter.offset(blockpos$mutableblockpos);
                                if (IcicleBlockUtil.isEmptyOrWaterOrLava(level, blockpos)) {
                                    flag = true;
                                    Block block = Blocks.ICE;
                                    level.setBlock(blockpos, block.defaultBlockState(), 2);
                                } else if (flag && level.getBlockState(blockpos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                                    break;
                                }

                                blockpos$mutableblockpos.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                            }
                        }
                    }
                }
            }
        }

        boolean isSuitableForWind(LargeIcicleFeatureConfiguration config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= (double)config.minBluntnessForWind;
        }
    }

    static final class WindOffsetter {
        private final int originY;
        @Nullable
        private final Vec3 windSpeed;

        WindOffsetter(int originY, RandomSource random, FloatProvider magnitude) {
            this.originY = originY;
            float f = magnitude.sample(random);
            float f1 = Mth.randomBetween(random, 0.0F, (float) Math.PI);
            this.windSpeed = new Vec3((double)(Mth.cos(f1) * f), 0.0, (double)(Mth.sin(f1) * f));
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
        }

        static LargeIcicleFeature.WindOffsetter noWind() {
            return new LargeIcicleFeature.WindOffsetter();
        }

        BlockPos offset(BlockPos pos) {
            if (this.windSpeed == null) {
                return pos;
            } else {
                int i = this.originY - pos.getY();
                Vec3 vec3 = this.windSpeed.scale((double)i);
                return pos.offset(Mth.floor(vec3.x), 0, Mth.floor(vec3.z));
            }
        }
    }
}
