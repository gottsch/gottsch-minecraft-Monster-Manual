package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantMovement;

/**
 * Contract for a ground-walking humanoid that can launch into flight.
 *
 * @author by Mark Gottschling on 7/26/2025
 */
public interface IWingedHumanoid {
    String IS_FLYING_TAG = "isFlying";

    boolean isWalking();

    boolean isFlying();

    void setMovementState(VolantMovement state);

    boolean isLaunching();

    boolean isLanding();

    boolean isLandAttackOnly();

    /**
     * Maximum height (in blocks above the launch position) this mob will climb to
     * when airborne. A true flyer (Gargoyle) returns a larger value; a low hoverer
     * (Margoyle) returns a small one.
     */
    double getMaxFlyHeight();
}
