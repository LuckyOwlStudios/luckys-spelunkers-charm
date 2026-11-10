package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-confirmed client support; aligns after vanilla interpolation and while rendering. */
public final class LiftClientSupport {
    private static final double TRANSITION_TICKS = 3.0;
    private static final Map<Level, Map<Integer, Anchor>> ANCHORS = new WeakHashMap<>();

    private LiftClientSupport() {}

    /**
     * Run once after vanilla has ticked/interpolated every entity. This keeps remote hitboxes and
     * frustum bounds on their server-confirmed deck too; moving them during the lift's own tick
     * made the result depend on order.
     */
    public static void alignRemoteBounds(Level level) {
        Map<Integer, Anchor> anchors = ANCHORS.get(level);
        if (anchors == null) return;
        var iterator = anchors.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Anchor anchor = entry.getValue();
            Entity root = level.getEntity(entry.getKey());
            Entity candidate = level.getEntity(anchor.liftId);
            if (root == null
                    || root.isRemoved()
                    || (anchor.liftId >= 0 && !(candidate instanceof AbstractLift))) {
                if (level.getGameTime() - anchor.start > 100) iterator.remove();
                continue;
            }
            if (anchor.liftId < 0 || root.isPassenger() || controlsLocally(root)) continue;
            AbstractLift lift = (AbstractLift) candidate;
            double oldShift = lift.yOld + anchor.relativeY - root.yOld;
            root.setPos(root.getX(), lift.getY() + anchor.relativeY, root.getZ());
            root.yOld = lift.yOld + anchor.relativeY;
            root.yo = root.yOld;
            root.setDeltaMovement(root.getDeltaMovement().x, 0, root.getDeltaMovement().z);
            root.setOnGround(true);
            alignPassengers(root, oldShift);
        }
    }

    private static void alignPassengers(Entity root, double oldShift) {
        for (Entity passenger : root.getPassengers()) {
            root.positionRider(passenger);
            passenger.yOld += oldShift;
            passenger.yo += oldShift;
            alignPassengers(passenger, oldShift);
        }
    }

    /** True for the local player and the root vehicle controlled by that player. */
    public static boolean controlsLocally(Entity entity) {
        return entity instanceof Player player && player.isLocalPlayer()
                || entity.getControllingPassenger() instanceof Player controller
                        && controller.isLocalPlayer();
    }

    public static void accept(Level level, LiftSupportPayload payload) {
        if (!level.isClientSide) return;
        Map<Integer, Anchor> anchors = ANCHORS.computeIfAbsent(level, ignored -> new HashMap<>());
        Anchor previous = anchors.get(payload.entityId());
        if (payload.liftId() < 0) {
            if (previous != null) {
                previous.liftId = -1;
                previous.start = level.getGameTime();
                previous.departureOffset = previous.lastOffset;
            }
        } else if (previous == null || previous.liftId != payload.liftId()) {
            anchors.put(
                    payload.entityId(),
                    new Anchor(payload.liftId(), payload.relativeY(), level.getGameTime()));
        } else {
            previous.relativeY = payload.relativeY();
        }
    }

    /** Offset added only during rendering, inherited by every mounted passenger. */
    public static double getRenderOffset(Entity entity, float partialTick) {
        if (!entity.level().isClientSide) return 0;
        Entity root = entity.getRootVehicle();
        if (controlsLocally(root)) return 0;
        Map<Integer, Anchor> anchors = ANCHORS.get(entity.level());
        if (anchors == null) return 0;
        Anchor anchor = anchors.get(root.getId());
        if (anchor == null) return 0;
        double age = Math.max(0, entity.level().getGameTime() + partialTick - anchor.start);
        if (anchor.liftId < 0) {
            if (age >= TRANSITION_TICKS) {
                anchors.remove(root.getId());
                return 0;
            }
            anchor.lastOffset = anchor.departureOffset * (1.0 - age / TRANSITION_TICKS);
            return anchor.lastOffset;
        }
        Entity candidate = entity.level().getEntity(anchor.liftId);
        if (!(candidate instanceof AbstractLift lift) || lift.isRemoved() || root.isRemoved())
            return 0;
        double deckY = Mth.lerp(partialTick, lift.yOld, lift.getY());
        double rootY = Mth.lerp(partialTick, root.yOld, root.getY());
        double offset = deckY + anchor.relativeY - rootY;
        anchor.lastOffset = offset * Math.min(1.0, age / TRANSITION_TICKS);
        return anchor.lastOffset;
    }

    private static final class Anchor {
        int liftId;
        float relativeY;
        long start;
        double lastOffset;
        double departureOffset;

        Anchor(int liftId, float relativeY, long start) {
            this.liftId = liftId;
            this.relativeY = relativeY;
            this.start = start;
        }
    }
}
