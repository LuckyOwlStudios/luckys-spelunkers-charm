package net.luckystudio.splelunkers_charm.worldgen.feature.custom.spread_blob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class ReplaceSpreadingSphereConfiguration implements FeatureConfiguration {

    public static final Codec<ReplaceSpreadingSphereConfiguration> CODEC = RecordCodecBuilder.create(
            spreadingSphereConfigurationInstance -> spreadingSphereConfigurationInstance.group(
                            BlockState.CODEC.fieldOf("target").forGetter(configuration -> configuration.targetState),
                            BlockState.CODEC.fieldOf("state").forGetter(configuration -> configuration.innerState),
                            BlockState.CODEC.fieldOf("outer_state").forGetter(configuration -> configuration.outerState),
                            IntProvider.codec(0, 12).fieldOf("radius").forGetter(configuration -> configuration.radius)
                    )
                    .apply(spreadingSphereConfigurationInstance, ReplaceSpreadingSphereConfiguration::new)
    );
    public final BlockState targetState;
    public final BlockState innerState;
    public final BlockState outerState;
    private final IntProvider radius;

    public ReplaceSpreadingSphereConfiguration(BlockState targetState, BlockState innerState, BlockState outerState, IntProvider radius) {
        this.targetState = targetState;
        this.innerState = innerState;
        this.outerState = outerState;
        this.radius = radius;
    }

    public IntProvider radius() {
        return this.radius;
    }
}
