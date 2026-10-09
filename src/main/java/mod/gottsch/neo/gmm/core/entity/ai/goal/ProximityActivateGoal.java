package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Blows a disguised mob's cover the moment a non-creative/non-spectator player wanders within range —
 * the proximity counterpart to {@code Mimic}'s hit/interact-triggered {@code activate()}. Runs
 * unconditionally (no {@link Flag}s claimed, so it never competes with or blocks any other goal) at
 * goal-selector priority 0.
 * <p>
 * The nearest-player scan is throttled to once every {@code scanInterval} ticks via
 * {@code mob.tickCount}, the same shape (and same default, 10 ticks) as {@code GraveZombie}'s own
 * dormant-scan ({@code tickBurrowed}'s {@code SCAN_INTERVAL}) — an unthrottled scan here would mean
 * every dormant instance (e.g. a dungeon room full of decoy armor stands) runs a nearest-player search
 * every single server tick for as long as it sits idle, for no perceptible benefit: a struck mob still
 * activates instantly regardless (see {@code AnimatedArmor#hurt}), this goal only needs to *feel*
 * responsive for proximity, and a sub-second window is imperceptible for that.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class ProximityActivateGoal extends Goal {
    private static final int DEFAULT_SCAN_INTERVAL = 10;

    private final Mob mob;
    private final double range;
    private final int scanInterval;
    private final Runnable onActivate;

    public ProximityActivateGoal(Mob mob, double range, Runnable onActivate) {
        this(mob, range, DEFAULT_SCAN_INTERVAL, onActivate);
    }

    public ProximityActivateGoal(Mob mob, double range, int scanInterval, Runnable onActivate) {
        this.mob = mob;
        this.range = range;
        this.scanInterval = scanInterval;
        this.onActivate = onActivate;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount % this.scanInterval != 0) {
            return false;
        }
        Player nearest = this.mob.level().getNearestPlayer(
                this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.range, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        return nearest != null;
    }

    @Override
    public void start() {
        this.onActivate.run();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
