package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;

import java.util.Random;
import java.util.function.ToIntFunction;

// Copied from the SnifferEggBlock and TurtleEggBlock
public class SpiderEgg extends RodBlock {
    public static final MapCodec<SpiderEgg> CODEC = simpleCodec(SpiderEgg::new);
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;

    public static final ToIntFunction<BlockState> LIGHT_EMISSION = blockState -> switch (blockState.getValue(HATCH)) {
        case 1 -> 5;
        case 2 -> 7;
        default -> 3;
    };

    private static final VoxelShape NORTH = Block.box(1.0, 1.0, 2.0, 15.0, 15.0, 16.0);
    private static final VoxelShape EAST = Block.box(0.0, 1.0, 1.0, 14.0, 15.0, 15.0);
    private static final VoxelShape SOUTH = Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 14.0);
    private static final VoxelShape WEST = Block.box(2.0, 1.0, 1.0, 16.0, 15.0, 15.0);
    private static final VoxelShape UP = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private static final VoxelShape DOWN = Block.box(1.0, 2.0, 1.0, 15.0, 16.0, 15.0);

    public SpiderEgg(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(HATCH, 0));
    }

    @Override
    protected MapCodec<SpiderEgg> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos().relative(direction.getOpposite()));
        return blockstate.is(this) && blockstate.getValue(FACING) == direction
                ? this.defaultBlockState().setValue(FACING, direction.getOpposite())
                : this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        boolean flag = hatchBoost(level, pos);
        if (!level.isClientSide() && flag) {
            level.levelEvent(3009, pos, 0);
        }

        int i = flag ? 12000 : 24000;
        int j = i / 3;
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(state));
        level.scheduleTick(pos, this, j + level.random.nextInt(300));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) {
            this.destroyEgg(level, state, pos, entity, 100);
        }

        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof Zombie)) {
            this.destroyEgg(level, state, pos, entity, 3);
        }

        super.fallOn(level, state, pos, entity, fallDistance);
    }

    private void destroyEgg(Level level, BlockState state, BlockPos pos, Entity entity, int chance) {
        if (this.canDestroyEgg(level, entity)) {
            if (!level.isClientSide && level.random.nextInt(chance) == 0 && state.is(ModBlocks.SPIDER_EGG)) {
                level.playSound(null, pos, ModSoundEvents.SPIDER_EGG_CRACK.get(), SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);
                level.destroyBlock(pos, false);
                if (ModList.get().isLoaded("cameraoverhaul")) {
                    System.out.println("Camera Overhaul is loaded, skipping hatching logic.");
                }
            }
        }
    }

    private boolean canDestroyEgg(Level level, Entity entity) {
        if (entity instanceof Turtle || entity instanceof Bat) {
            return false;
        } else {
            return entity instanceof LivingEntity && (entity instanceof Player || net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, entity));
        }
    }

    public int getHatchLevel(BlockState state) {
        return state.getValue(HATCH);
    }

    private boolean isReadyToHatch(BlockState state) {
        return this.getHatchLevel(state) == 2;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check for player within 8-block radius (adjust as needed)
        boolean playerNearby = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8)).isEmpty();

        if (!playerNearby) {
            return; // Don't hatch if no player is nearby
        }

        if (!this.isReadyToHatch(state)) {
            level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, this.getHatchLevel(state) + 1), 2);
            level.getChunk(pos).setUnsaved(true); // Optional: mark for save
            level.getChunkSource().getLightEngine().checkBlock(pos); // Force light recalculation
        } else {
            hatch(level, pos, random);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos blockpos = hit.getBlockPos();
            if (projectile.mayInteract(level, blockpos)
                    && projectile.mayBreak(level)
                    && projectile.getDeltaMovement().length() > 0.6) {
                level.destroyBlock(blockpos, true);
                if (state.getValue(HATCH) == 2) {
                    Random random = new Random();
                    RandomSource randomSource = RandomSource.create(random.nextLong());
                    hatch((ServerLevel) level, blockpos, randomSource);
                }
            }
        }
    }

    private void hatch(ServerLevel level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, ModSoundEvents.SPIDER_EGG_CRACK.get(), SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
        level.destroyBlock(pos, false);
        CaveSpider caveSpider = EntityType.CAVE_SPIDER.create(level);
        if (caveSpider != null) {
            Vec3 vec3 = pos.getCenter();
            caveSpider.setBaby(true);
            caveSpider.moveTo(vec3.x(), vec3.y(), vec3.z(), Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
            level.addFreshEntity(caveSpider);
        }
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    public static boolean hatchBoost(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.SNIFFER_EGG_HATCH_BOOST);
    }
}
