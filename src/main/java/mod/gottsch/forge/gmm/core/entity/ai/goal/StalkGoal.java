package mod.gottsch.forge.gmm.core.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Keeps a mob pressing toward a target it <em>cannot reach</em>, standing as close under it as the
 * floor allows rather than giving up where it happens to be.
 *
 * <h2>The failure it exists for</h2>
 * <p>A melee boss whose target is out of reach -- on a ledge, across a pit, behind a wall, up on an
 * arena's rim -- has nothing to do. {@code MeleeAttackGoal} asks the navigation for a path, gets a
 * partial one or none, walks as far as that takes it and then simply stops. In an arena that reads
 * as <strong>a boss standing in the middle of the room ignoring you</strong>, which is the specific
 * thing this was written to fix: a colossus the player can plink at from a gallery while it does
 * nothing is not a fight.
 *
 * <p>So this goal does the one useful thing available: it puts the mob <em>underneath</em> the
 * player, facing them, where its own attacks (a slam, a thrown rock, or simply waiting for them to
 * come down) have a chance of mattering. It is deliberately not a teleport, a leap or a
 * wall-climb -- a slow mob that cannot follow you up should look like it is trying to, not like it
 * has a solution.
 *
 * <h2>How the stalk point is found</h2>
 * <p>Walking BACK from the target: the goal asks the navigation for a path to the target's own
 * position, and if that cannot be reached, steps the probe one block at a time along the line back
 * toward the mob until a reachable point is found. That is a handful of {@code createPath} calls,
 * once every {@link #REPATH_INTERVAL_TICKS} rather than every tick, and it finds the nearest
 * standable ground under the target instead of the nearest node on a path that gave up early.
 *
 * <p>Flags: MOVE and LOOK. It must out-rank the melee goal for the same reason
 * {@code SmashBlocksGoal} does -- a lower-priority goal cannot take a flag a higher one holds, so
 * with melee above it the mob would keep drifting under the melee goal's own navigation and never
 * commit. Its entry test is the exact complement of melee's, though: it runs ONLY while the target
 * cannot be reached, so it can never starve a mob that could be attacking instead.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class StalkGoal extends Goal {
    /** Probing every tick would mean a path search per tick per mob; the answer changes slowly. */
    private static final int REPATH_INTERVAL_TICKS = 20;
    /** How many one-block steps back from the target the probe will try before giving up. */
    private static final int MAX_PROBE_STEPS = 10;
    /** How often the (expensive) reachability question is re-asked. See {@code cachedCanReach}. */
    private static final int REACH_CHECK_INTERVAL_TICKS = 10;

    private final PathfinderMob mob;
    private final double speedModifier;
    private final double maxRangeSqr;
    /** Inside this the mob is close enough that shuffling further would just be jitter. */
    private final double arriveDistanceSqr;

    private int repathCooldown;
    private BlockPos stalkPos;
    /**
     * The reach test is a full path search, and {@code canUse} is polled every tick for every goal
     * that is not running -- so the answer is cached. Whether a target is reachable does not change
     * from one tick to the next, and the cost of being half a second stale is that the mob starts
     * or stops stalking half a second late.
     */
    private int reachCheckCooldown;
    private boolean cachedCanReach;

    public StalkGoal(PathfinderMob mob, double speedModifier, double maxRange, double arriveDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.maxRangeSqr = maxRange * maxRange;
        this.arriveDistanceSqr = arriveDistance * arriveDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (this.mob.distanceToSqr(target) > this.maxRangeSqr) {
            return false;
        }
        // ONLY when the target cannot be walked to. While it can, the melee goal owns the mob.
        return !canReach(target) && !inStrikingRange(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive()
                && this.mob.distanceToSqr(target) <= this.maxRangeSqr
                && !canReach(target)
                && !inStrikingRange(target);
    }

    /**
     * Whether the mob could be hitting this target instead of walking toward it.
     *
     * <p><strong>This goal outranks the melee goal and claims MOVE and LOOK, so without this test
     * it starves the attack.</strong> That is exactly what happened the first time it ran: a player
     * standing on a one-block step is often unreachable by the navigation, so the mob stalked --
     * shuffling under someone it could have hit, for ever (Mark, 2026-09-13: "i'm 2 blocks away, up
     * 1 block, and it won't attack me").</p>
     *
     * <p>Uses the same body-shaped reach the tall melee goal attacks on, rather than a number of
     * its own, so the two cannot disagree about when the mob is in range.</p>
     */
    private boolean inStrikingRange(LivingEntity target) {
        return TallMeleeAttackGoal.withinBodyReach(this.mob, target);
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.stalkPos = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        // Looking up at them the whole time is most of what makes this read as stalking rather
        // than as pathing: the mob is aware of the player it cannot reach.
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (--this.repathCooldown > 0) {
            return;
        }
        this.repathCooldown = REPATH_INTERVAL_TICKS;

        BlockPos found = findStalkPos(target);
        if (found == null) {
            // Nowhere better to stand. Hold position rather than wandering off under another goal.
            this.mob.getNavigation().stop();
            return;
        }
        this.stalkPos = found;
        if (this.mob.distanceToSqr(found.getX() + 0.5D, found.getY(), found.getZ() + 0.5D)
                > this.arriveDistanceSqr) {
            this.mob.getNavigation().moveTo(found.getX() + 0.5D, found.getY(), found.getZ() + 0.5D,
                    this.speedModifier);
        } else {
            this.mob.getNavigation().stop();
        }
    }

    /** Where the mob is trying to stand, or null while it has nowhere to go. Exposed for tuning. */
    public BlockPos getStalkPos() {
        return this.stalkPos;
    }

    /**
     * The closest reachable point to the target, searched from the target BACKWARDS toward the mob.
     *
     * <p>Backwards rather than forwards because the useful answer is the one nearest the target:
     * the first reachable probe going back from underneath them is the closest the mob can stand,
     * and it stops the search immediately.</p>
     */
    private BlockPos findStalkPos(LivingEntity target) {
        double dx = this.mob.getX() - target.getX();
        double dz = this.mob.getZ() - target.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D) {
            return null;                       // already underneath them
        }
        double stepX = dx / length;
        double stepZ = dz / length;
        int steps = (int) Math.min(MAX_PROBE_STEPS, Math.max(1, Math.ceil(length)));
        for (int i = 0; i <= steps; i++) {
            BlockPos probe = BlockPos.containing(
                    target.getX() + stepX * i,
                    // from the target's own feet: the floor under a player on a ledge is the ledge,
                    // and the navigation's own node lookup handles the drop to the real floor
                    target.getY(),
                    target.getZ() + stepZ * i);
            Path path = this.mob.getNavigation().createPath(probe, 0);
            if (path != null && path.canReach()) {
                return probe;
            }
        }
        return null;
    }

    private boolean canReach(LivingEntity target) {
        if (--this.reachCheckCooldown <= 0) {
            this.reachCheckCooldown = REACH_CHECK_INTERVAL_TICKS;
            Path path = this.mob.getNavigation().createPath(target, 0);
            this.cachedCanReach = path != null && path.canReach();
        }
        return this.cachedCanReach;
    }
}
