package net.luckystudio.spelunkers_charm;

import net.luckystudio.spelunkers_charm.block.custom.blaster.ModMenuTypes;
import net.luckystudio.spelunkers_charm.init.*;
import net.luckystudio.spelunkers_charm.datagen.DataGenerators;
import net.luckystudio.spelunkers_charm.item.potion.ModPotions;
import net.luckystudio.spelunkers_charm.worldgen.biomes.ModRegion;
import net.luckystudio.spelunkers_charm.worldgen.biomes.ModSurfaceRuleData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SpelunkersCharm.MOD_ID)
public class SpelunkersCharm
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "spelunkers_charm";

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    // The constructor for the mod class is the first code run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SpelunkersCharm(IEventBus modEventBus, ModContainer modContainer) {

        // Register the commonSetup method for mod-loading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModPotions.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntityType.register(modEventBus);
        ModEntityType.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        ModFeature.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Needed for advanced structures
        ModStructureProcessorType.register(modEventBus);

        // Register the modded items to vanilla creative tab
        modEventBus.addListener(ModCreativeModeTabs::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, SpelunkersCharmConfig.COMMON_CONFIG);
        modContainer.registerConfig(ModConfig.Type.CLIENT, SpelunkersCharmConfig.CLIENT_CONFIG);

        modEventBus.addListener(DataGenerators::gatherData);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
        {
            // Weights are kept intentionally low as we add minimal biomes
            Regions.register(new ModRegion(SpelunkersCharm.id("spelunkers_charm"), 1));
            // Register our surface rules
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, MOD_ID, ModSurfaceRuleData.cave());
        });
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
