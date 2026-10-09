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
package mod.gottsch.neo.gmm.core.entity.projectile;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.neo.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One column of a firewall spell: travels in a straight horizontal line (no homing, no rise/fall)
 * and burns anything it sweeps through, ignoring block collisions entirely so it doesn't fizzle on
 * the first fence post. A caster-side launcher spawns a row of these side-by-side, perpendicular to
 * the casting direction, all sharing the same forward velocity, so the row advances as a single wall
 * instead of fanning out.
 * <p>
 * The flame column itself is spawned once, at the entity's first client-side tick, with each particle
 * given the column's own travel velocity so the stack drifts forward with the entity instead of being
 * redrawn every tick (see {@link #spawnColumnParticles()}).
 * <p>
 * Uses the {@code gmm:firewall_spell} damage type. Column height, sweep range, and damage are read
 * from the {@code gmm:mob_config} codec.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class FirewallColumnSpell extends GMMHurtingProjectile implements ItemSupplier {

	private static final EntityDataAccessor<Float> ORIGIN_X = SynchedEntityData.defineId(FirewallColumnSpell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ORIGIN_Z = SynchedEntityData.defineId(FirewallColumnSpell.class, EntityDataSerializers.FLOAT);

	/** local to each side (server/client) — the particle stack is spawned once by whichever side renders it. */
	private boolean particlesSpawned;

	public FirewallColumnSpell(EntityType<FirewallColumnSpell> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * {@code x2,y2,z2} is the direction to the target; forced to horizontal-only (y2 = 0) before
	 * delegating, so the column always travels at full speed regardless of the target's elevation
	 * instead of losing speed budget to a vertical component it's about to discard anyway.
	 */
	@Override
	public void init(LivingEntity owner, double x, double y, double z, double x2, double y2, double z2) {
		super.init(owner, x, y, z, x2, 0.0D, z2);
		setOrigin(this.getX(), this.getZ());
	}

	/**
	 * {@code x,y,z} here is a direction (see {@link GMMHurtingProjectile#init(LivingEntity, double, double, double)}),
	 * not a position — same horizontal-only treatment as the six-arg overload.
	 */
	@Override
	public void init(LivingEntity owner, double x, double y, double z) {
		super.init(owner, x, 0.0D, z);
		setOrigin(this.getX(), this.getZ());
	}

	private void setOrigin(double originX, double originZ) {
		this.entityData.set(ORIGIN_X, (float) originX);
		this.entityData.set(ORIGIN_Z, (float) originZ);
	}

	@Override
	public boolean isOnFire() {
		return true;
	}

	/**
	 * Straight-line horizontal sweep. Unlike {@link FireSpoutSpell}, block hits are ignored (a wall
	 * shouldn't stop for a single fence post) and entity hits don't end the flight, so the column keeps
	 * burning everything it passes through until it exceeds {@code wallRange}.
	 */
	@Override
	public void clientSideTick() {

		if (this.shouldBurn()) {
			this.setSecondsOnFire(1);
		}

		if (WorldInfo.isClientSide(level()) && !this.particlesSpawned) {
			spawnColumnParticles();
			this.particlesSpawned = true;
		}

		HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hitresult.getType() == HitResult.Type.ENTITY && !net.neoforged.neoforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
			this.onHit(hitresult);
		}

		this.checkInsideBlocks();
		Vec3 vec3 = this.getDeltaMovement();
		double d0 = this.getX() + vec3.x;
		double d1 = this.getY() + vec3.y;
		double d2 = this.getZ() + vec3.z;

		ProjectileUtil.rotateTowardsMovement(this, 0.2F);
		float f = this.getInertia();
		this.setDeltaMovement(vec3.add(this.xPower, this.yPower, this.zPower).scale((double) f));
		this.setPos(d0, d1, d2);

		double dx = d0 - this.entityData.get(ORIGIN_X);
		double dz = d2 - this.entityData.get(ORIGIN_Z);
		if (Math.sqrt(dx * dx + dz * dz) > MobConfigHelper.get(this).number("wallRange", 8.0)) {
			this.discard();
		}
	}

	/**
	 * Only entity hits are dispatched (to {@link #onHitEntity}); block hits are swallowed so the wall
	 * sweeps through terrain instead of stopping.
	 */
	@Override
	protected void onHit(HitResult hitResult) {
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			super.onHit(hitResult);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!WorldInfo.isClientSide(level())) {
			Entity target = hitResult.getEntity();
			Entity ownerEntity = this.getOwner();

			target.hurt(GMMDamageTypes.source(level(), GMMDamageTypes.FIREWALL_SPELL, this, ownerEntity), (float) MobConfigHelper.get(this).number("damage", 4.0));
			if (target instanceof LivingEntity) {
				this.doEnchantDamageEffects((LivingEntity) ownerEntity, target);
			}
		}
	}

	/**
	 * One-shot: {@link Level#addParticle} is a no-op on the logical server (only {@code ClientLevel}
	 * actually spawns anything), so this only does something the first time it runs on a physical
	 * client — and it's only invoked once (see {@link #particlesSpawned}), so the stack isn't redrawn
	 * every tick. Each particle carries the column's own horizontal velocity so it drifts forward with
	 * the entity under ordinary particle physics instead of being repainted.
	 */
	private void spawnColumnParticles() {
		double groundY = findGroundY(this.getX(), this.getY(), this.getZ());
		double height = MobConfigHelper.get(this).number("columnHeight", 2.5);
		ParticleOptions particle = getTrailParticle();
		for (double h = 0; h < height; h += 0.4D) {
			this.level().addParticle(particle, this.getX(), groundY + h, this.getZ(), this.xPower, 0.0D, this.zPower);
		}
	}

	/**
	 * Columns spawn at the caster's chest height, not at the ground, so the particle stack needs its
	 * own downward scan to find where to start.
	 */
	private double findGroundY(double x, double y, double z) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
		for (int i = 0; i < 16; i++) {
			if (!this.level().getBlockState(cursor).getCollisionShape(this.level(), cursor).isEmpty()) {
				return cursor.getY() + 1.0D;
			}
			cursor.move(0, -1, 0);
		}
		return y;
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.FLAME;
	}

	@Override
	public ItemStack getItem() {
		return new ItemStack(Items.FIRE_CHARGE);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(ORIGIN_X, 0F);
		this.entityData.define(ORIGIN_Z, 0F);
	}
}
