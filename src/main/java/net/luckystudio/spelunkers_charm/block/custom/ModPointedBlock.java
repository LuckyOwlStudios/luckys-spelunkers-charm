package net.luckystudio.spelunkers_charm.block.custom;

import com.google.common.annotations.VisibleForTesting;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// Pointed Dripstone Block is horribly coded, so we are making our own version of it.
public class ModPointedBlock extends PointedDripstoneBlock {

    private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public ModPointedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPointedDripstonePlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(
            BlockState state, Direction p_direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (p_direction != Direction.UP && p_direction != Direction.DOWN) {
            return state;
        } else {
            Direction direction = state.getValue(TIP_DIRECTION);
            if (direction == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) {
                return state;
            } else if (p_direction == direction.getOpposite() && !this.canSurvive(state, level, pos)) {
                if (direction == Direction.DOWN) {
                    level.scheduleTick(pos, this, 2);
                } else {
                    level.scheduleTick(pos, this, 1);
                }

                return state;
            } else {
                boolean flag = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
                DripstoneThickness dripstonethickness = calculateDripstoneThickness(level, pos, direction, flag);
                return state.setValue(THICKNESS, dripstonethickness);
            }
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos blockpos = hit.getBlockPos();
            if (projectile.mayInteract(level, blockpos)
                    && projectile.mayBreak(level)
                    && projectile instanceof ThrownTrident
                    && projectile.getDeltaMovement().length() > 0.6) {
                level.destroyBlock(blockpos, true);
            }
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (canDrip(state)) {
            float f = random.nextFloat();
            if (!(f > 0.12F)) {
                getFluidAboveStalactite(level, pos, state)
                        .filter(p_221848_ -> f < 0.02F || canFillCauldron(p_221848_.fluid))
                        .ifPresent(p_221881_ -> spawnDripParticle(level, pos, state, p_221881_.fluid));
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isStalagmite(state) && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            spawnFallingStalactite(state, level, pos);
        }
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        maybeTransferFluid(state, level, pos, random.nextFloat());
        if (random.nextFloat() < 0.011377778F && isStalactiteStartPos(state, level, pos)) {
            growStalactiteOrStalagmiteIfPossible(state, level, pos, random);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor levelaccessor = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getNearestLookingVerticalDirection().getOpposite();
        Direction direction1 = calculateTipDirection(levelaccessor, blockpos, direction);
        if (direction1 == null) {
            return null;
        } else {
            boolean flag = !context.isSecondaryUseActive();
            DripstoneThickness dripstonethickness = calculateDripstoneThickness(levelaccessor, blockpos, direction1, flag);
            return dripstonethickness == null
                    ? null
                    : this.defaultBlockState()
                    .setValue(TIP_DIRECTION, direction1)
                    .setValue(THICKNESS, dripstonethickness)
                    .setValue(WATERLOGGED, Boolean.valueOf(levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER));
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return 0.125F;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
            ModParticleTypes.spawnParticlesToServer(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 10, 0.1, 0.1, 0.1, 0.05,
                    ParticleTypes.SNOWFLAKE);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    private static void spawnFallingStalactite(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();
        BlockState blockstate = state;

        while (isStalactite(blockstate)) {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(level, blockpos$mutableblockpos, blockstate);
            if (isTip(blockstate, true)) {
                int i = Math.max(1 + pos.getY() - blockpos$mutableblockpos.getY(), 6);
                float f = 1.0F * (float)i;
                fallingblockentity.setHurtsEntities(f, 40);
                break;
            }

            blockpos$mutableblockpos.move(Direction.DOWN);
            blockstate = level.getBlockState(blockpos$mutableblockpos);
        }
    }

    @VisibleForTesting
    public static void growStalactiteOrStalagmiteIfPossible(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState blockstate = level.getBlockState(pos.above(1));
        BlockState blockstate1 = level.getBlockState(pos.above(2));
        if (canGrow(blockstate, blockstate1)) {
            BlockPos blockpos = findTip(state, level, pos, 7, false);
            if (blockpos != null) {
                BlockState blockstate2 = level.getBlockState(blockpos);
                if (canDrip(blockstate2) && canTipGrow(blockstate2, level, blockpos)) {
                    if (random.nextBoolean()) {
                        grow(level, blockpos, Direction.DOWN);
                    } else {
                        growStalagmiteBelow(level, blockpos);
                    }
                }
            }
        }
    }

    private static void growStalagmiteBelow(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (int i = 0; i < 10; i++) {
            blockpos$mutableblockpos.move(Direction.DOWN);
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            if (!blockstate.getFluidState().isEmpty()) {
                return;
            }

            if (isUnmergedTipWithDirection(blockstate, Direction.UP) && canTipGrow(blockstate, level, blockpos$mutableblockpos)) {
                grow(level, blockpos$mutableblockpos, Direction.UP);
                return;
            }

            if (isValidPointedDripstonePlacement(level, blockpos$mutableblockpos, Direction.UP) && !level.isWaterAt(blockpos$mutableblockpos.below())) {
                grow(level, blockpos$mutableblockpos.below(), Direction.UP);
                return;
            }

            if (!canDripThrough(level, blockpos$mutableblockpos, blockstate)) {
                return;
            }
        }
    }

    private static void grow(ServerLevel server, BlockPos pos, Direction direction) {
        BlockPos blockpos = pos.relative(direction);
        BlockState blockstate = server.getBlockState(blockpos);
        if (isUnmergedTipWithDirection(blockstate, direction.getOpposite())) {
            createMergedTips(blockstate, server, blockpos);
        } else if (blockstate.isAir() || blockstate.is(Blocks.WATER)) {
            createDripstone(server, blockpos, direction, DripstoneThickness.TIP);
        }
    }

    private static void createDripstone(LevelAccessor level, BlockPos pos, Direction direction, DripstoneThickness thickness) {
        BlockState blockstate = ModBlocks.ICICLE.get()
                .defaultBlockState()
                .setValue(TIP_DIRECTION, direction)
                .setValue(THICKNESS, thickness)
                .setValue(WATERLOGGED, Boolean.valueOf(level.getFluidState(pos).getType() == Fluids.WATER));
        level.setBlock(pos, blockstate, 3);
    }

    private static void createMergedTips(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos blockpos;
        BlockPos blockpos1;
        if (state.getValue(TIP_DIRECTION) == Direction.UP) {
            blockpos1 = pos;
            blockpos = pos.above();
        } else {
            blockpos = pos;
            blockpos1 = pos.below();
        }

        createDripstone(level, blockpos, Direction.DOWN, DripstoneThickness.TIP_MERGE);
        createDripstone(level, blockpos1, Direction.UP, DripstoneThickness.TIP_MERGE);
    }

    private static void spawnDripParticle(Level level, BlockPos pos, BlockState state, Fluid p_fluid) {
        Vec3 vec3 = state.getOffset(level, pos);
        double d1 = (double)pos.getX() + 0.5 + vec3.x;
        double d2 = (double)((float)(pos.getY() + 1) - 0.6875F) - 0.0625;
        double d3 = (double)pos.getZ() + 0.5 + vec3.z;
        Fluid fluid = getDripFluid(level, p_fluid);
        ParticleOptions particleoptions = fluid.getFluidType().getDripInfo() != null ? fluid.getFluidType().getDripInfo().dripParticle() : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
        if (particleoptions != null)
            level.addParticle(particleoptions, d1, d2, d3, 0.0, 0.0, 0.0);
    }

    @Nullable
    private static BlockPos findTip(BlockState state, LevelAccessor level, BlockPos pos, int maxIterations, boolean isTipMerge) {
        if (isTip(state, isTipMerge)) {
            return pos;
        } else {
            Direction direction = state.getValue(TIP_DIRECTION);
            BiPredicate<BlockPos, BlockState> bipredicate = (p_202023_, p_202024_) -> p_202024_.is(ModBlocks.ICICLE)
                    && p_202024_.getValue(TIP_DIRECTION) == direction;
            return findBlockVertical(level, pos, direction.getAxisDirection(), bipredicate, p_154168_ -> isTip(p_154168_, isTipMerge), maxIterations)
                    .orElse(null);
        }
    }

    @Nullable
    private static Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction dir) {
        Direction direction;
        if (isValidPointedDripstonePlacement(level, pos, dir)) {
            direction = dir;
        } else {
            if (!isValidPointedDripstonePlacement(level, pos, dir.getOpposite())) {
                return null;
            }

            direction = dir.getOpposite();
        }

        return direction;
    }

    private static DripstoneThickness calculateDripstoneThickness(LevelReader level, BlockPos pos, Direction dir, boolean isTipMerge) {
        Direction direction = dir.getOpposite();
        BlockState blockstate = level.getBlockState(pos.relative(dir));
        if (isPointedDripstoneWithDirection(blockstate, direction)) {
            return !isTipMerge && blockstate.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
        } else if (!isPointedDripstoneWithDirection(blockstate, dir)) {
            return DripstoneThickness.TIP;
        } else {
            DripstoneThickness dripstonethickness = blockstate.getValue(THICKNESS);
            if (dripstonethickness != DripstoneThickness.TIP && dripstonethickness != DripstoneThickness.TIP_MERGE) {
                BlockState blockstate1 = level.getBlockState(pos.relative(direction));
                return !isPointedDripstoneWithDirection(blockstate1, dir) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            } else {
                return DripstoneThickness.FRUSTUM;
            }
        }
    }

    public static boolean canDrip(BlockState state) {
        return isStalactite(state) && state.getValue(THICKNESS) == DripstoneThickness.TIP && !state.getValue(WATERLOGGED);
    }

    private static boolean canTipGrow(BlockState state, ServerLevel level, BlockPos pos) {
        Direction direction = state.getValue(TIP_DIRECTION);
        BlockPos blockpos = pos.relative(direction);
        BlockState blockstate = level.getBlockState(blockpos);
        if (!blockstate.getFluidState().isEmpty()) {
            return false;
        } else {
            return blockstate.isAir() ? true : isUnmergedTipWithDirection(blockstate, direction.getOpposite());
        }
    }

    private static Optional<BlockPos> findRootBlock(Level level, BlockPos pos, BlockState state, int maxIterations) {
        Direction direction = state.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> bipredicate = (p_202015_, p_202016_) -> p_202016_.is(ModBlocks.ICICLE)
                && p_202016_.getValue(TIP_DIRECTION) == direction;
        return findBlockVertical(
                level, pos, direction.getOpposite().getAxisDirection(), bipredicate, p_154245_ -> !p_154245_.is(ModBlocks.ICICLE), maxIterations
        );
    }

    private static boolean isValidPointedDripstonePlacement(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos blockpos = pos.relative(dir.getOpposite());
        BlockState blockstate = level.getBlockState(blockpos);
        return blockstate.isFaceSturdy(level, blockpos, dir) || isPointedDripstoneWithDirection(blockstate, dir);
    }

    private static boolean isTip(BlockState state, boolean isTipMerge) {
        if (!state.is(ModBlocks.ICICLE)) {
            return false;
        } else {
            DripstoneThickness dripstonethickness = state.getValue(THICKNESS);
            return dripstonethickness == DripstoneThickness.TIP || isTipMerge && dripstonethickness == DripstoneThickness.TIP_MERGE;
        }
    }

    private static boolean isUnmergedTipWithDirection(BlockState state, Direction dir) {
        return isTip(state, false) && state.getValue(TIP_DIRECTION) == dir;
    }

    private static boolean isStalactite(BlockState state) {
        return isPointedDripstoneWithDirection(state, Direction.DOWN);
    }

    private static boolean isStalagmite(BlockState state) {
        return isPointedDripstoneWithDirection(state, Direction.UP);
    }

    private static boolean isStalactiteStartPos(BlockState state, LevelReader level, BlockPos pos) {
        return isStalactite(state) && !level.getBlockState(pos.above()).is(ModBlocks.ICICLE);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    private static boolean isPointedDripstoneWithDirection(BlockState state, Direction dir) {
        return state.is(ModBlocks.ICICLE) && state.getValue(TIP_DIRECTION) == dir;
    }

    private static Optional<ModPointedBlock.FluidInfo> getFluidAboveStalactite(Level level, BlockPos pos, BlockState state) {
        return !isStalactite(state) ? Optional.empty() : findRootBlock(level, pos, state, 11).map(p_221876_ -> {
            BlockPos blockpos = p_221876_.above();
            BlockState blockstate = level.getBlockState(blockpos);
            Fluid fluid;
            if (blockstate.is(Blocks.MUD) && !level.dimensionType().ultraWarm()) {
                fluid = Fluids.WATER;
            } else {
                fluid = level.getFluidState(blockpos).getType();
            }

            return new ModPointedBlock.FluidInfo(blockpos, fluid, blockstate);
        });
    }

    private static boolean canFillCauldron(Fluid fluid) {
        return fluid.getFluidType().getDripInfo() != null;
    }

    private static boolean canGrow(BlockState dripstoneState, BlockState state) {
        return dripstoneState.is(ModBlocks.ICICLE) && state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static Fluid getDripFluid(Level level, Fluid fluid) {
        if (fluid.isSame(Fluids.EMPTY)) {
            return level.dimensionType().ultraWarm() ? Fluids.LAVA : Fluids.WATER;
        } else {
            return fluid;
        }
    }

    private static Optional<BlockPos> findBlockVertical(
            LevelAccessor level,
            BlockPos pos,
            Direction.AxisDirection axis,
            BiPredicate<BlockPos, BlockState> positionalStatePredicate,
            Predicate<BlockState> statePredicate,
            int maxIterations
    ) {
        Direction direction = Direction.get(axis, Direction.Axis.Y);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (int i = 1; i < maxIterations; i++) {
            blockpos$mutableblockpos.move(direction);
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            if (statePredicate.test(blockstate)) {
                return Optional.of(blockpos$mutableblockpos.immutable());
            }

            if (level.isOutsideBuildHeight(blockpos$mutableblockpos.getY()) || !positionalStatePredicate.test(blockpos$mutableblockpos, blockstate)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static boolean canDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        } else if (state.isSolidRender(level, pos)) {
            return false;
        } else if (!state.getFluidState().isEmpty()) {
            return false;
        } else {
            VoxelShape voxelshape = state.getCollisionShape(level, pos);
            return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, voxelshape, BooleanOp.AND);
        }
    }

    record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}
