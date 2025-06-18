package net.luckystudio.spelunkers_charm.events.tremor;

import com.google.common.collect.Maps;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.block.custom.GeyserBlock;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.entity.custom.rock.AbstractThrowableRock;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Random;

public class TremorBlockPosEvents {

    private static final Map<Block, AbstractThrowableRock.Type> ROCK_TO_DROP = Util.make(Maps.newHashMap(), map -> {
        map.put(Blocks.STONE, AbstractThrowableRock.Type.ROCK);
        map.put(Blocks.DEEPSLATE, AbstractThrowableRock.Type.DEEPSLATE_ROCK);
        map.put(Blocks.DRIPSTONE_BLOCK, AbstractThrowableRock.Type.DRIPSTONE_ROCK);
        map.put(ModBlocks.WILDSTONE.get(), AbstractThrowableRock.Type.DRIPSTONE_ROCK);
        map.put(Blocks.BASALT, AbstractThrowableRock.Type.BASALT_ROCK);
        map.put(ModBlocks.PERMAFROST.get(), AbstractThrowableRock.Type.ICE_BALL);
        map.put(Blocks.PACKED_ICE, AbstractThrowableRock.Type.ICE_BALL);
        map.put(Blocks.ICE, AbstractThrowableRock.Type.ICE_BALL);
    });

    public static void topEffectPos(Level level, BlockPos pos) {
        breakBlockEffectAt(level, pos);
        BlockState state = level.getBlockState(pos);

        // If the block is a falling block, schedule a tick to fall
        if (state.getBlock() instanceof FallingBlock fallingBlock) {
            level.scheduleTick(pos, fallingBlock, 1);
            return;
        }

        // If the block is a falling block like dripstone, break it so the stack of blocks fall
        if (state.getBlock() instanceof PointedDripstoneBlock) {
            level.destroyBlock(pos, true);
            return;
        }

        // If the block is a stone block, drop a rock
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            dropRock(level, state, pos);
        }
    }

    public static void bottomEffectPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GeyserBlock) {
            level.scheduleTick(pos, state.getBlock(), 1);
        }
        breakBlockEffectAt(level, pos);
    }

    // CLIENT SIDE ONLY
    private static void breakBlockEffectAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Random random = new Random();
        double particleAmount = SpelunkersCharmConfig.TREMOR_PARTICLE_INTENSITY.get() / 2;
        if ((random.nextFloat() < (float) particleAmount)) {
            level.levelEvent(null, 2001, pos, Block.getId(state));
        }
    }

    private static void dropRock(LevelAccessor levelAccessor, BlockState state, BlockPos pos) {
        AbstractThrowableRock.Type rockType = ROCK_TO_DROP.getOrDefault(state.getBlock(), AbstractThrowableRock.Type.ROCK);
        if (rockType != null) {
            ServerLevel serverLevel = (ServerLevel) levelAccessor;
            AbstractThrowableRock rock = AbstractThrowableRock.createRock(serverLevel, pos.getX(), pos.getY(), pos.getZ(), rockType, null);
            rock.setPos(pos.getX() + 0.5, pos.getY() - 0.1F, pos.getZ() + 0.5);
            levelAccessor.addFreshEntity(rock);
        }
    }
}
