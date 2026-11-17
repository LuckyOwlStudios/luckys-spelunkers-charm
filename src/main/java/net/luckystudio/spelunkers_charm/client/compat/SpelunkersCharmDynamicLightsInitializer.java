package net.luckystudio.spelunkers_charm.client.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;

/**
 * LambDynamicLights support, loaded by LambDynamicLights itself through the entrypoint in neoforge.mods.toml,
 * so none of this is touched when it isn't installed.
 * The mining helmet casts a spotlight where it points.
 */
public class SpelunkersCharmDynamicLightsInitializer implements DynamicLightsInitializer {

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        MiningHelmetSpotlights.enable(context.dynamicLightBehaviorManager());
    }

    @Override
    @SuppressWarnings("removal")
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        // Old entrypoint, replaced by the one above.
    }
}
