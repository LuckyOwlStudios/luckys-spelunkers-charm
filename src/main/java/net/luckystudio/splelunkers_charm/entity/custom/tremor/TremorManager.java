package net.luckystudio.splelunkers_charm.entity.custom.tremor;

import net.luckystudio.splelunkers_charm.ModConfig;
import net.luckystudio.splelunkers_charm.entity.ModEntityType;
import net.luckystudio.splelunkers_charm.sound.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TremorManager {

    public static void spawnTremor(Level level, BlockPos pos, int length, int intensity) {
        if (ModConfig.TREMORS.get()) {
            if (canTremor(level, pos)) {
                AABB searchBox = new AABB(pos).inflate(64);
                List<Tremor> tremors = level.getEntitiesOfClass(Tremor.class, searchBox);
                if (!tremors.isEmpty()) {
                    for (Tremor tremor : tremors) {
                        tremor.setLength(tremor.getLength() + length);
                        if (intensity > tremor.getMagnitude()) {
                            tremor.setMagnitude(tremor.getMagnitude() + intensity);
                        }
                    }
                } else {
                    Tremor tremor = new Tremor(ModEntityType.TREMOR.get(), level);
                    tremor.setLength(length);
                    tremor.setMagnitude(intensity);
                    tremor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(tremor);
                }
                System.out.println("Tremor spawned at " + pos);
            }
        }
    }

    private static boolean canTremor(Level level, BlockPos pos) {
        ResourceKey<Biome> biomeKey = level.getBiome(pos).getKey();
        assert biomeKey != null;
        return !(biomeKey.equals(Biomes.LUSH_CAVES) || biomeKey.equals(Biomes.DEEP_DARK));
    }

    public static void tremorTick(Tremor tremor) {
        Level level = tremor.level();
        BlockPos pos = tremor.blockPosition();

        // This handles the magnitude of the tremor slowly increasing as it starts and decreasing as it ends.
        if (!tremorShouldDieDown(tremor) && tremor.getMagnitude() < 120) {tremor.incrementIntensity(tremor);}
        if (tremorShouldDieDown(tremor) && tremor.getMagnitude() > 0) {tremor.decrementIntensity(tremor);}

        float intensity = tremor.getIntensity();

        float triggerChance = intensity / 120.0F;

        int repeatCount = tremor.getMagnitude() + 2;
        Random random = new Random();

        if (random.nextFloat() < triggerChance) {
            for (int i = 0; i < repeatCount; i++) {
                Optional<BlockPos> validTopEffectPos = findValidEffectPos(level, pos, Direction.UP);
                Optional<BlockPos> validBottomEffectPos = findValidEffectPos(level, pos, Direction.DOWN);

                validTopEffectPos.ifPresent(blockPos -> breakBlockEffectAt(level, blockPos));

                validBottomEffectPos.ifPresent(blockPos -> breakBlockEffectAt(level, blockPos));
            }
        }

        AABB search = new AABB(pos).inflate(64);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, search);

        for (ServerPlayer serverPlayerEntity : players) {
            if (tremor.getLifetime() % 40 == 0 && !tremorShouldDieDown(tremor)) {
                playTremorSound(serverPlayerEntity, pos);
            }
        }
        System.out.println(tremor.getLifetime() + " " + tremor.getLength());
    }

    public static boolean tremorShouldDieDown(Tremor tremor) {
        return tremor.getLifetime() >= tremor.getLength() - 120;
    }

    public static Optional<BlockPos> findValidEffectPos(Level level, BlockPos playerPos, Direction direction) {
        Random random = new Random();

        // Generate random offset within a 32-block radius
        int offsetX = random.nextInt(65) - 32;  // Range: -32 to 32
        int offsetY = random.nextInt(33) - 16;
        int offsetZ = random.nextInt(65) - 32;

        BlockPos randomPos = playerPos.offset(offsetX, offsetY, offsetZ); // Random horizontal position near player
        BlockPos.MutableBlockPos searchPos = randomPos.mutable();

        // Search upwards for the nearest non-replaceable block
        for (int y = 0; y <= 48; y++) {
            searchPos.move(direction);

            BlockState blockState = level.getBlockState(searchPos);
            if (!blockState.isAir() && !blockState.canBeReplaced()) {
                return Optional.of(searchPos.immutable());
            }
        }
        return Optional.empty(); // No valid block found
    }

    private static void breakBlockEffectAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Spawn block particle effect
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                0.5F, 0.5F, 0.5F);

        // Play block break sound
        level.playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private static void playTremorSound(ServerPlayer serverPlayer, BlockPos pos) {
        Random random = new Random();
        serverPlayer.connection.send(
                new ClientboundSoundPacket(
                        (Holder<SoundEvent>) ModSoundEvents.TREMOR,            // Sound event
                        SoundSource.NEUTRAL,         // Sound category
                        pos.getX(),                  // Sound position X
                        pos.getY(),                  // Sound position Y
                        pos.getZ(),                  // Sound position Z
                        3.0F,                        // Fixed volume
                        1.0F + (random.nextFloat() - 0.5F) / 2,  // Pitch variation (-0.25 to +0.25)
                        random.nextLong()
                )
        );
    }
}
