package net.luckystudio.spelunkers_charm.events;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.cave_borer.CaveBorer;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

// Mod bus events for our living entities: attributes and natural spawn rules
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityType.CAVE_BORER.get(), CaveBorer.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntityType.CAVE_BORER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CaveBorer::checkCaveBorerSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
