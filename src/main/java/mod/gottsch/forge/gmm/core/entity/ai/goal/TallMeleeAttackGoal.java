package mod.gottsch.forge.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.AABB;

/**
 * {@link MeleeAttackGoal} for a mob tall enough that vanilla's reach test lies to it.
 *
 * <h2>The bug this fixes</h2>
 * <p>Vanilla measures melee reach as a <strong>single 3D distance from the mob's FEET to the
 * target's FEET</strong>, against {@code (bbWidth * 2)^2}. That is fine for a mob two blocks tall,
 * where "how far away" and "how far away horizontally" are near enough the same question. For a
 * seven-block construct they are different questions entirely: a player standing on a ledge level
 * with its <em>head</em> is an arm's length from its face and six blocks away by vanilla's
 * arithmetic, so the mob never swings. Mark, 2026-09-13: <em>"when i'm at an elevated position
 * (near the colossus' head height), it doesn't try to attack but tries to pathfind until we're
 * standing on level ground."</em>
 *
 * <p>Worse, the mob does not merely fail to attack: {@code MeleeAttackGoal} keeps walking it toward
 * a target it believes is out of range, so a player on a ledge watches a boss shuffle about below
 * them indefinitely. That is the same "boss ignores you" shape {@link StalkGoal} addresses from the
 * other side -- this one is about the mob that IS in reach and does not know it.
 *
 * <h2>What it does instead</h2>
 * <p>Splits the question in two, which is what a body-shaped reach actually is:</p>
 * <ul>
 *   <li><strong>Horizontally</strong>, the vanilla reach: {@code bbWidth * 2 + target width}.</li>
 *   <li><strong>Vertically</strong>, anywhere the mob's own body spans, plus
 *       {@link #VERTICAL_SLACK} at each end -- it can swing down at something at its feet and up at
 *       something level with the top of its head, and no further.</li>
 * </ul>
 *
 * <p>Note this does NOT widen the horizontal reach, so the mob cannot hit anything it could not hit
 * before while standing on the same floor. It only stops the height of the mob being counted
 * against it.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class TallMeleeAttackGoal extends MeleeAttackGoal {
    /** How far past its own feet and head the mob may still land a blow. */
    private static final double VERTICAL_SLACK = 1.0D;

    /** Vanilla's interval between swings, which is what {@code MeleeAttackGoal} hard-codes. */
    public static final int DEFAULT_ATTACK_INTERVAL_TICKS = 20;

    /**
     * Ticks between swings.
     *
     * <p>Vanilla's is a hard-coded 20 inside {@code resetAttackCooldown()}, and the field it sets is
     * <strong>private</strong> -- there is no override that changes it, which is why this goal keeps
     * its own counter and ignores the inherited one entirely.</p>
     *
     * <p>It exists because a heavy mob should not swing at a light mob's rate: a blow worth 11
     * damage landing every second reads as a flurry, not as a hammer (Mark, 2026-09-13: "being a
     * powerful strike it shouldn't occur so fast"). The number is the mob's to choose.</p>
     */
    private final int attackIntervalTicks;
    private int cooldown;

    /** Vanilla's swing rate. */
    public TallMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this(mob, speedModifier, followingTargetEvenIfNotSeen, DEFAULT_ATTACK_INTERVAL_TICKS);
    }

    public TallMeleeAttackGoal(PathfinderMob mob, double speedModifier,
                               boolean followingTargetEvenIfNotSeen, int attackIntervalTicks) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.attackIntervalTicks = attackIntervalTicks;
    }

    // NOTE there is deliberately no start() override resetting the cooldown. This goal stops and
    // starts constantly in a real fight -- the target steps out of reach, the slam takes the flags,
    // the navigation loses its path -- and clearing the cooldown on entry would let a mob swing
    // every time it re-engaged, which is the opposite of a slow heavy attack. The counter survives
    // across restarts; only the first blow of a life lands immediately.

    @Override
    public void tick() {
        super.tick();          // NOTE: this is what calls checkAndPerformAttack below
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    /**
     * <strong>The entry test needs the same correction as the attack.</strong> Vanilla's
     * {@code canUse} falls back to {@code getAttackReachSqr >= distanceToSqr} when the navigation
     * cannot produce a path -- the same feet-to-feet measure -- so a mob standing next to a player
     * one block up, with nowhere to path, never even runs the goal. Found in play 2026-09-13:
     * <em>"i'm 2 blocks away, up 1 block, and it won't attack me."</em>
     */
    @Override
    public boolean canUse() {
        if (super.canUse()) {
            return true;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && withinBodyReach(this.mob, target);
    }

    @Override
    public boolean canContinueToUse() {
        if (super.canContinueToUse()) {
            return true;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && withinBodyReach(this.mob, target);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
        // distToEnemySqr is vanilla's feet-to-feet measure; deliberately ignored -- see the javadoc.
        if (!withinBodyReach(this.mob, enemy) || this.cooldown > 0) {
            return;
        }
        this.cooldown = this.attackIntervalTicks;
        this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.mob.doHurtTarget(enemy);
    }

    /**
     * Whether {@code enemy} is within a BODY-shaped reach of {@code mob}: the vanilla reach
     * horizontally, and anywhere the mob's own body spans (plus {@link #VERTICAL_SLACK}) vertically.
     *
     * <p>Static and public because it is not only the attack's question. Any goal that positions a
     * mob has to know when the mob could be hitting instead -- see {@link StalkGoal}, which would
     * otherwise hold the MOVE and LOOK flags while standing next to a player it could reach.</p>
     */
    public static boolean withinBodyReach(LivingEntity mob, LivingEntity enemy) {
        double reach = mob.getBbWidth() * 2.0D + enemy.getBbWidth();
        double dx = enemy.getX() - mob.getX();
        double dz = enemy.getZ() - mob.getZ();
        if (dx * dx + dz * dz > reach * reach) {
            return false;
        }
        AABB target = enemy.getBoundingBox();
        return target.maxY >= mob.getY() - VERTICAL_SLACK
                && target.minY <= mob.getY() + mob.getBbHeight() + VERTICAL_SLACK;
    }
}
