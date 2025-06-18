package net.luckystudio.spelunkers_charm.worldgen.feature.custom.directional_block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class DirectionalBlockFeatureConfiguration implements FeatureConfiguration {

    public static final Codec<DirectionalBlockFeatureConfiguration> CODEC = RecordCodecBuilder.create(
            geyserConfigurationInstance -> geyserConfigurationInstance.group(
                            BlockStateProvider.CODEC.fieldOf("blockProvider").forGetter(geyserConfiguration -> geyserConfiguration.blockStateProvider),
                            BlockStateProvider.CODEC.fieldOf("supportingBlockProvider").forGetter(geyserConfiguration -> geyserConfiguration.supportingBlockProvider)
                    )
                    .apply(geyserConfigurationInstance, DirectionalBlockFeatureConfiguration::new)
    );

    public final BlockStateProvider blockStateProvider;
    public final BlockStateProvider supportingBlockProvider;

    public DirectionalBlockFeatureConfiguration(BlockStateProvider blockStateProvider, BlockStateProvider supportingBlockStateProvider) {
        this.blockStateProvider = blockStateProvider;
        this.supportingBlockProvider = supportingBlockStateProvider;
    }
}
