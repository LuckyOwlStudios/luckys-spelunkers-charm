package net.luckystudio.splelunkers_charm.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ModParticleTypes {

    public static void spawnParticlesToServer(Level level, double posX, double posY, double posZ, int count, double offsetX, double offsetY, double offsetZ, double speed, ParticleOptions... particles) {
        for (ParticleOptions particle : particles) {
            if (particle == null || !(level instanceof ServerLevel serverLevel)) continue;

            serverLevel.sendParticles(
                    particle,
                    posX,
                    posY,
                    posZ,
                    count,      // count
                    offsetX, offsetY, offsetZ, // spread (X, Y, Z)
                    speed     // speed
            );
        }
    }

    public static void spawnSquareBorderParticles(Level level, ParticleOptions particle, BlockPos pos, float radius, int particleCount, float speed) {
        if (radius < 1 || particleCount <= 0) return;

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.1;
        double centerZ = pos.getZ() + 0.5;

        float sideLength = radius * 2 + 1;
        float perimeterLength = sideLength * 4 - 4;

        float edgeLength = perimeterLength / 4f;

        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / particleCount;
            double distance = t * perimeterLength;

            double x;
            double z;

            if (distance < edgeLength) {
                // Top edge (left to right)
                x = centerX - radius + distance;
                z = centerZ - radius;
            } else if (distance < edgeLength * 2) {
                // Right edge (top to bottom)
                x = centerX + radius;
                z = centerZ - radius + (distance - edgeLength);
            } else if (distance < edgeLength * 3) {
                // Bottom edge (right to left)
                x = centerX + radius - (distance - edgeLength * 2);
                z = centerZ + radius;
            } else {
                // Left edge (bottom to top)
                x = centerX - radius;
                z = centerZ + radius - (distance - edgeLength * 3);
            }

            // Calculate outward motion vector
            double dx = x - centerX;
            double dz = z - centerZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            double motionX = (dx / length) * speed;
            double motionZ = (dz / length) * speed;

            level.addAlwaysVisibleParticle(
                    particle,
                    x, centerY, z,
                    motionX, 0.0, motionZ
            );
        }
    }
}
