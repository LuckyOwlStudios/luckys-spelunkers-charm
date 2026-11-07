package net.luckystudio.spelunkers_charm.events.tremor;

import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.entity.custom.rock.AbstractThrowableRock;
import net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class TremorManager {

    /**
     * Starts a tremor at the given position, or strengthens one that is already running nearby.
     *
     * @param length       length in ticks
     * @param magnitude    strength of the tremor, clamped to the configured max magnitude
     * @param warningChance chance each nearby player hears the warning rumble when a new tremor starts
     * @param forceSpawn   skip the dimension, sky, difficulty, biome and config checks (used by the command)
     */
    public static void spawnTremor(Level level, BlockPos pos, int length, int magnitude, float warningChance, boolean forceSpawn) {
        if (forceSpawn || canTremor(level, pos)) {
            AABB searchBox = new AABB(pos).inflate(64);
            List<Tremor> tremors = level.getEntitiesOfClass(Tremor.class, searchBox);
            if (!tremors.isEmpty()) {
                // Merge into the existing tremor by keeping the stronger values instead of adding them,
                // so stacked explosions can't make tremors grow without limit.
                for (Tremor tremor : tremors) {
                    tremor.setLength(Math.max(tremor.getLength(), tremor.getLifetime() + length));
                    tremor.setMagnitude(Math.max(tremor.getMagnitude(), magnitude));
                }
            } else {
                List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchBox);
                for (ServerPlayer player : players) {
                    if (player.getRandom().nextFloat() < warningChance) {
                        player.playNotifySound(getRumbleSound(player), SoundSource.AMBIENT, 1.0F, 1.0F);
                    }
                }
                Tremor tremor = new Tremor(ModEntityType.TREMOR.get(), level);
                tremor.setLength(length);
                tremor.setMagnitude(magnitude);
                tremor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                level.addFreshEntity(tremor);
            }
        }
    }

    // This checks if the tremor can occur based on the level and position.
    static boolean canTremor(Level level, BlockPos pos) {
        // Check the config setting
        if (!SpelunkersCharmConfig.TREMORS.get()) return false;

        // Must be in the Overworld and not exposed to the sky
        if (level.dimension() != Level.OVERWORLD || level.canSeeSky(pos)) return false;

        // Must not be peaceful
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;

        ResourceKey<Biome> biomeKey = level.getBiome(pos).getKey();
        if (biomeKey == null) {
            return false;
        }

        // Tremors are not allowed in biomes in tag: NO_TREMORS
        return !level.getBiome(pos).is(ModBiomeTags.NO_TREMORS);
    }

    public static void tremorTick(Tremor tremor) {
        Level level = tremor.level();
        BlockPos pos = tremor.blockPosition();
        RandomSource random = level.getRandom();

        // Build up to full strength at the start and fade out at the end.
        int rampTicks = SpelunkersCharmConfig.TREMOR_RAMP_TIME.get() * 20;
        float step = rampTicks <= 0 ? 1.0F : 1.0F / rampTicks;
        float intensity = tremor.getIntensity();
        intensity = tremorShouldDieDown(tremor, rampTicks) ? intensity - step : intensity + step;
        tremor.setIntensity(intensity);
        intensity = tremor.getIntensity();
        if (intensity <= 0.0F) return;

        // Number of spots shaken this tick, scaled by the current intensity (randomly rounded so low intensities still do something).
        float attempts = (tremor.getMagnitude() + 2) * intensity;
        int repeatCount = Mth.floor(attempts) + (random.nextFloat() < attempts - Mth.floor(attempts) ? 1 : 0);

        // Just to help the sounds play nicer
        SoundEvent tremorSound = getTremorSound(level, pos);
        int soundInterval = tremorSound == ModSoundEvents.TREMOR_ICY.get() ? 80 : 40;

        if (tremor.tickCount % soundInterval == 0) {
            float pitch = 0.75F + random.nextFloat() * 0.5F; // 0.75 + [0.0 - 0.5] = [0.75 - 1.25]
            float volume = 0.75F * Math.max(0.25F, intensity);
            level.playSound(null, pos, tremorSound, SoundSource.AMBIENT, volume, pitch);
        }

        int radius = SpelunkersCharmConfig.TREMOR_EFFECT_RADIUS.get();
        RockBudget rockBudget = new RockBudget(level, pos, radius);

        for (int i = 0; i < repeatCount; i++) {
            Optional<BlockPos> validTopEffectPos = findValidEffectPos(level, pos, Direction.UP, radius);
            Optional<BlockPos> validBottomEffectPos = findValidEffectPos(level, pos, Direction.DOWN, radius);

            validTopEffectPos.ifPresent(blockPos -> {
                TremorBlockPosEvents.topEffectPos(level, blockPos, rockBudget);
            });

            validBottomEffectPos.ifPresent(blockPos -> {
                TremorBlockPosEvents.bottomEffectPos(level, blockPos);
            });
        }
    }

    public static boolean tremorShouldDieDown(Tremor tremor, int rampTicks) {
        return tremor.getLifetime() >= tremor.getLength() - rampTicks;
    }

    public static Optional<BlockPos> findValidEffectPos(Level level, BlockPos center, Direction direction, int radius) {
        RandomSource random = level.getRandom();
        int verticalRadius = Math.max(1, radius / 2);

        // Random offset within the effect radius
        int offsetX = random.nextInt(radius * 2 + 1) - radius;
        int offsetY = random.nextInt(verticalRadius * 2 + 1) - verticalRadius;
        int offsetZ = random.nextInt(radius * 2 + 1) - radius;

        BlockPos randomPos = center.offset(offsetX, offsetY, offsetZ);
        BlockPos.MutableBlockPos searchPos = randomPos.mutable();

        for (int i = 0; i <= 48; i++) {
            searchPos.move(direction);

            BlockState blockState = level.getBlockState(searchPos);

            if (!blockState.isAir() && !blockState.canBeReplaced()) {
                // Special case: skip certain pointed dripstone blocks
                if (blockState.getBlock() instanceof PointedDripstoneBlock) {
                    DripstoneThickness thickness = blockState.getValue(PointedDripstoneBlock.THICKNESS);
                    if (thickness != DripstoneThickness.BASE) {
                        continue;
                    }
                }

                // Check visibility: is the adjacent block in the opposite direction air?
                BlockPos adjacent = searchPos.relative(direction.getOpposite());
                BlockState adjacentState = level.getBlockState(adjacent);
                if (adjacentState.isAir()) {
                    return Optional.of(searchPos.immutable());
                }
            }
        }

        return Optional.empty(); // No valid block found
    }

    /**
     * Limits how many rocks one tremor drops: a chance per shaken ceiling, a cap per tick,
     * a cap on falling rocks already around the tremor, and a player nearby.
     */
    public static class RockBudget {
        private final Level level;
        private final BlockPos center;
        private final int radius;
        private int remainingThisTick;
        private int nearbyRocks = -1; // counted lazily, only when a rock is about to drop

        RockBudget(Level level, BlockPos center, int radius) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.remainingThisTick = SpelunkersCharmConfig.TREMOR_MAX_ROCKS_PER_TICK.get();
        }

        public boolean tryDropRock(BlockPos pos) {
            if (this.remainingThisTick <= 0) return false;
            if (this.level.getRandom().nextFloat() >= SpelunkersCharmConfig.TREMOR_ROCK_CHANCE.get()) return false;

            int playerRange = SpelunkersCharmConfig.TREMOR_ROCK_PLAYER_RANGE.get();
            if (!this.level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, playerRange)) return false;

            if (this.nearbyRocks < 0) {
                AABB box = new AABB(this.center).inflate(this.radius, Math.max(1, this.radius / 2) + 48, this.radius);
                this.nearbyRocks = this.level.getEntitiesOfClass(AbstractThrowableRock.class, box).size();
            }
            if (this.nearbyRocks >= SpelunkersCharmConfig.TREMOR_MAX_ROCKS_NEARBY.get()) return false;

            this.remainingThisTick--;
            this.nearbyRocks++;
            return true;
        }
    }

    // Get the sound event for the tremor based on the player's biome
    private static SoundEvent getRumbleSound(ServerPlayer serverPlayer) {
        Level level = serverPlayer.level();
        Holder<Biome> biomeKey = level.getBiome(serverPlayer.blockPosition());
        if (biomeKey.is(ModBiomeTags.IS_COLD_CAVE)
//                && SpelunkersCharmConfig.STONE_REPLACERS.get()
        ) {
            return ModSoundEvents.RUMBLE_ICY.get();
        } else {
            return ModSoundEvents.RUMBLE_GENERIC.get();
        }
    }

    // Get the sound event for the tremor based on the player's biome
    private static SoundEvent getTremorSound(Level level, BlockPos pos) {
        Holder<Biome> biomeKey = level.getBiome(pos);
        if (biomeKey.is(ModBiomeTags.IS_COLD_CAVE)
//                && SpelunkersCharmConfig.STONE_REPLACERS.get()
        ) {
            return ModSoundEvents.TREMOR_ICY.get();
        } else {
            return ModSoundEvents.TREMOR_GENERIC.get();
        }
    }
}
