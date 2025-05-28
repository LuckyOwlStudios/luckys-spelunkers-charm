package net.luckystudio.splelunkers_charm.worldgen.feature.custom.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class GeyserConfiguration implements FeatureConfiguration {

    public static final Codec<GeyserConfiguration> CODEC = RecordCodecBuilder.create(
            geyserConfigurationInstance -> geyserConfigurationInstance.group(
                            BlockStateProvider.CODEC.fieldOf("geyserProvider").forGetter(geyserConfiguration -> geyserConfiguration.geyserProvider),
                            BlockStateProvider.CODEC.fieldOf("blockProvider").forGetter(geyserConfiguration -> geyserConfiguration.blockProvider),
                            BlockStateProvider.CODEC.fieldOf("liquidProvider").forGetter(geyserConfiguration -> geyserConfiguration.liquidProvider)
                    )
                    .apply(geyserConfigurationInstance, GeyserConfiguration::new)
    );

    public final BlockStateProvider geyserProvider;
    public final BlockStateProvider blockProvider;
    public final BlockStateProvider liquidProvider;

    public GeyserConfiguration(BlockStateProvider geyserProvider, BlockStateProvider blockProvider, BlockStateProvider liquidProvider) {
        this.geyserProvider = geyserProvider;
        this.blockProvider = blockProvider;
        this.liquidProvider = liquidProvider;
    }
}
