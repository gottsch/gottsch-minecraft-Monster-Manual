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
 * A short-lived electric spark: a bright, flickering star that fades out fast. Rendered
 * <em>additively</em> and at full brightness, so it glows like light and overlapping sparks (as
 * along a chain-lightning arc) read as a hotter, jagged bolt rather than a row of dim dots. Animates
 * through the {@code gmm:electric_spark} frames over its brief life for the crackle/flicker. Used by
 * the Electric Skeleton for its on-hit chain arcs and its idle charge crackle.
 *
 * @author Mark Gottschling on 7/4/2026
 */
@OnlyIn(Dist.CLIENT)
public class ElectricSparkParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	protected ElectricSparkParticle(ClientLevel level, double x, double y, double z,
									double dx, double dy, double dz, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.sprites = sprites;
		this.gravity = 0.0F;
		this.friction = 0.6F;
		this.hasPhysics = false;                 // sparks arc through the air, they don't collide
		// long enough to actually register the bolt (a too-short flash was invisible mid-swing),
		// still snappy enough to read as lightning rather than a lingering mote
		this.lifetime = 10 + this.random.nextInt(8);  // 10–17 ticks (~0.5–0.85s)
		this.quadSize = 0.32F + this.random.nextFloat() * 0.14F;
		// a tiny crackle of motion around the spawn point (plus whatever tiny seed velocity was passed)
		this.xd = dx + (this.random.nextDouble() - 0.5D) * 0.02D;
		this.yd = dy + (this.random.nextDouble() - 0.5D) * 0.02D;
		this.zd = dz + (this.random.nextDouble() - 0.5D) * 0.02D;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			this.setSpriteFromAge(this.sprites);         // advance the flicker frame
			// hold near full brightness through most of its life, then snap-fade in the last
			// 30% — a linear fade from spawn made the spark look dim/washed-out immediately,
			// which was part of why the additive-blended bolt wasn't reading as electricity
			float lifeFraction = (float) this.age / this.lifetime;
			this.alpha = lifeFraction < 0.7F ? 1.0F : 1.0F - ((lifeFraction - 0.7F) / 0.3F);
		}
	}

	/** Full-bright: a spark is its own light source. */
	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ADDITIVE_GLOW;
	}

	/** Additive, full-bright particle pass (like the translucent sheet but glow-blended, no depth write). */
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
			return "GMM_ELECTRIC_GLOW";
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
			return new ElectricSparkParticle(level, x, y, z, dx, dy, dz, this.sprites);
		}
	}
}
