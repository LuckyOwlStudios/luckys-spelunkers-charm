package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpelunkersCharm.MOD_ID);

//    public static final Supplier<CreativeModeTab> SPELUNKERS_CHARM = CREATIVE_MODE_TABS.register("spelunkers_charm",
//            () -> CreativeModeTab.builder()
//                    .icon(() -> new ItemStack(ModItems.MINING_HELMET.get()))
//                    .title(Component.translatable("creative_tab.spelunkers_charm"))
//                    .displayItems((itemDisplayParameters, output) -> {
//                    })
//                    .build());

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {

            if (SpelunkersCharmConfig.STONE_REPLACERS.get()) {
                event.insertAfter(Items.CUT_RED_SANDSTONE_SLAB.getDefaultInstance(), ModBlocks.DUNESTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE.toStack(), ModBlocks.DUNESTONE_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_STAIRS.toStack(), ModBlocks.DUNESTONE_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_SLAB.toStack(), ModBlocks.DUNESTONE_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_WALL.toStack(), ModBlocks.DUNESTONE_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_BRICKS.toStack(), ModBlocks.DUNESTONE_BRICK_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_BRICK_STAIRS.toStack(), ModBlocks.DUNESTONE_BRICK_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_BRICK_SLAB.toStack(), ModBlocks.DUNESTONE_BRICK_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_BRICK_WALL.toStack(), ModBlocks.CHISELED_DUNESTONE_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.CHISELED_DUNESTONE_BRICKS.toStack(), ModBlocks.COBBLED_DUNESTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_DUNESTONE.toStack(), ModBlocks.COBBLED_DUNESTONE_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_DUNESTONE_STAIRS.toStack(), ModBlocks.COBBLED_DUNESTONE_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_DUNESTONE_SLAB.toStack(), ModBlocks.COBBLED_DUNESTONE_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

                event.insertAfter(ModBlocks.COBBLED_DUNESTONE_WALL.toStack(), ModBlocks.WILDSTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE.toStack(), ModBlocks.WILDSTONE_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_STAIRS.toStack(), ModBlocks.WILDSTONE_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_SLAB.toStack(), ModBlocks.WILDSTONE_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_WALL.toStack(), ModBlocks.WILDSTONE_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_BRICKS.toStack(), ModBlocks.WILDSTONE_BRICK_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_BRICK_STAIRS.toStack(), ModBlocks.WILDSTONE_BRICK_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_BRICK_SLAB.toStack(), ModBlocks.WILDSTONE_BRICK_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.WILDSTONE_BRICK_WALL.toStack(), ModBlocks.CHISELED_WILDSTONE_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.CHISELED_WILDSTONE_BRICKS.toStack(), ModBlocks.COBBLED_WILDSTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_WILDSTONE.toStack(), ModBlocks.COBBLED_WILDSTONE_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_WILDSTONE_STAIRS.toStack(), ModBlocks.COBBLED_WILDSTONE_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_WILDSTONE_SLAB.toStack(), ModBlocks.COBBLED_WILDSTONE_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

                event.insertAfter(ModBlocks.COBBLED_WILDSTONE_WALL.toStack(), ModBlocks.PERMAFROST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST.toStack(), ModBlocks.PERMAFROST_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_STAIRS.toStack(), ModBlocks.PERMAFROST_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_SLAB.toStack(), ModBlocks.PERMAFROST_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_WALL.toStack(), ModBlocks.PERMAFROST_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_BRICKS.toStack(), ModBlocks.PERMAFROST_BRICK_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_BRICK_STAIRS.toStack(), ModBlocks.PERMAFROST_BRICK_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_BRICK_SLAB.toStack(), ModBlocks.PERMAFROST_BRICK_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_BRICK_WALL.toStack(), ModBlocks.CHISELED_PERMAFROST_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.CHISELED_PERMAFROST_BRICKS.toStack(), ModBlocks.COBBLED_PERMAFROST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_PERMAFROST.toStack(), ModBlocks.COBBLED_PERMAFROST_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_PERMAFROST_STAIRS.toStack(), ModBlocks.COBBLED_PERMAFROST_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.COBBLED_PERMAFROST_SLAB.toStack(), ModBlocks.COBBLED_PERMAFROST_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }

            event.insertAfter(Items.AMETHYST_BLOCK.getDefaultInstance(), ModBlocks.AMETHYST_BRICKS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.AMETHYST_BRICKS.toStack(), ModBlocks.AMETHYST_BRICK_STAIRS.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.AMETHYST_BRICK_STAIRS.toStack(), ModBlocks.AMETHYST_BRICK_SLAB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.AMETHYST_BRICK_SLAB.toStack(), ModBlocks.AMETHYST_BRICK_WALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.AMETHYST_BRICK_WALL.toStack(), ModBlocks.CHISELED_AMETHYST_BLOCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.insertAfter(Items.GRAVEL.getDefaultInstance(), ModBlocks.SILT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.BLUE_ICE.getDefaultInstance(), ModBlocks.ICICLE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            if (SpelunkersCharmConfig.STONE_REPLACERS.get()) {
                event.insertAfter(Items.STONE.getDefaultInstance(), ModBlocks.DUNESTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE.toStack(), ModBlocks.PERMAFROST.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST.toStack(), ModBlocks.WILDSTONE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.COAL_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_COAL_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_COAL_ORE.toStack(), ModBlocks.PERMAFROST_COAL_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_COAL_ORE.toStack(), ModBlocks.WILDSTONE_COAL_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.IRON_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_IRON_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_IRON_ORE.toStack(), ModBlocks.PERMAFROST_IRON_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_IRON_ORE.toStack(), ModBlocks.WILDSTONE_IRON_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.COPPER_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_COPPER_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_COPPER_ORE.toStack(), ModBlocks.PERMAFROST_COPPER_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_COPPER_ORE.toStack(), ModBlocks.WILDSTONE_COPPER_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.GOLD_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_GOLD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_GOLD_ORE.toStack(), ModBlocks.PERMAFROST_GOLD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_GOLD_ORE.toStack(), ModBlocks.WILDSTONE_GOLD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.REDSTONE_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_REDSTONE_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_REDSTONE_ORE.toStack(), ModBlocks.PERMAFROST_REDSTONE_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_REDSTONE_ORE.toStack(), ModBlocks.WILDSTONE_REDSTONE_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.EMERALD_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_EMERALD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_EMERALD_ORE.toStack(), ModBlocks.PERMAFROST_EMERALD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_EMERALD_ORE.toStack(), ModBlocks.WILDSTONE_EMERALD_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.LAPIS_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_LAPIS_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_LAPIS_ORE.toStack(), ModBlocks.PERMAFROST_LAPIS_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_LAPIS_ORE.toStack(), ModBlocks.WILDSTONE_LAPIS_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(Items.DIAMOND_ORE.getDefaultInstance(), ModBlocks.DUNESTONE_DIAMOND_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.DUNESTONE_DIAMOND_ORE.toStack(), ModBlocks.PERMAFROST_DIAMOND_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.insertAfter(ModBlocks.PERMAFROST_DIAMOND_ORE.toStack(), ModBlocks.WILDSTONE_DIAMOND_ORE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }

            event.insertAfter(Items.RAW_GOLD_BLOCK.getDefaultInstance(), ModBlocks.BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.BOULDER.toStack(), ModBlocks.IRON_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.IRON_BOULDER.toStack(), ModBlocks.COPPER_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.COPPER_BOULDER.toStack(), ModBlocks.GOLD_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.GOLD_BOULDER.toStack(), ModBlocks.LUSH_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.LUSH_BOULDER.toStack(), ModBlocks.LUSH_IRON_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.LUSH_IRON_BOULDER.toStack(), ModBlocks.LUSH_COPPER_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.LUSH_COPPER_BOULDER.toStack(), ModBlocks.LUSH_GOLD_BOULDER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            event.insertAfter(Items.RED_MUSHROOM_BLOCK.getDefaultInstance(), ModBlocks.CAVE_MUSHROOM_BLOCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.RED_MUSHROOM.getDefaultInstance(), ModBlocks.CAVE_MUSHROOM.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            event.insertAfter(Items.WARPED_FUNGUS.getDefaultInstance(), ModBlocks.ROCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.ROCK.toStack(), ModBlocks.DEEPSLATE_ROCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.DEEPSLATE_ROCK.toStack(), ModBlocks.DRIPSTONE_ROCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.DRIPSTONE_ROCK.toStack(), ModBlocks.ICE_BALL.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.ICE_BALL.toStack(), ModBlocks.BASALT_ROCK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            event.insertAfter(Items.SCULK_SENSOR.getDefaultInstance(), ModBlocks.PACKED_WEB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.COBWEB.getDefaultInstance(), ModBlocks.WEB_VEIN.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.WEB_VEIN.toStack(), ModBlocks.HANGING_WEB.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.HANGING_WEB.toStack(), ModBlocks.SPIDER_EGG.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            event.insertAfter(Items.COBWEB.getDefaultInstance(), ModBlocks.DEEPSLATE_GEYSER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.DEEPSLATE_GEYSER.toStack(), ModBlocks.BASALT_GEYSER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.insertAfter(Items.TNT_MINECART.getDefaultInstance(), ModBlocks.WOODEN_LIFT_TRACK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.WOODEN_LIFT_TRACK.toStack(), ModBlocks.POWERED_LIFT_TRACK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.POWERED_LIFT_TRACK.toStack(), ModItems.SMALL_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.SMALL_LIFT.toStack(), ModItems.MEDIUM_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.MEDIUM_LIFT.toStack(), ModItems.LARGE_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.LARGE_LIFT.toStack(), ModBlocks.BLASTER.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.insertAfter(Items.TNT_MINECART.getDefaultInstance(), ModBlocks.WOODEN_LIFT_TRACK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.WOODEN_LIFT_TRACK.toStack(), ModBlocks.POWERED_LIFT_TRACK.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModBlocks.POWERED_LIFT_TRACK.toStack(), ModItems.SMALL_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.SMALL_LIFT.toStack(), ModItems.MEDIUM_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.MEDIUM_LIFT.toStack(), ModItems.LARGE_LIFT.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ModItems.LARGE_LIFT.toStack(), ModItems.MINING_HELMET.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS && event.getTab().contains(Items.SPIDER_EYE.getDefaultInstance())) {
            event.insertAfter(Items.SPIDER_EYE.getDefaultInstance(), ModBlocks.CAVE_MUSHROOM.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
