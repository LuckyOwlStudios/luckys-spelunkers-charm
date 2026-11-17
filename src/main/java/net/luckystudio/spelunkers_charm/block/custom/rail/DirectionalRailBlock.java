package net.luckystudio.spelunkers_charm.block.custom.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
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
        return super.rotate(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** The way this rail sends carts: its facing, kept on the rail's own line. */
    public static Direction travelDirection(BlockState state) {
        RailShape shape = state.getValue(SHAPE);
        Direction facing = state.getValue(FACING);
        boolean northSouth = shape == RailShape.NORTH_SOUTH
                || shape == RailShape.ASCENDING_NORTH || shape == RailShape.ASCENDING_SOUTH;
        if (northSouth) {
            return facing == Direction.NORTH || facing == Direction.EAST ? Direction.NORTH : Direction.SOUTH;
        }
        return facing == Direction.EAST || facing == Direction.SOUTH ? Direction.EAST : Direction.WEST;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (level.isClientSide()) return;
        Direction direction = travelDirection(state);
        Vec3 motion = cart.getDeltaMovement();

        if (state.getValue(POWERED)) {
            double speed = motion.x * direction.getStepX() + motion.z * direction.getStepZ();
            if (speed < MAX_PUSH_SPEED) {
                // Moving backwards counts as no speed, so a cart heading the wrong way is turned around.
                double pushed = Math.min(Math.max(speed, 0.0) + PUSH, MAX_PUSH_SPEED);
                cart.setDeltaMovement(direction.getStepX() * pushed, motion.y, direction.getStepZ() * pushed);
            }
            return;
        }

        // Stop the cart, then slide it to the middle of the block.
        cart.setDeltaMovement(0.0, motion.y, 0.0);
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double offset = direction.getAxis() == Direction.Axis.X ? centerX - cart.getX() : centerZ - cart.getZ();
        if (Math.abs(offset) < 1.0E-3) return;
        double step = Math.max(-CENTERING_SPEED, Math.min(CENTERING_SPEED, offset));
        if (direction.getAxis() == Direction.Axis.X) {
            cart.setPos(cart.getX() + step, cart.getY(), centerZ);
        } else {
            cart.setPos(centerX, cart.getY(), cart.getZ() + step);
        }
    }
}
