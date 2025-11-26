package net.luckystudio.spelunkers_charm.events.tremor;

import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TremorManager {

    public static void spawnTremor(Level level, BlockPos pos, int length, int intensity, float chance, boolean forceSpawn) {
        if (forceSpawn || canTremor(level, pos)) {
            AABB searchBox = new AABB(pos).inflate(64);
            List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchBox);
            List<Tremor> tremors = level.getEntitiesOfClass(Tremor.class, searchBox);
            if (!tremors.isEmpty()) {
                for (Tremor tremor : tremors) {
                    tremor.setLength(tremor.getLength() + length);
                    if (intensity > tremor.getMagnitude()) {
                        tremor.setMagnitude(tremor.getMagnitude() + intensity);
                    }
                }
            } else {
                for (ServerPlayer player : players) {
                    if (player.getRandom().nextFloat() < chance) {
                        player.playNotifySound(getRumbleSound(player), SoundSource.AMBIENT, 1.0F, 1.0F);
                    }
                }
                Tremor tremor = new Tremor(ModEntityType.TREMOR.get(), level);
                tremor.setLength(length);
                tremor.setMagnitude(intensity);
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

        // This handles the size of the tremor slowly increasing as it starts and decreasing as it ends.
        if (!tremorShouldDieDown(tremor) && tremor.getMagnitude() < 120) {tremor.incrementIntensity();}
        if (tremorShouldDieDown(tremor) && tremor.getMagnitude() > 0) {tremor.decrementIntensity();}

        int repeatCount = tremor.getMagnitude() + 2;

        // Just to help the sounds play nicer
        SoundEvent tremorSound = getTremorSound(level, pos);
        int soundInterval = tremorSound == ModSoundEvents.TREMOR_ICY ? 80 : 40;

        if (tremor.tickCount % soundInterval == 0) {
            float pitch = 0.75F + level.random.nextFloat() * 0.5F; // 0.75 + [0.0 - 0.5] = [0.75 - 1.25]
            level.playSound(null, pos, getTremorSound(level, pos), SoundSource.AMBIENT, 0.75F, pitch);
        }

        for (int i = 0; i < repeatCount; i++) {
            Optional<BlockPos> validTopEffectPos = findValidEffectPos(level, pos, Direction.UP);
            Optional<BlockPos> validBottomEffectPos = findValidEffectPos(level, pos, Direction.DOWN);

            validTopEffectPos.ifPresent(blockPos -> {
                TremorBlockPosEvents.topEffectPos(level, blockPos);
            });

            validBottomEffectPos.ifPresent(blockPos -> {
                TremorBlockPosEvents.bottomEffectPos(level, blockPos);
            });
        }
    }

    public static boolean tremorShouldDieDown(Tremor tremor) {
        return tremor.getLifetime() >= tremor.getLength() - 120;
    }

    public static Optional<BlockPos> findValidEffectPos(Level level, BlockPos playerPos, Direction direction) {
        Random random = new Random();

        // Generate random offset within a 32-block radius
        int offsetX = random.nextInt(65) - 32;
        int offsetY = random.nextInt(33) - 16;
        int offsetZ = random.nextInt(65) - 32;

        BlockPos randomPos = playerPos.offset(offsetX, offsetY, offsetZ);
        BlockPos.MutableBlockPos searchPos = randomPos.mutable();

        for (int i = 0; i <= 48; i++) {
            searchPos.move(direction);

            BlockState blockState = level.getBlockState(searchPos);

            if (!blockState.isAir() && !blockState.canBeReplaced()) {
                // Special case: skip certain pointed dripstone blocks
                if (blockState.getBlock() instanceof PointedDripstoneBlock dripstone) {
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
