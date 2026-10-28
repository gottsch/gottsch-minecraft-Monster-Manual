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
package mod.gottsch.forge.gmm.core.entity.monster;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * A mob with a breath weapon, driven by {@link mod.gottsch.forge.gmm.core.entity.ai.goal.BreathAttackGoal}.
 * The goal owns the cone, the timing and the recharge; the mob supplies what the breath <em>is</em>
 * (its particle, sound and damage type), what it does to whoever it catches, and who it spares.
 *
 * <p>{@link #isBreathing()} is synched, like {@link IChargingMob#isCharging()}, because the pose is a
 * client-side read.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public interface IBreathingMob {

    /** Synched: is a breath in progress right now. */
    boolean isBreathing();

    void setBreathing(boolean breathing);

    /** The particle streamed along the cone. */
    ParticleOptions getBreathParticle();

    /** Played once as the breath starts. */
    SoundEvent getBreathSound();

    /** The damage type each victim is hurt with (see {@code GMMDamageTypes}). */
    ResourceKey<DamageType> getBreathDamageType();

    /** Called once per victim per breath, after the breath damage landed. */
    void onBreathHit(LivingEntity target);

    /** Whoever this returns true for is never caught in the breath (pack mates, the owner). */
    boolean isBreathAlly(LivingEntity entity);
}
