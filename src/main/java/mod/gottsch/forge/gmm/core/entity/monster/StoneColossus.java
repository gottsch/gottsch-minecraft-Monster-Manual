package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.entity.ai.goal.DormancyGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.GroundSlamGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.StalkGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.TallMeleeAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A tall, lean stone construct that stands as scenery until someone walks into its arena, then
 * takes the room apart to get at them.
 *
 * <p><b>The dormancy is the identity of the mob</b>, not a spawn optimisation: it is the only thing
 * in the roster whose first moment is a surprise rather than an arrival. {@link DormancyGoal} holds
 * it still and wakes it; this class only owns the synched state, because the statue pose is drawn
 * client-side and the client never runs the goal.
 *
 * <p><b>Slow, heavy and telegraphed</b>, deliberately opposite to the {@link Minotaur}: the
 * Minotaur owns the charge, and two brutes sharing one tell would be one mob with two models. The
 * colossus trades speed for mass -- high health and armour, immovable, and a low attack rate, so
 * the danger is being cornered rather than a damage race.
 *
 * <p><b>Arrows chip, they do not wound.</b> Projectile damage is cut by
 * {@link #PROJECTILE_DAMAGE_SCALE} and a pickaxe swing is multiplied by {@link #PICKAXE_DAMAGE_SCALE},
 * which is a design statement rather than a balance knob: the answer to a thing made of stone is to
 * mine it, and standing off shooting arrows at it should be a bad trade. It matters most where the
 * consumer puts this mob -- in a room with one doorway, where shooting from safety would otherwise
 * be the whole fight.
 *
 * <p>Construct, not undead: immune to poison and wither, <em>not</em> immune to fire.
 *
 * <p>Too big for doors, by design. At 1.0 wide or more the pathfinder counts it as two blocks
 * ({@code floor(width + 1)}, see {@link Orc}), so it never paths through a 1-wide doorway, and its
 * height rules one out anyway. Do not narrow it to make it fit: it reaches a player through
 * {@link StalkGoal} and {@link TallMeleeAttackGoal}, whose reach comes from the hitbox width.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class StoneColossus extends GMMMonster implements IDormantMob, ISlammingMob, IBurstingMob, IHurlingMob {
    /**
     * Synched because both are CLIENT-side reads: {@code StoneColossusModel} holds the statue pose
     * while dormant and plays the rise while the flag is set. Both are FLAGS -- the tick the rise
     * started is client-local ({@link #wakeAnimStartTick}), for the reason {@link IDormantMob}
     * gives at length.
     */
    private static final EntityDataAccessor<Boolean> DORMANT =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WAKING =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);

    /**
     * Synched as a FLAG, deliberately -- see {@link ISlammingMob}. The tick the animation started
     * is client-local ({@link #slamAnimStartTick}) because the two sides' tick counters are not the
     * same clock.
     */
    private static final EntityDataAccessor<Boolean> SLAMMING =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);

    /**
     * The wall burst -- a flag and, unlike the slam, WHICH FIST. See {@link IBurstingMob}: the
     * striking arm is synched rather than derived so that two players watching the same punch see
     * the same arm.
     */
    private static final EntityDataAccessor<Boolean> BURSTING =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BURST_LEFT_FIST =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);

    /** The boulder heave; same flag-and-clock shape as the other two. See {@link IHurlingMob}. */
    private static final EntityDataAccessor<Boolean> HURLING =
            SynchedEntityData.defineId(StoneColossus.class, EntityDataSerializers.BOOLEAN);

    private static final String DORMANT_TAG = "dormant";

    /** How close a player has to come before the statue moves. */
    private static final double WAKE_RANGE = 8.0D;
    /**
     * The rise, in ticks. Long enough to be a moment -- the player should get to watch a piece of
     * the room stand up -- and short enough not to be a free hit on it.
     */
    private static final int WAKE_DURATION = 40;

    /**
     * How far it will stalk a target it cannot reach. Matched to FOLLOW_RANGE: inside the arena it
     * was designed for, anywhere the player can stand is somewhere it should be working toward.
     */
    private static final double STALK_RANGE = 32.0D;
    /** Close enough to be under them; shuffling nearer than this is jitter, not menace. */
    private static final double STALK_ARRIVE_DISTANCE = 2.0D;

    /**
     * The slam. {@link #SLAM_WINDUP_TICKS} is the player's window to get out of the radius and the
     * length of the model's raise, so it is deliberately long -- this is the mob's telegraph, and a
     * slam nobody can read is just damage. The trigger range is under the radius on purpose: it
     * commits when someone is well inside, and catches anyone at the edge as a bonus.
     */
    private static final double SLAM_TRIGGER_RANGE = 5.0D;
    private static final double SLAM_RADIUS = 6.0D;
    private static final float SLAM_DAMAGE = 8.0F;
    private static final int SLAM_WINDUP_TICKS = 25;
    private static final int SLAM_RECOVERY_TICKS = 15;
    private static final int SLAM_COOLDOWN_TICKS = 140;

    /**
     * The wall burst's wind-up and recovery. Shorter than the slam's, on purpose: the slam is an
     * area attack the player is meant to read and step out of, while the burst is aimed at a wall
     * that cannot dodge. Its telegraph is for the player standing BEHIND that wall -- long enough to
     * say "leave the doorway", not so long that a boss spends the fight cocking its arm.
     */
    private static final int BURST_WINDUP_TICKS = 20;
    private static final int BURST_RECOVERY_TICKS = 10;

    /**
     * The boulder heave. The LONGEST wind-up in the kit, and deliberately so: the target of a
     * thrown rock is a player who has backed out of reach, watching from somewhere safe, and the
     * heave is the whole of their warning. Raise this rather than lowering the damage if the
     * boulder ever feels unfair.
     */
    private static final int HURL_WINDUP_TICKS = 30;
    private static final int HURL_RECOVERY_TICKS = 12;

    /**
     * Ticks between melee swings, against vanilla's 20. A blow this heavy landing once a second
     * reads as a flurry rather than as a hammer (Mark, 2026-09-13), and the mob's threat is meant to
     * be the cornering, not the damage rate -- see the class javadoc.
     */
    private static final int MELEE_INTERVAL_TICKS = 45;

    /** Arrows chip, they do not wound. See the class javadoc: this is a statement, not a knob. */
    private static final float PROJECTILE_DAMAGE_SCALE = 0.5F;
    /** The answer to a thing made of stone is to mine it. */
    private static final float PICKAXE_DAMAGE_SCALE = 1.75F;

    /** Client-local; see {@link #tick()}. Never synched and never saved. */
    private int wakeAnimStartTick = NOT_WAKING;
    private int slamAnimStartTick = NOT_SLAMMING;
    private int burstAnimStartTick = NOT_BURSTING;
    private int hurlAnimStartTick = NOT_HURLING;

    public StoneColossus(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
    }

    @Override
    protected void registerGoals() {
        // Priority 0 and holds every flag the goal selector has: while this runs, nothing else does.
        this.goalSelector.addGoal(0, new DormancyGoal(this, WAKE_RANGE));
        // Priority 2, above both the stalk and the swing: the slam is the signature attack, and
        // when it is ready it wins. Its own long cooldown -- not a priority fight -- is what keeps
        // it from being the only thing the mob ever does.
        this.goalSelector.addGoal(2, new GroundSlamGoal(this,
                SLAM_TRIGGER_RANGE, SLAM_RADIUS, SLAM_DAMAGE, SLAM_COOLDOWN_TICKS));
        // ABOVE the melee goal, and safe there because its entry test is melee's exact complement:
        // it runs only while the target can neither be walked to nor reached. A lower-priority goal
        // could never take the MOVE flag off melee, so below it this would be inert -- the same
        // trap Dungeons2's SmashBlocksGoal fell into.
        this.goalSelector.addGoal(3, new StalkGoal(this, 1.0D, STALK_RANGE, STALK_ARRIVE_DISTANCE));
        // NOT the vanilla MeleeAttackGoal: its reach is one 3D distance from feet to feet, which
        // counts this mob's own height against it and leaves a player standing level with its head
        // unhittable. See TallMeleeAttackGoal.
        this.goalSelector.addGoal(4, new TallMeleeAttackGoal(this, 1.0D, true, MELEE_INTERVAL_TICKS));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * <p><strong>Scaled against THIS roster, not against D&amp;D.</strong> The first cut was 220,
     * reasoning from the name -- a D&amp;D colossus is fifty feet of mythic construct. Ours is seven
     * blocks, about twenty-one feet (Mark, 2026-09-13), which puts it with the Iron Golem and the
     * Ravager (100 each) rather than the Warden (500), and beside the {@code Daemon}'s 80 as the
     * other large-tier boss. 120 makes it the sturdiest thing in the roster by a clear margin
     * without being a different order of creature.</p>
     *
     * <p>And the health is not the whole picture: 14 armour with 6 toughness is most of what makes
     * it feel like stone, so raising this is the wrong lever if it dies too fast.</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                // a shade under the Minotaur's armed total (12), swung far more slowly
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                // it does not chip you either -- a hit puts you across the room
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                // immovable, as a thing of this mass should be
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                // slower than the Minotaur (0.23): the weight has to cost it something, and a boss
                // you cannot walk away from is a boss with no shape to its fight. Raised from 0.17
                // on 2026-09-17 -- at that speed backing out of its arena was free, which took the
                // pressure off the fight rather than shaping it.
                .add(Attributes.MOVEMENT_SPEED, 0.19D);
    }

    /**
     * Scales incoming damage by what it was dealt with, then wakes the mob.
     *
     * <p>The wake is the important half. A statue that could be shot to death before it moved would
     * make the whole dormancy a liability rather than an ambush, so <strong>any</strong> damage
     * brings it round -- including the arrow that only chips it.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            // A 7-block statue placed in a room with a low ceiling would otherwise grind itself to
            // death before a player ever saw it.
            return false;
        }
        float scaled = amount;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            scaled *= PROJECTILE_DAMAGE_SCALE;
        } else if (source.getEntity() instanceof LivingEntity attacker
                && attacker.getMainHandItem().is(ItemTags.PICKAXES)) {
            scaled *= PICKAXE_DAMAGE_SCALE;
        }
        boolean hurt = super.hurt(source, scaled);
        if (hurt) {
            wake();
        }
        return hurt;
    }

    /** A construct: nothing to poison and nothing to wither. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON || effect.getEffect() == MobEffects.WITHER) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /** Stone does not drown, and a statue standing in a flooded room is still a statue. */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** No nameplate, no hitbox highlight, no pushing: it is scenery until it is not. */
    @Override
    public boolean isPushable() {
        return !isDormant();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (!isDormant()) {
            super.doPush(entity);
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DORMANT, true);
        this.entityData.define(WAKING, false);
        this.entityData.define(SLAMMING, false);
        this.entityData.define(BURSTING, false);
        this.entityData.define(BURST_LEFT_FIST, false);
        this.entityData.define(HURLING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(DORMANT_TAG, isDormant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DORMANT_TAG)) {
            setDormant(tag.getBoolean(DORMANT_TAG));
        }
        // The rise is deliberately NOT persisted: a colossus caught mid-rise by a save is simply
        // finished rising when it loads. Replaying it would hand the player a second free window
        // on a mob they have already woken.
    }

    @Override
    public boolean isDormant() {
        return this.entityData.get(DORMANT);
    }

    @Override
    public void setDormant(boolean dormant) {
        this.entityData.set(DORMANT, dormant);
    }

    @Override
    public boolean isWaking() {
        return this.entityData.get(WAKING);
    }

    @Override
    public void setWaking(boolean waking) {
        this.entityData.set(WAKING, waking);
    }

    @Override
    public int getWakeAnimStartTick() {
        return this.wakeAnimStartTick;
    }

    @Override
    public void setWakeAnimStartTick(int tick) {
        this.wakeAnimStartTick = tick;
    }

    @Override
    public int getWakeDuration() {
        return WAKE_DURATION;
    }

    @Override
    public boolean isSlamming() {
        return this.entityData.get(SLAMMING);
    }

    @Override
    public void setSlamming(boolean slamming) {
        this.entityData.set(SLAMMING, slamming);
    }

    @Override
    public int getSlamAnimStartTick() {
        return this.slamAnimStartTick;
    }

    @Override
    public void setSlamAnimStartTick(int tick) {
        this.slamAnimStartTick = tick;
    }

    @Override
    public boolean isBursting() {
        return this.entityData.get(BURSTING);
    }

    @Override
    public void setBursting(boolean bursting) {
        this.entityData.set(BURSTING, bursting);
    }

    @Override
    public boolean isBurstLeftFisted() {
        return this.entityData.get(BURST_LEFT_FIST);
    }

    @Override
    public void setBurstLeftFisted(boolean leftFisted) {
        this.entityData.set(BURST_LEFT_FIST, leftFisted);
    }

    @Override
    public int getBurstAnimStartTick() {
        return this.burstAnimStartTick;
    }

    @Override
    public void setBurstAnimStartTick(int tick) {
        this.burstAnimStartTick = tick;
    }

    @Override
    public int getBurstWindupTicks() {
        return BURST_WINDUP_TICKS;
    }

    @Override
    public int getBurstRecoveryTicks() {
        return BURST_RECOVERY_TICKS;
    }

    @Override
    public boolean isHurling() {
        return this.entityData.get(HURLING);
    }

    @Override
    public void setHurling(boolean hurling) {
        this.entityData.set(HURLING, hurling);
    }

    @Override
    public int getHurlAnimStartTick() {
        return this.hurlAnimStartTick;
    }

    @Override
    public void setHurlAnimStartTick(int tick) {
        this.hurlAnimStartTick = tick;
    }

    @Override
    public int getHurlWindupTicks() {
        return HURL_WINDUP_TICKS;
    }

    @Override
    public int getHurlRecoveryTicks() {
        return HURL_RECOVERY_TICKS;
    }

    /**
     * Starts and clears the CLIENT's own attack clocks on the edges of the synched flags.
     *
     * <p>This is the half that makes the animation line up with the blow: each side times the
     * animation with its own counter, so they differ by network latency rather than by the
     * arbitrary gap between two {@code tickCount}s that started at different moments.</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            return;
        }
        if (isWaking()) {
            if (this.wakeAnimStartTick == NOT_WAKING) {
                this.wakeAnimStartTick = this.tickCount;
            }
        } else {
            this.wakeAnimStartTick = NOT_WAKING;
        }
        if (isSlamming()) {
            if (this.slamAnimStartTick == NOT_SLAMMING) {
                this.slamAnimStartTick = this.tickCount;
            }
        } else {
            this.slamAnimStartTick = NOT_SLAMMING;
        }
        if (isBursting()) {
            if (this.burstAnimStartTick == NOT_BURSTING) {
                this.burstAnimStartTick = this.tickCount;
            }
        } else {
            this.burstAnimStartTick = NOT_BURSTING;
        }
        if (isHurling()) {
            if (this.hurlAnimStartTick == NOT_HURLING) {
                this.hurlAnimStartTick = this.tickCount;
            }
        } else {
            this.hurlAnimStartTick = NOT_HURLING;
        }
    }

    @Override
    public int getSlamWindupTicks() {
        return SLAM_WINDUP_TICKS;
    }

    @Override
    public int getSlamRecoveryTicks() {
        return SLAM_RECOVERY_TICKS;
    }

    /** Silent while it is scenery -- an ambient groan would give the statue away. */
    @Override
    protected SoundEvent getAmbientSound() {
        return isDormant() ? null : SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.7F);
    }
}
