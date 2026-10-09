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

import mod.gottsch.neo.gmm.core.util.EquipmentUtil;
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Supplier;

/**
 * A disarm spell: on hitting a player it knocks a random equipped armor piece or held item to the
 * ground. Deals no damage and reads no codec tuning. The in-flight visual item is supplied by the
 * consumer via {@link #itemSupplier}.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class DisarmSpell extends GMMHurtingProjectile implements ItemSupplier {
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(DisarmSpell.class, EntityDataSerializers.ITEM_STACK);

	/** Consumer-supplied in-flight visual item (GMM registers no items). May be null until wired. */
	public static Supplier<Item> itemSupplier;

	public DisarmSpell(EntityType<DisarmSpell> entityType, Level level) {
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
	protected void onHitBlock(BlockHitResult p_37258_) {
		super.onHitBlock(p_37258_);
		if (!this.level().isClientSide) {
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
			if (target instanceof ServerPlayer) {
				this.playSound(SoundEvents.ALLAY_ITEM_TAKEN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);

				ServerPlayer player = (ServerPlayer) target;
				ItemStack itemStack = ItemStack.EMPTY;
				List<EquipmentSlot> equippedSlots;
				EquipmentSlot slot = null;
				if (level().getGameTime() % 4 == 0) {
					equippedSlots = EquipmentUtil.ARMOR_LIST.stream().filter(s -> player.getItemBySlot(s) != ItemStack.EMPTY).toList();
				} else {
					// disarm a held item
					equippedSlots = EquipmentUtil.HANDHELD_LIST.stream().filter(s -> player.getItemBySlot(s) != ItemStack.EMPTY).toList();
				}
				if (equippedSlots != null && !equippedSlots.isEmpty()) {
					slot = equippedSlots.get(player.getRandom().nextInt(equippedSlots.size()));
					itemStack = player.getItemBySlot(slot);
					if (itemStack != ItemStack.EMPTY) {
						player.setItemSlot(slot, ItemStack.EMPTY);
						Containers.dropItemStack(level(),
								(double) player.position().x + 0.5, (double) player.position().y + 0.5, player.position().z + 0.5,
								itemStack);
					}
				}
			}
			this.discard();
		}
	}

	public void setItem(ItemStack stack) {
		if (!stack.is(defaultItem()) || !stack.isComponentsPatchEmpty()) {
			this.getEntityData().set(DATA_ITEM_STACK, Util.make(stack.copy(), (itemStack) -> {
				itemStack.setCount(1);
			}));
		}
	}

	protected ItemStack getItemRaw() {
		return this.getEntityData().get(DATA_ITEM_STACK);
	}

	// TODO something else than smoke
	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.SPLASH;
	}

	@Override
	public ItemStack getItem() {
		ItemStack stack = this.getItemRaw();
		return stack.isEmpty() ? new ItemStack(defaultItem()) : stack;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEM_STACK, ItemStack.EMPTY);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ItemStack itemstack = this.getItemRaw();
		if (!itemstack.isEmpty()) {
			tag.put("Item", itemstack.save(this.registryAccess()));
		}

	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		ItemStack itemStack = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Item"));
		this.setItem(itemStack);
	}
}
