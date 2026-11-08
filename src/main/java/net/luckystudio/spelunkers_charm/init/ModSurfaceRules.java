package net.luckystudio.spelunkers_charm.init;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.worldgen.biomes.surface.BiomeTagCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Registers the codecs for our custom surface rule pieces, so the rules can be (de)serialized with the noise settings.
public class ModSurfaceRules {
    public static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> CONDITIONS =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, SpelunkersCharm.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends SurfaceRules.ConditionSource>, MapCodec<BiomeTagCondition>> BIOME_TAG =
            CONDITIONS.register("biome_tag", () -> BiomeTagCondition.CODEC.codec());

    public static void register(IEventBus eventBus) {
        CONDITIONS.register(eventBus);
    }
}
