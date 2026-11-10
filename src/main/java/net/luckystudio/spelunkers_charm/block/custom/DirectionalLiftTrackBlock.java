package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** A station: one command per rising input edge, with independent visual feedback. */
public final class DirectionalLiftTrackBlock extends LiftTrackBlock {
    public static final MapCodec<DirectionalLiftTrackBlock> CODEC =
            simpleCodec(DirectionalLiftTrackBlock::new);
    public static final DirectionProperty VERTICAL_DIRECTION =
            BlockStateProperties.VERTICAL_DIRECTION;
    public static final BooleanProperty SIGNAL_HELD = BooleanProperty.create("signal_held");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int COMMAND_TICKS = 2;
    public static final int DISPLAY_TICKS = 20;

    public DirectionalLiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(VERTICAL_DIRECTION, Direction.UP)
                        .setValue(SIGNAL_HELD, false)
                        .setValue(LIT, false));
    }

    @Override
    protected MapCodec<DirectionalLiftTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VERTICAL_DIRECTION, SIGNAL_HELD, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction =
                context.getPlayer() != null && context.getPlayer().getXRot() > 0
                        ? Direction.DOWN
                        : Direction.UP;
        return super.getStateForPlacement(context).setValue(VERTICAL_DIRECTION, direction);
    }

    @Override
    protected void updatePower(BlockState state, Level level, BlockPos pos) {
        boolean input = level.hasNeighborSignal(pos);
        boolean held = state.getValue(SIGNAL_HELD);
        if (input && !held) startPulse(state, (ServerLevel) level, pos);
        else if (!input && held)
            level.setBlock(pos, state.setValue(SIGNAL_HELD, false), Block.UPDATE_ALL);
    }

    private void startPulse(BlockState state, ServerLevel level, BlockPos pos) {
        // A queued display expiry must not deduplicate the new, earlier command expiry.
        // This replaces only block ticks at this station's single position.
        level.getBlockTicks().clearArea(new BoundingBox(pos));
        level.setBlock(
                pos,
                state.setValue(SIGNAL_HELD, true).setValue(POWERED, true).setValue(LIT, true),
                Block.UPDATE_ALL);
        level.scheduleTick(pos, this, COMMAND_TICKS);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            state = level.getBlockState(pos);
            if (state.getValue(LIT)) level.scheduleTick(pos, this, DISPLAY_TICKS - COMMAND_TICKS);
        } else if (state.getValue(LIT)) {
            // A visual-only expiry must not pulse observers or change nearby circuits.
            level.setBlock(
                    pos,
                    state.setValue(LIT, false),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            state = level.getBlockState(pos);
        }
        updatePower(state, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Direction direction = state.getValue(VERTICAL_DIRECTION).getOpposite();
            level.setBlock(pos, state.setValue(VERTICAL_DIRECTION, direction), Block.UPDATE_ALL);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.3F,
                    direction == Direction.UP ? 0.6F : 0.5F);
            player.displayClientMessage(
                    Component.translatable(
                            direction == Direction.UP
                                    ? "message.spelunkers_charm.lift_station_up"
                                    : "message.spelunkers_charm.lift_station_down"),
                    true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (String key :
                List.of(
                        "lift_station",
                        "lift_station_power",
                        "lift_station_direction",
                        "lift_station_toggle")) {
            tooltip.add(Component.translatable("tooltip.spelunkers_charm." + key));
        }
    }
}
