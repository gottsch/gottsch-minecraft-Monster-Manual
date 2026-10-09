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
package mod.gottsch.neo.gmm.core.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A caustic acid droplet. It clings for a moment (a "drip" forming on the bone), then gravity takes
 * over and it falls, fading out and vanishing when it lands — reading unmistakably as dripping acid
 * rather than a floating mote. Green teardrop sprite ({@code assets/gmm/textures/particle/acid_drip}).
 *
 * @author Mark Gottschling on 7/3/2026
 */
@OnlyIn(Dist.CLIENT)
public class AcidDripParticle extends TextureSheetParticle {
	private final int hangTime;

	protected AcidDripParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.setSize(0.1F, 0.1F);
		this.gravity = 0.0F;                 // cling first; gravity engages after the hang
		this.friction = 1.0F;
		this.xd = this.yd = this.zd = 0.0D;  // no launch spread — it forms in place
		this.lifetime = 40 + this.random.nextInt(20);
		this.hangTime = 2 + this.random.nextInt(3);
		this.quadSize = 0.06F + this.random.nextFloat() * 0.03F;
		this.pickSprite(sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		// engage gravity once the drip has "formed"
		if (this.age >= this.hangTime) {
			this.gravity = 0.14F;
		}
		super.tick();
		// splat: vanish on landing (a short fade-out over the tail of its lifetime handles the rest)
		if (this.onGround) {
			this.remove();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
									   double x, double y, double z, double dx, double dy, double dz) {
			return new AcidDripParticle(level, x, y, z, this.sprites);
		}
	}
}
