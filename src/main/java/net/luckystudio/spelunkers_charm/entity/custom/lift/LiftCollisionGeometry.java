package net.luckystudio.spelunkers_charm.entity.custom.lift;

/** Arithmetic shared by the physical deck and its passenger collision tests. */
public final class LiftCollisionGeometry {
    public static final double EPSILON = 1.0E-5;
    public static final double SUPPORT_TOLERANCE = 0.1;

    private LiftCollisionGeometry() {}

    public static boolean overlaps(double minA, double maxA, double minB, double maxB) {
        return maxA > minB + EPSILON && minA < maxB - EPSILON;
    }

    /**
     * Tests the swept upper face, including the explicit pull required on descent. A positive
     * passenger velocity is a jump, and must not be overwritten.
     */
    public static boolean supports(
            double oldTop,
            double newTop,
            double feet,
            double previousFeet,
            double velocityY,
            boolean allowPull) {
        if (velocityY > EPSILON) {
            return false;
        }
        boolean crossedTop = previousFeet >= oldTop - EPSILON && feet <= newTop + EPSILON;
        if (newTop >= oldTop) {
            return (feet >= oldTop - SUPPORT_TOLERANCE && feet <= newTop + SUPPORT_TOLERANCE)
                    || crossedTop;
        }
        boolean nearOldTop =
                feet >= oldTop - SUPPORT_TOLERANCE && feet <= oldTop + SUPPORT_TOLERANCE;
        boolean followingDeck =
                feet >= newTop - SUPPORT_TOLERANCE
                        && feet <= oldTop + SUPPORT_TOLERANCE
                        && previousFeet >= oldTop - SUPPORT_TOLERANCE;
        return crossedTop || (allowPull && (nearOldTop || followingDeck));
    }

    public static boolean pushedByBottom(
            double oldBottom, double newBottom, double head, double previousHead) {
        return newBottom < oldBottom - EPSILON
                && previousHead <= oldBottom + EPSILON
                && head >= newBottom - EPSILON;
    }
}
