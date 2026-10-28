/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * gottsch's Monster Manual is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gottsch's Monster Manual is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with gottsch's Monster Manual.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.gmm.core.entity.ai.goal;

import mod.gottsch.forge.gmm.core.config.MobConfig;
import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.forge.gmm.core.entity.monster.IBreathingMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A breath weapon: a forward cone, breathed for a short while, then spent until it recharges.
 * Written for the canine family's Hellhound and Winter Wolf, and parameterized so anything else
 * with a breath (a drake, a dragon) can reuse it.
 *
 * <h2>The cone</h2>
 * <p>The aim is fixed the tick the breath starts, at the target as it stood then. The cone does not
 * track, so a player who sees the mob rear back and steps sideways is out of it: that is the
 * counterplay. Everything inside the cone (within {@code breathRange} of the mouth and
 * {@code breathAngle} degrees either side of the aim, with line of sight) at any tick of the breath
 * is hurt <strong>once</strong>: one breath, one save, as in D&amp;D. Walking into the cone halfway
 * through still counts. Allies ({@link IBreathingMob#isBreathAlly}) are never caught.
 *
 * <h2>Recharge</h2>
 * <p>D&amp;D's "recharge 5-6": once spent, the goal rolls {@code breathRechargeChance} every
 * {@code breathRechargeInterval} ticks, and the breath is back on the first success. The defaults
 * (one in three, every two seconds) bring it back in about six seconds on average, a D&amp;D round,
 * but a player can never count on the gap.
 *
 * <p>Every number is per mob in {@code gmm:mob_config}, with the code defaults passed in as
 * {@link Defaults}. The flag {@code breath} turns the attack off.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class BreathAttackGoal<T extends Mob & IBreathingMob> extends Goal {

    /**
     * The code-side default for each config key, per mob.
     *
     * @param range           {@code breathRange}: cone length in blocks, from the mouth
     * @param angle           {@code breathAngle}: half-angle of the cone, in degrees
     * @param duration        {@code breathDuration}: ticks the breath lasts
     * @param damage          {@code breathDamage}: damage to each victim, once per breath
     * @param rechargeInterval {@code breathRechargeInterval}: ticks between recharge rolls
     * @param rechargeChance  {@code breathRechargeChance}: chance (0-1) that a roll recharges it
     *
     * @author Mark Gottschling on 10/28/2026
     */
    public record Defaults(double range, double angle, int duration, double damage,
                           int rechargeInterval, double rechargeChance) {}

    // particle look only: how many per tick, how widely they scatter inside the cone, and how far a
    // particle carries per unit of launch speed (vanilla flame/snowflake friction is ~0.96)
    private static final int PARTICLES_PER_TICK = 6;
    private static final double PARTICLE_SCATTER = 0.6D;
    private static final double PARTICLE_CARRY = 22.0D;

    private final T mob;
    private final Defaults defaults;

    private boolean charged = true;
    private int nextRechargeCheck;

    // this breath's numbers, read from the config once at start
    private double range;
    private double cosAngle;
    private int duration;
    private float damage;

    private int elapsed;
    private Vec3 aim = Vec3.ZERO;
    private final Set<UUID> caught = new HashSet<>();

    public BreathAttackGoal(T mob, Defaults defaults) {
        this.mob = mob;
        this.defaults = defaults;
        // a mob mid-breath stands and breathes; it does not walk, leap or look away
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        MobConfig config = MobConfigHelper.get(this.mob);
        if (!config.flag("breath", true)) {
            return false;
        }
        if (!this.charged && !tryRecharge(config)) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double reach = config.number("breathRange", this.defaults.range());
        return this.mob.distanceToSqr(target) <= reach * reach && this.mob.hasLineOfSight(target);
    }

    /** One recharge roll, at most once per interval. A breath that comes back stays ready until used. */
    private boolean tryRecharge(MobConfig config) {
        if (this.mob.tickCount < this.nextRechargeCheck) {
            return false;
        }
        this.nextRechargeCheck = this.mob.tickCount + rechargeInterval(config);
        if (this.mob.getRandom().nextDouble() < config.number("breathRechargeChance", this.defaults.rechargeChance())) {
            this.charged = true;
        }
        return this.charged;
    }

    private int rechargeInterval(MobConfig config) {
        return Math.max(1, (int) config.number("breathRechargeInterval", this.defaults.rechargeInterval()));
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isAlive() && this.elapsed < this.duration;
    }

    @Override
    public void start() {
        MobConfig config = MobConfigHelper.get(this.mob);
        this.range = config.number("breathRange", this.defaults.range());
        this.cosAngle = Math.cos(Math.toRadians(Mth.clamp(config.number("breathAngle", this.defaults.angle()), 0.0D, 90.0D)));
        this.duration = Math.max(1, (int) config.number("breathDuration", this.defaults.duration()));
        this.damage = (float) config.number("breathDamage", this.defaults.damage());
        this.elapsed = 0;
        this.caught.clear();

        LivingEntity target = this.mob.getTarget();
        Vec3 eye = this.mob.getEyePosition();
        Vec3 toTarget = target == null ? this.mob.getLookAngle()
                : new Vec3(target.getX(), target.getY(0.5D), target.getZ()).subtract(eye);
        this.aim = toTarget.lengthSqr() < 1.0E-6D ? this.mob.getLookAngle() : toTarget.normalize();

        this.mob.getNavigation().stop();
        this.mob.setBreathing(true);
        this.mob.level().playSound(null, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(),
                this.mob.getBreathSound(), SoundSource.HOSTILE, 1.0F, 0.8F + this.mob.getRandom().nextFloat() * 0.4F);
    }

    @Override
    public void stop() {
        this.mob.setBreathing(false);
        this.charged = false;
        this.nextRechargeCheck = this.mob.tickCount + rechargeInterval(MobConfigHelper.get(this.mob));
        this.caught.clear();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.elapsed++;
        faceAim();
        if (!(this.mob.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 mouth = mouth();
        spawnParticles(level, mouth);
        catchVictims(level, mouth);
    }

    /** Hold the whole body on the aim, so what the player sees is where the cone is. */
    private void faceAim() {
        float yaw = (float) (Math.toDegrees(Mth.atan2(this.aim.z, this.aim.x))) - 90.0F;
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
        Vec3 point = this.mob.getEyePosition().add(this.aim.scale(this.range));
        this.mob.getLookControl().setLookAt(point.x, point.y, point.z, 30.0F, 30.0F);
    }

    /** Just in front of the snout, along the aim. */
    private Vec3 mouth() {
        return this.mob.getEyePosition().add(this.aim.scale(this.mob.getBbWidth() * 0.6D));
    }

    private void spawnParticles(ServerLevel level, Vec3 mouth) {
        RandomSource random = this.mob.getRandom();
        double spread = Math.tan(Math.acos(this.cosAngle)) * PARTICLE_SCATTER;
        double speed = this.range / PARTICLE_CARRY;
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            Vec3 dir = this.aim.add(random.nextGaussian() * spread, random.nextGaussian() * spread * 0.5D,
                    random.nextGaussian() * spread).normalize();
            // count 0: the offset is sent as the particle's velocity, scaled by the speed argument
            level.sendParticles(this.mob.getBreathParticle(), mouth.x, mouth.y, mouth.z, 0,
                    dir.x, dir.y, dir.z, speed * (0.7D + random.nextDouble() * 0.6D));
        }
    }

    private void catchVictims(ServerLevel level, Vec3 mouth) {
        AABB box = new AABB(mouth, mouth).inflate(this.range);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::mayCatch)) {
            Vec3 toVictim = new Vec3(victim.getX(), victim.getY(0.5D), victim.getZ()).subtract(mouth);
            double distance = toVictim.length();
            if (distance > this.range + victim.getBbWidth() * 0.5D) {
                continue;
            }
            // point-blank, the victim's own width spans the cone; past that, test the angle
            if (distance > victim.getBbWidth() && toVictim.scale(1.0D / distance).dot(this.aim) < this.cosAngle) {
                continue;
            }
            if (!this.mob.hasLineOfSight(victim)) {
                continue;
            }
            this.caught.add(victim.getUUID());
            DamageSource source = GMMDamageTypes.source(level, this.mob.getBreathDamageType(), this.mob, this.mob);
            if (victim.hurt(source, this.damage)) {
                this.mob.onBreathHit(victim);
            }
        }
    }

    private boolean mayCatch(LivingEntity entity) {
        return entity != this.mob && entity.isAlive()
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                && !this.caught.contains(entity.getUUID())
                && !this.mob.isBreathAlly(entity);
    }
}
