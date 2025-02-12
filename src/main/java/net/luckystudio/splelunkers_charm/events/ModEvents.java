package net.luckystudio.splelunkers_charm.events;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.entity.custom.tremor.TremorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    /**
     *     This event is fired when an explosion occurs in the game.
     *     A Tremor is spawned at the center of the explosion if the explosion is in the Overworld or Nether and below y=32.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Vec3 center = event.getExplosion().center();
        if (center.y < 32 && (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER)) {
            float force = event.getExplosion().radius();
            BlockPos pos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
            ResourceKey<Biome> biomeKey = level.getBiome(pos).getKey();
            System.out.println(biomeKey);
            TremorManager.spawnTremor(level, pos, (int) (force * 60F), (int) force);
        }
    }

    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        TremorManager.spawnTremor(level, pos, 200, 1);
    }
}
