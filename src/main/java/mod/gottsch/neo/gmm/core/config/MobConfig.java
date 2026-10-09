/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
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
package mod.gottsch.neo.gmm.core.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Datapack-driven, codec-backed per-mob configuration.
 * <p>
 * gmm is a pure library and deliberately knows nothing about any consumer's mob set, so the
 * behavior payload is intentionally generic: numeric knobs live in {@link #properties} and boolean
 * knobs in {@link #flags}, keyed by string. The consuming mod (and gmm's own mob classes) read
 * known keys (e.g. {@code "healAmount"}, {@code "canOpenDoors"}) via the typed accessors.
 * <p>
 * Entries are supplied by consumers as JSON under {@code data/<namespace>/gmm/mob_config/<name>.json},
 * keyed by the mob's registered {@code EntityType} id (e.g. {@code ddenizens:ghoul}).
 *
 * @author by Mark Gottschling on 6/29/2026
 */
public record MobConfig(SpawnSettings spawn, Optional<SpawnSettings> netherSpawn,
                        Map<String, Double> properties, Map<String, Boolean> flags) {

    public static final SpawnSettings DEFAULT_SPAWN = new SpawnSettings(true, -64, 319);

    /** Fallback used when no entry exists for a mob, so lookups never NPE. */
    public static final MobConfig DEFAULT = new MobConfig(DEFAULT_SPAWN, Optional.empty(), Map.of(), Map.of());

    public static final Codec<MobConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpawnSettings.CODEC.optionalFieldOf("spawn", DEFAULT_SPAWN).forGetter(MobConfig::spawn),
            SpawnSettings.CODEC.optionalFieldOf("netherSpawn").forGetter(MobConfig::netherSpawn),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("properties", Map.of()).forGetter(MobConfig::properties),
            Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("flags", Map.of()).forGetter(MobConfig::flags)
    ).apply(instance, MobConfig::new));

    /** Numeric knob lookup with a code-side default. */
    public double number(String key, double defaultValue) {
        Double value = properties.get(key);
        return value != null ? value : defaultValue;
    }

    /** Boolean knob lookup with a code-side default. */
    public boolean flag(String key, boolean defaultValue) {
        Boolean value = flags.get(key);
        return value != null ? value : defaultValue;
    }

    /** The spawn settings for the requested dimension; nether falls back to the overworld block when absent. */
    public SpawnSettings spawnFor(boolean nether) {
        return nether ? netherSpawn.orElse(spawn) : spawn;
    }

    /**
     * Spawn-gating settings. Spawn weight/count are NOT here -- those remain data-driven via
     * Forge {@code add_spawns} biome modifiers. This only gates whether/where a mob may spawn:
     * enabled, height band, sky-exposure requirement, and whether darkness is required. Consumers
     * read these in their spawn predicate; gmm's own mob classes do not (gating is consumer-side).
     */
    public record SpawnSettings(boolean enabled, int minHeight, int maxHeight,
                                SkyVisibility skyVisibility, boolean requiresDarkness) {
        public static final Codec<SpawnSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(SpawnSettings::enabled),
                Codec.INT.optionalFieldOf("minHeight", -64).forGetter(SpawnSettings::minHeight),
                Codec.INT.optionalFieldOf("maxHeight", 319).forGetter(SpawnSettings::maxHeight),
                SkyVisibility.CODEC.optionalFieldOf("skyVisibility", SkyVisibility.ANY).forGetter(SpawnSettings::skyVisibility),
                Codec.BOOL.optionalFieldOf("requiresDarkness", true).forGetter(SpawnSettings::requiresDarkness)
        ).apply(instance, SpawnSettings::new));

        /** Standard-monster convenience: {@code ANY} sky + darkness required. */
        public SpawnSettings(boolean enabled, int minHeight, int maxHeight) {
            this(enabled, minHeight, maxHeight, SkyVisibility.ANY, true);
        }
    }
}
