package net.luckystudio.spelunkers_charm.client.compat;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.client.helmet.MiningHelmetBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Keeps one {@link MiningHelmetSpotlight} for every nearby entity wearing a lit mining helmet: you, other players,
 * and mobs or armor stands wearing one. Only loaded when LambDynamicLights is installed, through
 * {@link SpelunkersCharmDynamicLightsInitializer}.
 */
public final class MiningHelmetSpotlights {
    // Helmets farther than this from the camera don't cast a spotlight.
    private static final double MAX_DISTANCE = 64.0;
    // At most this many spotlights at once, nearest first in practice since far ones are skipped.
    private static final int MAX_SPOTLIGHTS = 8;

    private static final Int2ObjectMap<MiningHelmetSpotlight> SPOTLIGHTS = new Int2ObjectOpenHashMap<>();
    private static DynamicLightBehaviorManager manager;
    private static ClientLevel trackedLevel;

    private MiningHelmetSpotlights() {}

    static void enable(DynamicLightBehaviorManager behaviorManager) {
        if (manager != null) return;
        manager = behaviorManager;
        NeoForge.EVENT_BUS.addListener(MiningHelmetSpotlights::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            clear();
            trackedLevel = level;
        }
        Entity camera = minecraft.getCameraEntity();
        if (level == null || camera == null || !SpelunkersCharmConfig.MINING_HELMET_SPOTLIGHT.get()) {
            clear();
            return;
        }
        if (minecraft.isPaused()) return;

        double range = SpelunkersCharmConfig.MINING_HELMET_SPOTLIGHT_RANGE.get();
        IntSet seen = new IntOpenHashSet();
        for (Entity entity : level.entitiesForRendering()) {
            if (seen.size() >= MAX_SPOTLIGHTS) break;
            if (!(entity instanceof LivingEntity living) || !MiningHelmetBeam.hasLitLamp(living)) continue;
            if (living.isSleeping() || living.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;

            MiningHelmetSpotlight.Beam beam = MiningHelmetSpotlight.trace(living, range);
            MiningHelmetSpotlight spotlight = SPOTLIGHTS.get(living.getId());
            if (spotlight == null) {
                spotlight = new MiningHelmetSpotlight(beam);
                SPOTLIGHTS.put(living.getId(), spotlight);
                manager.add(spotlight);
            } else {
                spotlight.update(beam);
            }
            seen.add(living.getId());
        }

        // Drop spotlights of helmets that were taken off, switched off, or went out of range.
        var iterator = SPOTLIGHTS.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<MiningHelmetSpotlight> entry = iterator.next();
            if (!seen.contains(entry.getIntKey())) {
                manager.remove(entry.getValue());
                iterator.remove();
            }
        }
    }

    private static void clear() {
        if (SPOTLIGHTS.isEmpty()) return;
        for (MiningHelmetSpotlight spotlight : SPOTLIGHTS.values()) {
            manager.remove(spotlight);
        }
        SPOTLIGHTS.clear();
    }
}
