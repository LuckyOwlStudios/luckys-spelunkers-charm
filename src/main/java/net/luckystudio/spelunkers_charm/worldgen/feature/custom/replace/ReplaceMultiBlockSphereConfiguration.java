package net.luckystudio.spelunkers_charm.worldgen.feature.custom.replace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class ReplaceMultiBlockSphereConfiguration implements FeatureConfiguration {

    public static final Codec<ReplaceMultiBlockSphereConfiguration> CODEC = RecordCodecBuilder.create(
            p_68048_ -> p_68048_.group(
                            BlockState.CODEC.fieldOf("state").forGetter(replaceMultiBlockSphereConfiguration -> replaceMultiBlockSphereConfiguration.replaceState),
                            TagKey.hashedCodec(Registries.BLOCK).fieldOf("blocksToReplace").forGetter(p_204869_ -> p_204869_.blocksToReplace),
                            IntProvider.codec(0, 12).fieldOf("radius").forGetter(replaceMultiBlockSphereConfiguration -> replaceMultiBlockSphereConfiguration.radius)
                    )
                    .apply(p_68048_, ReplaceMultiBlockSphereConfiguration::new)
    );
    public final BlockState replaceState;
    public final TagKey<Block> blocksToReplace;
    private final IntProvider radius;

    public ReplaceMultiBlockSphereConfiguration(BlockState replaceState, TagKey<Block> blocksToReplace, IntProvider radius) {
        this.replaceState = replaceState;
        this.blocksToReplace = blocksToReplace;
        this.radius = radius;
    }

    public IntProvider radius() {
        return this.radius;
    }
}
