package mod.gottsch.neo.gmm.core.entity.ai.goal.thrall;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Standing {@link ThrallOrder#STAY} order: holds the current position, refusing to wander or
 * approach. Runs at a high priority (added by {@link Ownership#enthrall}) so its shared
 * {@link Goal.Flag#MOVE} flag pre-empts whatever wander/stroll goal the underlying mob already has --
 * this is what lets STAY work on a vanilla mob (e.g. an enthralled zombie) without touching its
 * existing goal set. Steps aside the instant the mob acquires a combat target, so it still fights
 * back if attacked; it just never approaches or wanders.
 *
 * @author Mark Gottschling
 */
public class ThrallStayGoal extends Goal {
    private final Mob mob;
    private final IOwnable ownable;

    public ThrallStayGoal(Mob mob) {
        this.mob = mob;
        this.ownable = Ownership.of(mob);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean shouldStay() {
        return ownable.getOwnershipType() == OwnershipType.THRALL
                && ownable.getThrallOrder() == ThrallOrder.STAY
                && mob.getTarget() == null;
    }

    @Override
    public boolean canUse() {
        return shouldStay();
    }

    @Override
    public boolean canContinueToUse() {
        return shouldStay();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
    }
}
