package mod.gottsch.neo.gmm.core.entity.monster.beholderkin;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.WeightedChanceSummonGoal;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class Gazer extends Beholderkin {

	/** Consumer-supplied weighted spell list; GMM owns no concrete spell. */
	public static WeightedCollection<Integer, CastSpellGoal.SpellLauncher> spellCaster;

	/** Consumer-supplied weighted list of minions Gazer can conjure. */
	public static WeightedCollection<Double, EntityType<? extends Mob>> summonMobs;

	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.GAZER_AMBIENT.get();

	public Gazer(EntityType<? extends FlyingMob> entityType, Level level) {
		super(entityType, level);
		this.moveControl = new BeholderkinMoveControl(this);
		this.xpReward = 10;
	}

	@Override
	protected void registerGoals() {
		MobConfig config = MobConfigHelper.get(this);
		this.goalSelector.addGoal(4, new BeholderkinBiteGoal(this, (int) config.number("biteCooldownTime", 20)));
		this.goalSelector.addGoal(5, new BeholderkinRandomFloatAroundGoal(this, (int) config.number("maxFloatHeight", 5)));
		this.goalSelector.addGoal(7, new BeholderkinLookGoal(this));

		if (spellCaster != null) {
			this.goalSelector.addGoal(6, new CastSpellGoal(this, (int) config.number("spellChargeTime", 80), spellCaster));
		}

		if (summonMobs != null) {
			this.goalSelector.addGoal(6, new WeightedChanceSummonGoal(this, (int) config.number("summonCooldownTime", 2400), 100, summonMobs, (int) config.number("minSummonSpawns", 1), (int) config.number("maxSummonSpawns", 1)));
		}

		// NOTE Boulder-targeting is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * this method needs to be assigned to the EntityType during EntityAttributeCreationEvent event.
	 * @return
	 */
	public static AttributeSupplier.Builder prepareAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.ATTACK_DAMAGE, 6.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0D)
				.add(Attributes.ARMOR, 3.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 1.0D)
				.add(Attributes.MAX_HEALTH, 18.0)
				.add(Attributes.FOLLOW_RANGE, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18F);
	}

	@Override
	public int getAmbientSoundInterval() {
		return 100;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ambientSound != null ? ambientSound.get() : null;
	}
}
