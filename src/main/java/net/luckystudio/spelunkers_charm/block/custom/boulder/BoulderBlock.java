package net.luckystudio.spelunkers_charm.block.custom.boulder;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.spelunkers_charm.block.util.enums.BlockPart;
import net.luckystudio.spelunkers_charm.block.custom.boulder.entity.Boulder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class BoulderBlock extends Block {

    public static final MapCodec<BoulderBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    propertiesCodec(), // This comes from Block base class
                    BoulderType.CODEC.fieldOf("type").forGetter(block -> block.type)
            ).apply(instance, BoulderBlock::new)
    );

    public static final EnumProperty<HangingType> HANGING_TYPE = ModBlockStateProperties.HANGING_TYPE;
    public static final EnumProperty<BlockPart> BLOCK_PART = ModBlockStateProperties.BLOCK_PART;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty GENERATE = BooleanProperty.create("generate");
    public static final BooleanProperty FULL = BooleanProperty.create("full");
    public final BoulderType type;

    public BoulderBlock(Properties properties, BoulderType type) {
        super(properties.pushReaction(PushReaction.BLOCK));
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BLOCK_PART, BlockPart.TOP_MIDDLE)
                .setValue(GENERATE, false) // Default to true to generate the boulder structure
                .setValue(FULL, true) // Default to true for full boulder
                .setValue(HANGING_TYPE, HangingType.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, GENERATE, FULL, BLOCK_PART, HANGING_TYPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedDirection = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();

        // Centered 3x3x3 region from pos offset by (0,1,0)
        BlockPos center = clickedPos.relative(clickedDirection); // pos.move(0,1,0)

        // Define bounding box for entity check
        AABB entityBox = new AABB(
                Vec3.atLowerCornerOf(center.offset(-1, -1, -1)),
                Vec3.atLowerCornerOf(center.offset(2, 2, 2))
        );

        // Entity check
        if (!level.getEntitiesOfClass(LivingEntity.class, entityBox).isEmpty()) {
            return null;
        }

        BlockPos minPos = new BlockPos((int) entityBox.minX, (int) entityBox.minY, (int) entityBox.minZ);
        BlockPos maxPos = new BlockPos((int) entityBox.maxX - 1, (int) entityBox.maxY - 1, (int) entityBox.maxZ - 1);

        for (BlockPos searchPos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (!level.getBlockState(searchPos).canBeReplaced()) {
                return null;
            }
        }

        BlockState hangingState = level.getBlockState(center.above(2));

        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(HANGING_TYPE, getHangType(hangingState))
                .setValue(GENERATE, true) // Set to true to generate the boulder structure
                .setValue(BLOCK_PART, BlockPart.BOTTOM_MIDDLE);
    }

    private HangingType getHangType(BlockState hangingFromState) {
        if (hangingFromState.getBlock() instanceof ChainBlock && hangingFromState.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y) {
            return HangingType.CHAINED;
        }
        return HangingType.NONE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        Direction offsetDirection = state.getValue(FACING);
        BlockPos offsetPos;
        if (offsetDirection == Direction.DOWN) {
            offsetPos = pos.relative(offsetDirection, 2);
        } else {
            offsetPos = pos.relative(offsetDirection).below();
        }
        if (state.getValue(BLOCK_PART) == BlockPart.BOTTOM_MIDDLE && state.getValue(GENERATE)) {
            if (!isMoving && !oldState.is(state.getBlock())) { // prevent recursion
                placeBoulder(level, offsetPos, this.type, state.getValue(HANGING_TYPE));
            }
        }
    }

    public static void placeBoulder(Level level, BlockPos pos, BoulderType type, HangingType hangingType) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlock(pos, BoulderUtils.getBoulderFromType(type)
                    .setValue(BLOCK_PART, BlockPart.BOTTOM_MIDDLE)
                    .setValue(GENERATE, false)
                    .setValue(HANGING_TYPE, hangingType), 3);
            BlockPos boulderPos = pos.offset(-1, 0, -1); // Adjust position to place the structure
            ResourceLocation structureId = switch (type) {
                case STONE -> SpelunkersCharm.id("boulder");
                case IRON -> SpelunkersCharm.id("iron_boulder");
                case COPPER -> SpelunkersCharm.id("copper_boulder");
                case GOLD -> SpelunkersCharm.id("gold_boulder");
                case LUSH -> SpelunkersCharm.id("lush_boulder");
                case LUSH_IRON -> SpelunkersCharm.id("lush_iron_boulder");
                case LUSH_COPPER -> SpelunkersCharm.id("lush_copper_boulder");
                case LUSH_GOLD -> SpelunkersCharm.id("lush_gold_boulder");
            };
            StructureTemplate template = serverLevel.getStructureManager().getOrCreate(structureId);
            template.placeInWorld(serverLevel,
                    boulderPos,
                    boulderPos,
                    new StructurePlaceSettings()
                            .setRotation(Rotation.NONE)
                            .setMirror(Mirror.NONE)
                            .setIgnoreEntities(true)
                            .addProcessor(new BlockStateProcessor(hangingType)),
                    serverLevel.random,
                    3
            );
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockPos bottomMiddlePos = getBottomMiddlePos(state, pos);
        for (BlockPos checkPos : boulderPositions(bottomMiddlePos)) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof BoulderBlock) {
                level.setBlock(checkPos, checkState.setValue(FULL, false), 3);
            }
        }
        super.destroy(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!state.getValue(FULL)) return;
        BlockPos bottomMiddlePos = getBottomMiddlePos(state, pos);
        BlockState blockStateHangingFrom = level.getBlockState(bottomMiddlePos.above(3));
        if (!isFullBoulder(state, level, pos)) return;
        if (!isSupported(level, pos)) {
            for (BlockPos checkPos : boulderPositions(bottomMiddlePos)) {
                BlockState checkState = level.getBlockState(checkPos);
                if (isBoulderBlock(checkState)) {
                    level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            Boulder boulder = new Boulder(level, bottomMiddlePos);
            boulder.setBoulderType(this.type);
            level.addFreshEntity(boulder);
        } else {
            for (BlockPos checkPos : boulderPositions(bottomMiddlePos)) {
                BlockState checkState = level.getBlockState(checkPos);
                if (isBoulderBlock(checkState)) {
                    if (checkState.getBlock() instanceof BoulderBlock) {
                        level.setBlock(checkPos, checkState.setValue(HANGING_TYPE, getHangType(blockStateHangingFrom)), 3);
                    }
                }
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    private BlockPos getBottomMiddlePos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING).getOpposite();
        BlockPart blockPart = state.getValue(BLOCK_PART);
        return switch (blockPart) {
            case BOTTOM_CORNER -> pos.relative(facing).relative(facing.getClockWise());
            case BOTTOM_SIDE -> pos.relative(facing);
            case MIDDLE_CORNER -> pos.relative(facing).relative(facing.getClockWise()).below();
            case MIDDLE_SIDE -> pos.relative(facing).below();
            case TOP_CORNER -> pos.relative(facing).relative(facing.getClockWise()).below(2);
            case TOP_SIDE -> pos.relative(facing).below(2);
            case TOP_MIDDLE -> pos.below(2);
            default -> pos; // Default case if none match
        };
    }

    public boolean isBoulderBlock(BlockState state) {
        return state.is(ModBlocks.BOULDER.get()) ||
                state.is(ModBlocks.IRON_BOULDER.get()) ||
                state.is(ModBlocks.COPPER_BOULDER.get()) ||
                state.is(ModBlocks.GOLD_BOULDER.get()) ||
                state.is(ModBlocks.LUSH_BOULDER.get()) ||
                state.is(ModBlocks.LUSH_IRON_BOULDER.get()) ||
                state.is(ModBlocks.LUSH_COPPER_BOULDER.get()) ||
                state.is(ModBlocks.LUSH_GOLD_BOULDER.get()) ||
                state.is(BoulderUtils.getRawCore(type));
    }

    private boolean isFullBoulder(BlockState state, Level level, BlockPos pos) {
        BlockPos bottomMiddlePos = getBottomMiddlePos(state, pos);
        for (BlockPos checkPos : boulderPositions(bottomMiddlePos)) {
            if (!isBoulderBlock(level.getBlockState(checkPos))) {
                return false;
            }
        }
        return true;
    }

    private List<BlockPos> boulderPositions(BlockPos bottomMiddlePos) {
        return List.of(
                bottomMiddlePos,
                bottomMiddlePos.north(),
                bottomMiddlePos.north().east(),
                bottomMiddlePos.east(),
                bottomMiddlePos.south().east(),
                bottomMiddlePos.south(),
                bottomMiddlePos.south().west(),
                bottomMiddlePos.west(),
                bottomMiddlePos.north().west(),
                bottomMiddlePos.above(),
                bottomMiddlePos.above().north(),
                bottomMiddlePos.above().north().east(),
                bottomMiddlePos.above().east(),
                bottomMiddlePos.above().south().east(),
                bottomMiddlePos.above().south(),
                bottomMiddlePos.above().south().west(),
                bottomMiddlePos.above().west(),
                bottomMiddlePos.above().north().west(),
                bottomMiddlePos.above(2),
                bottomMiddlePos.above(2).north(),
                bottomMiddlePos.above(2).north().east(),
                bottomMiddlePos.above(2).east(),
                bottomMiddlePos.above(2).south().east(),
                bottomMiddlePos.above(2).south(),
                bottomMiddlePos.above(2).south().west(),
                bottomMiddlePos.above(2).west(),
                bottomMiddlePos.above(2).north().west()
        );
    }

    // Should only be run at the bottom_middle part of the boulder
    private boolean isSupported(Level level, BlockPos pos) {
        BlockPos bottomMiddlePos = getBottomMiddlePos(level.getBlockState(pos), pos);
        // Has block under it to be supported
        return isSuspended(level, bottomMiddlePos) ||
                !level.getBlockState(bottomMiddlePos.below()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().north()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().north().east()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().east()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().south().east()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().south()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().south().west()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().west()).canBeReplaced()
                || !level.getBlockState(bottomMiddlePos.below().north().west()).canBeReplaced();
    }

    private boolean isSuspended(Level level, BlockPos bottomMiddlePos) {
        BlockState state = level.getBlockState(bottomMiddlePos.above(3));
        return state.getBlock() instanceof ChainBlock &&
                state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.spelunkers_charm.boulder_block").withStyle(ChatFormatting.GRAY));
    }
}
