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
 * The Burning Skeleton's aura-pulse flame: a single particle that travels <em>flat</em> outward along
 * the ground and then, once it has covered the aura's reach, <em>bends upward</em> — the "shoots out
 * then rises" gesture, done as one particle's own motion (a vanilla flame can't change course
 * mid-flight, and its fixed lifespan made stacked emissions pile into a solid disc). Fully client-side
 * and short-lived, so it costs no server traffic and leaves no lingering trail.
 *
 * <p>The spawn velocity args are repurposed as the flight plan: {@code (dx, dz)} is the outward
 * horizontal velocity and {@code dy} carries the flat reach (blocks) to cover before rising.
 *
 * @author Mark Gottschling on 7/6/2026
 */
@OnlyIn(Dist.CLIENT)
public class BurningAuraFlameParticle extends TextureSheetParticle {
	// Upward speed once the runner reaches the rim, and how long it keeps rising before it's gone
	// (gentler + taller than the first pass: ~0.12 * 12 ≈ 1.4 blocks of rise).
	private static final double RISE_SPEED = 0.12D;
	private static final int RISE_TICKS = 12;

	private final int flatTicks;
	private boolean rising;

	protected BurningAuraFlameParticle(ClientLevel level, double x, double y, double z,
									   double dx, double reach, double dz, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.gravity = 0.0F;
		this.friction = 1.0F;            // constant speed so the flat reach is predictable
		this.hasPhysics = false;         // it's a telegraph, not a physical flame — don't snag on blocks
		this.xd = dx;
		this.yd = 0.0D;
		this.zd = dz;
		// how many ticks of flat travel cover the reach, from the (constant) horizontal speed
		double hSpeed = Math.sqrt(dx * dx + dz * dz);
		this.flatTicks = hSpeed > 1.0E-4D ? (int) Math.ceil(reach / hSpeed) : 1;
		this.lifetime = this.flatTicks + RISE_TICKS;
		this.quadSize = 0.14F + this.random.nextFloat() * 0.04F;
		this.pickSprite(sprites);
	}

	@Override
	public void tick() {
		// at the rim, stop travelling out and leap upward instead
		if (!this.rising && this.age >= this.flatTicks) {
			this.rising = true;
			this.xd = 0.0D;
			this.zd = 0.0D;
			this.yd = RISE_SPEED;
		}
		super.tick();
		if (!this.removed) {
			// fade over the whole life so the tail of the rise dissolves rather than popping out
			this.alpha = 1.0F - ((float) this.age / this.lifetime);
		}
	}

	/** Full-bright: a flame is its own light source. */
	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ADDITIVE_GLOW;
	}

	/** Additive, full-bright pass so overlapping runners read as hotter fire (shared shape with the spark). */
	private static final ParticleRenderType ADDITIVE_GLOW = new ParticleRenderType() {
		@Override
		public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			RenderSystem.setShader(GameRenderer::getParticleShader);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		// 1.21 has no end() hook: ParticleEngine draws the batch, then restores depthMask/blend itself.

		@Override
		public String toString() {
			return "GMM_BURNING_AURA_GLOW";
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
			return new BurningAuraFlameParticle(level, x, y, z, dx, dy, dz, this.sprites);
		}
	}
}
