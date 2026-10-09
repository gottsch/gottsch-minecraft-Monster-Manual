package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import java.util.function.DoubleSupplier;

/**
 * A {@link MeleeAttackGoal} that strikes from further away than the mob's hitbox would normally allow —
 * for a monster whose reach comes from a limb rather than its body: {@code BlackPudding}'s tendril
 * lash, and (planned) the Roper/Reaper grapple tentacle.
 * <p>
 * <b>Why a goal and not only a {@code Mob} override.</b> In 1.20.1 {@link MeleeAttackGoal} ignored
 * {@code Mob#getMeleeAttackRangeSqr} and recomputed its range inline, so the only hook was the goal's own
 * {@code getAttackReachSqr}. 1.20.5 replaced that distance test with a bounding-box overlap
 * ({@code Mob#isWithinMeleeAttackRange} / {@code Mob#getAttackBoundingBox}), which the goal now does
 * consult. This goal keeps working for any mob by testing its own widened box as well, so a mob that
 * also widens {@code getAttackBoundingBox} (as {@code BlackPudding} does) is not counted twice: the
 * two boxes are the same size, and the test is "either one".
 * <p>
 * The bonus is supplied rather than fixed so it can be read live from a mob's config, and so a mob can
 * return zero when it should not currently have the reach at all (a Black Pudding's split children
 * have no tendrils, so they get no lash and no extra range).
 *
 * @author Mark Gottschling on 8/24/2026
 */
public class ExtendedReachMeleeAttackGoal extends MeleeAttackGoal {

    /** {@code Mob.DEFAULT_ATTACK_REACH}, which is private: how far vanilla inflates the hitbox to attack. */
    private static final double VANILLA_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;

    private final DoubleSupplier reachBonusSupplier;

    /**
     * @param reachBonusSupplier extra reach in blocks, queried each time the range is tested. Return
     *                           0 to fall back to exactly vanilla's behaviour.
     */
    public ExtendedReachMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen,
                                        DoubleSupplier reachBonusSupplier) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.reachBonusSupplier = reachBonusSupplier;
    }

    /**
     * Vanilla's test, OR the mob's hitbox widened by vanilla's reach plus the bonus. Horizontal only,
     * like vanilla's: the bonus is a longer arm, not a taller one.
     */
    @Override
    protected boolean canPerformAttack(LivingEntity target) {
        double bonus = reachBonusSupplier.getAsDouble();
        if (bonus <= 0.0D) {
            return super.canPerformAttack(target);
        }
        double reach = VANILLA_ATTACK_REACH + bonus;
        boolean inReach = this.mob.isWithinMeleeAttackRange(target)
                || this.mob.getBoundingBox().inflate(reach, 0.0D, reach).intersects(target.getHitbox());
        return this.isTimeToAttack() && inReach && this.mob.getSensing().hasLineOfSight(target);
    }
}
