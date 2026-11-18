package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client effects at the exposed rims, rather than inside the opaque deck. */
@OnlyIn(Dist.CLIENT)
public final class LiftClientEffects {
    private LiftClientEffects() {}

    public static void spawnGearSparks(AbstractLift lift, double dy) {
        Minecraft minecraft = Minecraft.getInstance();
        if ((lift.tickCount & 1) != 0
                || minecraft.level != lift.level()
                || minecraft.getCameraEntity() == null
                || minecraft.getCameraEntity().distanceToSqr(lift) > 48 * 48) return;
        double verticalSign = -Math.signum(dy);
        for (BlockPos guide : lift.getGuidePositions()) {
            double dx = lift.getX() - guide.getX() - 0.5;
            double dz = lift.getZ() - guide.getZ() - 0.5;
            Direction inward =
                    Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.EAST : Direction.WEST)
                            : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            Direction tangent = inward.getClockWise();
            for (int n = 0; n < 2; n++) {
                double scatter = (lift.getRandom().nextDouble() - 0.5) * 0.22;
                double x =
                        guide.getX()
                                + 0.5
                                + inward.getStepX() * 0.57
                                + tangent.getStepX() * scatter;
                double z =
                        guide.getZ()
                                + 0.5
                                + inward.getStepZ() * 0.57
                                + tangent.getStepZ() * scatter;
                // Both exposed rims are used so a player above the deck can
                // also see sparks. Every spark travels opposite to the lift.
                double rim = n == 0 ? verticalSign : -verticalSign;
                double y = lift.getY() + 0.5 + rim * 0.70;
                Particle spark =
                        minecraft.particleEngine.createParticle(
                                ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
                if (spark != null) {
                    // Vanilla's provider quarters velocity and lives 2-3 ticks.
                    // Keep its sprite, with enough travel and lifetime to see it.
                    spark.setLifetime(8 + lift.getRandom().nextInt(4));
                    spark.setColor(1.0F, 0.65F + lift.getRandom().nextFloat() * 0.15F, 0.15F);
                    double sideways = (lift.getRandom().nextDouble() - 0.5) * 0.07;
                    spark.setParticleSpeed(
                            inward.getStepX() * 0.025 + tangent.getStepX() * sideways,
                            verticalSign * (0.14 + lift.getRandom().nextDouble() * 0.06),
                            inward.getStepZ() * 0.025 + tangent.getStepZ() * sideways);
                }
            }
        }
    }
}
