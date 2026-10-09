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
public class DeathTyrant extends Beholderkin {

	/** Consumer-supplied single spell launcher (Paralysis); GMM owns no concrete spell. */
	public static CastSpellGoal.SpellLauncher spellCaster;

	/** Consumer-supplied weighted list of minions DeathTyrant can conjure. */
	public static WeightedCollection<Double, EntityType<? extends Mob>> summonMobs;

	/** Consumer-supplied rare Daemon-summon target. */
	public static EntityType<? extends Mob> summonDaemon;

	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.DEATH_TYRANT_AMBIENT.get();

	public DeathTyrant(EntityType<? extends FlyingMob> entityType, Level level) {
		super(entityType, level);
		this.moveControl = new BeholderkinMoveControl(this);
		this.xpReward = 20;
	}

	@Override
	protected void registerGoals() {
		MobConfig config = MobConfigHelper.get(this);
		this.goalSelector.addGoal(4, new BeholderkinBiteGoal(this, (int) config.number("biteCooldownTime", 40)));
		this.goalSelector.addGoal(5, new BeholderkinRandomFloatAroundGoal(this, (int) config.number("maxFloatHeight", 8)));
		this.goalSelector.addGoal(7, new BeholderkinLookGoal(this));

		if (spellCaster != null) {
			this.goalSelector.addGoal(6, new CastSpellGoal(this, (int) config.number("spellChargeTime", 80), spellCaster));
		}

		if (summonMobs != null) {
			this.goalSelector.addGoal(6, new WeightedChanceSummonGoal(this, (int) config.number("summonCooldownTime", 1200), 100, summonMobs, (int) config.number("minSummonSpawns", 2), (int) config.number("maxSummonSpawns", 5)));
		}
		if (summonDaemon != null) {
			this.goalSelector.addGoal(6, new WeightedChanceSummonGoal(this, (int) config.number("summonDaemonCooldownTime", 2400), 40, summonDaemon, 1, 1));
		}
		// NOTE unaffected by Boulders
		// TODO need custom hurtbyTarget like headless
		// TODO headless hurtby needs to be become a stand alone class that any mob can use
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * this method needs to be assigned to the EntityType during EntityAttributeCreationEvent event.
	 * @return
	 */
	public static AttributeSupplier.Builder prepareAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.ATTACK_DAMAGE, 8.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5D)
				.add(Attributes.ARMOR, 3.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 3.0D)
				.add(Attributes.MAX_HEALTH, 36.0)
				.add(Attributes.FOLLOW_RANGE, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.20F);
	}

	@Override
	public boolean requiresCustomPersistence() {
		return !MobConfigHelper.get(this).flag("despawn", true);
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
}
