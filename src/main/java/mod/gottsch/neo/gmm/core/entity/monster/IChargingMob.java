package mod.gottsch.neo.gmm.core.entity.monster;

/**
 * Implemented by a GMM mob that wants a visible tell while {@code ChargeAttackGoal} is winding up
 * and running -- typically a lowered head and a flattened body, so a player can read "it is about
 * to run at me" before it does.
 *
 * <p>Same shape and same contract as {@link ICastingMob}: the goal calls
 * {@link #setCharging(boolean)} at {@code start()}/{@code stop()} only if the mob implements this,
 * always via {@code instanceof} and never a hard cast, so a charging mob that wants no pose change
 * simply does not implement it.
 *
 * @author Mark Gottschling on 9/7/2026
 */
public interface IChargingMob {
    void setCharging(boolean charging);

    boolean isCharging();
}
