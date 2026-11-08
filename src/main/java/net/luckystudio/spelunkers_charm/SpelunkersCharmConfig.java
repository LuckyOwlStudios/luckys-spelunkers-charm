package net.luckystudio.spelunkers_charm;

import net.luckystudio.spelunkers_charm.block.custom.boulder.BoulderOre;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

// This class is responsible for managing the configuration of the Spelunkers Charm mod.
// I used https://github.com/vectorwing/FarmersDelight/blob/1.21/src/main/java/vectorwing/farmersdelight/common/Configuration.java as a reference for this class.
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
    public static ModConfigSpec.BooleanValue ACTIVE_GEYSERS;
    public static ModConfigSpec.BooleanValue BOULDERS;
    public static final Map<BoulderOre, ModConfigSpec.BooleanValue> MODDED_BOULDERS = new EnumMap<>(BoulderOre.class);

    // === CATEGORY TREMORS CONFIGURATION ===
    public static String CATEGORY_TREMORS = "tremors";
    public static ModConfigSpec.IntValue TREMOR_CHECK_INTERVAL;
    public static ModConfigSpec.DoubleValue TREMOR_SHALLOW_CHANCE;
    public static ModConfigSpec.DoubleValue TREMOR_DEEP_CHANCE;
    public static ModConfigSpec.IntValue TREMOR_SHALLOW_LENGTH;
    public static ModConfigSpec.IntValue TREMOR_DEEP_LENGTH;
    public static ModConfigSpec.IntValue TREMOR_MAX_LENGTH;
    public static ModConfigSpec.IntValue TREMOR_MAX_MAGNITUDE;
    public static ModConfigSpec.IntValue TREMOR_WARNING_TIME;
    public static ModConfigSpec.IntValue TREMOR_RAMP_TIME;
    public static ModConfigSpec.IntValue TREMOR_EFFECT_RADIUS;
    public static ModConfigSpec.DoubleValue TREMOR_ROCK_CHANCE;
    public static ModConfigSpec.IntValue TREMOR_MAX_ROCKS_PER_TICK;
    public static ModConfigSpec.IntValue TREMOR_MAX_ROCKS_NEARBY;
    public static ModConfigSpec.IntValue TREMOR_ROCK_PLAYER_RANGE;
    public static ModConfigSpec.BooleanValue EXPLOSION_TREMORS;
    public static ModConfigSpec.IntValue EXPLOSION_TREMOR_MAX_Y;
    public static ModConfigSpec.DoubleValue EXPLOSION_TREMOR_SECONDS_PER_RADIUS;

    // === CLIENT CONFIGURATION ===
    public static String CATEGORY_CLIENT = "client";
    public static ModConfigSpec.DoubleValue TREMOR_VOLUME;

    static {
        ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

        COMMON_BUILDER.comment("World Generation Settings").push(CATEGORY_WORLD_GENERATION);

        STONE_REPLACERS = COMMON_BUILDER
                .comment("The entire overworld stone generation is replaced with a custom stone replacer system.")
                .gameRestart()
                .define("stoneReplacers", false);

        TREMORS = COMMON_BUILDER
                .comment("Whether the tremor event can occur")
                .define("tremors", true);

        ROCKS = COMMON_BUILDER
                .comment("Whether small rock piles (stone, deepslate, dripstone and basalt rocks) generate around the world")
                .define("rocks", true);

        GEYSERS = COMMON_BUILDER
                .comment("Whether the geysers generate around the world")
                .define("geysers", true);

        ACTIVE_GEYSERS = COMMON_BUILDER
                .comment("Whether geysers can erupt. Turn this off to keep geysers as decoration only")
                .define("activeGeysers", true);

        BOULDERS = COMMON_BUILDER
                .comment("Whether boulders generate in caves")
                .define("boulders", true);

        COMMON_BUILDER.comment("Ore boulders for ores added by other mods. They only generate when a mod adds the ore",
                "(found through the c:storage_blocks/raw_<ore> tag), so these do nothing without such a mod.").push("moddedBoulders");
        for (BoulderOre ore : BoulderOre.values()) {
            if (!ore.isModded()) continue;
            MODDED_BOULDERS.put(ore, COMMON_BUILDER
                    .comment("Whether " + ore.getName() + " boulders generate when a mod adds " + ore.getName())
                    .define(ore.getName(), true));
        }
        COMMON_BUILDER.pop();

        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Tremor Settings (only used when worldGeneration.tremors is true)").push(CATEGORY_TREMORS);

        TREMOR_CHECK_INTERVAL = COMMON_BUILDER
                .comment("How often, in seconds, each player is checked for a natural tremor")
                .defineInRange("checkIntervalSeconds", 600, 10, 3600);

        TREMOR_SHALLOW_CHANCE = COMMON_BUILDER
                .comment("Chance of a natural tremor per check when the player is at y=64 or higher")
                .defineInRange("shallowChance", 0.01, 0.0, 1.0);

        TREMOR_DEEP_CHANCE = COMMON_BUILDER
                .comment("Chance of a natural tremor per check at the bottom of the world. Values in between scale with depth")
                .defineInRange("deepChance", 0.15, 0.0, 1.0);

        TREMOR_SHALLOW_LENGTH = COMMON_BUILDER
                .comment("Length in seconds of a natural tremor at y=64 or higher")
                .defineInRange("shallowLengthSeconds", 20, 5, 300);

        TREMOR_DEEP_LENGTH = COMMON_BUILDER
                .comment("Length in seconds of a natural tremor at the bottom of the world")
                .defineInRange("deepLengthSeconds", 40, 5, 300);

        TREMOR_MAX_LENGTH = COMMON_BUILDER
                .comment("Hard cap on how long any tremor can last, in seconds, including when tremors stack")
                .defineInRange("maxLengthSeconds", 60, 5, 600);

        TREMOR_MAX_MAGNITUDE = COMMON_BUILDER
                .comment("Highest magnitude a tremor can reach. Natural tremors scale from 1 at y=64 to this at the bottom of the world")
                .defineInRange("maxMagnitude", 5, 1, 20);

        TREMOR_WARNING_TIME = COMMON_BUILDER
                .comment("Seconds between the warning rumble and the tremor starting")
                .defineInRange("warningSeconds", 5, 0, 60);

        TREMOR_RAMP_TIME = COMMON_BUILDER
                .comment("Seconds a tremor takes to build up to full strength, and to fade out at the end")
                .defineInRange("rampSeconds", 6, 0, 60);

        TREMOR_EFFECT_RADIUS = COMMON_BUILDER
                .comment("Horizontal radius in blocks around the tremor where blocks shake and rocks fall. Vertical range is half of this")
                .defineInRange("effectRadius", 32, 4, 64);

        TREMOR_ROCK_CHANCE = COMMON_BUILDER
                .comment("Chance that a shaken stone ceiling drops a rock")
                .defineInRange("rockChance", 0.15, 0.0, 1.0);

        TREMOR_MAX_ROCKS_PER_TICK = COMMON_BUILDER
                .comment("Most rocks a single tremor can drop in one tick")
                .defineInRange("maxRocksPerTick", 1, 0, 20);

        TREMOR_MAX_ROCKS_NEARBY = COMMON_BUILDER
                .comment("A tremor stops dropping rocks while this many falling rocks already exist within its effect radius")
                .defineInRange("maxRocksNearby", 20, 0, 500);

        TREMOR_ROCK_PLAYER_RANGE = COMMON_BUILDER
                .comment("Rocks only drop within this many blocks of a player")
                .defineInRange("rockPlayerRange", 32, 4, 128);

        EXPLOSION_TREMORS = COMMON_BUILDER
                .comment("Whether underground explosions cause tremors")
                .define("explosionTremors", true);

        EXPLOSION_TREMOR_MAX_Y = COMMON_BUILDER
                .comment("Explosions must be below this Y level to cause a tremor")
                .defineInRange("explosionMaxY", 32, -64, 320);

        EXPLOSION_TREMOR_SECONDS_PER_RADIUS = COMMON_BUILDER
                .comment("Seconds of tremor per block of explosion radius (TNT has a radius of 4)")
                .defineInRange("explosionSecondsPerRadius", 3.0, 0.0, 30.0);

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();

        ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

        CLIENT_BUILDER.comment("Client settings").push(CATEGORY_CLIENT);
//        TREMOR_VOLUME = CLIENT_BUILDER.comment("The volume of tremors")
//                .defineInRange("tremorVolume", 0.75, 0.0,1.0);
        CLIENT_BUILDER.pop();

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}

