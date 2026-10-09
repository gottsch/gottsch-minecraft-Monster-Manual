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
package mod.gottsch.neo.gmm.core.sound;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

/**
 * GMM's default {@link SoundEvent}s, shipped with matching {@code .ogg} assets under
 * {@code assets/gmm/sounds/} + {@code assets/gmm/sounds.json}.
 * <p>
 * These make a mob's audio as integral to the library as its model/texture: GMM mob classes use the
 * relevant {@code GMMSounds} entry as the <em>default</em> for their {@code Supplier<SoundEvent>}
 * hook, which any consumer can still override with its own sound (default-with-override, same shape
 * as the projectile {@code itemSupplier} defaults). A registered {@code SoundEvent} is required for
 * {@code playSound} — this is registry <em>infrastructure</em>, not game content, so it does not
 * break gmm's "registers no content" rule (same category as the {@code gmm:mob_config} registry).
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class GMMSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GMM.MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> SHADOW_AMBIENT = registerSoundEvent("entity.shadow.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> DAEMON_AMBIENT = registerSoundEvent("entity.daemon.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEHOLDER_AMBIENT = registerSoundEvent("entity.beholder.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_TYRANT_AMBIENT = registerSoundEvent("entity.death_tyrant.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> GAZER_AMBIENT = registerSoundEvent("entity.gazer.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> SPECTATOR_AMBIENT = registerSoundEvent("entity.spectator.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> SHADOWLORD_AMBIENT = registerSoundEvent("entity.shadowlord.ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> SHADOWLORD_STEP = registerSoundEvent("entity.shadowlord.step");
	/** shared wing-flap ambient, used by Gargoyle, Margoyle, and WingedSkeleton. */
	public static final DeferredHolder<SoundEvent, SoundEvent> WING_FLAP = registerSoundEvent("entity.wing.flap");
	/** shared ambient, used by the Mimic family (Chest Mimic, Barrel Mimic, ...). */
	public static final DeferredHolder<SoundEvent, SoundEvent> MIMIC_AMBIENT = registerSoundEvent("entity.mimic.ambient");

	/** Called from the GMM constructor to attach the registry to the mod event bus. */
	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
	}

	private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, name)));
	}
}
