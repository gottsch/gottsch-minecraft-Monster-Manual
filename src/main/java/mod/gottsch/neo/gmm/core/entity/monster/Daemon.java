package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;

/**
 * Generalized from Dungeon Denizens. GMM owns no concrete firespout projectile, so
 * {@link DaemonShootSpellsGoal} launches through the consumer-supplied {@link #fireSpoutLauncher}
 * static hook (same shape as {@code Orc#projectileLauncher}).
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class Daemon extends GMMMonster {
	public static final double MELEE_DISTANCE_SQUARED = 25D;

	/** Creates and launches a firespout arc from (x,y,z) to (x2,y2,z2); GMM owns no concrete spell. */
	@FunctionalInterface
	public interface FireSpoutLauncher {
		void launch(Mob daemon, double x, double y, double z, double x2, double y2, double z2);
	}

	public static FireSpoutLauncher fireSpoutLauncher;
	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.DAEMON_AMBIENT.get();

	private double flameParticlesTime;
	private int particlesReset = 4;

	public Daemon(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
		this.xpReward = 10;
	}

	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(4, new DaemonShootSpellsGoal(this, (int) MobConfigHelper.get(this).number("firespoutCooldownTime", 200)));
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3D, false));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.2F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new SummonedOwnerTargetGoal(this));
		// NOTE Boulder-targeting is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true, (entity) -> {
			if (entity instanceof Player) {
				return getSummonedOwner() == null || !(getSummonedOwner() instanceof Player);
			}
			return true;
		}));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.ATTACK_DAMAGE, 8.0D)
				.add(Attributes.MAX_HEALTH, 80.0D)
				.add(Attributes.ARMOR, 20.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 5.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
				.add(Attributes.ATTACK_KNOCKBACK, 2.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.2D)
				.add(Attributes.FOLLOW_RANGE, 50D);
	}

	@Override
	public boolean requiresCustomPersistence() {
		return !MobConfigHelper.get(this).flag("despawn", true);
	}

	@Override
	public void aiStep() {
		if (this.level().isClientSide && !isInWater()) {
			for(int i = 0; i < 2; ++i) {
				this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
			}
			this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);

			if (--particlesReset <= 0) {
				double x = 0.75D * Math.sin(flameParticlesTime);
				double z = 0.75D * Math.cos(flameParticlesTime);
				this.level().addParticle(ParticleTypes.FLAME, this.position().x + x, this.position().y + 0.1D, position().z + z, 0, 0, 0);
				particlesReset = 4;
			}
			flameParticlesTime++;
			flameParticlesTime = flameParticlesTime % 360;
		}
		// summon lifespan is handled generically via Ownership.tickLifespan (customServerAiStep) for
		// SUMMONED daemons; a naturally-spawned daemon is OwnershipType.NONE and never expires.
		super.aiStep();
	}

	@Override
	public boolean canSummonedHaveOwner() {
		return true;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ambientSound != null ? ambientSound.get() : null;
	}

	/**
	 *
	 * @author Mark Gottschling on 7/1/2026
	 *
	 */
	static class DaemonShootSpellsGoal extends Goal {
		private static final int DEFAULT_COOLDOWN_TIME = 200;
		private final Daemon daemon;
		private int cooldownTime;
		private int maxCooldownTime;
		private boolean shootFireSpouts;
		private int maxFireSpouts;
		private int fireSpoutCount;
		private Vec3 viewVector;
		private Vec3 daemonPosition;
		private Vec3 targetPosition;
		private Double lastY;

		public DaemonShootSpellsGoal(Daemon mob) {
			this(mob, DEFAULT_COOLDOWN_TIME);
		}

		public DaemonShootSpellsGoal(Daemon mob, int maxCooldownTime) {
			this.daemon = mob;
			this.maxCooldownTime = maxCooldownTime;
			this.maxFireSpouts = (int) MobConfigHelper.get(mob).number("firespoutMaxDistance", 10);
		}

		@Override
		public boolean canUse() {
			return this.daemon.getTarget() != null;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			this.cooldownTime = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			--this.cooldownTime;
			if (cooldownTime < 0) {
				cooldownTime = 0;
			}

			LivingEntity target = daemon.getTarget();
			if (target != null) {
				double distanceToTarget = getDistanceToTarget();
				if (distanceToTarget > MELEE_DISTANCE_SQUARED && daemon.hasLineOfSight(target)) {

					if (this.cooldownTime == 0) {
						// +6.0 gives you a 2 block buffer from the spell radius
						if (distanceToTarget < (9.0 + (maxFireSpouts * maxFireSpouts))) {
							shootFireSpouts = true;

							// save viewVector, daemon position, player position
							this.viewVector = daemon.getViewVector(1.0F);
							this.daemonPosition = new Vec3(daemon.getX(), daemon.getY(0.5), daemon.getZ());
							this.targetPosition = new Vec3(target.getX(), target.getY(0.5), target.getZ());
							this.cooldownTime = maxCooldownTime;
							lastY = null;
						}
					}
				}
			}

			if (shootFireSpouts && daemon.level().getGameTime() % 3 == 0) {
				if (fireSpoutCount < maxFireSpouts) {
					// view vector
					Vec3 vec3 = this.viewVector;
					double offsetMultiplier = 1;
					// calculate the offset multiplier to position (2 + x) blocks away from daemon towards target
					if (Math.abs(vec3.x) > Math.abs(vec3.z)) {
						offsetMultiplier = Math.abs((2 + fireSpoutCount) /  vec3.x);
					}
					else {
						offsetMultiplier = Math.abs((2 + fireSpoutCount) / vec3.z);
					}

					// move vector to originating position
					double x = daemonPosition.x + vec3.x * offsetMultiplier;
					double y = Math.floor(daemonPosition.y);
					double z = daemonPosition.z + vec3.z * offsetMultiplier;

					if (lastY == null) {
						lastY = Double.valueOf(y);
					}

					// calculate Y (on ground)
					BlockPos pos = new BlockPos((int)Math.floor(x), (int)y, (int)Math.floor(z));
					BlockState state = daemon.level().getBlockState(pos);
					int count = 0;
					while (true) {
						count++;
						if (!state.getFluidState().isEmpty() || !daemon.level().getBlockState(pos.below()).getFluidState().isEmpty()) {
							resetShootFireSpout();
							return;
						}
						else if (state.getBlock() == Blocks.AIR) {
							if (daemon.level().getBlockState(pos.below()).getBlock() != Blocks.AIR) {
								break;
							}
							pos = pos.below();
						}
						else {
							pos = pos.above();
							if (daemon.level().getBlockState(pos).getBlock() == Blocks.AIR) {
								break;
							}
						}
						if (count > maxFireSpouts) {
							resetShootFireSpout();
							return;
						}
						state = daemon.level().getBlockState(pos);
					}
					if (pos.getY() - lastY > 2 || lastY - pos.getY() > 2) {
						resetShootFireSpout();
						return;
					}
					y = pos.getY();
					lastY = Double.valueOf(y);

					// caculate vector of spout ie upwards target position = y + 5.0D
					double x2 = x;
					double y2 = y + 5.0D;
					double z2 = z;

					// create the firespout via the consumer-supplied launcher
					if (fireSpoutLauncher != null) {
						fireSpoutLauncher.launch(daemon, x, y, z, x2, y2, z2);
					}
					// add some particle effects
					((ServerLevel) daemon.level()).sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 5, 0, 0, 0, (double)0.15F);

					// increment the count
					fireSpoutCount++;
					// turn off is max spouts are reached
					if (fireSpoutCount >= maxFireSpouts) {
						resetShootFireSpout();
					}
				}
				else {
					resetShootFireSpout();
				}
			}
		}

		public void resetShootFireSpout() {
			shootFireSpouts = false;
			fireSpoutCount = 0;
			lastY = null;
		}

		protected double getAttackReachSqr(LivingEntity entity) {
			return (double)(daemon.getBbWidth() * 2.2F * daemon.getBbWidth() * 2.0F + entity.getBbWidth());
		}

		public double getDistanceToTarget() {
			if (daemon.getTarget() != null) {
				return daemon.distanceToSqr(daemon.getTarget().getX(), daemon.getTarget().getY(), daemon.getTarget().getZ());
			}
			return 0;
		}
	}
}
