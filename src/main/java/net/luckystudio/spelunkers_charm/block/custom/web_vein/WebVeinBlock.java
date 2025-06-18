package net.luckystudio.spelunkers_charm.block.custom.web_vein;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// We had to create a new one and not use the existing SimpleMultiFaceBlock because it does too much we don't need.
public class WebVeinBlock extends MultifaceBlock implements WebVeinBehavior {
    private static final VoxelShape UP_AABB = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    public static final MapCodec<WebVeinBlock> CODEC = simpleCodec(WebVeinBlock::new);
    private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new WebVeinBlock.VeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
    private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
            new WebVeinBlock.VeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
    );

    public WebVeinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState());
    }

    @Override
    protected MapCodec<WebVeinBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return UP_AABB;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return veinSpreader;
    }

    public MultifaceSpreader getSameSpaceSpreader() {
        return this.sameSpaceSpreader;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.getItemInHand().is(this.asItem()) || super.canBeReplaced(state, useContext);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        double d0 = Math.abs(entity.getDeltaMovement().y);
        if (d0 < 0.1 && !entity.isSteppingCarefully()) {
            double d1 = 0.4 + d0 * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(d1, 1.0, d1));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public int attemptUseCharge(
            WebSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos pos, RandomSource random, WebSpreader spreader, boolean shouldConvertBlocks
    ) {
        if (shouldConvertBlocks && this.attemptPlaceWeb(spreader, level, cursor.getPos(), random)) {
            return cursor.getCharge() - 1;
        } else {
            return random.nextInt(spreader.chargeDecayRate()) == 0 ? Mth.floor((float)cursor.getCharge() * 0.5F) : cursor.getCharge();
        }
    }

    private boolean attemptPlaceWeb(WebSpreader spreader, LevelAccessor level, BlockPos pos, RandomSource random) {
        BlockState blockstate = level.getBlockState(pos);
        TagKey<Block> tagkey = spreader.replaceableBlocks();

        for (Direction direction : Direction.allShuffled(random)) {
            if (hasFace(blockstate, direction)) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate1 = level.getBlockState(blockpos);
                if (blockstate1.is(tagkey)) {
                    BlockState blockstate2 = ModBlocks.PACKED_WEB.get().defaultBlockState();
                    level.setBlock(blockpos, blockstate2, 3);
                    Block.pushEntitiesUp(blockstate1, blockstate2, level, blockpos);
                    level.playSound(null, blockpos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.veinSpreader.spreadAll(blockstate2, level, blockpos, spreader.isWorldGeneration());
                    Direction direction1 = direction.getOpposite();

                    for (Direction direction2 : DIRECTIONS) {
                        if (direction2 != direction1) {
                            BlockPos blockpos1 = blockpos.relative(direction2);
                            BlockState blockstate3 = level.getBlockState(blockpos1);
                            if (blockstate3.is(this)) {
                                this.onDischarged(level, blockstate3, blockpos1, random);
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    class VeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
        private final MultifaceSpreader.SpreadType[] spreadTypes;

        public VeinSpreaderConfig(MultifaceSpreader.SpreadType... spreadTypes) {
            super(WebVeinBlock.this);
            this.spreadTypes = spreadTypes;
        }

        @Override
        public boolean stateCanBeReplaced(BlockGetter level, BlockPos pos, BlockPos spreadPos, Direction direction, BlockState state) {
            BlockState blockstate = level.getBlockState(spreadPos.relative(direction));
            if (!blockstate.is(ModBlocks.PACKED_WEB) && !blockstate.is(ModBlocks.SPIDER_EGG) && !blockstate.is(Blocks.MOVING_PISTON)) {
                if (pos.distManhattan(spreadPos) == 2) {
                    BlockPos blockpos = pos.relative(direction.getOpposite());
                    if (level.getBlockState(blockpos).isFaceSturdy(level, blockpos, direction)) {
                        return false;
                    }
                }

                FluidState fluidstate = state.getFluidState();
                if (!fluidstate.isEmpty() && !fluidstate.is(Fluids.WATER)) {
                    return false;
                } else {
                    return state.is(BlockTags.FIRE)
                            ? false
                            : state.canBeReplaced() || super.stateCanBeReplaced(level, pos, spreadPos, direction, state);
                }
            } else {
                return false;
            }
        }

        @Override
        public MultifaceSpreader.SpreadType[] getSpreadTypes() {
            return this.spreadTypes;
        }

        @Override
        public boolean isOtherBlockValidAsSource(BlockState otherBlock) {
            return !otherBlock.is(ModBlocks.WEB_VEIN);
        }
    }
}
