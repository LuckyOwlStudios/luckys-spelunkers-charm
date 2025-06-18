package net.luckystudio.spelunkers_charm.worldgen.feature.custom.packed_ice_cluster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class IcicleClusterConfiguration implements FeatureConfiguration {
    public static final Codec<IcicleClusterConfiguration> CODEC = RecordCodecBuilder.create(
            p_160784_ -> p_160784_.group(
                            Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.floorToCeilingSearchRange),
                            IntProvider.codec(1, 128).fieldOf("height").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.height),
                            IntProvider.codec(1, 128).fieldOf("radius").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.radius),
                            Codec.intRange(0, 64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.maxStalagmiteStalactiteHeightDiff),
                            Codec.intRange(1, 64).fieldOf("height_deviation").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.heightDeviation),
                            IntProvider.codec(0, 128).fieldOf("icicle_block_layer_thickness").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.icicleBlockLayerThickness),
                            FloatProvider.codec(0.0F, 2.0F).fieldOf("density").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.density),
                            FloatProvider.codec(0.0F, 2.0F).fieldOf("wetness").forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.wetness),
                            Codec.floatRange(0.0F, 1.0F)
                                    .fieldOf("chance_of_icicle_column_at_max_distance_from_center")
                                    .forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.chanceOfDripstoneColumnAtMaxDistanceFromCenter),
                            Codec.intRange(1, 64)
                                    .fieldOf("max_distance_from_edge_affecting_chance_of_icicle_column")
                                    .forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn),
                            Codec.intRange(1, 64)
                                    .fieldOf("max_distance_from_center_affecting_height_bias")
                                    .forGetter(packedIceClusterConfiguration -> packedIceClusterConfiguration.maxDistanceFromCenterAffectingHeightBias)
                    )
                    .apply(p_160784_, IcicleClusterConfiguration::new)
    );
    public final int floorToCeilingSearchRange;
    public final IntProvider height;
    public final IntProvider radius;
    public final int maxStalagmiteStalactiteHeightDiff;
    public final int heightDeviation;
    public final IntProvider icicleBlockLayerThickness;
    public final FloatProvider density;
    public final FloatProvider wetness;
    public final float chanceOfDripstoneColumnAtMaxDistanceFromCenter;
    public final int maxDistanceFromEdgeAffectingChanceOfDripstoneColumn;
    public final int maxDistanceFromCenterAffectingHeightBias;

    public IcicleClusterConfiguration(
            int floorToCeilingSearchRange,
            IntProvider height,
            IntProvider radius,
            int maxStalagmiteStalactiteHeightDiff,
            int heightDeviation,
            IntProvider icicleBlockLayerThickness,
            FloatProvider density,
            FloatProvider wetness,
            float chanceOfDripstoneColumnAtMaxDistanceFromCenter,
            int maxDistanceFromEdgeAffectingChanceOfDripstoneColumn,
            int maxDistanceFromCenterAffectingHeightBias
    ) {
        this.floorToCeilingSearchRange = floorToCeilingSearchRange;
        this.height = height;
        this.radius = radius;
        this.maxStalagmiteStalactiteHeightDiff = maxStalagmiteStalactiteHeightDiff;
        this.heightDeviation = heightDeviation;
        this.icicleBlockLayerThickness = icicleBlockLayerThickness;
        this.density = density;
        this.wetness = wetness;
        this.chanceOfDripstoneColumnAtMaxDistanceFromCenter = chanceOfDripstoneColumnAtMaxDistanceFromCenter;
        this.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn = maxDistanceFromEdgeAffectingChanceOfDripstoneColumn;
        this.maxDistanceFromCenterAffectingHeightBias = maxDistanceFromCenterAffectingHeightBias;
    }
}
