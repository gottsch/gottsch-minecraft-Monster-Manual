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
package mod.gottsch.neo.gmm.core.client;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.particle.AcidDripParticle;
import mod.gottsch.neo.gmm.core.client.particle.BurningAuraFlameParticle;
import mod.gottsch.neo.gmm.core.client.particle.ElectricSparkParticle;
import mod.gottsch.neo.gmm.core.client.particle.OculusOrbParticle;
import mod.gottsch.neo.gmm.core.client.particle.SlimeTrailParticle;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Client-only mod-bus setup for gmm's shared assets. Currently just binds the sprite-based
 * {@code ParticleProvider}s for {@link GMMParticles}. (Entity renderers stay consumer-registered
 * — see each consumer's ClientSetup — but particle providers are pure gmm-asset wiring, so gmm owns
 * them, mirroring how gmm owns its SoundEvents.)
 *
 * @author Mark Gottschling on 7/3/2026
 */
@EventBusSubscriber(modid = GMM.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GMMClientSetup {

	@SubscribeEvent
	public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(GMMParticles.ACID_DRIP.get(), AcidDripParticle.Provider::new);
		// blood drip reuses the acid-drip behaviour verbatim, just a red teardrop sprite.
		event.registerSpriteSet(GMMParticles.BLOOD_DRIP.get(), AcidDripParticle.Provider::new);
		event.registerSpriteSet(GMMParticles.ELECTRIC_SPARK.get(), ElectricSparkParticle.Provider::new);
		event.registerSpriteSet(GMMParticles.BURNING_AURA.get(), BurningAuraFlameParticle.Provider::new);
		event.registerSpriteSet(GMMParticles.SLIME_TRAIL.get(), SlimeTrailParticle.Provider::new);
		event.registerSpriteSet(GMMParticles.OCULUS_ORB.get(), OculusOrbParticle.Provider::new);
	}
}
