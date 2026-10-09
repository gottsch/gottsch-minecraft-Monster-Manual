package mod.gottsch.neo.gmm.core.entity.ai.goal.thrall;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Standing {@link ThrallOrder#FOLLOW} order: stays near its owner when it has no combat target.
 * Added by {@link Ownership#enthrall} to any enthralled mob's own {@code goalSelector}, regardless of
 * whether that mob is a GMM mob or not.
 *
 * @author Mark Gottschling
 */
public class ThrallFollowOwnerGoal extends Goal {
    private static final double START_DISTANCE = 10.0D;
    private static final double STOP_DISTANCE = 2.0D;

    private final Mob mob;
    private final IOwnable ownable;
    private final double speedModifier;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public ThrallFollowOwnerGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.ownable = Ownership.of(mob);
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (ownable.getOwnershipType() != OwnershipType.THRALL || ownable.getThrallOrder() != ThrallOrder.FOLLOW) {
            return false;
        }
        if (mob.getTarget() != null) {
            // fighting takes priority; the mob's own attack-approach goal owns movement instead.
            return false;
        }
        LivingEntity candidate = Ownership.resolveOwner(mob);
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }
        if (mob.distanceToSqr(candidate) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && mob.getTarget() == null
                && !mob.getNavigation().isDone()
                && mob.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            if (!mob.isLeashed() && !mob.isPassenger()) {
                mob.getNavigation().moveTo(owner, speedModifier);
            }
        }
    }
}
