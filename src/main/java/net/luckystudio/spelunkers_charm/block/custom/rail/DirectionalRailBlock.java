// Directional rail block.
package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A rail with a direction. Without power it stops carts and settles them exactly in the middle of the block.
 * With power it sends carts the way it faces.
 */
public class DirectionalRailBlock extends PoweredRailBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Speed added each tick to a cart that is slower than {@link #MAX_PUSH_SPEED}. */
    private static final double PUSH = 0.06;

    /** The rail never pushes a cart faster than this, in blocks per tick. */
    private static final double MAX_PUSH_SPEED = 0.4;

    /** How far a stopped cart is moved toward the middle of the block each tick. */
    private static final double CENTERING_SPEED = 0.2;

    public DirectionalRailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation)
                .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** The way this rail sends carts: its facing, kept on the rail's own line. */
    public static Direction travelDirection(BlockState state) {
        RailShape shape = state.getValue(SHAPE);
        Direction facing = state.getValue(FACING);
        boolean northSouth =
                shape == RailShape.NORTH_SOUTH
                        || shape == RailShape.ASCENDING_NORTH
                        || shape == RailShape.ASCENDING_SOUTH;
        if (northSouth) {
            return facing == Direction.NORTH || facing == Direction.EAST
                    ? Direction.NORTH
                    : Direction.SOUTH;
        }
        return facing == Direction.EAST || facing == Direction.SOUTH
                ? Direction.EAST
                : Direction.WEST;
    }

    /** Clicking with an empty hand turns the rail around. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(FACING, travelDirection(state).getOpposite()), 3);
            level.playSound(
                    null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.5F, 0.7F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (level.isClientSide()
                || !net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings
                        .isEngine(cart)) return;
        if (!state.getValue(POWERED)) {
            net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartHandbrake.engage(cart);
            return;
        }
        Direction direction = travelDirection(state);
        net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartHandbrake.release(cart);
        if (cart instanceof LocomotiveMinecart engine) {
            if (engine.isDrivingAgainst(direction)) engine.switchDirection();
            engine.setDriveDirection(direction);
        }
        if (cart instanceof net.minecraft.world.entity.vehicle.MinecartFurnace furnace) {
            Vec3 previous = new Vec3(furnace.xPush, 0, furnace.zPush);
            if (previous.x * direction.getStepX() + previous.z * direction.getStepZ() < 0)
                ((net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartMotionVisuals)
                                        cart)
                                .spelunkersCharm$motion()
                                .reversalRequested =
                        true;
            furnace.xPush = direction.getStepX();
            furnace.zPush = direction.getStepZ();
        }
    }

    @Override
    public boolean isActivatorRail() {
        return false;
    }

    @Override
    protected boolean findPoweredRailSignal(
            Level level, BlockPos pos, BlockState state, boolean forward, int depth) {
        return false;
    }

    @Override
    protected void updateState(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.level.block.Block source) {
        boolean powered = false;
        for (Direction direction :
                new Direction[] {
                    Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
                }) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).getBlock() instanceof PoweredRailBlock) continue;
            if (level.getSignal(neighbor, direction) > 0) {
                powered = true;
                break;
            }
        }
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
            level.updateNeighborsAt(pos.below(), this);
            if (state.getValue(SHAPE).isAscending()) level.updateNeighborsAt(pos.above(), this);
        }
    }

    /** Whether this cart is being held still by an unpowered directional rail. */
    public static boolean isHolding(BlockState state) {
        return state.getBlock() instanceof DirectionalRailBlock && !state.getValue(POWERED);
    }

    /**
     * Stops the cart and slides it to the middle of the block. Called while a cart passes, and again after
     * each cart move (see MinecartEvents), because a fast cart can move on past the rail within one tick.
     */
    public static void holdCart(BlockState state, BlockPos pos, AbstractMinecart cart) {
        if (!net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings.isEngine(
                cart)) return;
        Direction direction = travelDirection(state);
        Vec3 motion = cart.getDeltaMovement();
        cart.setDeltaMovement(0.0, motion.y, 0.0);
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double offset =
                direction.getAxis() == Direction.Axis.X
                        ? centerX - cart.getX()
                        : centerZ - cart.getZ();
        // Close enough: leave the cart alone so it sits perfectly still.
        if (Math.abs(offset) < 0.01) return;
        double step = Math.max(-CENTERING_SPEED, Math.min(CENTERING_SPEED, offset));
        if (direction.getAxis() == Direction.Axis.X) {
            cart.setPos(cart.getX() + step, cart.getY(), centerZ);
        } else {
            cart.setPos(centerX, cart.getY(), cart.getZ() + step);
        }
    }
}
