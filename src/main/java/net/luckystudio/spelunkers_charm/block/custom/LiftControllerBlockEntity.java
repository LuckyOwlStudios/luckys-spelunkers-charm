package net.luckystudio.spelunkers_charm.block.custom;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import com.mojang.logging.LogUtils;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftSubLevelDriver;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.datagen.itemTags.ModItemTags;
import net.luckystudio.spelunkers_charm.entity.util.MineralType;
import net.luckystudio.spelunkers_charm.entity.util.WoodType;
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.world.phys.AABB;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumSet;
import java.util.Locale;

/**
 * The heart of a Sable lift. It sits in the middle of the deck, inside the deck's sub-level, and
 * steers that sub-level up and down the lift tracks.
 *
 * <p>The deck is pinned to the world so it can only slide straight up and down: no sideways drift
 * and no tipping, however lopsided the thing built on it is. Each physics step this sets the deck's
 * vertical speed to what {@link LiftSubLevelDriver} asks for.
 */
public class LiftControllerBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** Everything but vertical sliding is locked. */
    private static final EnumSet<ConstraintJointAxis> LOCKED_AXES =
            EnumSet.of(
                    ConstraintJointAxis.LINEAR_X,
                    ConstraintJointAxis.LINEAR_Z,
                    ConstraintJointAxis.ANGULAR_X,
                    ConstraintJointAxis.ANGULAR_Y,
                    ConstraintJointAxis.ANGULAR_Z);

    @Nullable private AbstractLift.Type type;
    private Direction facing = Direction.NORTH;
    private double shaftX, shaftZ;

    @Nullable private LiftSubLevelDriver driver;
    @Nullable private GenericConstraintHandle rail;
    private boolean warnedRail;
    private MovementType savedMovement = MovementType.STATIONARY;
    private MovementType savedPreferred = MovementType.UP;
    private int savedHoldRow = Integer.MIN_VALUE;
    private WoodType wood = WoodType.SPRUCE;
    private MineralType mineral = MineralType.IRON;
    /** Last movement sent to clients, so they only hear about changes. */
    private MovementType syncedMovement = MovementType.STATIONARY;
    private boolean dismantling;

    /** Client only: a never-spawned lift entity the renderer, sounds and sparks read from. */
    @Nullable private AbstractLift clientLift;

    public LiftControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityType.LIFT_CONTROLLER.get(), pos, state);
    }

    /** Called on the world block just before it's assembled into a sub-level. */
    public void setup(AbstractLift.Type type, Direction facing, double shaftX, double shaftZ) {
        this.type = type;
        this.facing = facing;
        this.shaftX = shaftX;
        this.shaftZ = shaftZ;
        this.driver = null;
        setChanged();
    }

    @Nullable
    public AbstractLift.Type getLiftType() {
        return type;
    }

    public Direction getFacing() {
        return facing;
    }

    public double getShaftX() {
        return shaftX;
    }

    public double getShaftZ() {
        return shaftZ;
    }

    public WoodType getWood() {
        return wood;
    }

    public MineralType getMineral() {
        return mineral;
    }

    /** What clients should show: the server's movement as last synced. */
    public MovementType getShownMovement() {
        return savedMovement;
    }

    @Nullable
    public AbstractLift getClientLift() {
        return clientLift;
    }

    public void setClientLift(@Nullable AbstractLift lift) {
        this.clientLift = lift;
    }

    /** Half the deck's width: 1, 2 or 3 blocks out from the controller. */
    public int radius() {
        return radius(type);
    }

    public static int radius(@Nullable AbstractLift.Type type) {
        if (type == null) return 0;
        return switch (type) {
            case SMALL -> 1;
            case MEDIUM -> 2;
            case LARGE -> 3;
        };
    }

    /** The controller whose deck includes {@code pos}, looking around a lift part. */
    @Nullable
    public static LiftControllerBlockEntity find(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LiftControllerBlockEntity self) return self;
        for (int dx = -3; dx <= 3; ++dx)
            for (int dz = -3; dz <= 3; ++dz) {
                if (level.getBlockEntity(pos.offset(dx, 0, dz))
                                instanceof LiftControllerBlockEntity controller
                        && Math.abs(dx) <= controller.radius()
                        && Math.abs(dz) <= controller.radius()) return controller;
            }
        return null;
    }

    @Nullable
    private LiftSubLevelDriver driver() {
        if (driver == null && type != null && level != null && !level.isClientSide) {
            driver = new LiftSubLevelDriver(level, type, facing, shaftX, shaftZ);
            driver.restore(savedMovement, savedPreferred, savedHoldRow);
        }
        return driver;
    }

    // ---------------------------------------------------------------- Sable hooks

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        LiftSubLevelDriver driver = driver();
        if (driver == null) return;
        Vector3d deck = deckCenter(subLevel);
        driver.tick(deck.x, deck.y, deck.z, worldBounds(subLevel));
        // On tracks it rides a rail; off them it's a loose physics object that falls and tumbles.
        if (driver.onTracks() && isUpright(subLevel)) attachRail(subLevel);
        else dropRail();
        if (driver.movement() != syncedMovement) {
            syncedMovement = driver.movement();
            savedMovement = syncedMovement;
            sync();
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        LiftSubLevelDriver driver = this.driver;
        if (driver == null || rail == null || handle == null || !handle.isValid()) return;
        double vy = driver.targetVelocity(deckY(subLevel), timeStep);
        if (Double.isNaN(vy)) return;
        Vector3d linear = handle.getLinearVelocity(new Vector3d());
        Vector3d angular = handle.getAngularVelocity(new Vector3d());
        // Replace the current velocity outright: only the vertical speed we want, no spin.
        handle.addLinearAndAngularVelocity(
                new Vector3d(-linear.x, vy - linear.y, -linear.z), angular.negate());
    }

    /** World Y of the bottom of the deck: this block's bottom face, moved out of the plot. */
    private double deckY(ServerSubLevel subLevel) {
        return deckCenter(subLevel).y;
    }

    /** World position of the deck's bottom center. */
    private Vector3d deckCenter(ServerSubLevel subLevel) {
        BlockPos pos = getBlockPos();
        Vector3d local = new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return subLevel.logicalPose().transformPosition(local, new Vector3d());
    }

    /**
     * World box around every block in the sub-level (the deck and anything built on it). Only
     * meaningful while upright, which is the only time the driver moves it.
     */
    @Nullable
    private static AABB worldBounds(ServerSubLevel subLevel) {
        BoundingBox3ic plot = subLevel.getPlot().getBoundingBox();
        if (plot == null || plot.volume() <= 0) return null;
        Vector3d a =
                subLevel.logicalPose()
                        .transformPosition(new Vector3d(plot.minX(), plot.minY(), plot.minZ()), new Vector3d());
        Vector3d b =
                subLevel.logicalPose()
                        .transformPosition(
                                new Vector3d(plot.maxX() + 1, plot.maxY() + 1, plot.maxZ() + 1), new Vector3d());
        return new AABB(a.x, a.y, a.z, b.x, b.y, b.z); // AABB sorts the corners itself
    }

    /**
     * A deck that fell off and landed tipped over or turned can't go back on a rail, even if it
     * happens to land between tracks: it would lock in crooked. Within about 3 degrees counts.
     */
    private static boolean isUpright(ServerSubLevel subLevel) {
        Quaterniond q = new Quaterniond(subLevel.logicalPose().orientation()).normalize();
        return Math.abs(q.w) > Math.cos(Math.toRadians(3.0) / 2.0);
    }

    /** Pin the deck to a vertical rail. The pin isn't saved, so it's added again after loading. */
    private void attachRail(ServerSubLevel subLevel) {
        if (rail != null && rail.isValid()) return;
        rail = null;
        if (warnedRail) return;
        if (!(SubLevelContainer.getContainer(subLevel.getLevel())
                instanceof ServerSubLevelContainer container)) return;
        MassData mass = subLevel.getMassTracker();
        if (mass == null || mass.isInvalid() || mass.getCenterOfMass() == null) return;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        Vector3d localAnchor = new Vector3d(mass.getCenterOfMass());
        Vector3d worldAnchor = subLevel.logicalPose().transformPosition(localAnchor, new Vector3d());
        Quaterniond worldFrame = new Quaterniond();
        Quaterniond bodyFrame =
                new Quaterniond(subLevel.logicalPose().orientation()).invert().mul(worldFrame).normalize();
        GenericConstraintConfiguration config =
                new GenericConstraintConfiguration(
                        worldAnchor, localAnchor, worldFrame, bodyFrame, EnumSet.copyOf(LOCKED_AXES));
        try {
            rail = pipeline.addConstraint(null, subLevel, config);
        } catch (RuntimeException e) {
            rail = null;
        }
        if ((rail == null || !rail.isValid()) && !warnedRail) {
            warnedRail = true;
            LOGGER.warn("Sable refused the rail for the lift at {}", getBlockPos());
        }
    }

    // ---------------------------------------------------------------- player use

    public void use(Player player) {
        LiftSubLevelDriver driver = driver();
        if (driver == null || level == null) return;
        ServerSubLevel subLevel = containingSubLevel();
        if (subLevel == null) return;
        driver.use(player, deckY(subLevel));
    }

    @Nullable
    private ServerSubLevel containingSubLevel() {
        if (level == null || level.isClientSide) return null;
        return Sable.HELPER.getContaining(this) instanceof ServerSubLevel server ? server : null;
    }

    /** Planks change the wood, minerals change the metal. Returns whether the item was used. */
    public boolean restyle(ItemStack stack) {
        WoodType newWood = WoodType.getWoodType(stack);
        if (newWood != null && newWood != wood) {
            wood = newWood;
            sync();
            return true;
        }
        MineralType newMineral = stack.is(ModItemTags.MINERALS) ? MineralType.getMineralType(stack) : null;
        if (newMineral != null && newMineral != mineral) {
            mineral = newMineral;
            sync();
            return true;
        }
        return false;
    }

    /**
     * Breaks the whole lift: every deck block and this controller. Gives the lift item back unless
     * the player breaking it is in creative.
     */
    public void dismantle(@Nullable Player breaker) {
        if (dismantling || level == null || level.isClientSide) return;
        dismantling = true;
        onBroken();
        BlockPos center = getBlockPos();
        int r = radius();
        for (int dx = -r; dx <= r; ++dx)
            for (int dz = -r; dz <= r; ++dz) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!pos.equals(center) && level.getBlockState(pos).getBlock() instanceof LiftPartBlock)
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        if (type != null && (breaker == null || !breaker.getAbilities().instabuild))
            Block.popResource(level, center, new ItemStack(liftItem(type)));
        level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static Item liftItem(AbstractLift.Type type) {
        return switch (type) {
            case SMALL -> ModItems.SMALL_LIFT.get();
            case MEDIUM -> ModItems.MEDIUM_LIFT.get();
            case LARGE -> ModItems.LARGE_LIFT.get();
        };
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** The controller was broken: free the tracks and let the deck go. */
    public void onBroken() {
        if (driver != null) driver.clearGuides();
        dropRail();
    }

    private void dropRail() {
        if (rail != null && rail.isValid()) rail.remove();
        rail = null;
    }

    @Override
    public void setRemoved() {
        dropRail();
        if (clientLift != null) clientLift.discard();
        clientLift = null;
        super.setRemoved();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (type == null) return;
        tag.putString("liftType", type.name().toLowerCase(Locale.ROOT));
        tag.putString("facing", facing.getSerializedName());
        tag.putDouble("shaftX", shaftX);
        tag.putDouble("shaftZ", shaftZ);
        LiftSubLevelDriver d = driver;
        tag.putString("movement", (d != null ? d.movement() : savedMovement).getSerializedName());
        tag.putString("preferred", (d != null ? d.preferred() : savedPreferred).getSerializedName());
        tag.putInt("holdRow", d != null ? d.holdRow() : savedHoldRow);
        tag.putString("woodType", wood.getSerializedName());
        tag.putString("mineralType", mineral.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("liftType", 8)) return;
        try {
            type = AbstractLift.Type.valueOf(tag.getString("liftType").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = null;
            return;
        }
        Direction saved = Direction.byName(tag.getString("facing"));
        facing = saved != null && saved.getAxis().isHorizontal() ? saved : Direction.NORTH;
        shaftX = tag.getDouble("shaftX");
        shaftZ = tag.getDouble("shaftZ");
        savedMovement = MovementType.byName(tag.getString("movement"));
        savedPreferred = MovementType.byName(tag.getString("preferred"));
        savedHoldRow = tag.contains("holdRow", 3) ? tag.getInt("holdRow") : Integer.MIN_VALUE;
        if (tag.contains("woodType", 8)) wood = WoodType.byName(tag.getString("woodType"));
        if (tag.contains("mineralType", 8)) mineral = MineralType.byName(tag.getString("mineralType"));
        // Clients get this tag as updates come in; only the server owns a driver.
        if (level == null || !level.isClientSide) driver = null;
    }
}
