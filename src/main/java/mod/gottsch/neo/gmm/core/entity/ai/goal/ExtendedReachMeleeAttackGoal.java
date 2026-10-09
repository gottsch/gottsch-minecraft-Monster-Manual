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
 * <b>This class exists because overriding {@code Mob#getMeleeAttackRangeSqr} does not work.</b> That
 * looks like the obvious hook, and it is what other vanilla code paths consult, but in 1.20.1
 * {@link MeleeAttackGoal} never calls it — its own {@code getAttackReachSqr} recomputes the range
 * inline from {@code mob.getBbWidth()}, so a {@code Mob}-level override is silently ignored and the
 * mob keeps attacking at its default range. Verified against the 1.20.1 bytecode. The only way to
 * widen the range this goal actually uses is to override the goal's own method, which is what happens
 * here.
 * <p>
 * The bonus is supplied rather than fixed so it can be read live from a mob's config, and so a mob can
 * return zero when it should not currently have the reach at all (a Black Pudding's split children
 * have no tendrils, so they get no lash and no extra range).
 *
 * @author Mark Gottschling on 8/24/2026
 */
public class ExtendedReachMeleeAttackGoal extends MeleeAttackGoal {

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
     * Vanilla's value is a squared distance, so the bonus cannot simply be added to it — it has to be
     * unsquared, extended, and squared again.
     */
    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        double baseSqr = super.getAttackReachSqr(target);
        double bonus = reachBonusSupplier.getAsDouble();
        if (bonus <= 0.0D) {
            return baseSqr;
        }
        double reach = Math.sqrt(baseSqr) + bonus;
        return reach * reach;
    }
}
