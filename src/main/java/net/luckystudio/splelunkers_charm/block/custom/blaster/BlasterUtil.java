package net.luckystudio.splelunkers_charm.block.custom.blaster;

import net.luckystudio.splelunkers_charm.block.util.enums.GeyserType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlasterUtil {

    public static void shoot(Level level, BlockPos pos, GeyserType geyserType, Direction facing) {
        if (geyserType == GeyserType.NONE) return;

        boolean isLava = geyserType == GeyserType.LAVA;
        boolean isWater = geyserType == GeyserType.WATER;
        boolean isSnow = geyserType == GeyserType.SNOW;

        if (level.getGameTime() % 5 == 0) {
            level.playSound(null, pos.above(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        ParticleOptions mainParticle = isLava ? ParticleTypes.FLAME :
                isWater ? ParticleTypes.CLOUD :
                        ParticleTypes.SNOWFLAKE;

        ParticleOptions secondaryParticle = isLava ? ParticleTypes.SMOKE :
                ParticleTypes.DRIPPING_WATER;

        Vec3 blockCenter = Vec3.atCenterOf(pos);
        Vec3 directionVec = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        Vec3 particleOrigin = blockCenter.add(directionVec.scale(0.6));
        Vec3 particleVelocity = directionVec.scale(0.25);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 2; i++) { // Increase to 5 particles per tick for visibility
                double offsetX = 0, offsetY = 0, offsetZ = 0;

                // Randomize offset perpendicular to facing axis
                switch (facing.getAxis()) {
                    case X -> {
                        offsetY = (level.random.nextDouble() - 0.5) * 0.75;
                        offsetZ = (level.random.nextDouble() - 0.5) * 0.75;
                    }
                    case Y -> {
                        offsetX = (level.random.nextDouble() - 0.5) * 0.75;
                        offsetZ = (level.random.nextDouble() - 0.5) * 0.75;
                    }
                    case Z -> {
                        offsetX = (level.random.nextDouble() - 0.5) * 0.75;
                        offsetY = (level.random.nextDouble() - 0.5) * 0.75;
                    }
                }

                double px = particleOrigin.x + offsetX;
                double py = particleOrigin.y + offsetY;
                double pz = particleOrigin.z + offsetZ;

                serverLevel.sendParticles(mainParticle, px, py, pz, 0, particleVelocity.x, particleVelocity.y, particleVelocity.z, 1.0);
                serverLevel.sendParticles(secondaryParticle, px, py, pz, 0, particleVelocity.x, particleVelocity.y, particleVelocity.z, 1.0);
            }
        }

        // Damage logic
        DamageSource damageSource = isLava ? level.damageSources().source(DamageTypes.ON_FIRE) :
                isSnow ? level.damageSources().source(DamageTypes.FREEZE) :
                        null;

        AABB damageBox = new AABB(pos).move(directionVec).expandTowards(directionVec.scale(5));
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageBox);
        for (LivingEntity entity : targets) {
            if (!isWater && damageSource != null) {
                entity.hurt(damageSource, 2.0F);
            }
            entity.addDeltaMovement(directionVec.scale(0.1));

            if (isLava) {
                entity.setRemainingFireTicks(40);
            } else if (isSnow) {
                entity.setTicksFrozen(40);
            }
        }
    }
}
