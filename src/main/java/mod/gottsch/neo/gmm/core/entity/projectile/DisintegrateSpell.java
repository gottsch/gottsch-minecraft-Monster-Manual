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
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A disintegration spell (uses the {@code gmm:disintegrate_spell} damage type). It burns through
 * blocks until its {@code power} budget is exhausted; both the on-hit damage and the initial power
 * budget are read from the {@code gmm:mob_config} codec. The in-flight visual item is supplied by
 * the consumer via {@link #itemSupplier}.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class DisintegrateSpell extends GMMHurtingProjectile implements ItemSupplier {
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(DisintegrateSpell.class, EntityDataSerializers.ITEM_STACK);

	/** Consumer-supplied in-flight visual item (GMM registers no items). May be null until wired. */
	public static Supplier<Item> itemSupplier;

	private final ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator();

	/** block-penetration budget; -1 = not yet resolved from the codec (see {@link #getPower()}). */
	private float power = -1f;

	public DisintegrateSpell(EntityType<DisintegrateSpell> entityType, Level level) {
		super(entityType, level);
	}

	private static Item defaultItem() {
		return itemSupplier != null ? itemSupplier.get() : Items.FIRE_CHARGE;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		if (WorldInfo.isClientSide(level())) {
			return;
		}
		BlockState state = level().getBlockState(blockHitResult.getBlockPos());
		FluidState fluidState = level().getFluidState(blockHitResult.getBlockPos());
		Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(null, level(), blockHitResult.getBlockPos(), state, fluidState);

		if(resistance.isPresent()) {
			if (resistance.get() < getPower()) {
				// destroy the block
				level().setBlock(blockHitResult.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
				// generate particles
				for (int p = 0; p < 20; p++) {
					double xSpeed = random.nextGaussian() * 0.02D;
					double ySpeed = random.nextGaussian() * 0.02D;
					double zSpeed = random.nextGaussian() * 0.02D;
					((ServerLevel) level()).sendParticles(ParticleTypes.EXPLOSION,
							blockHitResult.getBlockPos().getX() + 0.5D,
							blockHitResult.getBlockPos().getY(),
							blockHitResult.getBlockPos().getZ() + 0.5D,
							1, xSpeed, ySpeed, zSpeed, (double) 0.15F);
				}

				// play sizzle sound
				this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
			}
			// reduce power by resistance
			setPower(Math.max(0, getPower() - resistance.get()));
		}
		if (getPower() <= 0) {
			this.discard();
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		this.playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!this.level().isClientSide) {
			Entity target = hitResult.getEntity();
			Entity ownerEntity = this.getOwner();
			target.hurt(GMMDamageTypes.source(level(), GMMDamageTypes.DISINTEGRATE_SPELL, this, ownerEntity), (float) MobConfigHelper.get(this).number("damage", 8.0));
			if (target instanceof LivingEntity) {
				this.doEnchantDamageEffects((LivingEntity)ownerEntity, target);
				discard();
			}
		}
	}

	public void setItem(ItemStack stack) {
		if (!stack.is(defaultItem()) || stack.hasTag()) {
			this.getEntityData().set(DATA_ITEM_STACK, Util.make(stack.copy(), (itemStack) -> {
				itemStack.setCount(1);
			}));
		}
	}

	protected ItemStack getItemRaw() {
		return this.getEntityData().get(DATA_ITEM_STACK);
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.PORTAL;
	}

	@Override
	public ItemStack getItem() {
		ItemStack stack = this.getItemRaw();
		return stack.isEmpty() ? new ItemStack(defaultItem()) : stack;
	}

	@Override
	protected void defineSynchedData() {
		this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ItemStack itemstack = this.getItemRaw();
		if (!itemstack.isEmpty()) {
			tag.put("Item", itemstack.save(new CompoundTag()));
		}

	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		ItemStack itemStack = ItemStack.of(tag.getCompound("Item"));
		this.setItem(itemStack);
	}

	public float getPower() {
		if (power < 0) {
			power = (float) MobConfigHelper.get(this).number("damage", 8.0);
		}
		return power;
	}

	public void setPower(float power) {
		this.power = power;
	}
}
