package net.luckystudio.spelunkers_charm.block.custom;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientEffects;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSounds;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/**
 * Client side of a Sable lift. Keeps a never-spawned lift entity in step with the deck, so the
 * lift's existing model animation, gear sparks and motor sound can be reused as they are.
 */
public final class LiftControllerClient {
    private LiftControllerClient() {}

    public static void tick(Level level, BlockPos pos, BlockState state, LiftControllerBlockEntity be) {
        AbstractLift.Type type = be.getLiftType();
        if (type == null) return;

        // Where the deck really is: the controller's bottom center, moved out of Sable's plot.
        Vector3d world = new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (Sable.HELPER.getContainingClient(be) instanceof SubLevel subLevel)
            subLevel.logicalPose().transformPosition(world, world);

        AbstractLift lift = be.getClientLift();
        boolean fresh = lift == null;
        if (fresh) {
            lift =
                    switch (type) {
                        case SMALL -> new SmallLift(level, world.x, world.y, world.z);
                        case MEDIUM -> new MediumLift(level, world.x, world.y, world.z);
                        case LARGE -> new LargeLift(level, world.x, world.y, world.z);
                    };
            be.setClientLift(lift);
        }
        double oldY = fresh ? world.y : lift.getY();
        lift.xo = lift.getX();
        lift.yo = oldY;
        lift.zo = lift.getZ();
        lift.setPos(world.x, world.y, world.z);
        lift.setYRot(be.getFacing().toYRot());
        lift.setMovementType(be.getShownMovement());
        lift.setWoodType(be.getWood());
        lift.setMineralType(be.getMineral());
        lift.tickCount++;

        double dy = world.y - oldY;
        LiftClientSounds.update(lift, dy);
        MovementType movement = be.getShownMovement();
        if (Math.abs(dy) > 1.0E-6 && (movement == MovementType.UP || movement == MovementType.DOWN))
            LiftClientEffects.spawnGearSparks(lift, dy);
    }
}
