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
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
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
        attachRail(subLevel);
        driver.tick(deckY(subLevel));
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        LiftSubLevelDriver driver = this.driver;
        if (driver == null || handle == null || !handle.isValid()) return;
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
        BlockPos pos = getBlockPos();
        Vector3d local = new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return subLevel.logicalPose().transformPosition(local, new Vector3d()).y;
    }

    /** Pin the deck to a vertical rail. The pin isn't saved, so it's added again after loading. */
    private void attachRail(ServerSubLevel subLevel) {
        if (rail != null && rail.isValid()) return;
        rail = null;
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
        super.setRemoved();
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
        driver = null;
    }
}
