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
package mod.gottsch.neo.gmm.core;

import mod.gottsch.neo.gmm.core.capability.GMMCapabilities;
import mod.gottsch.neo.gmm.core.config.GMMRegistries;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * gottsch's Monster Manual (gmm) is a pure base-class library, NOT a content mod.
 * It deliberately registers no EntityTypes, items, or other game objects of its own.
 * Instead it supplies reusable base mob classes, AI goals, models, renderers, textures,
 * and helpers. Consuming mods (e.g. Village Dungeons, gottsch's Monster Manual) depend on gmm
 * and register their own EntityTypes (villagedungeons:rat, dungeondenizens:rat, ...),
 * reusing the gmm-namespaced assets. One-off mobs may register in their own mod while
 * still contributing their model/renderer/texture back to this library for reuse.
 * <p>
 * The only registrations are <em>infrastructure</em>, not game content: a datapack registry
 * ({@link GMMRegistries#MOB_CONFIG} -- gmm defines the registry + codec but ships no entries;
 * consumers supply the JSON) and default {@link GMMSounds} SoundEvents (a registered SoundEvent is
 * required to be playable; the matching {@code .ogg} assets ship in gmm, and mob classes use them as
 * an overridable default). Neither adds mobs, items, or other world content.
 *
 * @author by Mark Gottschling on 4/11/2025
 */
@Mod(GMM.MOD_ID)
public class GMM {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "gmm";

    public GMM(IEventBus modEventBus, ModContainer container) {
        // by design gmm registers no game content; only its datapack registry (see class javadoc)
        // and default SoundEvents (registry infrastructure, not content -- same category exception).
        modEventBus.register(GMMRegistries.class);
        GMMSounds.register(modEventBus);
        GMMParticles.register(modEventBus);
        GMMMobEffects.register(modEventBus);
        GMMCapabilities.register(modEventBus);
    }
}
