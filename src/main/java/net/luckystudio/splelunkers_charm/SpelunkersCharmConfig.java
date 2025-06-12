package net.luckystudio.splelunkers_charm;

import net.neoforged.neoforge.common.ModConfigSpec;

// This class is responsible for managing the configuration of the Spelunkers Charm mod.
// I used https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/Configuration.java as a reference for this class.
//@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class SpelunkersCharmConfig {

    // === Spec Builders ===
    public static ModConfigSpec COMMON_CONFIG;
    public static ModConfigSpec CLIENT_CONFIG;

    // === CATEGORY WORLD GENERATION CONFIGURATION ===
    public static String CATEGORY_WORLD_GENERATION = "worldGeneration";
    public static ModConfigSpec.BooleanValue STONE_REPLACERS;
    public static ModConfigSpec.BooleanValue TREMORS;
    public static ModConfigSpec.BooleanValue ROCKS;
    public static ModConfigSpec.BooleanValue GEYSERS;
    public static ModConfigSpec.BooleanValue BOULDERS;

    // === CLIENT CONFIGURATION ===
    public static String CATEGORY_CLIENT = "client";
    public static ModConfigSpec.DoubleValue TREMOR_VOLUME;
    public static ModConfigSpec.DoubleValue TREMOR_PARTICLE_INTENSITY;

    static {
        ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

        COMMON_BUILDER.comment("World Generation Settings").push(CATEGORY_WORLD_GENERATION);

        STONE_REPLACERS = COMMON_BUILDER
                .comment("The entire overworld stone generation is replaced with a custom stone replacer system.")
                .define("stoneReplacers", false);

        TREMORS = COMMON_BUILDER
                .comment("Whether the tremor event can occur")
                .define("tremors", true);

        ROCKS = COMMON_BUILDER
                .comment("Do rocks generate around the world")
                .define("rocks", true);

        GEYSERS = COMMON_BUILDER
                .comment("Whether the geysers generate around the world")
                .define("geysers", true);

        BOULDERS = COMMON_BUILDER
                .comment("Whether the geysers generate around the world")
                .define("geysers", true);

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();

        ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

        CLIENT_BUILDER.comment("Client settings").push(CATEGORY_CLIENT);
        TREMOR_VOLUME = CLIENT_BUILDER.comment("Should the hunger bar have a gilded overlay when the player has the Nourishment effect?")
                .defineInRange("tremorVolume", 0.75, 0.0,1.0);
        TREMOR_PARTICLE_INTENSITY = CLIENT_BUILDER.comment("How many particles should be spawned when a tremor occurs. This can help performance on lower-end systems.")
                .defineInRange("tremorParticleIntensity", 1.0, 0.0, 1.0);
        CLIENT_BUILDER.pop();

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}

