package net.luckystudio.splelunkers_charm.worldgen.feature.custom.spread_blob;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import javax.annotation.Nullable;

public class ReplaceSpreadingBlobsFeature extends Feature<ReplaceSpreadingSphereConfiguration> {

    public ReplaceSpreadingBlobsFeature(Codec<ReplaceSpreadingSphereConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ReplaceSpreadingSphereConfiguration> context) {
        ReplaceSpreadingSphereConfiguration config = context.config();
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        Block targetBlock = config.targetState.getBlock();

        BlockPos origin = findTarget(
                world,
                context.origin().mutable().clamp(Direction.Axis.Y, world.getMinBuildHeight() + 1, world.getMaxBuildHeight() - 1),
                targetBlock
        );

        if (origin == null) {
            return false;
        }

        int radiusX = config.radius().sample(random);
        int radiusY = config.radius().sample(random);
        int radiusZ = config.radius().sample(random);
        int maxRadius = Math.max(radiusX, Math.max(radiusY, radiusZ));
        boolean placedAny = false;

        for (BlockPos pos : BlockPos.withinManhattan(origin, radiusX, radiusY, radiusZ)) {
            if (pos.distManhattan(origin) > maxRadius) {
                continue;
            }

            BlockState currentState = world.getBlockState(pos);
            if (!currentState.is(targetBlock)) continue;

            boolean isEdge = false;

            // Check 6 directions to see if any neighbor would NOT be replaced (is outside the blob)
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (neighbor.distManhattan(origin) > maxRadius || !world.getBlockState(neighbor).is(targetBlock)) {
                    isEdge = true;
                    break;
                }
            }

            BlockState toPlace = isEdge ? config.outerState : config.innerState;
            this.setBlock(world, pos, toPlace);
            placedAny = true;
        }

        return placedAny;
    }

    @Nullable
    private static BlockPos findTarget(LevelAccessor level, BlockPos.MutableBlockPos pos, Block targetBlock) {
        while (pos.getY() > level.getMinBuildHeight() + 1) {
            if (level.getBlockState(pos).is(targetBlock)) {
                return pos.immutable(); // return immutable to prevent side effects
            }
            pos.move(Direction.DOWN);
        }
        return null;
    }
}
