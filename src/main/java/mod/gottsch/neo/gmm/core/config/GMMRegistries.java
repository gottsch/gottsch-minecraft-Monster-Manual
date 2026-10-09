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

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Registers gmm's datapack-driven registries. This is the only thing gmm registers, and it
 * registers no game content -- only a data registry whose entries are supplied by consumers as
 * JSON. Subscribed onto the mod event bus from the {@link GMM} constructor.
 *
 * @author by Mark Gottschling on 6/29/2026
 */
public class GMMRegistries {

    /**
     * Per-mob config registry. Entries live at {@code data/<namespace>/gmm/mob_config/<name>.json}
     * and are keyed {@code <namespace>:<name>} -- matching {@code EntityType.getKey()}.
     */
    public static final ResourceKey<Registry<MobConfig>> MOB_CONFIG =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "mob_config"));

    @SubscribeEvent
    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        // codec is reused as the network codec so entries sync to clients (future-proofing).
        event.dataPackRegistry(MOB_CONFIG, MobConfig.CODEC, MobConfig.CODEC);
    }
}
