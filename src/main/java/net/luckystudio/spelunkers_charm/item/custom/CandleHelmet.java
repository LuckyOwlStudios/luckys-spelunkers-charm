package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class CandleHelmet extends ArmorItem {

    private final ResourceLocation TEXTURE = SpelunkersCharm.id("textures/armor/candle_helmet.png");

    public CandleHelmet(Properties properties) {
        super(ArmorMaterials.CHAIN, Type.HELMET, properties);
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof LivingEntity living)) return;
        if (living.getItemBySlot(EquipmentSlot.HEAD) != stack) return;

        // Check environmental conditions and handle candle state
        handleCandleEnvironment(stack, level, living);

        // Handle visual effects only if candle is lit
        if (isCandleLit(stack) && level.isClientSide) {
            spawnCandleParticles(level, living);
        }
    }

    private void handleCandleEnvironment(ItemStack stack, Level level, LivingEntity entity) {
        boolean isInWater = entity.isUnderWater();
        boolean isInRain = isEntityInRain(entity, level);
        boolean currentlyLit = isCandleLit(stack);

        // Check if candle should be extinguished
        if (currentlyLit && (isInWater || isInRain)) {
            extinguishCandle(stack);
            playExtinguishSound(level, entity);
        }
        // Check if candle should be relit (when leaving water/rain)
        else if (!currentlyLit && !isInWater && !isInRain) {
            relightCandle(stack);
            playRelightSound(level, entity);
        }
    }

    private void spawnCandleParticles(Level level, LivingEntity living) {
        Random random = new Random();
        if (random.nextFloat() >= 0.1f) return; // 10% chance to spawn particles

        Vec3 position = living.trackingPosition();
        float yOffset = living.getBbHeight() + 0.25f;

        // Spawn candle flame particles
        level.addParticle(ParticleTypes.SMALL_FLAME,
                position.x, position.y + yOffset, position.z,
                0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE,
                position.x, position.y + yOffset, position.z,
                0, 0.01, 0);
    }

    private boolean isEntityInRain(LivingEntity entity, Level level) {
        if (!(entity instanceof Player)) return false;

        BlockPos entityPos = entity.blockPosition();
        Biome biome = level.getBiome(entityPos).value();

        // Check if it's raining and entity is exposed to sky
        return level.isRaining()
                && biome.hasPrecipitation()
                && level.canSeeSky(entityPos.above())
                && level.isRainingAt(entityPos);
    }

    private boolean isCandleLit(ItemStack stack) {
        // If no LIGHT component exists, default to lit (true)
        return stack.getOrDefault(ModDataComponents.LIGHT.get(), true);
    }

    private void extinguishCandle(ItemStack stack) {
        stack.set(ModDataComponents.LIGHT.get(), false);
    }

    private void relightCandle(ItemStack stack) {
        stack.set(ModDataComponents.LIGHT.get(), true);
    }

    private void playExtinguishSound(Level level, LivingEntity entity) {
        if (level.isClientSide) return; // Only play on server side

        // Play fire extinguish sound
        level.playSound(
                null, // null = play for all players
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.8f,  // Volume
                1.2f   // Pitch (slightly higher for small candle)
        );

        // Also play a subtle lava extinguish (sizzle) for water
        if (entity.isUnderWater()) {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.PLAYERS,
                    0.4f,  // Quieter
                    2.0f   // High pitch for small sizzle
            );
        }
    }

    private void playRelightSound(Level level, LivingEntity entity) {
        if (level.isClientSide) return; // Only play on server side

        // Play flint and steel sound for relighting
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS,
                0.6f,  // Volume
                1.4f   // Pitch (higher for small candle)
        );
    }

    /**
     * Public method to manually toggle candle state (for use with flint & steel)
     */
    public static void toggleCandle(ItemStack stack) {
        boolean currentlyLit = stack.getOrDefault(ModDataComponents.LIGHT.get(), true);
        stack.set(ModDataComponents.LIGHT.get(), !currentlyLit);
    }

    /**
     * Public method to check if a candle helmet is lit
     */
    public static boolean isCandleHelmetLit(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LIGHT.get(), true);
    }

    /**
     * Public method to manually extinguish candle (for other interactions)
     */
    public static void extinguishCandleHelmet(ItemStack stack) {
        stack.set(ModDataComponents.LIGHT.get(), false);
    }

    /**
     * Public method to manually light candle (for other interactions)
     */
    public static void lightCandleHelmet(ItemStack stack) {
        stack.set(ModDataComponents.LIGHT.get(), true);
    }
}