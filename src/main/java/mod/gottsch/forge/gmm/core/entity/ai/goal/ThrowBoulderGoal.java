package mod.gottsch.forge.gmm.core.entity.ai.goal;

import mod.gottsch.forge.gmm.core.entity.monster.IHurlingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A heavy, heavily telegraphed thrown attack for a mob that <strong>does not chase</strong>.
 *
 * <h2>Why not {@link ThrowProjectileGoal}</h2>
 * <p>That goal is a thrower's whole fight: it approaches when out of range, backs away when the
 * target crowds it, and falls back to fists at point blank. All three are wrong for a slow brute
 * that owns one room. It must not retreat &mdash; it is the immovable thing in the fight &mdash; and
 * it already has a stalk goal for closing and a melee goal for point blank. What is missing is only
 * the throw itself: stand still, heave, let go.
 *
 * <p>So this is deliberately the narrow version. It <strong>roots</strong> the mob for the whole
 * blow (a heave that can be walked out of is not a commitment), claims MOVE so nothing walks it
 * away mid-throw, and does nothing at all outside its range band.
 *
 * <h2>The band, and why it has a floor as well as a ceiling</h2>
 * <p>The throw exists to answer a player standing where the mob cannot reach them &mdash; across a
 * pit, up on a gallery, back down a corridor. Close in, that player is a melee problem and a mob
 * that lobs rocks at someone standing on its foot reads as broken, so {@link #minRange} keeps the
 * two apart. The ceiling is simply how far it is worth throwing.
 *
 * <p><strong>Line of sight is required</strong>, and that is what ties this to the wall burst: a
 * doorway is a blind spot for a tall mob (its eyes are above the opening), so before the wall comes
 * down there is nobody to throw at. The breach is what gives this goal its target.
 *
 * <p>The projectile is the consumer's &mdash; the goal supplies the moment and the spawn point, and
 * reuses {@link ThrowProjectileGoal.ProjectileLauncher} rather than declaring a second identical
 * interface. Whether a rock may break a wall where it lands is a property of the world, not of the
 * mob, so a library that cannot know whose walls these are throws nothing of its own.
 *
 * @author Mark Gottschling on 9/14/2026
 */
public class ThrowBoulderGoal extends Goal {

    /** Where the boulder leaves from, relative to the mob: overhead, and a little in front. */
    private static final double SPAWN_HEIGHT_FRACTION = 0.9D;
    private static final double SPAWN_FORWARD = 0.6D;

    private final Mob mob;
    private final IHurlingMob hurler;
    private final ThrowProjectileGoal.ProjectileLauncher launcher;
    private final double minRangeSqr;
    private final double maxRangeSqr;
    private final int cooldownTicks;

    /** Counts down between throws; started high so the first one is not thrown on sight. */
    private int cooldown;
    /** Ticks into the current heave, or -1. Server-side; the flag is what crosses the wire. */
    private int hurlTicks = -1;

    public ThrowBoulderGoal(Mob mob, ThrowProjectileGoal.ProjectileLauncher launcher,
                            double minRange, double maxRange, int cooldownTicks) {
        this.mob = mob;
        this.hurler = mob instanceof IHurlingMob hurlingMob ? hurlingMob : null;
        this.launcher = launcher;
        this.minRangeSqr = minRange * minRange;
        this.maxRangeSqr = maxRange * maxRange;
        this.cooldownTicks = cooldownTicks;
        this.cooldown = cooldownTicks;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // In reach it is a melee problem, whatever the flat distance says -- a tall mob's reach is
        // body-shaped, so a player on a ledge beside its head is closer than the arithmetic looks.
        if (TallMeleeAttackGoal.withinBodyReach(this.mob, target)) {
            return false;
        }
        double distSqr = this.mob.distanceToSqr(target);
        return distSqr >= this.minRangeSqr && distSqr <= this.maxRangeSqr
                && this.mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        // Once the heave has started it finishes, even if the target steps behind something. The
        // rock is already overhead; a mob that puts it down again looks like it changed its mind.
        LivingEntity target = this.mob.getTarget();
        return this.hurlTicks >= 0 && target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.hurlTicks = 0;
        if (this.hurler != null) {
            this.hurler.setHurling(true);
        }
    }

    @Override
    public void stop() {
        this.hurlTicks = -1;
        this.cooldown = this.cooldownTicks;
        if (this.hurler != null) {
            // The pose is driven entirely by the synched flag and nothing else clears it: a goal
            // that ends mid-heave would otherwise leave the mob holding a rock for ever.
            this.hurler.setHurling(false);
        }
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        // Rooted and watching. Rooted is the point: the wind-up is the player's warning, and a
        // warning you can only see if you happen to be looking at a moving mob is not one.
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        int windup = this.hurler == null ? 1 : Math.max(1, this.hurler.getHurlWindupTicks());
        int recovery = this.hurler == null ? 0 : Math.max(0, this.hurler.getHurlRecoveryTicks());

        this.hurlTicks++;
        if (this.hurlTicks == windup) {
            throwIt(target);
        } else if (this.hurlTicks >= windup + recovery) {
            // stop() does the rest -- including the cooldown, which is measured from the END of the
            // throw so that lengthening the wind-up cannot silently shorten the gap between rocks.
            this.hurlTicks = -1;
        }
    }

    private void throwIt(LivingEntity target) {
        if (this.launcher == null) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.mob.getYRot()).scale(SPAWN_FORWARD);
        this.launcher.launch(this.mob, target,
                this.mob.getX() + forward.x,
                this.mob.getY() + this.mob.getBbHeight() * SPAWN_HEIGHT_FRACTION,
                this.mob.getZ() + forward.z);
    }
}
