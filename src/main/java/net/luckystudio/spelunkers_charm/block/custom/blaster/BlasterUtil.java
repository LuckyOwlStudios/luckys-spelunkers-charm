package net.luckystudio.spelunkers_charm.block.custom.blaster;

import net.luckystudio.spelunkers_charm.block.util.enums.GeyserType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared jet logic for blasters and erupting geysers. Called every tick on both sides:
 * the client spawns the particles locally (no particle packets), the server plays the sound,
 * and both sides push entities (the client side is what moves the local player smoothly).
 */
public final class BlasterUtil {
    private static final int PARTICLES_PER_TICK = 2;
    private static final double PARTICLE_SPREAD = 0.75;
    private static final double PARTICLE_SPEED = 0.25;
    private static final double RANGE = 5.0;
    private static final double PUSH = 0.1;
    private static final float DAMAGE = 2.0F;
    private static final int EFFECT_TICKS = 40;

    private BlasterUtil() {}

    public static void shoot(Level level, BlockPos pos, GeyserType type, Direction facing) {
        if (type == GeyserType.NONE) return;

        if (level.isClientSide) {
            spawnParticles(level, pos, type, facing);
        } else if (level.getGameTime() % 5 == 0) {
            level.playSound(null, pos.above(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 0.75F);
        }

        affectEntities(level, pos, type, facing);
    }

    private static void spawnParticles(Level level, BlockPos pos, GeyserType type, Direction facing) {
        ParticleOptions main = switch (type) {
            case LAVA -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.CLOUD;
            default -> ParticleTypes.SNOWFLAKE;
        };
        ParticleOptions secondary = type == GeyserType.LAVA ? ParticleTypes.SMOKE : ParticleTypes.DRIPPING_WATER;

        double vx = facing.getStepX() * PARTICLE_SPEED;
        double vy = facing.getStepY() * PARTICLE_SPEED;
        double vz = facing.getStepZ() * PARTICLE_SPEED;
        double originX = pos.getX() + 0.5 + facing.getStepX() * 0.6;
        double originY = pos.getY() + 0.5 + facing.getStepY() * 0.6;
        double originZ = pos.getZ() + 0.5 + facing.getStepZ() * 0.6;
        RandomSource random = level.random;

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            // Spread the particles across the face the jet comes out of
            double a = (random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double b = (random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double x = originX, y = originY, z = originZ;
            switch (facing.getAxis()) {
                case X -> { y += a; z += b; }
                case Y -> { x += a; z += b; }
                case Z -> { x += a; y += b; }
            }
            level.addParticle(main, x, y, z, vx, vy, vz);
            level.addParticle(secondary, x, y, z, vx, vy, vz);
        }
    }

    private static void affectEntities(Level level, BlockPos pos, GeyserType type, Direction facing) {
        Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal());
        AABB area = new AABB(pos).move(direction).expandTowards(direction.scale(RANGE));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);
        if (targets.isEmpty()) return;

        DamageSource damage = switch (type) {
            case LAVA -> level.damageSources().onFire();
            case SNOW -> level.damageSources().freeze();
            default -> null;
        };
        Vec3 push = direction.scale(PUSH);

        for (LivingEntity entity : targets) {
            if (damage != null) {
                entity.hurt(damage, DAMAGE);
            }
            entity.addDeltaMovement(push);
            if (type == GeyserType.LAVA) {
                entity.setRemainingFireTicks(EFFECT_TICKS);
            } else if (type == GeyserType.SNOW) {
                entity.setTicksFrozen(EFFECT_TICKS);
            }
        }
    }
}
