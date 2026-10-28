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
package mod.gottsch.forge.gmm.core.entity.monster.canine;

import mod.gottsch.forge.gmm.core.config.MobConfig;
import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.forge.gmm.core.entity.ai.goal.BreathAttackGoal;
import mod.gottsch.forge.gmm.core.entity.monster.IBreathingMob;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The D&amp;D winter wolf: a large, white, cunning wolf of the far north that breathes frost. Its
 * frost breath ({@link BreathAttackGoal}, {@code gmm:frost_breath}) chills whoever it catches the
 * way the Frost Skeleton's blow does (Slowness plus vanilla's powder-snow freeze shiver), and it
 * cannot freeze itself, so powder snow and freezing never hurt it.
 *
 * <p>Large: hitbox 0.85 x 1.2 ({@link #WIDTH}/{@link #HEIGHT}), renderer scale 1.4. 36 HP, 6 bite,
 * 2 armour, 0.3 knockback resistance.
 *
 * <p>{@code gmm:mob_config}: the breath keys (defaults: {@code breathRange} 6, {@code breathAngle}
 * 30, {@code breathDuration} 30, {@code breathDamage} 6, {@code breathRechargeInterval} 40,
 * {@code breathRechargeChance} 0.333), {@code breathSlowTicks} (100), {@code breathSlowAmplifier}
 * (1), {@code breathFreezeTicks} (140), plus the {@link GMMCanine} keys.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class WinterWolf extends GMMCanine implements IBreathingMob {
    public static final float WIDTH = 0.85F;
    public static final float HEIGHT = 1.2F;

    private static final BreathAttackGoal.Defaults BREATH = new BreathAttackGoal.Defaults(
            6.0D, 30.0D, 30, 6.0D, 40, 1.0D / 3.0D);
    // the Frost Skeleton's chill: Slowness II for 5 s and a 7 s freeze shiver
    private static final int DEFAULT_BREATH_SLOW_TICKS = 100;
    private static final int DEFAULT_BREATH_SLOW_AMPLIFIER = 1;
    private static final int DEFAULT_BREATH_FREEZE_TICKS = 140;

    public WinterWolf(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new BreathAttackGoal<>(this, BREATH));
    }

    @Override
    protected TagKey<EntityType<?>> getPackTag() {
        return GMMTags.EntityTypes.WINTER_WOLF_PACK;
    }

    @Override
    protected TagKey<EntityType<?>> getCompanionPool() {
        return GMMTags.EntityTypes.WINTER_WOLF_COMPANIONS;
    }

    @Override
    protected float getBaseVoicePitch() {
        return 0.85F;
    }

    @Override
    public ParticleOptions getBreathParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    public SoundEvent getBreathSound() {
        return SoundEvents.POWDER_SNOW_BREAK;
    }

    @Override
    public ResourceKey<DamageType> getBreathDamageType() {
        return GMMDamageTypes.FROST_BREATH;
    }

    /** The Frost Skeleton's chill (see {@code FrostSkeleton#doHurtTarget}), with configurable numbers. */
    @Override
    public void onBreathHit(LivingEntity target) {
        MobConfig config = MobConfigHelper.get(this);
        int slowTicks = (int) config.number("breathSlowTicks", DEFAULT_BREATH_SLOW_TICKS);
        if (slowTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks,
                    (int) config.number("breathSlowAmplifier", DEFAULT_BREATH_SLOW_AMPLIFIER)), this);
        }
        int freezeTicks = (int) config.number("breathFreezeTicks", DEFAULT_BREATH_FREEZE_TICKS);
        if (freezeTicks > 0 && target.canFreeze()) {
            target.setTicksFrozen(target.getTicksFrozen() + freezeTicks);
        }
    }

    /** Immune to freezing and powder snow: it is a creature of the cold. */
    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public void aiStep() {
        // frost drifting off the coat
        if (this.level().isClientSide && this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6D), this.getRandomY(),
                    this.getRandomZ(0.6D), 0.0D, -0.02D, 0.0D);
        }
        super.aiStep();
    }
}
