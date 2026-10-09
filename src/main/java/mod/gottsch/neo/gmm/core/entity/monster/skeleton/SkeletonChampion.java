package mod.gottsch.neo.gmm.core.entity.monster.skeleton;



import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.AvoidCrowdGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.RaiseShieldGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * An elite skeleton that has kept its wits: a pack leader that <em>rallies</em> the skeletons around
 * it (a periodic Strength + Speed buff to nearby allies, a data-driven ally set) and fights like a
 * commander rather than a grunt — it deliberately keeps to the <em>edge</em> of its group so a
 * player's sweep or AoE catches its minions, not it (see {@link AvoidCrowdGoal}). Melee, sun-burns
 * like any surface skeleton.
 *
 * <p>Rally range / cadence / buff duration and the spacing behaviour are codec-driven
 * ({@code gmm:mob_config}); each can be tuned or disabled by consumers. The rally ally set is the
 * {@link GMMTags.EntityTypes#SKELETON_CHAMPION_RALLY_ALLIES} entity-type tag, which consumers populate
 * with their own skeleton types (gmm ships the empty key, same pattern as the ally-alert goals).
 *
 * <p>Always carries a shield (from {@code skeleton_champion/shields}) -- unlike SkeletonWarrior/Wight's
 * configurable-chance roll, a pack leader is never left without one. {@link RaiseShieldGoal} raises it
 * whenever a target closes to melee range, giving real vanilla shield-block damage reduction.
 *
 * @author Mark Gottschling on 7/6/2026
 */
public class SkeletonChampion extends GMMMonster {

    // Rally: how far the buff reaches, how often it re-applies, and how long each application lasts
    // (duration is kept a touch longer than the interval so a held pack stays buffed without flicker).
    // Interval corrected 2026-07-13: 60 ticks (3s) made the champion re-trigger the war-cry burst
    // constantly, reading as spammy rather than a periodic rally -- 200 ticks (10s) still keeps allies
    // continuously buffed (duration 240 outlasts the interval) without the particle/sound spam.
    private static final double DEFAULT_RALLY_RANGE = 8.0D;
    private static final int DEFAULT_RALLY_INTERVAL = 200;
    private static final int DEFAULT_RALLY_DURATION = 240;

    // Spacing: how close a mob has to be to crowd the leader, how many it takes to make it reposition,
    // and how far from its target it's willing to stray while doing so (stays in the fight).
    private static final double DEFAULT_CROWD_RADIUS = 4.0D;
    private static final int DEFAULT_MIN_CROWD = 2;
    private static final double DEFAULT_SPACING_MAX_TARGET_DISTANCE = 12.0D;

    // Shield (see RaiseShieldGoal): unlike SkeletonWarrior/Wight, the Champion always carries one --
    // no probability roll, see populateDefaultEquipmentSlots.
    private static final double DEFAULT_SHIELD_BLOCK_RANGE = 4.0D;
    private static final int DEFAULT_SHIELD_BLOCK_COOLDOWN = 40;
    private static final int DEFAULT_SHIELD_MAX_BLOCK_TICKS = 100;

    public SkeletonChampion(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        MobConfig config = MobConfigHelper.get(this);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        // leader spacing sits ABOVE the melee goal on purpose: when it's crowded it steps to open
        // ground (preempting the approach); once clear, canUse() fails and the melee goal takes over
        if (config.flag("avoidCrowd", true)) {
            this.goalSelector.addGoal(4, new AvoidCrowdGoal(this,
                    config.number("crowdRadius", DEFAULT_CROWD_RADIUS),
                    (int) config.number("minCrowd", DEFAULT_MIN_CROWD),
                    config.number("spacingMaxTargetDistance", DEFAULT_SPACING_MAX_TARGET_DISTANCE),
                    1.1D));
        }
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, false));
        if (config.flag("shieldBlocking", true)) {
            this.goalSelector.addGoal(5, new RaiseShieldGoal(this,
                    config.number("shieldBlockRange", DEFAULT_SHIELD_BLOCK_RANGE),
                    (int) config.number("shieldBlockCooldown", DEFAULT_SHIELD_BLOCK_COOLDOWN),
                    (int) config.number("shieldMaxBlockTicks", DEFAULT_SHIELD_MAX_BLOCK_TICKS)));
        }
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // an Overworld mini-boss / pack leader: clearly out-classes the rank-and-file variants
                // (~2x a normal skeleton's HP, a champion's blade, visibly plated). Magma stays the
                // stronger bruiser — it's a Nether "demon"; this is the Overworld elite.
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                // the plate is baked into the rig/texture, not equipped ArmorItems (populateDefaultEquipmentSlots
                // only ever fills MAINHAND) -- so the damage reduction has to come from the attribute
                // directly. 15.0/0.0 matches a full vanilla iron armor set (2+6+5+2 armor, 0 toughness
                // per piece) -- same "attribute-only baked armor" idiom IronSkeleton already uses.
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData) {
        groupData = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return groupData;
    }

    /**
     * Arm the champion with one random weapon from the {@code skeleton_champion/weapons} tag, and
     * always a shield from {@code skeleton_champion/shields} -- unlike SkeletonWarrior/Wight's
     * configurable-chance roll, the pack leader always carries one (see {@link RaiseShieldGoal}).
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        Optional<Item> weapon = BuiltInRegistries.ITEM.getRandomElementOf(GMMTags.Items.SKELETON_CHAMPION_WEAPONS, this.random).map(Holder::value);
        if (weapon.isPresent()) {
            // an elite carries a pristine blade (no worn damage, unlike the rank-and-file warrior);
            // a modest drop chance makes felling one a worthwhile prize without flooding the loot pool
            this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 0.15F;
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon.get()));
        }
        Optional<Item> shield = BuiltInRegistries.ITEM.getRandomElementOf(GMMTags.Items.SKELETON_CHAMPION_SHIELDS, this.random).map(Holder::value);
        if (shield.isPresent()) {
            this.handDropChances[EquipmentSlot.OFFHAND.getIndex()] = 0.15F;
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(shield.get()));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // rally the pack on the configured cadence — a leader is only worth the name while it lives
        if (rallies() && this.tickCount % Math.max(1, rallyInterval()) == 0
                && this.level() instanceof ServerLevel serverLevel) {
            rally(serverLevel);
        }
    }

    /**
     * Refresh a short Strength + Speed buff on nearby tagged allies. Fires the visible "war-cry" only
     * when it actually rallies someone (so the champion doesn't sparkle into an empty field), and marks
     * each buffed ally with a matching puff so it's clear who got rallied. The buff effects are also
     * {@code visible=true}, so allies additionally show the vanilla Strength/Speed swirls for as long as
     * they're buffed.
     */
    private void rally(ServerLevel level) {
        List<Mob> allies = level.getEntitiesOfClass(Mob.class,
                this.getBoundingBox().inflate(rallyRange()),
                m -> m != this && m.isAlive() && m.getType().is(GMMTags.EntityTypes.SKELETON_CHAMPION_RALLY_ALLIES));
        if (allies.isEmpty()) {
            return;
        }
        int duration = rallyDuration();
        for (Mob ally : allies) {
            // ambient=false, visible=true, showIcon=false — a battlefield buff, not a beacon aura
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true, false), this);
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true, false), this);
            // a small empower-puff on the ally that was just rallied
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    ally.getX(), ally.getY(0.6D), ally.getZ(), 6, 0.25D, 0.4D, 0.25D, 0.05D);
        }
        // the leader's rally burst — a spray of empower sparkles from the champion itself
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                this.getX(), this.getY(1.0D), this.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.15D);
    }

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    public void aiStep() {
        if (this.isSunBurnTick()) {
            this.igniteForSeconds(8);
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SKELETON_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    // 1.74 (vanilla skeleton's eye height) * 1.2 -- matches the 1.2x hitbox/render scale DD's
    // ModEntities/SkeletonChampionRenderer apply so the champion physically reads as bigger.
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(2.088F);
    }
}
