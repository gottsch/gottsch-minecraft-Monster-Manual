package mod.gottsch.neo.gmm.core.entity.monster.beholderkin;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.EnthrallGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.WeightedChanceSummonGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.MobHurtByTargetGoal;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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
 * The Beholder-kin flagship: casts a weighted mix of spells and can summon minions (incl. a rare
 * Daemon). GMM owns no concrete spell/summon/sound, so the consumer supplies them via the static
 * hooks below (same shape as {@code Orc#projectileLauncher} / {@code Shadow#ambientSound}).
 *
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class Beholder extends Beholderkin {

	/** Consumer-supplied weighted spell list; GMM owns no concrete spell. */
	public static WeightedCollection<Integer, CastSpellGoal.SpellLauncher> spellCaster;

	/** Consumer-supplied weighted list of minions Beholder can conjure. */
	public static WeightedCollection<Double, EntityType<? extends Mob>> summonMobs;

	/** Consumer-supplied rare Daemon-summon target. */
	public static EntityType<? extends Mob> summonDaemon;

	public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.BEHOLDER_AMBIENT.get();

	/** Spawns escort minions alongside a natural spawn (see {@code GMMFlyingMonster#getCompanionPool}). */
	@Override
	protected TagKey<EntityType<?>> getCompanionPool() {
		return GMMTags.EntityTypes.BEHOLDER_COMPANIONS;
	}

	public Beholder(EntityType<? extends FlyingMob> entityType, Level level) {
		super(entityType, level);
		this.moveControl = new BeholderkinMoveControl(this);
		this.xpReward = 20;
	}

	@Override
	protected void registerGoals() {
		MobConfig config = MobConfigHelper.get(this);
		this.goalSelector.addGoal(4, new BeholderkinBiteGoal(this, (int) config.number("biteCooldownTime", 40)));
		this.goalSelector.addGoal(5, new BeholderkinRandomFloatAroundGoal(this, (int) config.number("maxFloatHeight", 7)));
		this.goalSelector.addGoal(8, new BeholderkinLookGoal(this));

		if (spellCaster != null) {
			this.goalSelector.addGoal(6, new CastSpellGoal(this, (int) config.number("spellChargeTime", 80), spellCaster));
		}

		this.goalSelector.addGoal(6, new EnthrallGoal(this, GMMTags.EntityTypes.BEHOLDER_ENTHRALL_CANDIDATES,
				(int) config.number("enthrallChargeTime", 100),
				(int) config.number("enthrallCooldownTime", 600),
				(int) config.number("maxThralls", 6),
				GMMParticles.OCULUS_ORB.get()));

		if (summonMobs != null) {
			this.goalSelector.addGoal(7, new WeightedChanceSummonGoal(this, (int) config.number("summonCooldownTime", 1200), 100, summonMobs, (int) config.number("minSummonSpawns", 1), (int) config.number("maxSummonSpawns", 1)));
		}
		if (summonDaemon != null) {
			this.goalSelector.addGoal(7, new WeightedChanceSummonGoal(this, (int) config.number("summonDaemonCooldownTime", 2400), 10, summonDaemon, 1, 1));
		}

		// NOTE unaffected by Boulders

		// retaliate against whatever hurts it (e.g. a mob caught in its own stray spellfire) instead of
		// passively eating hits while it keeps casting at its original target.
		this.targetSelector.addGoal(1, new MobHurtByTargetGoal(this));
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
