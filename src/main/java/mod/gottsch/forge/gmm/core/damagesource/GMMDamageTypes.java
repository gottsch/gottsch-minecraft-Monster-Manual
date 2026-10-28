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
package mod.gottsch.forge.gmm.core.damagesource;

import mod.gottsch.forge.gmm.core.GMM;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ResourceKey}s for GMM's spell damage types. These are data-driven — the actual
 * {@link DamageType} definitions live as JSON under {@code data/gmm/damage_type/} (GMM ships data,
 * but registers nothing in code). Any mod consuming GMM's spell projectiles gets these for free.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class GMMDamageTypes {
    public static final ResourceKey<DamageType> FIRESPOUT_SPELL = register("firespout_spell");
    public static final ResourceKey<DamageType> HARM_SPELL = register("harm_spell");
    public static final ResourceKey<DamageType> DISINTEGRATE_SPELL = register("disintegrate_spell");
    public static final ResourceKey<DamageType> FIREWALL_SPELL = register("firewall_spell");
    public static final ResourceKey<DamageType> SPIKE_GROWTH_SPELL = register("spike_growth_spell");
    // Bodak's Death Gaze (see core.entity.monster.zombie.Bodak) -- not a spell projectile, but shares
    // this same data-driven damage-type mechanism.
    public static final ResourceKey<DamageType> DEATH_GAZE = register("death_gaze");
    // Bodak's Withering Gaze (WitheringGazeSpell) -- the ranged, no-windup necrotic bolt companion to
    // Death Gaze's melee-range stare.
    public static final ResourceKey<DamageType> WITHERING_GAZE_SPELL = register("withering_gaze_spell");
    // Leech (core.entity.monster.Leech). Both bypass armour (data/minecraft/tags/damage_type/
    // bypasses_armor.json): the drain is from inside the armour, and ripping one off is the player's
    // own doing, so plate must not make it free.
    public static final ResourceKey<DamageType> LEECH_DRAIN = register("leech_drain");
    public static final ResourceKey<DamageType> LEECH_RIP = register("leech_rip");
    // Breath weapons (core.entity.ai.goal.BreathAttackGoal): the Hellhound's and the Winter Wolf's.
    // Named for the element, not the mob, so the next breather reuses them. fire_breath is in
    // minecraft:is_fire (fire-immune mobs ignore it, Fire Protection reduces it) and frost_breath in
    // minecraft:is_freezing (blazes, striders and magma cubes take extra).
    public static final ResourceKey<DamageType> FIRE_BREATH = register("fire_breath");
    public static final ResourceKey<DamageType> FROST_BREATH = register("frost_breath");

    private static ResourceKey<DamageType> register(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GMM.MOD_ID, name));
    }

    /**
     * Builds a {@link DamageSource} for the given damage-type key from the level's registry — the
     * same thing vanilla's {@code DamageSources.source(ResourceKey)} does, but public and without
     * requiring an access transformer (that method is private; a library shouldn't impose an AT).
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    /**
     * As {@link #source(Level, ResourceKey)}, but carrying what dealt the damage: {@code direct} is
     * the thing that touched the victim (the spell projectile), {@code causing} is who is to blame
     * (its caster). Either may be null.
     *
     * <p><strong>Use this one for anything thrown.</strong> The entity-less form has no position, and
     * vanilla's shield test ({@code LivingEntity#isDamageSourceBlocked}) returns "not blocked" the
     * moment {@code getSourcePosition()} is null &mdash; it cannot tell whether the attack came from
     * in front. So a spell hurt through the bare form could never be blocked, whichever way the player
     * faced (found 2026-09-10 from Dungeons2's Beholder). Carrying the entities also gives the kill
     * credit, the retaliation target and the "... by X" death message their attacker, and lets the
     * damage type's {@code when_caused_by_living_non_player} scaling apply, as it does for arrows.</p>
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key,
                                      @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),
                direct, causing);
    }
}
