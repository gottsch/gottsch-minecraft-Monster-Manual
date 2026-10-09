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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A small glowing eye-orb mote used for the Beholder's Enthrall charm ability (see
 * {@code EnthrallGoal}): drifts gently rather than falling or launching, and fades in then out over
 * its life so a cluster of them reads as a hypnotic shimmer around the caster/target rather than a
 * spark or splash. Additively lit like {@link ElectricSparkParticle} — an eye-charm is its own light.
 *
 * @author Mark Gottschling on 7/9/2026
 */
@OnlyIn(Dist.CLIENT)
public class OculusOrbParticle extends TextureSheetParticle {

	protected OculusOrbParticle(ClientLevel level, double x, double y, double z,
								 double dx, double dy, double dz, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.gravity = 0.0F;
		this.friction = 0.98F;
		this.hasPhysics = false;
		this.lifetime = 20 + this.random.nextInt(12);
		this.quadSize = 0.15F + this.random.nextFloat() * 0.1F;
		// a slow, lazy drift rather than a launch — plus whatever seed velocity was passed
		this.xd = dx + (this.random.nextDouble() - 0.5D) * 0.01D;
		this.yd = dy + 0.01D + (this.random.nextDouble() - 0.5D) * 0.01D;
		this.zd = dz + (this.random.nextDouble() - 0.5D) * 0.01D;
		this.pickSprite(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			// fade in quickly, then fade out over the back half of its life
			float lifeFraction = (float) this.age / this.lifetime;
			this.alpha = lifeFraction < 0.15F ? lifeFraction / 0.15F
					: (lifeFraction < 0.5F ? 1.0F : 1.0F - (lifeFraction - 0.5F) / 0.5F);
		}
	}

	/** Full-bright: a charm-eye mote is its own light source. */
	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ADDITIVE_GLOW;
	}

	/** Additive, full-bright particle pass (same as {@link ElectricSparkParticle}'s). */
	private static final ParticleRenderType ADDITIVE_GLOW = new ParticleRenderType() {
		@Override
		public void begin(BufferBuilder builder, TextureManager textureManager) {
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			RenderSystem.setShader(GameRenderer::getParticleShader);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		@Override
		public void end(Tesselator tesselator) {
			tesselator.end();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
		}

		@Override
		public String toString() {
			return "GMM_OCULUS_GLOW";
		}
	};

	@OnlyIn(Dist.CLIENT)
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
									   double x, double y, double z, double dx, double dy, double dz) {
			return new OculusOrbParticle(level, x, y, z, dx, dy, dz, this.sprites);
		}
	}
}
