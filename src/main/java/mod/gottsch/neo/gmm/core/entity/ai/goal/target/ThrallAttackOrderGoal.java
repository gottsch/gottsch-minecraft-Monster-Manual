package mod.gottsch.neo.gmm.core.entity.ai.goal.target;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;

/**
 * Standing {@link ThrallOrder#ATTACK} order: holds the {@link Goal.Flag#TARGET} flag for a thrall
 * whose owner issued {@link Ownership#issueAttackOrder}, which sets {@link Mob#setTarget} directly
 * before this goal ever runs. This goal doesn't pick the target itself -- it just outranks the mob's
 * other target goals (HurtByTargetGoal, NearestAttackableTargetGoal, etc.) so none of them steal the
 * TARGET flag and overwrite the owner's designated target.
 * <p>
 * One-shot: once the target dies or otherwise can't be resolved, the order reverts to
 * {@link ThrallOrder#FOLLOW} and the mob's normal target goals take back over.
 *
 * @author Mark Gottschling
 */
public class ThrallAttackOrderGoal extends TargetGoal {
    private final IOwnable ownable;

    public ThrallAttackOrderGoal(Mob mob) {
        super(mob, false);
        this.ownable = Ownership.of(mob);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return ownable.getOwnershipType() == OwnershipType.THRALL
                && ownable.getThrallOrder() == ThrallOrder.ATTACK
                && this.mob.getTarget() != null
                && this.mob.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        if (ownable.getThrallOrder() == ThrallOrder.ATTACK) {
            ownable.setThrallOrder(ThrallOrder.FOLLOW);
        }
        super.stop();
    }
}
