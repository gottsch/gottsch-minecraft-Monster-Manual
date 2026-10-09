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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;

import java.util.Optional;

/**
 * Static lookup helpers for the {@link GMMRegistries#MOB_CONFIG} datapack registry.
 *
 * @author by Mark Gottschling on 6/29/2026
 */
public class MobConfigHelper {

    private MobConfigHelper() {}

    /**
     * Looks up the config for the given mob id, returning {@link MobConfig#DEFAULT} when no entry
     * (or no registry) is present so callers never deal with null.
     */
    public static MobConfig get(LevelAccessor level, ResourceLocation entityId) {
        return find(level, entityId).orElse(MobConfig.DEFAULT);
    }

    /** Convenience overload keyed by the entity's registered type. */
    public static MobConfig get(Entity entity) {
        return get(entity.level(), EntityType.getKey(entity.getType()));
    }

    /**
     * Returns the config entry if one actually exists, or empty. Lets callers distinguish a real
     * datapack entry from "no data" (e.g. the DD spawn bridge falls back to Forge Config on empty).
     */
    public static Optional<MobConfig> find(LevelAccessor level, ResourceLocation entityId) {
        return level.registryAccess()
                .registry(GMMRegistries.MOB_CONFIG)
                .flatMap(registry -> Optional.ofNullable(registry.get(entityId)));
    }

    /** Convenience overload keyed by the entity's registered type. */
    public static Optional<MobConfig> find(Entity entity) {
        return find(entity.level(), EntityType.getKey(entity.getType()));
    }
}
