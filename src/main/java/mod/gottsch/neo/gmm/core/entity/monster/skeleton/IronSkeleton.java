package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A heavily-armored, sun-proof skeleton. Doesn't burn in daylight (only smokes) and hits hard.
 *
 * @author Mark Gottschling on Feb 3, 2024
 */
public class IronSkeleton extends BowSkeleton {

    public IronSkeleton(EntityType<? extends BowSkeleton> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeGoal(getFleeSunGoal());
        this.goalSelector.addGoal(3, new IronSkeletonFleeSunGoal(this, 1.0D));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BowSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 50D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 10D)
                .add(Attributes.ARMOR_TOUGHNESS, 2D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData, CompoundTag tag) {

        double attackBonus = round(random.nextDouble() * 8D, 2);
        this.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("random iron skeleton bonus", attackBonus, AttributeModifier.Operation.ADDITION));

        return groupData;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficultyInstance) {
        // not any armor nor weapons
    }

    @Override
    public void aiStep() {
        if (this.isSunBurnTick()) {
            // BUT doesn't not get damaged
            if (!level().isClientSide) {
                if (level().getGameTime() % 2 == 0) {
                    for (int i = 0; i < 3; ++i) {
                        ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),3,  0D, 0D, 0D, 0);
                    }
                    ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 1, 0D, 0D, 0D, 0);
                    ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 1, 0D, 0D, 0D, 0);
                } else {
                    ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 5, 0D, 0D, 0D, 0);
                }
            }
        }
        super.aiStep();
    }

    /**
     * Ensure that Iron Skeletons don't burn in sunlight.
     */
    @Override
    public boolean getBurnsInSun() {
        return false;
    }

    public static double round(double x, long fraction) {
        return (double) Math.round(x * fraction) / fraction;
    }

    /**
     * This Flee version doesn't require that the mob be on fire.
     */
    public class IronSkeletonFleeSunGoal extends FleeSunGoal {
        private final Level level;
        public IronSkeletonFleeSunGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier);
            this.level = mob.level();
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null) {
                return false;
            } else if (!this.level.isDay()) {
                return false;
            } else if (!this.level.canSeeSky(this.mob.blockPosition())) {
                return false;
            } else {
                return !this.mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? false : this.setWantedPos();
            }
        }
    }
}
