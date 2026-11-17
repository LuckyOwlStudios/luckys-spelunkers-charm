package net.luckystudio.spelunkers_charm.client.helmet;

import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared helpers for the mining helmet's flashlight: who has a lit lamp, where the lamp sits on the model,
 * and how far the beam travels before it hits a block.
 */
public final class MiningHelmetBeam {
    // Lamp lens center in MiningHelmetModel space, in model pixels from the head pivot.
    // The lamp box is addBox(-2, -10, -6, 4, 4, 2): its front face is at z = -6 and its center at y = -8.
    public static final float LAMP_UP_PX = 8.0F;
    public static final float LAMP_FORWARD_PX = 6.05F; // a hair in front of the lens so the beam doesn't z-fight it

    private MiningHelmetBeam() {}

    /** True if the entity wears a mining helmet whose lamp is on. */
    public static boolean hasLitLamp(LivingEntity living) {
        if (living.isSpectator() || living.isRemoved()) return false;
        ItemStack head = living.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.MINING_HELMET.get()) && head.getOrDefault(ModDataComponents.LIGHT.get(), true);
    }

    /**
     * Distance from {@code from} along {@code direction} (normalized) to the first block in the way,
     * or {@code maxDistance} if nothing is hit. Plants, water and other blocks without collision don't stop the beam.
     */
    public static double traceDistance(Level level, Vec3 from, Vec3 direction, double maxDistance, LivingEntity source) {
        BlockHitResult hit = trace(level, from, direction, maxDistance, source);
        if (hit.getType() == HitResult.Type.MISS) return maxDistance;
        return Math.min(maxDistance, hit.getLocation().distanceTo(from));
    }

    public static BlockHitResult trace(Level level, Vec3 from, Vec3 direction, double maxDistance, LivingEntity source) {
        Vec3 to = from.add(direction.scale(maxDistance));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
    }
}
