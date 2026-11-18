package net.luckystudio.spelunkers_charm.entity.custom.lift;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.luckystudio.spelunkers_charm.block.custom.LiftControllerBlockEntity;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Builds a lift deck out of real blocks and turns it into a Sable sub-level. */
public final class LiftSubLevels {
    private LiftSubLevels() {}

    /** Whether Sable is running physics in this level. */
    public static boolean available(ServerLevel level) {
        return SubLevelContainer.getContainer(level) instanceof ServerSubLevelContainer;
    }

    /**
     * Places the deck where the probe lift would have stood and assembles it. The probe is a lift
     * entity from {@link LiftPlacement} that was checked but never added to the world.
     *
     * @return whether the sub-level was created
     */
    public static boolean assemble(ServerLevel level, AbstractLift probe, AbstractLift.Type type) {
        if (!available(level)) return false;

        BlockPos center = probe.blockPosition();
        int radius = LiftControllerBlockEntity.radius(type);
        BlockState deck = ModBlocks.LIFT_PART.get().defaultBlockState();
        Set<BlockPos> blocks = new HashSet<>();
        for (int dx = -radius; dx <= radius; ++dx)
            for (int dz = -radius; dz <= radius; ++dz) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!level.getBlockState(pos).canBeReplaced()) return false;
                blocks.add(pos.immutable());
            }

        for (BlockPos pos : blocks)
            if (!pos.equals(center)) level.setBlock(pos, deck, Block.UPDATE_ALL);
        level.setBlock(center, ModBlocks.LIFT_CONTROLLER.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(center) instanceof LiftControllerBlockEntity controller)
            controller.setup(type, probe.getDirection(), probe.getX(), probe.getZ());

        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        ServerSubLevel subLevel =
                bounds == null ? null : SubLevelAssemblyHelper.assembleBlocks(level, center, blocks, bounds);
        if (subLevel == null || subLevel.isRemoved()) {
            // Sable said no. Don't leave a half-built deck behind.
            for (BlockPos pos : blocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return false;
        }
        return true;
    }
}
