package net.luckystudio.spelunkers_charm.worldgen.feature.custom.ore;

import com.mojang.serialization.Codec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ModOreFeature extends OreFeature {

    public ModOreFeature(Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        if (!SpelunkersCharmConfig.STONE_REPLACERS.get()) return false;
        return super.place(context);
    }
}
