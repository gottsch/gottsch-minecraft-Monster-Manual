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

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.neo.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * An earth spell that erupts a stationary rock spike at a fixed point (block-entity rendering — see
 * {@link mod.gottsch.neo.gmm.core.client.renderer.entity.SpikeGrowthSpellRenderer} and the spell dev
 * guide §2) after a telegraph, deals one AoE damage pulse to whatever's caught nearby, then lingers
 * briefly before discarding. Uses the {@code gmm:spike_growth_spell} damage type. Telegraph/linger
 * time, damage, and radius are read from the {@code gmm:mob_config} codec.
 * <p>
 * The telegraph is a ground-hugging crumble trail that travels from the caster's position to the
 * eruption point over the telegraph window (a static effect at the eruption point alone is invisible
 * if the viewer is already standing on it) — see {@link #spawnTravelingTelegraph(int)}.
 * <p>
 * Unlike every other spell here, this one never travels itself — {@link #init(LivingEntity, double, double, double, double, double, double)}
 * treats {@code x,y,z} as the eruption point (not a spawn point to fly from) and ignores the direction
 * entirely, snapping onto the ground below in case the target was mid-air or standing on a
 * slab/stairs. Always use the 6-arg overload; the 4-arg (direction-only, spawns at the owner) doesn't
 * make sense for a target-anchored effect and is intentionally left unoverridden.
 * <p>
 * Consumers should cast via the static {@link #cast(EntityType, LivingEntity, LivingEntity)} rather
 * than constructing/{@code init}-ing an instance directly: the full spell is three spikes in a
 * chevron, not one, and that shape is intrinsic to Spike Growth itself (like a real spell's fixed area
 * of effect) rather than a per-caster choice, so it lives here instead of in each consumer's
 * {@code CastSpellGoal.SpellLauncher}.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class SpikeGrowthSpell extends GMMHurtingProjectile {

	private static final EntityDataAccessor<Float> ORIGIN_X = SynchedEntityData.defineId(SpikeGrowthSpell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ORIGIN_Y = SynchedEntityData.defineId(SpikeGrowthSpell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ORIGIN_Z = SynchedEntityData.defineId(SpikeGrowthSpell.class, EntityDataSerializers.FLOAT);

	public SpikeGrowthSpell(EntityType<SpikeGrowthSpell> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * Casts the full spell: three spikes in a chevron — one at the target, two pulled back toward the
	 * caster and out to either side. {@code backOffset}/{@code sideOffset} (how far back/out the two
	 * flanking spikes sit) are {@code gmm:mob_config}-tunable like every other number on this spell,
	 * just not per-mob — the chevron itself is not optional.
	 */
	public static void cast(EntityType<SpikeGrowthSpell> entityType, LivingEntity caster, LivingEntity target) {
		MobConfig config = MobConfigHelper.get(caster.level(), EntityType.getKey(entityType));
		double backOffset = config.number("backOffset", 1.5);
		double sideOffset = config.number("sideOffset", 1.5);

		double dx = target.getX() - caster.getX();
		double dz = target.getZ() - caster.getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);
		double dirX = horizDist > 1.0E-4 ? dx / horizDist : 1.0D;
		double dirZ = horizDist > 1.0E-4 ? dz / horizDist : 0.0D;
		double perpX = -dirZ;   // rotate the caster->target direction 90 degrees in the XZ plane
		double perpZ = dirX;

		// tip at the target, two flanking spikes pulled back toward the caster and out to each side
		double[][] offsets = {
				{0.0D, 0.0D},
				{-dirX * backOffset + perpX * sideOffset, -dirZ * backOffset + perpZ * sideOffset},
				{-dirX * backOffset - perpX * sideOffset, -dirZ * backOffset - perpZ * sideOffset}
		};

		for (double[] offset : offsets) {
			SpikeGrowthSpell spell = new SpikeGrowthSpell(entityType, caster.level());
			spell.init(caster, target.getX() + offset[0], target.getY(), target.getZ() + offset[1], 0.0D, 0.0D, 0.0D);
			caster.level().addFreshEntity(spell);
		}
	}

	@Override
	public void init(LivingEntity owner, double x, double y, double z, double x2, double y2, double z2) {
		super.init(owner, x, y, z, 0.0D, 0.0D, 0.0D);
		this.setPos(x, findGroundY(x, y, z), z);
		this.entityData.set(ORIGIN_X, (float) owner.getX());
		this.entityData.set(ORIGIN_Y, (float) findGroundY(owner.getX(), owner.getY(), owner.getZ()));
		this.entityData.set(ORIGIN_Z, (float) owner.getZ());
	}

	private double findGroundY(double x, double y, double z) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
		for (int i = 0; i < 8; i++) {
			if (!this.level().getBlockState(cursor).getCollisionShape(this.level(), cursor).isEmpty()) {
				return cursor.getY() + 1.0D;
			}
			cursor.move(0, -1, 0);
		}
		return y;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	/** Stationary — zero velocity, so this just telegraphs in place, detonates once, then lingers. */
	@Override
	public void clientSideTick() {
		int telegraphTicks = (int) MobConfigHelper.get(this).number("telegraphTicks", 40.0);
		int lingerTicks = (int) MobConfigHelper.get(this).number("lingerTicks", 30.0);

		if (this.tickCount < telegraphTicks) {
			spawnTravelingTelegraph(telegraphTicks);
		} else if (this.tickCount == telegraphTicks) {
			detonate();
		} else if (this.tickCount > telegraphTicks + lingerTicks) {
			this.discard();
		}
	}

	/**
	 * A ground-hugging crumble trail that travels from the caster's position to the eruption point
	 * over the telegraph window — same {@code BlockParticleOption} idiom {@code GraveZombie} uses for
	 * its dirt burst, but sampling whatever block is actually underfoot at each point along the line
	 * (via {@link #findGroundY}) instead of assuming dirt, since the ground between caster and target
	 * could be anything.
	 */
	private void spawnTravelingTelegraph(int telegraphTicks) {
		if (!WorldInfo.isClientSide(level())) {
			return;
		}
		float progress = (float) this.tickCount / telegraphTicks;
		double x = Mth.lerp(progress, (double) this.entityData.get(ORIGIN_X), this.getX());
		double z = Mth.lerp(progress, (double) this.entityData.get(ORIGIN_Z), this.getZ());
		double yHint = Mth.lerp(progress, (double) this.entityData.get(ORIGIN_Y), this.getY());
		double groundY = findGroundY(x, yHint, z);
		BlockPos groundPos = BlockPos.containing(x, groundY - 0.5D, z);
		BlockState groundState = this.level().getBlockState(groundPos);
		for (int i = 0; i < 6; i++) {
			this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
					x + (random.nextDouble() - 0.5D) * 0.5D, groundY + 0.05D, z + (random.nextDouble() - 0.5D) * 0.5D,
					0.0D, 0.12D, 0.0D);
		}
	}

	private void detonate() {
		if (WorldInfo.isClientSide(level())) {
			// same ground-crumble idiom as the traveling telegraph, but a bigger, faster burst -- reads
			// as the spike shoving all that dirt/rock out of the way as it erupts, not a generic blast.
			BlockPos groundPos = BlockPos.containing(getX(), getY() - 0.5D, getZ());
			BlockState groundState = this.level().getBlockState(groundPos);
			for (int p = 0; p < 30; p++) {
				this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
						getX() + (random.nextDouble() - 0.5D) * 0.8D, getY() + 0.1D, getZ() + (random.nextDouble() - 0.5D) * 0.8D,
						(random.nextDouble() - 0.5D) * 0.1D, 0.3D + random.nextDouble() * 0.2D, (random.nextDouble() - 0.5D) * 0.1D);
			}
			return;
		}

		this.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.9F + random.nextFloat() * 0.2F);
		double radius = MobConfigHelper.get(this).number("radius", 1.5);
		float damage = (float) MobConfigHelper.get(this).number("damage", 5.0);
		// initial vertical velocity to peak at roughly knockupHeight blocks, using MC's standard 0.08
		// blocks/tick^2 entity gravity: v = sqrt(2 * g * h) -- an approximation (ignores air drag) but
		// close enough for a knockback feel, not physics precision.
		double knockupHeight = MobConfigHelper.get(this).number("knockupHeight", 2.0);
		double knockupVelocity = Math.sqrt(2 * 0.08 * knockupHeight);
		AABB aabb = new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + 2.0D, getZ() + radius);
		List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, aabb, e -> e != this.getOwner());
		Entity ownerEntity = this.getOwner();
		for (LivingEntity target : targets) {
			target.hurt(GMMDamageTypes.source(level(), GMMDamageTypes.SPIKE_GROWTH_SPELL, this, ownerEntity), damage);
			target.push(0.0D, knockupVelocity, 0.0D);
			if (ownerEntity instanceof LivingEntity) {
				this.doEnchantDamageEffects((LivingEntity) ownerEntity, target);
			}
		}
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(ORIGIN_X, 0F);
		this.entityData.define(ORIGIN_Y, 0F);
		this.entityData.define(ORIGIN_Z, 0F);
	}
}
