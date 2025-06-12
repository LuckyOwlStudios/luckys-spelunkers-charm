package net.luckystudio.splelunkers_charm.worldgen.feature.custom.replace;

import com.mojang.serialization.Codec;
import net.luckystudio.splelunkers_charm.SpelunkersCharmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import javax.annotation.Nullable;

public class ReplaceMultiBlockBlobFeature extends Feature<ReplaceMultiBlockSphereConfiguration> {

    public ReplaceMultiBlockBlobFeature(Codec<ReplaceMultiBlockSphereConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ReplaceMultiBlockSphereConfiguration> context) {
        if (!SpelunkersCharmConfig.STONE_REPLACERS.get()) return false;
        ReplaceMultiBlockSphereConfiguration config = context.config();
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        TagKey<Block> blockTag = config.blocksToReplace;

        BlockPos original = context.origin();
        int clampedY = Math.max(world.getMinBuildHeight() + 1, Math.min(original.getY(), world.getMaxBuildHeight() - 1));
        BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(original.getX(), clampedY, original.getZ());

        BlockPos targetPos = findTarget(world, origin, blockTag);
        if (targetPos == null) {
            return false;
        }
        System.out.println("Pos is not null: ");
        int radiusX = config.radius().sample(random);
        int radiusY = config.radius().sample(random);
        int radiusZ = config.radius().sample(random);
        int maxRadius = Math.max(radiusX, Math.max(radiusY, radiusZ));
        boolean placedAny = false;

        for (BlockPos pos : BlockPos.withinManhattan(targetPos, radiusX, radiusY, radiusZ)) {
            if (pos.distManhattan(targetPos) > maxRadius) continue;

            BlockState current = world.getBlockState(pos);
            if (current.is(blockTag)) {
                this.setBlock(world, pos, config.replaceState);
                placedAny = true;
            }
        }

        return placedAny;
    }

    @Nullable
    private static BlockPos findTarget(LevelAccessor level, BlockPos.MutableBlockPos pos, TagKey<Block> targets) {
        while (pos.getY() > level.getMinBuildHeight() + 1) {
            BlockState state = level.getBlockState(pos);
            if (state.is(targets)) {
                return pos.immutable();
            }
            pos.move(Direction.DOWN);
        }
        return null;
    }
}