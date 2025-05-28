package net.luckystudio.splelunkers_charm.worldgen.feature.custom.icicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class IcicleConfiguration implements FeatureConfiguration {
    public static final Codec<IcicleConfiguration> CODEC = RecordCodecBuilder.create(
            icicleConfigurationInstance -> icicleConfigurationInstance.group(
                            Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_taller_dripstone").orElse(0.2F).forGetter(icicleConfiguration -> icicleConfiguration.chanceOfTallerDripstone),
                            Codec.floatRange(0.0F, 1.0F)
                                    .fieldOf("chance_of_directional_spread")
                                    .orElse(0.7F)
                                    .forGetter(p_191292_ -> p_191292_.chanceOfDirectionalSpread),
                            Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spread_radius2").orElse(0.5F).forGetter(icicleConfiguration -> icicleConfiguration.chanceOfSpreadRadius2),
                            Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spread_radius3").orElse(0.5F).forGetter(icicleConfiguration -> icicleConfiguration.chanceOfSpreadRadius3)
                    )
                    .apply(icicleConfigurationInstance, IcicleConfiguration::new)
    );
    public final float chanceOfTallerDripstone;
    public final float chanceOfDirectionalSpread;
    public final float chanceOfSpreadRadius2;
    public final float chanceOfSpreadRadius3;

    public IcicleConfiguration(float chanceOfTallerDripstone, float chanceOfDirectionalSpread, float chanceOfSpreadRadius2, float chanceOfSpreadRadius3) {
        this.chanceOfTallerDripstone = chanceOfTallerDripstone;
        this.chanceOfDirectionalSpread = chanceOfDirectionalSpread;
        this.chanceOfSpreadRadius2 = chanceOfSpreadRadius2;
        this.chanceOfSpreadRadius3 = chanceOfSpreadRadius3;
    }
}
