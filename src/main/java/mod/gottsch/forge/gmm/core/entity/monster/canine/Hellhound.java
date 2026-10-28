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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * The D&amp;D hell hound: a fiend in the shape of a black hound, hunting in packs and breathing
 * fire. Its fire breath ({@link BreathAttackGoal}, {@code gmm:fire_breath}) is a short cone that
 * sets whoever it catches alight; it is immune to fire and lava, and walks through fire as if it
 * were not there.
 *
 * <p>Kept apart from the {@link mod.gottsch.forge.gmm.core.entity.monster.EmberHound} on purpose. That
 * is a fire elemental in hound shape (a body of embers, full-bright, a burning bite, put out by
 * water); this is a flesh-and-blood fiend (a black coat with ember markings and glowing eyes, an
 * ordinary bite, and the breath, pack and howl the Ember Hound does not have). Water does nothing
 * to it.
 *
 * <p>Hitbox 0.7 x 0.95 ({@link #WIDTH}/{@link #HEIGHT}), renderer scale 1.1. 28 HP, 5 bite, 4
 * armour.
 *
 * <p>{@code gmm:mob_config}: the breath keys (defaults: {@code breathRange} 5, {@code breathAngle}
 * 30, {@code breathDuration} 30, {@code breathDamage} 6, {@code breathRechargeInterval} 40,
 * {@code breathRechargeChance} 0.333), {@code breathFireSeconds} (4), plus the {@link GMMCanine}
 * keys.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class Hellhound extends GMMCanine implements IBreathingMob {
    public static final float WIDTH = 0.7F;
    public static final float HEIGHT = 0.95F;

    private static final BreathAttackGoal.Defaults BREATH = new BreathAttackGoal.Defaults(
            5.0D, 30.0D, 30, 6.0D, 40, 1.0D / 3.0D);
    private static final int DEFAULT_BREATH_FIRE_SECONDS = 4;

    public Hellhound(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // fire is no obstacle to a fiend that cannot burn
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new BreathAttackGoal<>(this, BREATH));
    }

    @Override
    protected TagKey<EntityType<?>> getPackTag() {
        return GMMTags.EntityTypes.HELLHOUND_PACK;
    }

    @Override
    protected TagKey<EntityType<?>> getCompanionPool() {
        return GMMTags.EntityTypes.HELLHOUND_COMPANIONS;
    }

    @Override
    protected float getBaseVoicePitch() {
        return 0.7F;
    }

    @Override
    public ParticleOptions getBreathParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public SoundEvent getBreathSound() {
        return SoundEvents.BLAZE_SHOOT;
    }

    @Override
    public ResourceKey<DamageType> getBreathDamageType() {
        return GMMDamageTypes.FIRE_BREATH;
    }

    @Override
    public void onBreathHit(LivingEntity target) {
        int seconds = (int) MobConfigHelper.get(this).number("breathFireSeconds", DEFAULT_BREATH_FIRE_SECONDS);
        if (seconds > 0) {
            target.setSecondsOnFire(seconds);
        }
    }

    /**
     * Fire-immune by nature (fire, lava, its own pack's breath), so a consumer that forgets
     * {@code .fireImmune()} on its {@code EntityType.Builder} still gets a hound that doesn't burn.
     */
    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void aiStep() {
        // a fiend's breath between breaths: the odd wisp of smoke from the muzzle
        if (this.level().isClientSide && this.random.nextInt(8) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getEyeY(), this.getZ(),
                    0.0D, 0.02D, 0.0D);
        }
        super.aiStep();
    }
}
