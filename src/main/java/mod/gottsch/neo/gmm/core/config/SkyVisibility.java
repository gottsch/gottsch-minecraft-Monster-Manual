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
package mod.gottsch.neo.gmm.core.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * A mob's sky-exposure requirement for natural spawning, as a data-driven spawn-gating knob on
 * {@link MobConfig.SpawnSettings}. Consumers read it in their spawn predicate.
 * <ul>
 *   <li>{@link #ANY} — no sky requirement (standard monster; the default).</li>
 *   <li>{@link #MUST_SEE} — may spawn only where the sky is visible (surface-only).</li>
 *   <li>{@link #MUST_NOT_SEE} — may spawn only where the sky is NOT visible (underground-only).</li>
 * </ul>
 *
 * @author by Mark Gottschling on 7/1/2026
 */
public enum SkyVisibility implements StringRepresentable {
    ANY("any"),
    MUST_SEE("must_see"),
    MUST_NOT_SEE("must_not_see");

    public static final Codec<SkyVisibility> CODEC = StringRepresentable.fromEnum(SkyVisibility::values);

    private final String name;

    SkyVisibility(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
