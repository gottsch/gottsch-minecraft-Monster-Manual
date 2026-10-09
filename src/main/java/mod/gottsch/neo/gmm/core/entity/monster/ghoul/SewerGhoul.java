package mod.gottsch.neo.gmm.core.entity.monster.ghoul;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * @author by Mark Gottschling on 11/6/2025
 */
public class SewerGhoul extends AbstractGhoul {
	public static final float WIDTH = 0.6F;
	public static final float HEIGHT = 1.68F;

	/**
	 *
	 * @param entityType
	 * @param level
	 */
	public SewerGhoul(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
	}

	protected void registerGoals() {
		super.registerGoals();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.ATTACK_DAMAGE, 3.0D)
				.add(Attributes.MAX_HEALTH, 22D) // more health than a zombie
				.add(Attributes.FOLLOW_RANGE, 15.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27F);  // faster than zombie, not as fast as regular Ghoul
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HUSK_DEATH;
	}

	protected SoundEvent getStepSound() {
		return SoundEvents.HUSK_STEP;
	}
}
