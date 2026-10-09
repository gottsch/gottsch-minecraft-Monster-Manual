package mod.gottsch.neo.gmm.core.entity.ai.goal.thrall;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Standing {@link ThrallOrder#GUARD} order: holds a specific bound position ({@link
 * IOwnable#getGuardPos()}), issued by the owner via {@link Ownership#issueGuardOrder}, returning to
 * it once idle. Unlike {@link ThrallFollowOwnerGoal} this position is independent of the owner's own
 * location, so a thrall can be posted somewhere the owner never goes.
 *
 * @author Mark Gottschling
 */
public class ThrallGuardGoal extends Goal {
    private static final double START_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 2.0D;

    private final Mob mob;
    private final IOwnable ownable;
    private final double speedModifier;
    private int timeToRecalcPath;

    public ThrallGuardGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.ownable = Ownership.of(mob);
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (ownable.getOwnershipType() != OwnershipType.THRALL || ownable.getThrallOrder() != ThrallOrder.GUARD) {
            return false;
        }
        if (mob.getTarget() != null) {
            return false;
        }
        BlockPos guardPos = ownable.getGuardPos();
        return guardPos != null && mob.distanceToSqr(Vec3.atCenterOf(guardPos)) > START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) {
            return false;
        }
        BlockPos guardPos = ownable.getGuardPos();
        return guardPos != null && !mob.getNavigation().isDone()
                && mob.distanceToSqr(Vec3.atCenterOf(guardPos)) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos guardPos = ownable.getGuardPos();
        if (guardPos == null) {
            return;
        }
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            Vec3 pos = Vec3.atCenterOf(guardPos);
            mob.getNavigation().moveTo(pos.x, pos.y, pos.z, speedModifier);
        }
    }
}
