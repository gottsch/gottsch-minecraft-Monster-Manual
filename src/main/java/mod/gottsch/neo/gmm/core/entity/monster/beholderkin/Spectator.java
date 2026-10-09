package mod.gottsch.neo.gmm.core.entity.monster.beholderkin;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Supplier;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;

/**
 * Generalized from Dungeon Denizens. Owner tracking comes from {@link GMMFlyingMonster}'s
 * built-in synced-UUID system (previously a bespoke, unpersisted field on this class) --
 * {@link #canSummonedHaveOwner()} opts Spectator into it when chance-summoned.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class Spectator extends Beholderkin {

	/** Consumer-supplied single spell launcher (Paralysis); GMM owns no concrete spell. */
	public static CastSpellGoal.SpellLauncher spellCaster;

	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.SPECTATOR_AMBIENT.get();

	public Spectator(EntityType<? extends FlyingMob> entityType, Level level) {
		super(entityType, level);
		this.moveControl = new BeholderkinMoveControl(this);
		this.xpReward = 8;
	}

	@Override
	protected void registerGoals() {
		MobConfig config = MobConfigHelper.get(this);
		this.goalSelector.addGoal(0, new BeholderkinBiteGoal(this, (int) config.number("biteCooldownTime", 20)));
		this.goalSelector.addGoal(1, new Spectator.SpectatorChargeAttackGoal(this, 2D));
		this.goalSelector.addGoal(5, new BeholderkinRandomFloatAroundGoal(this, (int) config.number("maxFloatHeight", 3)));
		this.goalSelector.addGoal(7, new BeholderkinLookGoal(this));
		if (spellCaster != null) {
			this.goalSelector.addGoal(6, new CastSpellGoal(this, (int) config.number("spellChargeTime", 80), spellCaster));
		}
		// TODO in future if player can summon, then it should follow the player
		//this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));

		this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
		// NOTE Boulder-targeting is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
	}

	/**
	 * this method needs to be assigned to the EntityType during EntityAttributeCreationEvent event.
	 * @return
	 */
	public static AttributeSupplier.Builder prepareAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.ATTACK_DAMAGE, 4.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0D)
				.add(Attributes.ARMOR, 3.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 0D)
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.FOLLOW_RANGE, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2F);
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ambientSound != null ? ambientSound.get() : null;
	}

	@Override
	public boolean canSummonedHaveOwner() {
		return true;
	}

	/*
	 *
	 */
	class SpectatorChargeAttackGoal extends Goal {
		private final Mob mob;
		private boolean charging;
		private long cooldownTime = 200;
		private long cooldownCount = 0;
		private double speedModifier;

		public SpectatorChargeAttackGoal(Mob mob, double speedModifier) {
			this.mob = mob;
			this.speedModifier = speedModifier;
			charging = false;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		public boolean canUse() {
			long gameTime = this.mob.level().getGameTime();
			if (gameTime - cooldownCount < cooldownTime) {
				return false;
			}

			LivingEntity livingEntity = this.mob.getTarget();
			if (livingEntity != null && livingEntity.isAlive()
					&& !Spectator.this.getMoveControl().hasWanted()) {
				cooldownCount = gameTime;
				return Spectator.this.distanceToSqr(livingEntity) > 4.0D;
			} else {
				return false;
			}
		}

		public boolean canContinueToUse() {
			if (Spectator.this.getMoveControl().hasWanted() && this.isCharging() && Spectator.this.getTarget() != null && Spectator.this.getTarget().isAlive()) {
				return true;
			}
			return false;
		}

		// initialize goal
		public void start() {
			LivingEntity livingentity = this.mob.getTarget();
			if (livingentity != null) {
				Vec3 vec3 = livingentity.getEyePosition();
				((Spectator)this.mob).moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, this.speedModifier);
			}

			this.setIsCharging(true);
			this.mob.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
		}

		// clean up when goal is complete
		public void stop() {
			this.setIsCharging(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		// execute while goal is running
		public void tick() {
			LivingEntity livingentity = Spectator.this.getTarget();
			if (livingentity != null) {
				if (Spectator.this.getBoundingBox().inflate(0.5f).intersects(livingentity.getBoundingBox())) {
					Spectator.this.doHurtTarget(livingentity);
					setIsCharging(false);
				} else {
					double d0 = Spectator.this.distanceToSqr(livingentity);
					if (d0 < 9.0D) {
						Vec3 vec3 = livingentity.getEyePosition();
						Spectator.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, this.speedModifier);
					}
				}

			}
			// adding this because sometimes the mob gets stuck never reaching wanted pos
			Vec3 vec3 = new Vec3(
					Spectator.this.getMoveControl().getWantedX(),
					Spectator.this.getMoveControl().getWantedY(),
					Spectator.this.getMoveControl().getWantedZ());
			double distanceToGoalSqr = Spectator.this.distanceToSqr(vec3);
			if (distanceToGoalSqr < 0.5D) {
				setIsCharging(false);
			}
		}

		public boolean isCharging() {
			return this.charging;
		}

		public void setIsCharging(boolean charging) {
			this.charging = charging;
		}

	}
}
