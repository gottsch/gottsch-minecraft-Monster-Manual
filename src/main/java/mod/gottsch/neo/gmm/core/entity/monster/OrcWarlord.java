package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.AvoidCrowdGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.WeightedChanceSummonGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.AllyAlertHurtByTargetGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.AllyAlertNearestAttackableTargetGoal;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.neoforged.neoforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * The war-chief of an orc band: an {@link Orc} that leads rather than swings first.
 *
 * <p>Three things, and they are one fight rather than three abilities. He <strong>calls</strong>
 * reinforcements ({@link #summonMobs}); he <strong>rallies</strong> what arrives &mdash; a periodic
 * Strength + Speed refresh over a data-driven ally set; and he <strong>alerts</strong> that same set
 * the moment he acquires a target, so spotting the chief is being spotted by the whole warband. He
 * also keeps to the <em>edge</em> of his own troops so a player's sweep or AoE catches the grunts
 * instead of him (see {@link AvoidCrowdGoal}).</p>
 *
 * <h2>Why this extends Orc rather than GMMMonster</h2>
 * <p>Not convenience: {@code OrcModel.setupAnim} casts its entity to {@link Orc} to read the
 * shoulder-pad / hair / bracer bits, so a warlord that were not an Orc would crash the renderer on
 * first sight of one. Extending also inherits the cosmetic roll, the data-driven weapon and the
 * ranged-throw option for free, which is the whole reason the warlord looks like <em>its own
 * warband's</em> leader rather than a different monster wearing red.</p>
 *
 * <h2>Where it diverges from SkeletonChampion</h2>
 * <p>The rally and the crowd-spacing are the Champion's, deliberately &mdash; same cadence, same
 * {@code gmm:mob_config} keys. Two pack leaders behaving by different rules would be two things to
 * learn for no gain, and the Champion's numbers have been played.</p>
 *
 * <p>The <strong>summon and the alert are what make him his own mob</strong> rather than a reskin.
 * The Champion's identity is that he improves what is already in the room; the Warlord's is that he
 * makes the room busier. Without them the two are mechanically the same monster.</p>
 *
 * <h2>What a consumer must do, and both halves fail SILENTLY</h2>
 * <ol>
 *   <li>{@link #summonMobs} &mdash; a {@code public static} the library leaves null, exactly like
 *       {@link Orc#projectileLauncher}. Unset, the horn goal is never added and the chief simply
 *       never calls anyone. No warning, nothing in the log.</li>
 *   <li>{@link GMMTags.EntityTypes#ORC_WARLORD_RALLY_ALLIES} &mdash; gmm ships this EMPTY, because
 *       gmm registers no entities and its datagen can only name vanilla ids. Unpopulated, both the
 *       rally and the alert reach nothing.</li>
 * </ol>
 * <p>A warlord with neither is a slightly tough orc that looks like a boss. Both are one line each
 * in a consumer's setup, and neither announces its absence &mdash; which is the whole reason they
 * are listed here rather than left to be discovered.</p>
 *
 * @author Mark Gottschling on Sep 5, 2026
 */
public class OrcWarlord extends Orc {

    // Rally: matched to SkeletonChampion's, including the 2026-07-13 correction that took the
    // interval from 60 ticks to 200 -- at 3s the war-cry burst reads as spam rather than a rally.
    // Duration outlasts the interval so a held warband stays buffed without flicker.
    private static final double DEFAULT_RALLY_RANGE = 10.0D;
    private static final int DEFAULT_RALLY_INTERVAL = 200;
    private static final int DEFAULT_RALLY_DURATION = 240;

    // Spacing: how close a grunt has to be to crowd the chief, how many it takes before he
    // repositions, and how far from his target he will stray doing it (he stays in the fight).
    private static final double DEFAULT_CROWD_RADIUS = 4.0D;
    private static final int DEFAULT_MIN_CROWD = 2;
    private static final double DEFAULT_SPACING_MAX_TARGET_DISTANCE = 12.0D;

    // Summon: the horn. FOUR TIMES the rally's cadence, not twice -- the rally refreshes a buff and
    // can afford to be frequent, while this ADDS BODIES, and bodies do not expire. At 400 ticks a
    // chief in an enclosed boss room out-produces what a player in iron can clear, and the fight
    // stops being a fight and becomes attrition. 800 leaves room to kill what arrived before the
    // next horn. Mark's call, 2026-09-05.
    private static final int DEFAULT_SUMMON_COOLDOWN_TIME = 800;
    private static final double DEFAULT_SUMMON_CHANCE = 100.0D;
    private static final int DEFAULT_MIN_SUMMON_SPAWNS = 1;
    private static final int DEFAULT_MAX_SUMMON_SPAWNS = 2;

    /**
     * Consumer-supplied reinforcements. The shared library owns no mob, so a consuming mod sets this
     * (in its common setup) to its own orc types &mdash; the same contract
     * {@code Beholder.summonMobs} uses, and the same {@link Orc#projectileLauncher} trap: left null
     * the warlord simply never calls anyone, with no warning and nothing in the log.
     *
     * <p><strong>This is what makes the rally worth having.</strong> A warlord dropped into a room
     * of skeletons has nothing his {@code rally_allies} tag matches, so his signature ability is
     * inert through no fault of the authoring. Summoning his own warband means he creates his own
     * rally targets: the horn brings orcs, the rally buffs them, and the two abilities compose into
     * one readable fight instead of one working and one doing nothing.</p>
     */
    public static WeightedCollection<Double, EntityType<? extends Mob>> summonMobs;

    public OrcWarlord(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        MobConfig config = MobConfigHelper.get(this);

        // SWAP the two target goals Orc installed for the ally-alerting forms, rather than adding
        // ours alongside at a higher priority: both share Goal.Flag.TARGET, so a leftover pair
        // would never run, and dead goals in a selector read as intent to whoever comes next.
        // super.registerGoals() is still called so the movement goals stay in step with Orc's.
        this.targetSelector.removeAllGoals(
                g -> g instanceof HurtByTargetGoal || g instanceof NearestAttackableTargetGoal);
        // The chief spotting you is the whole warband spotting you -- the one thing a leader should
        // do that nothing else in the roster does. Same tag as the rally: what he leads, he alerts.
        this.targetSelector.addGoal(1, new AllyAlertHurtByTargetGoal(this,
                GMMTags.EntityTypes.ORC_WARLORD_RALLY_ALLIES));
        this.targetSelector.addGoal(2, new AllyAlertNearestAttackableTargetGoal<>(this,
                Player.class, true, GMMTags.EntityTypes.ORC_WARLORD_RALLY_ALLIES));

        // The horn. Guarded on the static because an unset pool would summon nothing every cooldown
        // rather than doing nothing -- a goal that runs and produces no mob is harder to diagnose
        // than one that was never added.
        if (summonMobs != null && config.flag("summon", true)) {
            this.goalSelector.addGoal(2, new WeightedChanceSummonGoal(this,
                    (int) config.number("summonCooldownTime", DEFAULT_SUMMON_COOLDOWN_TIME),
                    config.number("summonChance", DEFAULT_SUMMON_CHANCE),
                    summonMobs,
                    (int) config.number("minSummonSpawns", DEFAULT_MIN_SUMMON_SPAWNS),
                    (int) config.number("maxSummonSpawns", DEFAULT_MAX_SUMMON_SPAWNS)));
        }
        // ABOVE the attack goal that reassessWeaponGoal() installs at priority 4, for the reason
        // SkeletonChampion documents: when crowded he steps to open ground first, and once clear
        // canUse() fails and the attack goal takes back over.
        if (config.flag("avoidCrowd", true)) {
            this.goalSelector.addGoal(3, new AvoidCrowdGoal(this,
                    config.number("crowdRadius", DEFAULT_CROWD_RADIUS),
                    (int) config.number("minCrowd", DEFAULT_MIN_CROWD),
                    config.number("spacingMaxTargetDistance", DEFAULT_SPACING_MAX_TARGET_DISTANCE),
                    1.1D));
        }
    }

    /**
     * A chief out-classes his own grunts by roughly the margin the Champion out-classes a skeleton
     * warrior, and no further.
     *
     * <p>Deliberately NOT scaled to sit above the Beholder or the Daemon: this is a warband leader,
     * the payoff for a shallow dungeon, and it inherits {@link Orc#finalizeSpawn}'s random spawn
     * bonuses on top of these &mdash; up to +10 health and +2.5 attack &mdash; so the printed
     * numbers are the floor rather than the whole story.</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 48.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.6D)
                // Baked plate, attribute-only -- populateDefaultEquipmentSlots fills MAINHAND and
                // nothing else, the same idiom SkeletonChampion and IronSkeleton use. 10 sits
                // between the rank-and-file orc's 2 and the Champion's full-iron 15: an orc chief
                // is armoured in scavenged scrap, not a matched harness.
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    /**
     * A chief wears the whole rig, where a grunt's apparel is four coin flips.
     *
     * <p>{@link Orc#finalizeSpawn} rolls each of the shoulder pads, the hair and the bracers
     * independently, so one warlord in sixteen turns up wearing none of it &mdash; and any of the
     * missing pieces reads as a rank-and-file orc who happens to be holding the cleaver. The kit is
     * what tells a player at a glance which one is the chief, and that has to be true of every one
     * of them, not most.</p>
     *
     * <p>Set AFTER {@code super}, deliberately: the roll happens in there, so setting first would be
     * overwritten by it. Everything else that method does &mdash; the weapon, the spawn bonuses, the
     * ranged roll this class forces back off &mdash; is still wanted, which is why this overrides the
     * result rather than replacing the method.</p>
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData,
                                        CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);
        setData(ALL_APPAREL);
        return data;
    }

    /**
     * The chief's blade comes from his own pool, not the warband's.
     *
     * <p>Overridden rather than inherited so a pack of orcs carrying stone axes does not put a
     * stone axe in the chief's hand. Drop chance is the Champion's 0.15 rather than {@link Orc}'s
     * 0.25: felling a leader should be worth something without flooding the loot pool.</p>
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        Optional<Item> weapon = ForgeRegistries.ITEMS.tags()
                .getTag(GMMTags.Items.ORC_WARLORD_WEAPONS).getRandomElement(this.random);
        if (weapon.isPresent()) {
            this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 0.15F;
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon.get()));
            return;
        }
        // An empty warlord pool is a datapack the consumer has emptied, not a reason to stand
        // unarmed: fall back to the warband's own weapons rather than to bare fists.
        super.populateDefaultEquipmentSlots(randomSource, difficulty);
    }

    /**
     * A warlord leads from the front and does not throw rocks.
     *
     * <p>{@link Orc} rolls {@code isRanged} at spawn, and a chief who spends the fight lobbing
     * stones from the back undoes both the rally (which is radius-limited) and the crowd-spacing
     * (which assumes he wants to be near the fight). Forced off here rather than left to the roll,
     * because a warlord that is sometimes an artillery piece is two mobs sharing one name.</p>
     */
    @Override
    public boolean isRanged() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (rallies() && this.tickCount % Math.max(1, rallyInterval()) == 0
                && this.level() instanceof ServerLevel serverLevel) {
            rally(serverLevel);
        }
    }

    /**
     * Refresh a short Strength + Speed buff on nearby tagged allies.
     *
     * <p>Fires the visible war-cry only when it actually rallies someone, so a lone chief does not
     * sparkle into an empty room, and puffs each ally that was buffed so it is readable who is
     * being led. Copied in behaviour from {@code SkeletonChampion#rally} rather than shared: the
     * two differ only in the tag, and hoisting a four-line loop into a base class to save four
     * lines would put a rally in every GMMMonster's inheritance chain.</p>
     */
    private void rally(ServerLevel level) {
        List<Mob> allies = level.getEntitiesOfClass(Mob.class,
                this.getBoundingBox().inflate(rallyRange()),
                m -> m != this && m.isAlive()
                        && m.getType().is(GMMTags.EntityTypes.ORC_WARLORD_RALLY_ALLIES));
        if (allies.isEmpty()) {
            return;
        }
        int duration = rallyDuration();
        for (Mob ally : allies) {
            // ambient=false, visible=true, showIcon=false -- a battlefield buff, not a beacon aura
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true, false), this);
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true, false), this);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    ally.getX(), ally.getY(0.6D), ally.getZ(), 4, 0.25D, 0.4D, 0.25D, 0.0D);
        }
        // The chief's own bellow. ANGRY_VILLAGER rather than the Champion's TOTEM_OF_UNDYING:
        // a totem burst reads as undead necromancy, which is the Champion's flavour and not this
        // one's -- an orc chief is shouting, not raising anyone.
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                this.getX(), this.getY(1.0D), this.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.0D);
    }

    private double rallyRange() {
        return MobConfigHelper.get(this).number("rallyRange", DEFAULT_RALLY_RANGE);
    }

    private int rallyInterval() {
        return (int) MobConfigHelper.get(this).number("rallyInterval", DEFAULT_RALLY_INTERVAL);
    }

    private int rallyDuration() {
        return (int) MobConfigHelper.get(this).number("rallyDuration", DEFAULT_RALLY_DURATION);
    }

    private boolean rallies() {
        return MobConfigHelper.get(this).flag("rally", true);
    }

    /** 1.2x the orc's own, matching the render scale a consumer gives the warlord. */
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return super.getStandingEyeHeight(pose, dimensions) * 1.2F;
    }
}
