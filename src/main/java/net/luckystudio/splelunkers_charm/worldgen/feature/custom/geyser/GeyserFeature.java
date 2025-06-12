package net.luckystudio.splelunkers_charm.worldgen.feature.custom.geyser;

import com.mojang.serialization.Codec;
import net.luckystudio.splelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

// Copy this class from the HugeBrownMushroomFeature class in the Minecraft source code
public class GeyserFeature extends Feature<GeyserConfiguration> {

    public GeyserFeature(Codec<GeyserConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GeyserConfiguration> context) {
        if (!SpelunkersCharmConfig.GEYSERS.get()) return false;
        WorldGenLevel worldgenlevel = context.level();
        BlockPos blockpos = context.origin();
        GeyserConfiguration geyserConfiguration = context.config();
        this.setBlock(worldgenlevel, blockpos, geyserConfiguration.geyserProvider.getState(RandomSource.create(), blockpos));
        this.setBlock(worldgenlevel, blockpos.north().below(), geyserConfiguration.blockProvider.getState(RandomSource.create(), blockpos.north().below()));
        this.setBlock(worldgenlevel, blockpos.south().below(), geyserConfiguration.blockProvider.getState(RandomSource.create(), blockpos.south().below()));
        this.setBlock(worldgenlevel, blockpos.east().below(), geyserConfiguration.blockProvider.getState(RandomSource.create(), blockpos.east().below()));
        this.setBlock(worldgenlevel, blockpos.west().below(), geyserConfiguration.blockProvider.getState(RandomSource.create(), blockpos.west().below()));
        this.setBlock(worldgenlevel, blockpos.below().below(), geyserConfiguration.liquidProvider.getState(RandomSource.create(), blockpos.below().below()));
        this.setBlock(worldgenlevel, blockpos.below(), geyserConfiguration.liquidProvider.getState(RandomSource.create(), blockpos.below()));
        return true;
    }
}
