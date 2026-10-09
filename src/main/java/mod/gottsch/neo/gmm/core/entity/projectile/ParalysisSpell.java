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
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

/**
 * A brief, full-movement-root paralysis spell -- applies {@link GMMMobEffects#PARALYZED}, not
 * vanilla Slowness, so a struck target genuinely can't walk for the duration rather than just
 * moving slower (fitting for the "generally powerful beings" -- Beholder/Gazer/DeathTyrant/
 * Spectator -- that cast it). Damage + root duration are read from the {@code gmm:mob_config}
 * codec (keyed by this projectile's registered type id, {@code slow}); the fallback default
 * duration below is deliberately short (30 ticks / 1.5s, down from the old 200-tick Slowness
 * default) since a full root is much stronger than a partial speed reduction. The in-flight
 * visual item is supplied by the consumer via
 * {@link #itemSupplier}.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class ParalysisSpell extends GMMHurtingProjectile implements ItemSupplier {
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(ParalysisSpell.class, EntityDataSerializers.ITEM_STACK);

	/** Consumer-supplied in-flight visual item (GMM registers no items). May be null until wired. */
	public static Supplier<Item> itemSupplier;

	public ParalysisSpell(EntityType<ParalysisSpell> entityType, Level level) {
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
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		this.playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);

		if (!this.level().isClientSide) {
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!this.level().isClientSide) {
			Entity target = hitResult.getEntity();
			Entity ownerEntity = this.getOwner();
			target.hurt(level().damageSources().indirectMagic(this, ownerEntity), (int) MobConfigHelper.get(this).number("damage", 2.0));

			if (target instanceof LivingEntity) {
				int duration = (int) MobConfigHelper.get(this).number("duration", 30.0);
				((LivingEntity)target).addEffect(new MobEffectInstance(GMMMobEffects.PARALYZED.get(), duration, 0, false, true, true), this);
				this.doEnchantDamageEffects((LivingEntity)ownerEntity, target);
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
		return ParticleTypes.CRIMSON_SPORE;
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
}
