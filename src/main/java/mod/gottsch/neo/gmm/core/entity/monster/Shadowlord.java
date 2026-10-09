package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.WeightedChanceSummonGoal;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import mod.gottsch.neo.gottschcore.random.RandomHelper;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import mod.gottsch.neo.gottschcore.world.WorldInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;

/**
 * Generalized from Dungeon Denizens. Shares Shadow's anti-metal {@code hurt()} mechanic --
 * duplicated here rather than extracted to a shared base (a future "Shadowkin" extraction
 * remains an option, but is out of scope for this migration).
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class Shadowlord extends GMMMonster {
	private static final int SUN_BURN_SECONDS = 2;
	protected static final double MELEE_DISTANCE_SQUARED = 16D;
	protected static final double SUMMON_DISTANCE_SQUARED = 1024D;
	protected static final double SHOOT_DISTANCE_SQUARED = 4096D;
	protected static final int SUMMON_CHARGE_TIME = 2400;

	/** Consumer-supplied single spell launcher (Harm); GMM owns no concrete spell. */
	public static CastSpellGoal.SpellLauncher spellCaster;
	/** Consumer-supplied weighted list of minions Shadowlord can conjure. */
	public static WeightedCollection<Double, EntityType<? extends Mob>> summonMobs;
	/** Consumer-supplied rare Daemon-summon target. */
	public static EntityType<? extends Mob> summonDaemon;
	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.SHADOWLORD_AMBIENT.get();
	public static Supplier<SoundEvent> stepSound = () -> GMMSounds.SHADOWLORD_STEP.get();
	/** Consumer-supplied default weapon; GMM owns no concrete item. */
	public static Supplier<Item> weapon;

	private double auraOfBlindessTime;
	private int drainCooldownTime;

	public Shadowlord(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
		this.xpReward = 8;
	}

	protected void registerGoals() {
		MobConfig config = MobConfigHelper.get(this);
		this.goalSelector.addGoal(2, new RestrictSunGoal(this));
		this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.1D));
		if (spellCaster != null) {
			this.goalSelector.addGoal(4, new CastSpellGoal(this, (int) config.number("harmChargeTime", 50), MELEE_DISTANCE_SQUARED, spellCaster));
		}

		if (summonMobs != null) {
			this.goalSelector.addGoal(7, new WeightedChanceSummonGoal(this, (int) config.number("summonCooldownTime", 1200), 100, summonMobs, (int) config.number("minSummonSpawns", 1), (int) config.number("maxSummonSpawns", 2)));
		}
		if (summonDaemon != null) {
			this.goalSelector.addGoal(7, new WeightedChanceSummonGoal(this,
					(int) config.number("summonDaemonCooldownTime", 2400),
					config.number("summonDaemonProbability", 25.0),
					summonDaemon, 1, 1));
		}

		this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.1D, false));

		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		// NOTE Boulder-targeting is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.ATTACK_DAMAGE, 8.0D)
				.add(Attributes.MAX_HEALTH, 50.0D)
				.add(Attributes.ARMOR, 10.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 1.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.MOVEMENT_SPEED, 0.2F)
				.add(Attributes.FOLLOW_RANGE, 80D);
	}

	@Override
	public boolean requiresCustomPersistence() {
		return !MobConfigHelper.get(this).flag("despawn", true);
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEAD;
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			MobSpawnType spawnType, SpawnGroupData groupData, CompoundTag tag) {

		groupData = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);

		RandomSource randomSource = level.getRandom();
		this.populateDefaultEquipmentSlots(randomSource, difficulty);

		return groupData;
	}

	protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficultyInstance) {
		this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 0.75F;
		if (weapon != null) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon.get()));
		}
	}

	/**
	 * Only executed on server side.
	 */
	public void drain(LivingEntity entity, float amount) {
		if (drainCooldownTime > 0) {
			return;
		}
		drainCooldownTime = (int) MobConfigHelper.get(this).number("drainCooldownTime", 400);

		// add damage to Shadowlord's health
		setHealth(Math.min(getMaxHealth(), getHealth() + amount));
		if (!WorldInfo.isClientSide(this.level())) {
			for (int p = 0; p < 40; p++) {
				double xSpeed = random.nextGaussian() * 0.02D;
				double ySpeed = random.nextGaussian() * 0.02D;
				double zSpeed = random.nextGaussian() * 0.02D;
				((ServerLevel)level()).sendParticles(ParticleTypes.SOUL, blockPosition().getX() + 0.5D, blockPosition().getY(), blockPosition().getZ() + 0.5D, 1, xSpeed, ySpeed, zSpeed, (double)0.15F);
			}
		}
	}

	@Override
	public void aiStep() {
		/*
		 * Create a ring of smoke particles to delineate the boundary of the Aura of Blindness
		 */
		if (WorldInfo.isClientSide(this.level())) {
			// general particles around body
			for(int i = 0; i < 2; ++i) {
				this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
			}

			double x = Math.sin(auraOfBlindessTime);
			double z = Math.cos(auraOfBlindessTime);
			this.level().addParticle(ParticleTypes.SMOKE, this.position().x + x, position().y, position().z + z, 0, 0, 0);
			this.level().addParticle(ParticleTypes.SMOKE, this.position().x + (x*2D), position().y, position().z + (z*2D), 0, 0, 0);
			auraOfBlindessTime++;
			auraOfBlindessTime = auraOfBlindessTime % 360;
		}

		/*
		 * Apply Aura of Blindness to Players
		 */
		double distance = 2;
		AABB aabb = AABB.unitCubeFromLowerCorner(this.position()).inflate(distance, distance, distance);
		List<? extends Player> list = this.level().getEntitiesOfClass(Player.class, aabb, EntitySelector.NO_SPECTATORS);
		Iterator<? extends Player> iterator = list.iterator();
		while (iterator.hasNext()) {
			Player target = (Player)iterator.next();
			// test if player is wearing golden helmet
			ItemStack helmetStack = target.getItemBySlot(EquipmentSlot.HEAD);
			if (helmetStack.isEmpty() || helmetStack.getItem() != Items.GOLDEN_HELMET) {
				// inflict blindness for 1 second
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) MobConfigHelper.get(this).number("blindnessDuration", 40), 0), this);
			}
		}

		// decrement the drainCooldownTime
		drainCooldownTime = Math.max(--drainCooldownTime, 0);

		// set on fire if in sun
		if (this.isSunBurnTick()) {
			this.setSecondsOnFire(SUN_BURN_SECONDS);
		}
		super.aiStep();
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if (super.doHurtTarget(target)) {
			if (target instanceof Player) {
				// inflict poison
				if (RandomHelper.checkProbability(this.random, MobConfigHelper.get(this).number("poisonProbability", 25.0))) {
					((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.POISON, (int) MobConfigHelper.get(this).number("poisonDuration", 200), 0), this);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * The Shadowlord's signature defense, shared with Shadow: it resists metal weapons (higher
	 * tier -> less damage), takes full damage + a bonus from gold and the consumer-defined bane
	 * weapons (which also negate the attacker's WEAKNESS penalty), and a guaranteed minimum from
	 * non-weapon strikes.
	 */
	@Override
	public boolean hurt(DamageSource damageSource, float amount) {
		if (WorldInfo.isClientSide(this.level())) {
			return false;
		}

		if (damageSource.getEntity() != null && damageSource.getEntity() instanceof Player player) {
			ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);

			if (!heldStack.isEmpty()) {
				if (heldStack.is(Items.GOLDEN_SWORD) || heldStack.is(GMMTags.Items.SHADOW_BANE)) {
					// increase damage to that of iron tier + negate the weakness from the strike power
					amount += 2.0F + weaknessNegation(player);
				}
				else {
					if (heldStack.getItem() instanceof TieredItem tieredItem) {
						int penalty = 0;
						Tier tier = tieredItem.getTier();
						if (tier == Tiers.NETHERITE) {
							penalty = -3;
						} else if (tier == Tiers.DIAMOND) {
							penalty = -2;
						} else if (tier == Tiers.IRON) {
							penalty = -1;
						} else if (tier == Tiers.STONE || tier == Tiers.WOOD) {
							// don't incur a penalty
						}
						amount += penalty;
					} else {
						// all other items
						amount = Math.max(amount, 4.0f); // same as stone sword
					}
				}
				GMM.LOGGER.debug("new strike amount -> {}", amount);
			}
		}
		return super.hurt(damageSource, amount);
	}

	/** Bonus that cancels out the attacker's WEAKNESS penalty so bane weapons always land full force. */
	private static float weaknessNegation(Player player) {
		if (player.hasEffect(MobEffects.WEAKNESS)) {
			return 4.0F * (player.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1);
		}
		return 0.0F;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ambientSound != null ? ambientSound.get() : null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(this.getStepSound(), 0.15F, 1.0F);
	}

	@Nullable
	protected SoundEvent getStepSound() {
		return stepSound != null ? stepSound.get() : null;
	}
}
