package mod.gottsch.forge.gmm.core.entity.ai.goal;

import mod.gottsch.forge.gmm.core.entity.monster.ISlammingMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The heavy, telegraphed ground attack: wind up with both fists overhead, then bring them down and
 * hit <em>everything around the mob at once</em>.
 *
 * <h2>What it is for</h2>
 * <p>A slow melee boss has one bad matchup: a player who circles it. Its single-target swing is
 * aimed at whoever it is facing, so strafing beats it. The slam has no facing and no target -- it
 * is an area, and the counter to it is <strong>distance</strong>. That turns the fight from a
 * dodging problem into a positioning one, which is what a small arena is actually good for.
 *
 * <p>The wind-up is the point, not a cost. {@link ISlammingMob#getSlamWindupTicks()} is the
 * player's window to read it and get out, and the model raises the fists over exactly those ticks,
 * so what the player sees IS the timer.
 *
 * <h2>Two things the library will not decide</h2>
 * <p>GMM registers nothing and owns no world, so both of the effects that touch something other
 * than the mob are consumer-supplied statics -- the same pattern as {@code Orc.projectileLauncher}:
 * <ul>
 *   <li>{@link #damageSource} -- a consumer that ships a damage type gets attribution, a death
 *       message and a hook its own gear can test. Unset, the slam is an ordinary mob attack.</li>
 *   <li>{@link #blockEffect} -- <strong>whether a slam may break the world is a property of the
 *       WORLD</strong>, not of the mob. Unset, the slam breaks nothing at all, which is the right
 *       default for a library that cannot know whose walls these are.</li>
 * </ul>
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class GroundSlamGoal extends Goal {

    /**
     * Consumer-supplied damage source for the blow. Null leaves it a plain mob attack.
     * See the class javadoc.
     */
    public static Function<Mob, DamageSource> damageSource = null;

    /**
     * Consumer-supplied world effect, called once at the moment of impact with the slamming mob and
     * the radius it slammed. Null means the slam moves nobody's blocks.
     */
    public static BiConsumer<Mob, Double> blockEffect = null;

    private final Mob mob;
    private final ISlammingMob slammer;
    private final double triggerRange;
    private final double radius;
    private final float damage;
    private final int cooldownTicks;

    private int cooldown;
    private boolean struck;
    /**
     * The SERVER's own count of the slam. Not read from the entity and not synched: the client has
     * its own clock for the animation, and a shared tick number between the two sides is exactly
     * the bug this replaced. See {@link ISlammingMob}.
     */
    private int elapsed;

    public GroundSlamGoal(Mob mob, double triggerRange, double radius, float damage, int cooldownTicks) {
        this.mob = mob;
        this.slammer = (ISlammingMob) mob;
        this.triggerRange = triggerRange;
        this.radius = radius;
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        // MOVE and LOOK: a mob mid-slam is committed to it and does not walk out of its own attack.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || !this.mob.onGround()) {
            return false;
        }
        // Horizontal only. The slam is a shockwave across the floor, so someone directly above the
        // mob on a ledge is squarely in it -- and that is the case it most needs to answer.
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return dx * dx + dz * dz <= this.triggerRange * this.triggerRange;
    }

    @Override
    public boolean canContinueToUse() {
        return this.elapsed <= this.slammer.getSlamWindupTicks() + this.slammer.getSlamRecoveryTicks();
    }

    @Override
    public void start() {
        this.struck = false;
        this.elapsed = 0;
        this.slammer.setSlamming(true);
        this.mob.getNavigation().stop();
        // The wind-up is audible as well as visible -- stone grinding as it rears back -- so a
        // player looking elsewhere still gets the warning.
        this.mob.level().playSound(null, this.mob.blockPosition(),
                SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.4F, 0.5F);
    }

    @Override
    public void stop() {
        this.slammer.setSlamming(false);
        this.cooldown = this.cooldownTicks;
        this.struck = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        // Rooted through the wind-up: the tell is worthless if the mob can chase you during it.
        this.mob.setDeltaMovement(0.0D, this.mob.getDeltaMovement().y, 0.0D);

        this.elapsed++;
        if (this.struck || this.elapsed < this.slammer.getSlamWindupTicks()) {
            return;
        }
        this.struck = true;
        strike();
    }

    /** The moment of impact: one pass over everything standing in the radius. */
    private void strike() {
        // NOT an anvil (Mark, 2026-09-13: it "doesn't really go with the slam action") -- that is a
        // metal object landing, and this is a stone fist driven into the floor. A body-blow thud
        // pitched down, with the floor itself cracking under it.
        this.mob.level().playSound(null, this.mob.blockPosition(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.5F);
        this.mob.level().playSound(null, this.mob.blockPosition(),
                SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
        dust();

        DamageSource source = damageSource != null
                ? damageSource.apply(this.mob)
                : this.mob.damageSources().mobAttack(this.mob);

        AABB area = this.mob.getBoundingBox().inflate(this.radius, 2.0D, this.radius);
        for (LivingEntity victim : this.mob.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> canHit(e))) {
            double dx = victim.getX() - this.mob.getX();
            double dz = victim.getZ() - this.mob.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > this.radius) {
                continue;                       // the box is square; the shockwave is not
            }
            // Full force underfoot, a shove at the rim. Never nothing -- being clipped by it should
            // still tell the player they were too close.
            float falloff = (float) Math.max(0.35D, 1.0D - distance / this.radius);
            victim.hurt(source, this.damage * falloff);
            victim.knockback(KNOCKBACK * falloff, -dx, -dz);
            victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, LIFT * falloff, 0.0D));
            // without this the server's velocity change never reaches a player's client
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOW_TICKS, SLOW_AMPLIFIER));
        }

        if (blockEffect != null) {
            blockEffect.accept(this.mob, this.radius);
        }
    }

    /**
     * Hurts players and whatever it is fighting, and nothing else.
     *
     * <p>The same rule the Annihilation Ray settled on: an encounter whose boss knocks its own
     * escort about disarms itself, and a stray hit turns {@code HurtByTargetGoal} into infighting
     * that the player then watches instead of joining.</p>
     */
    private boolean canHit(Entity entity) {
        return entity != this.mob
                && entity.isAlive()
                && EntitySelector.NO_SPECTATORS.test(entity)
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                && (entity instanceof Player || entity == this.mob.getTarget())
                && !this.mob.isAlliedTo(entity);
    }

    /** A ring of the floor's own material thrown up at the impact. */
    private void dust() {
        if (!(this.mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState floor = level.getBlockState(this.mob.blockPosition().below());
        for (int i = 0; i < DUST_PUFFS; i++) {
            double angle = (Math.PI * 2.0D) * i / DUST_PUFFS;
            double x = this.mob.getX() + Math.cos(angle) * this.radius * 0.6D;
            double z = this.mob.getZ() + Math.sin(angle) * this.radius * 0.6D;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                    x, this.mob.getY() + 0.2D, z, 6, 0.3D, 0.1D, 0.3D, 0.05D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** Sideways shove at full force; scaled down toward the rim. */
    private static final double KNOCKBACK = 1.8D;
    /** Enough lift to take the target off the floor, so the knockback carries. */
    private static final double LIFT = 0.55D;
    private static final int SLOW_TICKS = 60;
    private static final int SLOW_AMPLIFIER = 1;
    private static final int DUST_PUFFS = 12;
}
