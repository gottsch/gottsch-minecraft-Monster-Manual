package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * A reusable ranged-throw attack goal for any {@link Mob}. The mob holds ground at throwing range,
 * approaches when out of sight/range, backs away when the target crowds it (unless cornered), and
 * falls back to fists at point-blank. When it has held ground long enough it launches a projectile.
 * <p>
 * The goal is deliberately projectile-agnostic so it can live in the shared library: the consumer
 * supplies a {@link ProjectileLauncher} that creates and launches whatever projectile it likes from
 * the spawn point the goal computes (the mob's throwing/right hand). Whether the mob is currently a
 * ranged thrower is supplied as a {@link Predicate} so the same class also has no knowledge of the
 * consumer mob's "is ranged" flag.
 * <p>
 * A {@code null} launcher is also valid -- the goal then does positioning only (approach/retreat/
 * hold-ground + the point-blank melee fallback), never swings or "throws" anything. Useful for a
 * caster that should stand off at range (e.g. driven by a separate {@code CastSpellGoal}, which
 * declares no {@code Goal.Flag}s and so never contests this goal for movement control) but still
 * needs the same approach/retreat/melee-fallback positioning a ranged thrower already has.
 * <p>
 * Generalized from Dungeon Denizens' inner OrcThrowRockGoal.
 *
 * @author Mark Gottschling
 */
public class ThrowProjectileGoal extends Goal {
    private static final int DEFAULT_CHARGE_TIME = 40;
    private static final float DEFAULT_ATTACK_RADIUS = 16F;
    private static final int MELEE_COOLDOWN_TIME = 20;
    private static final float RETREAT_RANGE_FACTOR = 0.45F;

    /**
     * Creates and launches a projectile from the given spawn point toward the target.
     * The goal computes the spawn point (forward + to the mob's right, around shoulder height);
     * the launcher owns the projectile type and its flight (e.g. a ballistic lob).
     */
    @FunctionalInterface
    public interface ProjectileLauncher {
        void launch(Mob shooter, LivingEntity target, double x, double y, double z);
    }

    private final Mob mob;
    private final double speedModifier;
    private final int maxChargeTime;
    private final float attackRadiusSqr;
    private final Predicate<Mob> canThrow;
    private final ProjectileLauncher launcher;

    private int chargeTime;
    private int meleeCooldown;
    private int stuckTicks;
    private double prevDistSqr;

    public ThrowProjectileGoal(Mob mob, Predicate<Mob> canThrow, ProjectileLauncher launcher) {
        this(mob, 1.0D, DEFAULT_CHARGE_TIME, DEFAULT_ATTACK_RADIUS, canThrow, launcher);
    }

    public ThrowProjectileGoal(Mob mob, double speedModifier, int maxChargeTime, float attackRadius,
                               Predicate<Mob> canThrow, ProjectileLauncher launcher) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.maxChargeTime = maxChargeTime;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.canThrow = canThrow;
        this.launcher = launcher;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && canThrow.test(mob);
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.chargeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        double d0 = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = mob.getSensing().hasLineOfSight(target);

        boolean inMeleeRange = d0 <= this.getAttackReachSqr(target);

        // is the target in the band where the mob would rather back away?
        boolean inRetreatBand = !inMeleeRange && canSee
                && d0 <= (double) this.attackRadiusSqr
                && d0 < (double) (this.attackRadiusSqr * RETREAT_RANGE_FACTOR);

        // "can't escape" = in the retreat band but not actually gaining distance (cornered,
        // or the target is keeping pace). Judge by distance gained -- the pathfinder hands
        // back a path even when boxed in, and a wall-shuffle keeps a little velocity.
        if (inRetreatBand && d0 <= this.prevDistSqr + 0.05D) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
        }
        this.prevDistSqr = d0;
        boolean cantEscape = this.stuckTicks >= 8;

        // movement tiers: point-blank -> stand and fight; too far / no sight -> approach;
        // retreat band and able to escape -> back away; else -> hold ground and throw.
        boolean throwing = false;
        if (inMeleeRange) {
            mob.getNavigation().stop();
            faceTarget(target);
        } else if (d0 > (double) this.attackRadiusSqr || !canSee) {
            mob.getNavigation().moveTo(target, this.speedModifier);
        } else if (inRetreatBand && !cantEscape) {
            Vec3 away = new Vec3(mob.getX() - target.getX(), 0.0D, mob.getZ() - target.getZ());
            if (away.lengthSqr() > 1.0E-4D) {
                Vec3 dest = mob.position().add(away.normalize().scale(5.0D));
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, this.speedModifier);
            }
        } else {
            // in throwing range (or cornered with no escape): stand, face, and throw
            mob.getNavigation().stop();
            faceTarget(target);
            throwing = true;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // point-blank: it can't keep its distance, so it defends with fists on a cooldown.
        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }
        if (inMeleeRange && this.meleeCooldown <= 0) {
            mob.swing(InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
            this.meleeCooldown = MELEE_COOLDOWN_TIME;
        }

        // throw only while holding ground (in range or cornered, facing the target) and
        // not in melee -- never mid-retreat with its back turned. A null launcher (positioning-only
        // use, see class javadoc) never reaches the swing/launch below -- chargeTime just sits at 0.
        if (!throwing || !canSee || inMeleeRange || this.launcher == null) {
            this.chargeTime = 0;
        } else if (++this.chargeTime >= this.maxChargeTime) {
            mob.swing(InteractionHand.MAIN_HAND);

            // spawn at the mob's throwing (right) hand -- forward, off to the side, and
            // around shoulder height -- so it reads as a throw rather than a spit.
            Vec3 view = mob.getViewVector(1.0F);
            double fx = view.x;
            double fz = view.z;
            double fLen = Math.sqrt(fx * fx + fz * fz);
            if (fLen < 1.0E-4D) {
                fLen = 1.0D;
            }
            fx /= fLen;
            fz /= fLen;
            // mob's right-hand side (perpendicular to facing, on the ground plane)
            double rightX = -fz;
            double rightZ = fx;
            final double FORWARD_OFFSET = 0.3D;
            final double SIDE_OFFSET = 0.45D;
            double spawnX = mob.getX() + fx * FORWARD_OFFSET + rightX * SIDE_OFFSET;
            double spawnY = mob.getEyeY() - 0.3D;
            double spawnZ = mob.getZ() + fz * FORWARD_OFFSET + rightZ * SIDE_OFFSET;
            this.launcher.launch(mob, target, spawnX, spawnY, spawnZ);
            this.chargeTime = 0;
        }
    }

    protected double getAttackReachSqr(LivingEntity entity) {
        return (double) (mob.getBbWidth() * 2.0F * mob.getBbWidth() * 2.0F + entity.getBbWidth());
    }

    /** Snap the mob's body to face the target (only when reasonably close). */
    private void faceTarget(LivingEntity target) {
        if (target != null && target.distanceToSqr(mob) < 4096.0D) {
            double dx = target.getX() - mob.getX();
            double dz = target.getZ() - mob.getZ();
            mob.setYRot(-((float) Mth.atan2(dx, dz)) * (180F / (float) Math.PI));
            mob.yBodyRot = mob.getYRot();
        }
    }
}
