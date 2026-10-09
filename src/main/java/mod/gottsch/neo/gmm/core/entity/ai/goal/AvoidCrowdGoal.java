package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Keeps a mob from getting boxed into a knot of other mobs <em>while it is engaging a target</em>.
 * Meant for a "leader" type (the Skeleton Champion) that should hover at the edge of its own pack
 * rather than in the middle of it, so a player's sweep / AoE / stray swing doesn't catch it in the
 * scrum. Purely positional — it never drops the target and never runs from the player; it just steps
 * to open space and lets its minions form the front line.
 *
 * <p>Algorithm, each time it fires (throttled): gather the mobs crowding the leader, build a
 * repulsion vector that points away from them (nearer mobs push harder, ~1/distance), ask the
 * pathfinder for a reachable spot in that direction, and reject the spot if it would pull the leader
 * out of engagement range of its target. If a valid spot is found, walk there. Because it only holds
 * the {@link Flag#MOVE} flag and sits at a higher priority than the attack goal, it preempts the
 * approach when the leader is crowded and yields to it (attacks) once the space is clear.
 *
 * @author Mark Gottschling on 7/6/2026
 */
public class AvoidCrowdGoal extends Goal {
    private static final int SEARCH_RADIUS = 6;
    private static final int SEARCH_Y = 4;

    private final PathfinderMob mob;
    private final double crowdRadius;
    private final int minCrowd;
    private final double maxTargetDistanceSq;
    private final double speedModifier;

    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private int cooldown;

    /**
     * @param crowdRadius       how close another mob must be to count as "crowding"
     * @param minCrowd          how many crowding mobs it takes before the leader bothers to reposition
     * @param maxTargetDistance don't step to a spot farther than this from the target (stay engaged)
     * @param speedModifier     move speed while repositioning
     */
    public AvoidCrowdGoal(PathfinderMob mob, double crowdRadius, int minCrowd, double maxTargetDistance, double speedModifier) {
        this.mob = mob;
        this.crowdRadius = crowdRadius;
        this.minCrowd = minCrowd;
        this.maxTargetDistanceSq = maxTargetDistance * maxTargetDistance;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // throttle: don't recompute a new spot every tick (keeps it from jittering in place)
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        // the crowd = other mobs pressing in (getEntitiesOfClass(Mob) already excludes players, so a
        // player target never counts as crowd; skip the target itself in case it's a mob)
        List<Mob> crowd = this.mob.level().getEntitiesOfClass(Mob.class,
                this.mob.getBoundingBox().inflate(this.crowdRadius),
                other -> other != this.mob && other != target && other.isAlive());
        if (crowd.size() < this.minCrowd) {
            return false;
        }

        // repulsion: away-vectors summed, weighted by closeness (a mob at distance d contributes a
        // vector of magnitude ~1/d, so the nearest neighbours dominate the direction we flee)
        Vec3 self = this.mob.position();
        Vec3 push = Vec3.ZERO;
        for (Mob other : crowd) {
            Vec3 away = self.subtract(other.position());
            double d = away.length();
            if (d > 1.0E-3D) {
                push = push.add(away.scale(1.0D / (d * d)));
            }
        }
        // horizontal only — we walk, we don't levitate
        push = new Vec3(push.x, 0.0D, push.z);
        if (push.lengthSqr() < 1.0E-6D) {
            // symmetric squeeze: surrounded evenly enough that the repulsion vectors cancel out. This
            // is exactly the "boxed into a knot" case the goal exists for, so falling through to "wait
            // and retry" (the old behaviour) meant it silently never escaped a true ring formation --
            // the vectors cancel again on the very next attempt since the ring hasn't changed. Pick a
            // random horizontal bearing instead, so a symmetric surround still produces an escape
            // attempt rather than a permanent no-op.
            double angle = this.mob.getRandom().nextDouble() * (Math.PI * 2.0D);
            push = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        Vec3 spot = DefaultRandomPos.getPosTowards(this.mob, SEARCH_RADIUS, SEARCH_Y, push.normalize(),
                (float) Math.PI / 2.0F);
        if (spot == null || spot.distanceToSqr(target.position()) > this.maxTargetDistanceSq) {
            // nowhere reachable that keeps us in the fight — hold and retry shortly
            this.cooldown = 20;
            return false;
        }

        this.wantedX = spot.x;
        this.wantedY = spot.y;
        this.wantedZ = spot.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() && this.mob.getTarget() != null;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
    }

    @Override
    public void stop() {
        // brief settle before it's allowed to reposition again, so it isn't perpetually backpedaling
        this.cooldown = 10 + this.mob.getRandom().nextInt(10);
    }
}
