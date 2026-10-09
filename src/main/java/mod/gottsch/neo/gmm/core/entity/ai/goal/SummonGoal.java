package mod.gottsch.neo.gmm.core.entity.ai.goal;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import mod.gottsch.neo.gottschcore.world.WorldInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.NaturalSpawner;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 
 * @author Mark Gottschling on Apr 19, 2022
 *
 */
public abstract class SummonGoal extends Goal {
	/** Default lifespan (ticks) of a summoned mob before it is unsummoned; overridable via mob_config. */
	protected static final int DEFAULT_SUMMON_LIFESPAN = 2400;

	protected int cooldownTime;
	protected int cooldownCount;
	
	/**
	 * 
	 * @param summonCoolDownTime
	 */
	public SummonGoal(int summonCoolDownTime) {
		this.cooldownTime = summonCoolDownTime;
	}

	@Override
	public boolean canUse() {
        return true;
    }

	/**
	 * 
	 * @param level
	 * @param random
	 * @param owner
	 * @param entityType
	 * @param coords
	 * @param target
	 * @return
	 */
	protected boolean spawn(ServerLevel level, RandomSource random, LivingEntity owner, EntityType<? extends Mob> entityType, ICoords coords, LivingEntity target) {
		for (int i = 0; i < 20; i++) { // 20 tries
			int spawnX = coords.getX() + Mth.nextInt(random, 1, 2) * Mth.nextInt(random, -1, 1);
			int spawnY = coords.getY() + Mth.nextInt(random, 1, 2) * Mth.nextInt(random, -1, 1);
			int spawnZ = coords.getZ() + Mth.nextInt(random, 1, 2) * Mth.nextInt(random, -1, 1);

			ICoords spawnCoords = new Coords(spawnX, spawnY, spawnZ);

			boolean isSpawned = false;
			if (!WorldInfo.isClientSide(level)) {
				if (SpawnPlacements.isSpawnPositionOk(entityType, level, spawnCoords.toPos())) {
					Mob mob = entityType.create(level);
					mob.setPos((double)spawnX, (double)spawnY, (double)spawnZ);
					mob.setTarget(target);
					if (mob instanceof IGMMMonster gmmMob && gmmMob.canSummonedHaveOwner()) {
						// summoned minion: owned by the summoner and given a lifespan, after which it is
						// unsummoned (Ownership.tickLifespan). Lifespan is a property of the summoned mob.
						int lifespan = (int) MobConfigHelper.get(mob).number("summonLifespan", DEFAULT_SUMMON_LIFESPAN);
						Ownership.stampOwnership(mob, owner, OwnershipType.SUMMONED, lifespan);
					}

					EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(spawnCoords.toPos()), MobSpawnType.MOB_SUMMONED, null);
					level.addFreshEntityWithPassengers(mob);
					if (mob.isAddedToLevel()) {
						isSpawned = true;
					}
				}
				
				if (isSpawned) {
					for (int p = 0; p < 20; p++) {
						double xSpeed = random.nextGaussian() * 0.02D;
						double ySpeed = random.nextGaussian() * 0.02D;
						double zSpeed = random.nextGaussian() * 0.02D;
						level.sendParticles(ParticleTypes.POOF, owner.blockPosition().getX() + 0.5D, owner.blockPosition().getY(), owner.blockPosition().getZ() + 0.5D, 1, xSpeed, ySpeed, zSpeed, (double)0.15F);
					}
					return true;
				}
			}

		}
		return false;
	}

	public int getCooldownTime() {
		return cooldownTime;
	}

	public SummonGoal setCooldownTime(int cooldownTime) {
		this.cooldownTime = cooldownTime;
		return this;
	}

	public int getCooldownCount() {
		return cooldownCount;
	}

	public SummonGoal setCooldownCount(int cooldownCount) {
		this.cooldownCount = cooldownCount;
		return this;
	}
}
