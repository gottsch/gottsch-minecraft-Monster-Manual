package mod.gottsch.neo.gmm.core.entity.ai.goal;

import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * A reusable "charge up, then cast" spell goal for any {@link Mob}. The goal handles the
 * range/line-of-sight check and the charge-up timer; the consumer supplies a {@link SpellLauncher}
 * that creates and launches whatever spell projectile it likes, so this class has no knowledge of
 * any concrete spell and can live in the shared library.
 * <p>
 * Generalized from Dungeon Denizens' WeightedCastProjectileGoal (multi-spell, weighted) and
 * CastParalysisGoal (its single-spell special case) -- {@link #CastSpellGoal(Mob, int, SpellLauncher)}
 * covers the latter by wrapping a single-entry {@link WeightedCollection}.
 * <p>
 * The charge-up telegraph particle ({@link ParticleTypes#WITCH} by default, or {@code null} to disable
 * the telegraph entirely) and the post-cast cooldown (0 ticks by default, i.e. it can start charging
 * again immediately) are both consumer-tunable via the full constructor -- added when Bodak's Withering
 * Gaze needed a real gap between casts instead of firing again the instant a charge completes, and
 * turned out to also need the telegraph disabled outright (Bodak already has its own dedicated smoke
 * telegraph tied to Death Gaze; layering the goal's own particle on top -- even recolored to match --
 * read as a second, redundant cue rather than reusing the existing one). Every pre-existing constructor
 * still defaults to {@code ParticleTypes.WITCH} / no cooldown, so every existing caster (Beholder,
 * Gazer, DeathTyrant, Spectator, Shadowlord, OrcShaman) is unaffected.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class CastSpellGoal extends Goal {
    private static final int DEFAULT_CHARGE_TIME = 80;

    /**
     * Creates and launches a spell projectile from the given spawn point toward the target.
     * The goal computes the spawn point (in front of the caster, at half-height); the launcher
     * owns the projectile type and computes its own direction/velocity toward the target.
     */
    @FunctionalInterface
    public interface SpellLauncher {
        void cast(Mob caster, LivingEntity target, double x, double y, double z);
    }

    private final Mob mob;
    private final WeightedCollection<Integer, SpellLauncher> spells;
    private final int chargeTime;
    /** Extra "don't cast in melee" buffer beyond the caster's own attack reach; 0 = attack reach only. */
    private final double meleeDistance;
    /** Ticks of cooldown after a completed cast before charging can start again; 0 = no cooldown. */
    private final int cooldownTime;
    private final ParticleOptions particle;
    private int chargeTimeCount;
    /**
     * Game-time (see {@code Level#getGameTime()}) at which the cooldown from the last cast expires.
     * A timestamp rather than a decrementing counter, since this goal isn't guaranteed to keep ticking
     * while it's not the selected/running goal (canUse() is what needs to see the cooldown, not tick()).
     */
    private long cooldownUntilTime;

    public CastSpellGoal(Mob caster, WeightedCollection<Integer, SpellLauncher> spells) {
        this(caster, DEFAULT_CHARGE_TIME, spells);
    }

    public CastSpellGoal(Mob caster, int chargeTime, WeightedCollection<Integer, SpellLauncher> spells) {
        this(caster, chargeTime, 0D, spells);
    }

    public CastSpellGoal(Mob caster, int chargeTime, double meleeDistance, WeightedCollection<Integer, SpellLauncher> spells) {
        this(caster, chargeTime, meleeDistance, 0, ParticleTypes.WITCH, spells);
    }

    public CastSpellGoal(Mob caster, int chargeTime, SpellLauncher singleSpell) {
        this(caster, chargeTime, 0D, singleSpell);
    }

    public CastSpellGoal(Mob caster, int chargeTime, double meleeDistance, SpellLauncher singleSpell) {
        this(caster, chargeTime, meleeDistance, new WeightedCollection<Integer, SpellLauncher>().add(1, singleSpell));
    }

    /** Full constructor: consumer-tunable cooldown and charge-telegraph particle. */
    public CastSpellGoal(Mob caster, int chargeTime, double meleeDistance, int cooldownTime, ParticleOptions particle,
                          WeightedCollection<Integer, SpellLauncher> spells) {
        this.mob = caster;
        this.spells = spells;
        this.chargeTime = chargeTime;
        this.meleeDistance = meleeDistance;
        this.cooldownTime = cooldownTime;
        this.particle = particle;
    }

    /** Full constructor, single-spell convenience. */
    public CastSpellGoal(Mob caster, int chargeTime, double meleeDistance, int cooldownTime, ParticleOptions particle,
                          SpellLauncher singleSpell) {
        this(caster, chargeTime, meleeDistance, cooldownTime, particle,
                new WeightedCollection<Integer, SpellLauncher>().add(1, singleSpell));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() == null) {
            return false;
        }
        if (this.cooldownTime > 0 && this.mob.level().getGameTime() < this.cooldownUntilTime) {
            return false;
        }
        double minDistSqr = Math.max(this.meleeDistance, this.getAttackReachSqr(mob.getTarget()));
        return this.mob.distanceToSqr(mob.getTarget().getX(), mob.getTarget().getY(), mob.getTarget().getZ()) > minDistSqr;
    }

    @Override
    public void start() {
        this.chargeTimeCount = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            if (target.distanceToSqr(this.mob) < 4096.0D && this.mob.hasLineOfSight(target)) {
                ++this.chargeTimeCount;

                // charge-up telegraph: a few motes around the caster every few ticks, same idiom as
                // EnthrallGoal, generalized here so every CastSpellGoal-driven spell gets a cast tell
                // for free rather than each spell/goal re-adding its own. A null particle turns this
                // off entirely -- for a consumer (e.g. Bodak) that already has its own dedicated
                // charge telegraph and doesn't want a second, unrelated one layered on top.
                if (this.particle != null && this.chargeTimeCount % 5 == 0 && mob.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(this.particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7D, mob.getZ(),
                            3, mob.getBbWidth() * 0.3D, 0.2D, mob.getBbWidth() * 0.3D, 0.01D);
                }

                if (this.chargeTimeCount >= chargeTime) {
                    Vec3 view = this.mob.getViewVector(1.0F);
                    double x = this.mob.getX() + view.x * 2.0D;
                    double y = this.mob.getY(0.5D);
                    double z = this.mob.getZ() + view.z * 2.0D;

                    SpellLauncher spell = this.spells.next();
                    spell.cast(this.mob, target, x, y, z);
                    this.chargeTimeCount = 0;
                    if (this.cooldownTime > 0) {
                        this.cooldownUntilTime = this.mob.level().getGameTime() + this.cooldownTime;
                    }
                    this.mob.playSound(SoundEvents.SPLASH_POTION_THROW, 0.5F, 0.4F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
                }
            } else if (this.chargeTimeCount > 0) {
                --this.chargeTimeCount;
            }
        }
    }

    protected double getAttackReachSqr(LivingEntity entity) {
        return (double) (this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + entity.getBbWidth());
    }
}
