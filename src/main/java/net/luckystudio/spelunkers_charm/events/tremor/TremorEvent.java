package net.luckystudio.spelunkers_charm.events.tremor;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TremorEvent {

    /**
     * | Y-Level | Normalized Depth | Length Range | Intensity | Chance |
     * | ------- | ---------------- | ------------ | --------- | ------ |
     * | 64+     | 0.0              | \~400 ±50    | 1         | 5%     |
     * | 0       | \~0.75           | \~700 ±50    | \~4       | \~57%  |
     * | -64     | 1.0              | 800 ±50      | 5         | 75%    |
     * @param event
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer minecraftServer = event.getServer();
        ServerLevel overworld = minecraftServer.getLevel(Level.OVERWORLD);
        if (overworld == null || overworld.getServer().getWorldData().isFlatWorld()) return;

        // Every 5 minutes
        if (minecraftServer.getTickCount() % (20 * 60 * 5) == 0) {
            for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                Random random = new Random();

                // Base chance of tremor attempt (1 in 5)
                if (random.nextFloat() < 0.2F) {
                    float normalizedDepth = getNormalizedDepth(player);
                    float depthChance = 0.05F + normalizedDepth * 0.7F;

                    int intensity = Mth.floor(1 + normalizedDepth * 4); // [1, 5]
                    int baseLength = Mth.floor(400 + normalizedDepth * 400); // [400, 800]
                    int length = baseLength + random.nextInt(101) - 50; // ±50 tick variance

                    TremorManager.spawnTremor(
                            player.level(),
                            player.blockPosition(),
                            Math.max(length, 400), // safety: never less than 400
                            intensity,
                            depthChance,
                            false
                    );
                }
            }
        }
    }

    private static float getNormalizedDepth(ServerPlayer player) {
        int y = player.getBlockY();
        int minY = player.level().getMinBuildHeight(); // typically -64
        int maxY = 64;

        return Mth.clamp((maxY - y) / (float)(maxY - minY), 0.0F, 1.0F);
    }

    /**
     *     This event is fired when an explosion occurs in the game.
     *     A Tremor is spawned at the center of the explosion if the explosion is in the Overworld or Nether and below y=32.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Vec3 center = event.getExplosion().center();
        if (center.y < 32 && (level.dimension() == Level.OVERWORLD)) {
            float force = event.getExplosion().radius();
            BlockPos pos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
            TremorManager.spawnTremor(level, pos, (int) (force * 60F), (int) force, 0.5F, false);
        }
    }
}
