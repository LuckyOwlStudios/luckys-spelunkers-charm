package net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.worldgen.biomes.ModBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBiomeTagProvider extends BiomeTagsProvider {
    public ModBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, SpelunkersCharm.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        super.addTags(provider);

        this.tag(ModBiomeTags.NO_TREMORS)
                .add(Biomes.LUSH_CAVES)
                .add(Biomes.DEEP_DARK)
                .addOptional(ModBiomes.SPIDER_CAVE.location())
        ;

        this.tag(ModBiomeTags.IS_COLD_CAVE)
                .add(Biomes.FROZEN_OCEAN)
                .add(Biomes.DEEP_FROZEN_OCEAN)
                .add(Biomes.JAGGED_PEAKS)
                // Removed, as it's not a cold biome, it's just very tall!
//                .add(Biomes.FROZEN_PEAKS)
                .add(Biomes.GROVE)
                .add(Biomes.SNOWY_SLOPES)
                .add(Biomes.SNOWY_TAIGA)
                .add(Biomes.FROZEN_RIVER)
                .add(Biomes.SNOWY_BEACH)
                .add(Biomes.SNOWY_PLAINS)
                .add(Biomes.ICE_SPIKES)
        ;

        this.tag(ModBiomeTags.IS_HOT_CAVE)
                .add(Biomes.DESERT)
                .add(Biomes.SAVANNA)
                .add(Biomes.WINDSWEPT_SAVANNA)
                .add(Biomes.SAVANNA_PLATEAU)
                .add(Biomes.BADLANDS)
                .add(Biomes.WOODED_BADLANDS)
                .add(Biomes.ERODED_BADLANDS)
        ;

        this.tag(ModBiomeTags.IS_JUNGLE_CAVE)
                .add(Biomes.JUNGLE)
                .add(Biomes.SPARSE_JUNGLE)
                .add(Biomes.BAMBOO_JUNGLE)
        ;
    }
}
