// Minecart motion visuals.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

public interface MinecartMotionVisuals {
    MinecartMotionState spelunkersCharm$motion();

    float spelunkersCharm$braking();

    void spelunkersCharm$setBraking(float strength);

    int spelunkersCharm$replacedCartId();

    void spelunkersCharm$setReplacedCartId(int id);

    void spelunkersCharm$setOnRails(boolean rails);

    boolean spelunkersCharm$airborne();

    void spelunkersCharm$setAirborne(boolean airborne);

    float spelunkersCharm$flightYaw();

    float spelunkersCharm$flightPitch();

    void spelunkersCharm$setFlightPose(float yaw, float pitch);

    int spelunkersCharm$hookSides();

    void spelunkersCharm$setHookSides(int sides);

    float spelunkersCharm$bodyYaw();

    void spelunkersCharm$setBodyYaw(float yaw);

    boolean spelunkersCharm$flightTrain();

    void spelunkersCharm$setFlightTrain(boolean active);
}
