// Cargo rail block.
package net.luckystudio.spelunkers_charm.block.custom.rail;

import com.mojang.serialization.MapCodec;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.JukeboxMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Like a detector rail, but it measures what is in the cart. It gives a redstone signal of 0 to 15 for how
 * full the cart is (chests, hoppers, the locomotive's fuel slot, blocks in a block cart, a jukebox cart's disc).
 * An empty cart, or no cart, gives no signal.
 */
public class CargoRailBlock extends BaseRailBlock {

    public static final MapCodec<CargoRailBlock> CODEC = simpleCodec(CargoRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final IntegerProperty CARGO = IntegerProperty.create("cargo", 0, 15);

    public CargoRailBlock(Properties properties) {
        super(true, properties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(SHAPE, RailShape.NORTH_SOUTH)
                        .setValue(CARGO, 0)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<CargoRailBlock> codec() {
        return CODEC;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, CARGO, WATERLOGGED);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * Gives its signal to every side except the block below it, unlike a detector rail. That way a hopper
     * under the rail isn't locked by the signal and can unload the cart on top. It never powers blocks directly.
     */
    @Override
    protected int getSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? 0 : state.getValue(CARGO);
    }

    @Override
    protected int getDirectSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(CARGO);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof AbstractMinecart) {
            check(level, pos, state);
        }
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        check(level, pos, state);
    }

    private void check(Level level, BlockPos pos, BlockState state) {
        if (!state.is(this)) return;
        int cargo = 0;
        AABB box =
                new AABB(
                        pos.getX() + 0.2,
                        pos.getY(),
                        pos.getZ() + 0.2,
                        pos.getX() + 0.8,
                        pos.getY() + 0.8,
                        pos.getZ() + 0.8);
        List<AbstractMinecart> carts =
                level.getEntitiesOfClass(
                        AbstractMinecart.class, box, EntitySelector.ENTITY_STILL_ALIVE);
        for (AbstractMinecart cart : carts) {
            cargo = Math.max(cargo, cargoOf(cart));
        }
        if (state.getValue(CARGO) != cargo) {
            level.setBlock(pos, state.setValue(CARGO, cargo), 3);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        // Keep looking while a cart is here or the signal is on, so it clears when the cart leaves
        // or is emptied.
        if (cargo > 0 || !carts.isEmpty()) {
            level.scheduleTick(pos, this, 5);
        }
    }

    /** How full a cart is, 0 to 15. */
    public static int cargoOf(AbstractMinecart cart) {
        if (cart
                instanceof
                net.luckystudio.spelunkers_charm.entity.custom.minecart.MineCrateMinecart crate)
            return fullness(
                    crate.storage().count(),
                    net.luckystudio.spelunkers_charm.block.custom.minecrate.CrateStorage.CAPACITY);
        if (cart
                instanceof
                net.luckystudio.spelunkers_charm.entity.custom.minecart.TankMinecart tank)
            return fullness(tank.storage().getFluidAmount(), tank.storage().getCapacity());
        if (cart instanceof BlockMinecart blocks) return blocks.getComparatorLevel();
        if (cart instanceof JukeboxMinecart jukebox) return jukebox.getComparatorLevel();
        if (cart instanceof Container container)
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        return 0;
    }

    private static int fullness(int amount, int capacity) {
        return amount <= 0 ? 0 : 1 + (int) (14L * Math.min(amount, capacity) / capacity);
    }
}
