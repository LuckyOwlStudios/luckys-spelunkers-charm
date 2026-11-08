package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserType;
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A geyser sits on top of lava or water and cycles DORMANT -> CHARGING -> ERUPTING -> DORMANT.
 * <p>
 * Geysers generate naturally and often in clusters, so the block is built to cost nothing while dormant:
 * <ul>
 *     <li>Phase timing lives in {@link GeyserBlockEntity}, not in the block state, so a cycle only changes
 *     the block three times instead of sending a block update every tick.</li>
 *     <li>The block entity only ticks while charging or erupting. Dormant geysers have no ticker.</li>
 *     <li>Eruption particles are spawned on the client, so the server sends no particle packets.</li>
 * </ul>
 */
public class GeyserBlock extends Block implements EntityBlock {
    public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);
    public static final EnumProperty<GeyserState> GEYSER_STATE = ModBlockStateProperties.GEYSER_STATE;
    public static final EnumProperty<GeyserType> GEYSER_TYPE = ModBlockStateProperties.GEYSER_TYPE;

    /** Ticks spent charging before the geyser erupts. */
    public static final int CHARGE_TICKS = 60;
    /** Ticks the geyser keeps erupting. */
    public static final int ERUPTION_TICKS = 100;

    /** Block event sent to clients when a geyser erupts, so they spawn the burst locally. */
    private static final int ERUPT_EVENT = 1;
    private static final int BURST_LAVA_PARTICLES = 16;
    private static final int BURST_RING_PARTICLES = 20;
    private static final double BURST_RING_SPEED = 0.05;
    private static final int AMBIENT_PARTICLES = 3;

    public GeyserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(GEYSER_STATE, GeyserState.DORMANT)
                .setValue(GEYSER_TYPE, GeyserType.NONE));
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GEYSER_STATE, GEYSER_TYPE);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Type: what the geyser shoots depends on the fluid below it
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return this.defaultBlockState().setValue(GEYSER_TYPE, typeFor(below));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Only the block below decides the type, so ignore updates from every other side
        return direction == Direction.DOWN ? state.setValue(GEYSER_TYPE, typeFor(neighborState)) : state;
    }

    private static GeyserType typeFor(BlockState below) {
        if (below.is(Blocks.LAVA)) return GeyserType.LAVA;
        if (below.is(Blocks.WATER)) return GeyserType.WATER;
        return GeyserType.NONE;
    }

    /** Whether the geyser is resting on a fluid and ready to be set off. */
    public static boolean canStartCharging(BlockState state) {
        return state.getValue(GEYSER_STATE) == GeyserState.DORMANT && state.getValue(GEYSER_TYPE) != GeyserType.NONE;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Triggers: random ticks, flint and steel / fire charges, and tremors (via scheduled ticks)
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return canStartCharging(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tryStartCharging(level, pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(GEYSER_STATE) == GeyserState.DORMANT) {
            // Tremors schedule a tick on geysers in the floor to set them off
            tryStartCharging(level, pos, state);
        } else {
            // A geyser saved mid-cycle by an older version has no block entity yet. Looking it up creates one,
            // and its ticker finishes the cycle.
            level.getBlockEntity(pos);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        boolean isFlintAndSteel = stack.is(Items.FLINT_AND_STEEL);
        if (!isFlintAndSteel && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        // Don't use up the item on a geyser that can't go off
        if (!canStartCharging(state)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && tryStartCharging(level, pos, state)) {
            if (isFlintAndSteel) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.consume(1, player);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Starts charging a dormant geyser that sits on a fluid. Returns whether it started.
     * The block entity takes it from here: it erupts after {@link #CHARGE_TICKS}.
     */
    public boolean tryStartCharging(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !canStartCharging(state) || !SpelunkersCharmConfig.ACTIVE_GEYSERS.get()) {
            return false;
        }
        level.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.CHARGING), UPDATE_ALL);
        level.playSound(null, pos, ModSoundEvents.GEYSER_CHARGE.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Cycle, driven by GeyserBlockEntity on the server
    // ---------------------------------------------------------------------------------------------------------------

    void erupt(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.ERUPTING), UPDATE_ALL);
        level.blockEvent(pos, this, ERUPT_EVENT, 0);

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5;
        switch (state.getValue(GEYSER_TYPE)) {
            case LAVA -> level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.5F, 0.1F);
            case WATER -> level.playSound(null, x, y, z, ModSoundEvents.GEYSER_ERUPT_WATER.get(), SoundSource.BLOCKS, 0.5F, 0.1F);
            default -> {}
        }
    }

    void stopErupting(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(GEYSER_STATE, GeyserState.DORMANT), UPDATE_ALL);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id != ERUPT_EVENT) {
            return super.triggerEvent(state, level, pos, id, param);
        }
        if (level.isClientSide) {
            spawnEruptionBurst(level, pos, state.getValue(GEYSER_TYPE), level.random);
        }
        // Returning true on the server is what sends the event to nearby clients
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Block entity
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeyserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        GeyserState geyserState = state.getValue(GEYSER_STATE);
        if (level.isClientSide) {
            // Clients only need to tick for the jet particles
            return geyserState == GeyserState.ERUPTING ? ticker(type, GeyserBlockEntity::clientTick) : null;
        }
        // Dormant geysers don't tick at all
        return geyserState != GeyserState.DORMANT ? ticker(type, GeyserBlockEntity::serverTick) : null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> ticker(BlockEntityType<T> type, BlockEntityTicker<? super GeyserBlockEntity> ticker) {
        return type == ModBlockEntityType.GEYSER_BLOCK_ENTITY.get() ? (BlockEntityTicker<T>) ticker : null;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Client particles
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(GEYSER_STATE) != GeyserState.DORMANT) return;
        ParticleOptions particle = switch (state.getValue(GEYSER_TYPE)) {
            case LAVA -> ParticleTypes.LARGE_SMOKE;
            case WATER -> ParticleTypes.CLOUD;
            default -> null;
        };
        if (particle == null) return;

        // Plain addParticle respects the player's particle setting, unlike addAlwaysVisibleParticle
        for (int i = 0; i < AMBIENT_PARTICLES; i++) {
            level.addParticle(particle,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.0 + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }

    private static void spawnEruptionBurst(Level level, BlockPos pos, GeyserType type, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5;
        switch (type) {
            case LAVA -> {
                for (int i = 0; i < BURST_LAVA_PARTICLES; i++) {
                    level.addParticle(ParticleTypes.LAVA, x, y, z,
                            random.nextGaussian() * 0.5, random.nextGaussian() * 0.5, random.nextGaussian() * 0.5);
                }
                spawnParticleRing(level, x, y, z, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            }
            case WATER -> spawnParticleRing(level, x, y, z, ParticleTypes.CLOUD);
            default -> {}
        }
    }

    private static void spawnParticleRing(Level level, double x, double y, double z, ParticleOptions particle) {
        for (int i = 0; i < BURST_RING_PARTICLES; i++) {
            double angle = 2 * Math.PI * i / BURST_RING_PARTICLES;
            level.addParticle(particle, x, y, z, Math.cos(angle) * BURST_RING_SPEED, 0.0, Math.sin(angle) * BURST_RING_SPEED);
        }
    }
}
