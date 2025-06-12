package net.luckystudio.splelunkers_charm.worldgen.biomes;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.*;

public class ModRegion extends Region {

    public ModRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper)
    {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();
        // Overlap Vanilla's parameters with our own for our cave biomes.
        // The parameters for this biome are chosen arbitrarily.
        new ParameterPointListBuilder()
                .temperature(Temperature.WARM, Temperature.HOT) // Narrow to one temperature slice
                .humidity(Humidity.HUMID, Humidity.WET)
                .continentalness(Continentalness.INLAND)
                .erosion(Erosion.EROSION_3) // Narrow to one erosion slice
                .depth(Depth.UNDERGROUND)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING) // Just one slice
                .build().forEach(point -> builder.add(point, ModBiomes.SPIDER_CAVE));

        // Add our points to the mapper
        builder.build().forEach(mapper);
    }
}
