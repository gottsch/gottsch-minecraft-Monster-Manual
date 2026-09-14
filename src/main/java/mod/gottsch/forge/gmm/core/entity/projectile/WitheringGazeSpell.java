package mod.gottsch.forge.gmm.core.entity.projectile;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.damagesource.GMMDamageTypes;
import net.minecraft.Util;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
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
 * Bodak's Withering Gaze: a direct-damage necrotic bolt (uses the {@code gmm:withering_gaze_spell}
 * damage type) -- the ranged companion to Death Gaze's melee-range, look-detection-gated stare. Unlike
 * Death Gaze, this has no look-direction requirement at all; it's a plain {@link CastSpellGoal}-driven
 * cast (line-of-sight + range, gated by {@code spellMinRange} so it won't fire point-blank), closer to
 * {@code HarmSpell} than anything look-based -- so a Bodak still punishes a player who kites at range
 * instead of standing and staring it down. Damage is read from the {@code gmm:mob_config} codec; the
 * in-flight visual item is supplied by the consumer via {@link #itemSupplier} (GMM registers no items).
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class WitheringGazeSpell extends GMMHurtingProjectile implements ItemSupplier {
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(WitheringGazeSpell.class, EntityDataSerializers.ITEM_STACK);

	/** Consumer-supplied in-flight visual item (GMM registers no items). May be null until wired. */
	public static Supplier<Item> itemSupplier;

	public WitheringGazeSpell(EntityType<WitheringGazeSpell> entityType, Level level) {
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
			target.hurt(GMMDamageTypes.source(level(), GMMDamageTypes.WITHERING_GAZE_SPELL, this, ownerEntity),
					(int) MobConfigHelper.get(this).number("damage", 8.0));
			if (target instanceof LivingEntity) {
				this.doEnchantDamageEffects((LivingEntity) ownerEntity, target);
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
		return ParticleTypes.SOUL;
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
