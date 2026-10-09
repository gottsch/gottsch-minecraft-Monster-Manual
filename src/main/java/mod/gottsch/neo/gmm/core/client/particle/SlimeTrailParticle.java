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
 * A flat smear of ooze left behind on the ground — unlike {@link AcidDripParticle} this doesn't fall
 * or land, it forms in place and just lingers, fading out near the end of its life. Reuses vanilla's
 * {@code spell_0}..{@code spell_7} sprite frames (the soft round dot behind splash-potion/ambient
 * effect particles), tinted per spawn call rather than shipping a dedicated texture. Used by
 * {@code GelatinousCube} for its trail; any other ooze could reuse it with a different tint.
 *
 * @author Mark Gottschling on 7/7/2026
 */
@OnlyIn(Dist.CLIENT)
public class SlimeTrailParticle extends TextureSheetParticle {

	protected SlimeTrailParticle(ClientLevel level, double x, double y, double z,
								  float r, float g, float b, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.setSize(0.25F, 0.02F);
		this.gravity = 0.0F;
		this.hasPhysics = false;    // it's already flush with the ground — nothing to collide with
		this.xd = this.yd = this.zd = 0.0D;
		this.lifetime = 60 + this.random.nextInt(40);
		this.quadSize = 0.2F + this.random.nextFloat() * 0.15F;
		this.setColor(r, g, b);
		this.pickSprite(sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			// fade out over the last third of its life instead of popping out abruptly
			float lifeFraction = (float) this.age / this.lifetime;
			this.alpha = lifeFraction < 0.66F ? 1.0F : 1.0F - (lifeFraction - 0.66F) / 0.34F;
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
			// dx/dy/dz are repurposed as the RGB tint (0..1), same trick as vanilla's entity-effect particle
			return new SlimeTrailParticle(level, x, y, z, (float) dx, (float) dy, (float) dz, this.sprites);
		}
	}
}
