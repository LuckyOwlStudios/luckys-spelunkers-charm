package net.luckystudio.spelunkers_charm.events.tremor;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;


@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TremorEvent {

    /**
     * Natural tremors. Every check interval, each Overworld player rolls a chance that scales with depth,
     * from shallowChance at y=64 and above to deepChance at the bottom of the world.
     * Magnitude and length also scale with depth. All values come from the "tremors" config section.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer minecraftServer = event.getServer();
        ServerLevel overworld = minecraftServer.getLevel(Level.OVERWORLD);
        if (overworld == null || overworld.getServer().getWorldData().isFlatWorld()) return;
        if (!SpelunkersCharmConfig.TREMORS.get()) return;

        int interval = SpelunkersCharmConfig.TREMOR_CHECK_INTERVAL.get() * 20;
        if (minecraftServer.getTickCount() % interval != 0) return;

        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            RandomSource random = player.getRandom();
            float normalizedDepth = getNormalizedDepth(player);

            double shallowChance = SpelunkersCharmConfig.TREMOR_SHALLOW_CHANCE.get();
            double deepChance = SpelunkersCharmConfig.TREMOR_DEEP_CHANCE.get();
            if (random.nextDouble() >= Mth.lerp(normalizedDepth, shallowChance, deepChance)) continue;

            int maxMagnitude = SpelunkersCharmConfig.TREMOR_MAX_MAGNITUDE.get();
            int magnitude = 1 + Math.round(normalizedDepth * (maxMagnitude - 1)); // [1, maxMagnitude]

            int shallowLength = SpelunkersCharmConfig.TREMOR_SHALLOW_LENGTH.get() * 20;
            int deepLength = SpelunkersCharmConfig.TREMOR_DEEP_LENGTH.get() * 20;
            int baseLength = Mth.floor(Mth.lerp(normalizedDepth, shallowLength, deepLength));
            int length = baseLength + random.nextInt(101) - 50; // ±50 tick variance

            TremorManager.spawnTremor(
                    player.level(),
                    player.blockPosition(),
                    Math.max(length, 100), // safety: never less than 5 seconds
                    magnitude,
                    1.0F, // always warn players about a natural tremor
                    false
            );
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
     *     A Tremor is spawned at the center of the explosion if the explosion is in the Overworld, below the configured Y level (default 32).
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Vec3 center = event.getExplosion().center();
        if (!SpelunkersCharmConfig.EXPLOSION_TREMORS.get()) return;
        if (center.y < SpelunkersCharmConfig.EXPLOSION_TREMOR_MAX_Y.get() && (level.dimension() == Level.OVERWORLD)) {
            float force = event.getExplosion().radius();
            int length = (int) (force * SpelunkersCharmConfig.EXPLOSION_TREMOR_SECONDS_PER_RADIUS.get() * 20);
            if (length <= 0) return;
            BlockPos pos = BlockPos.containing(center);
            TremorManager.spawnTremor(level, pos, length, Math.max(1, (int) force), 0.5F, false);
        }
    }
}
