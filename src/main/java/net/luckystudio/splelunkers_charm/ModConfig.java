package net.luckystudio.splelunkers_charm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SpelunkersCharm.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // World Generation Settings
    static {
        BUILDER.push("worldgen");

        TREMORS = BUILDER
                .comment("Whether the tremor event can occur")
                .define("tremors", true);

        ROCKS = BUILDER
                .comment("Whether the rocks generate around the world")
                .define("rocks", true);

        DEEPSLATE_ROCKS = BUILDER
                .comment("Whether the deepslate rocks generate around the world")
                .define("deepslateRocks", true);

        DRIPSTONE_ROCKS = BUILDER
                .comment("Whether the dripstone rocks generate around the world")
                .define("dripstoneRocks", true);

        BASALT_ROCKS = BUILDER
                .comment("Whether the basalt rocks generate around the world")
                .define("basaltRocks", true);

        GEYSERS = BUILDER
                .comment("Whether the geysers generate around the world")
                .define("geysers", true);

        BUILDER.pop();
    }

    // Miscellaneous Settings
    static {
        BUILDER.push("misc");

        MAGIC_NUMBER = BUILDER
                .comment("A magic number")
                .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

        MAGIC_NUMBER_INTRODUCTION = BUILDER
                .comment("What you want the introduction message to be for the magic number")
                .define("magicNumberIntroduction", "The magic number is... ");

        BUILDER.pop();
    }

    // Item Settings
    static {
        BUILDER.push("items");

        ITEM_STRINGS = BUILDER
                .comment("A list of items to log on common setup.")
                .defineListAllowEmpty("items", List.of(Items.APPLE.toString()), ModConfig::validateItemName);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Configuration Values
    public static final ModConfigSpec.BooleanValue TREMORS;
    public static final ModConfigSpec.BooleanValue ROCKS;
    public static final ModConfigSpec.BooleanValue DEEPSLATE_ROCKS;
    public static final ModConfigSpec.BooleanValue DRIPSTONE_ROCKS;
    public static final ModConfigSpec.BooleanValue BASALT_ROCKS;
    public static final ModConfigSpec.BooleanValue GEYSERS;
    public static final ModConfigSpec.IntValue MAGIC_NUMBER;
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS;

    public static boolean tremors;
    public static boolean rocks;
    public static boolean deepslateRocks;
    public static boolean dripstoneRocks;
    public static boolean basaltRocks;
    public static boolean geysers;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        tremors = TREMORS.get();
        rocks = ROCKS.get();
        deepslateRocks = DEEPSLATE_ROCKS.get();
        dripstoneRocks = DRIPSTONE_ROCKS.get();
        basaltRocks = BASALT_ROCKS.get();
        geysers = GEYSERS.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
    }
}

