package mod.gottsch.neo.gmm.core.entity.ai.goal;

import mod.gottsch.neo.gmm.core.entity.monster.IDormantMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Holds a {@link IDormantMob} still while it is scenery, and wakes it when someone comes close.
 *
 * <h2>Why a goal rather than {@code setNoAi}</h2>
 * <p>{@code setNoAi(true)} is the obvious way to freeze a mob and the wrong one here: it stops the
 * mob being ticked as a mob at all, which also stops gravity. A statue spawned a block above its
 * floor would hang there for ever, and one placed by a spawner would never settle. This goal takes
 * <strong>every flag</strong> at priority 0 instead, so no other goal can run while it holds them,
 * and the mob keeps falling, burning, drowning and taking hits like anything else.
 *
 * <p>It also covers the wake-up window ({@link IDormantMob#isInert()}), so the mob owns no goals
 * until its animation has played. Without that the first thing a player sees is a statue landing a
 * hit, which reads as a bug rather than as an ambush.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class DormancyGoal extends Goal {
    /**
     * Ticks a freshly spawned statue ignores everyone for.
     *
     * <p>Without it a mob placed from a spawn egg wakes on the tick it appears -- the player is
     * standing right there, holding the egg -- so the statue is never seen and the rise reads as a
     * glitch rather than as an ambush (Mark, 2026-09-13: "when placing the egg, the colossus is
     * already standing up"). Three seconds is long enough to look at it and short enough not to
     * feel like a delay in an arena, where the player arrives from across the room anyway.</p>
     */
    private static final int SPAWN_GRACE_TICKS = 60;

    /**
     * Server-side ticks since the rise began. The goal owns this because the goal is the only
     * thing that runs while the mob is inert, and because the timing must NOT be synched -- see
     * {@link IDormantMob} for the two cuts that got that wrong.
     */
    private int wakeTicks;

    private final Mob mob;
    private final IDormantMob dormant;
    private final double wakeRange;

    public DormancyGoal(Mob mob, double wakeRange) {
        this.mob = mob;
        this.dormant = (IDormantMob) mob;
        this.wakeRange = wakeRange;
        // Every flag the goal selector hands out, deliberately: this goal exists to starve the
        // others of them. NOTE it cannot starve the TARGET goals -- Mob keeps a SEPARATE
        // GoalSelector for targeting with its own locked flags -- so a dormant mob may still be
        // handed a target. tick() clears it rather than relying on the flag.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return this.dormant.isInert();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dormant.isInert();
    }

    @Override
    public void start() {
        this.wakeTicks = 0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        // A statue does not drift: whatever pushed it (a piston, a player shoving past) is undone
        // rather than accumulated, but vertical motion is left alone so it still falls.
        this.mob.setDeltaMovement(0.0D, this.mob.getDeltaMovement().y, 0.0D);

        if (this.dormant.isDormant()) {
            this.mob.setTarget(null);
            if (this.mob.tickCount < SPAWN_GRACE_TICKS) {
                return;
            }
            Player player = this.mob.level().getNearestPlayer(this.mob, this.wakeRange);
            // Creative and spectator players walk past without setting it off -- the first is
            // Mark authoring, the second cannot be attacked anyway.
            if (player != null && !player.isCreative() && !player.isSpectator()) {
                this.dormant.wake();
                this.wakeTicks = 0;
            }
            return;
        }

        // The rise. Counted here, on the server, and published as a flag: the clients each time
        // the animation against their own counter. See IDormantMob.
        if (this.dormant.isWaking() && ++this.wakeTicks >= this.dormant.getWakeDuration()) {
            this.dormant.setWaking(false);
        }
    }
}
