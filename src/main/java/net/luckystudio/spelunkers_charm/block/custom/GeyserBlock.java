package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.block.custom.blaster.BlasterUtil;
import net.luckystudio.spelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GeyserBlock extends Block {
    public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);
    public static final EnumProperty<GeyserState> GEYSER_STATE = ModBlockStateProperties.GEYSER_STATE;
    public static final EnumProperty<GeyserType> GEYSER_TYPE = ModBlockStateProperties.GEYSER_TYPE;
    public static final IntegerProperty DURATION = IntegerProperty.create("duration", 0, 100);

    public GeyserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(GEYSER_STATE, GeyserState.DORMANT)
                .setValue(GEYSER_TYPE, GeyserType.NONE)
                .setValue(DURATION, 0));
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GEYSER_STATE, GEYSER_TYPE, DURATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockState = this.defaultBlockState();
        return blockState.setValue(GEYSER_TYPE, determineType(pContext.getLevel().getBlockState(pContext.getClickedPos().below())));
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        BlockPos blockPosBelow = pPos.below();
        return pState.setValue(GEYSER_TYPE, determineType(pLevel.getBlockState(blockPosBelow)));
    }

    private static GeyserType determineType(BlockState neighborState) {
        if (neighborState.getBlock() == Blocks.LAVA) {
            return GeyserType.LAVA;
        } else if (neighborState.getBlock() == Blocks.WATER) {
            return GeyserType.WATER;
        } else {
            return GeyserType.NONE;
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        Item item = pStack.getItem();
        if (item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE) {
            if (item == Items.FLINT_AND_STEEL) {
                pStack.hurtAndBreak(1, pPlayer, LivingEntity.getSlotForHand(pHand));
            } else {
                pStack.shrink(1);
            }
            tryAndStartCharging(pLevel, pPos, pState);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(GEYSER_STATE) == GeyserState.DORMANT && pState.getValue(GEYSER_TYPE) != GeyserType.NONE;
    }

    @Override
    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        tryAndStartCharging(pLevel, pPos, pState);
        super.randomTick(pState, pLevel, pPos, pRandom);
    }

    // Also used by the tremor event to start charging the geyser
    public void tryAndStartCharging(Level world, BlockPos pos, BlockState state) {
        // Will need to add
//        if (!SpelunkersCharmConfig.ACTIVE_GEYSERS.get()) return;
        if (world.getBlockState(pos.below()).getBlock() == Blocks.LAVA || world.getBlockState(pos.below()).getBlock() == Blocks.WATER) {
            world.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.CHARGING).setValue(DURATION, 0), 3);
            world.playSound(null, pos, ModSoundEvents.GEYSER_CHARGE.get(), SoundSource.BLOCKS, 1.0f, 0.5F);
            world.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        super.tick(pState, pLevel, pPos, pRandom);
        System.out.println("Tick");
        int duration = pState.getValue(DURATION);
        GeyserState geyserState = pState.getValue(GEYSER_STATE);
        switch (geyserState) {
            case CHARGING -> handleChargingState(pState, pLevel, pPos, duration);
            case ERUPTING -> handleActiveState(pState, pLevel, pPos, duration);
            default -> {}
        }
    }

    private void handleChargingState(BlockState state, ServerLevel serverLevel, BlockPos pos, int duration) {
        if (duration < 60) {
            serverLevel.setBlock(pos, state.setValue(DURATION, duration + 1), 3);
        } else {
            start(state, serverLevel, pos);
        }
        serverLevel.scheduleTick(pos, this, 1);
    }

    private void start(BlockState state, ServerLevel serverWorld, BlockPos pos) {
        serverWorld.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.ERUPTING).setValue(DURATION, 0), 3);

        double xx = pos.getX() + 0.5;
        double yy = pos.getY() + 1.1;
        double zz = pos.getZ() + 0.5;
        int particleCount = 20;
        double speed = 0.1;

        GeyserType geyserType = state.getValue(GEYSER_TYPE);
        if (geyserType == GeyserType.LAVA) {
            serverWorld.playSound(null, xx, yy, zz, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 0.1f);
            serverWorld.sendParticles(
                    ParticleTypes.LAVA,
                    xx, yy, zz,
                    16, // Number of particles (0 to spawn one with velocity)
                    0.0, 0.0, 0.0,
                    0.5
            );
            spawnParticleRing(serverWorld, xx, yy, zz, particleCount, speed, ParticleTypes.CAMPFIRE_COSY_SMOKE);
        } else if (geyserType == GeyserType.WATER) {
            serverWorld.playSound(null, xx, yy, zz, ModSoundEvents.GEYSER_ERUPT_WATER.get(), SoundSource.BLOCKS, 0.5F, 0.1f);
            spawnParticleRing(serverWorld, xx, yy, zz, particleCount, speed, ParticleTypes.CLOUD);
        }
    }

    private void spawnParticleRing(ServerLevel world, double x, double y, double z, int count, double speed, SimpleParticleType particleType) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double xSpeed = Math.cos(angle) * speed;
            double zSpeed = Math.sin(angle) * speed;

            world.sendParticles(
                    particleType,
                    x, y, z,
                    0, // Number of particles (0 to spawn one with velocity)
                    xSpeed, 0.0, zSpeed,
                    0.5
            );
        }
    }

    private void handleActiveState(BlockState state, ServerLevel level, BlockPos pos, int duration) {
        GeyserType geyserType = state.getValue(GEYSER_TYPE);
        if (duration < 100) {
            BlasterUtil.shoot(level, pos, geyserType, Direction.UP);
            level.setBlock(pos, state.setValue(DURATION, duration + 1), 3);
            level.scheduleTick(pos, this, 1);
        } else {
            level.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.DORMANT).setValue(DURATION, 0), 3);
        }
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        super.animateTick(pState, pLevel, pPos, pRandom);
        if (pState.getValue(GEYSER_TYPE) != GeyserType.NONE && pState.getValue(GEYSER_STATE) == GeyserState.DORMANT) {
            SimpleParticleType particleEffect = getAmbientParticleEffect(pState);
            if (particleEffect != null) {
                for (int i = 0; i < 3; i++) {
                    double d = (double)pPos.getX() + pRandom.nextDouble();
                    double e = (double)pPos.getY() + pRandom.nextDouble() * 0.5 + 1.0;
                    double f = (double)pPos.getZ() + pRandom.nextDouble();
                    pLevel.addAlwaysVisibleParticle(particleEffect, true, d, e, f, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private SimpleParticleType getAmbientParticleEffect(BlockState state) {
        GeyserType geyserType = state.getValue(GEYSER_TYPE);
        return switch (geyserType) {
            case LAVA -> ParticleTypes.LARGE_SMOKE;
            case WATER -> ParticleTypes.CLOUD;
            default -> null;
        };
    }
}
