package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.function.BiFunction;

/**
 * "Bloody Bones" — a skeleton that refuses to stay dead. When a lethal blow lands it does not die:
 * it <b>collapses into a heap of bones</b> on the ground, lies dormant a few seconds, then
 * <b>resurrects</b> and fights again — up to a capped number of times ({@code maxResurrects}, default 3).
 *
 * <p>The only way to put it down for good is to kill it the <b>right</b> way:
 * <ul>
 *   <li><b>burn the bones</b> — any fire/lava damage (or a damage type in the
 *       {@code gmm:bloody_bones/true_kill} tag, e.g. a modpack's "holy"/"smite") is a true kill,
 *       whether it is standing or already a downed pile;</li>
 *   <li><b>finish the pile</b> — any melee/entity hit landed while it is a downed heap ends it.</li>
 * </ul>
 * While inert (collapsing / downed / rising) its health is pinned to 1 and its AI is off, so any
 * qualifying hit is a clean one-shot finisher and a stray fire tick reliably burns it out.
 *
 * <p>Runs on the shared vanilla-skeleton rig; the collapse/rise is a <b>procedural crumple</b> in
 * {@code BloodyBonesModel} driven by the synced {@link #DATA_PHASE} + a locally-ticked {@code phaseTicks}.
 * The collapse/rise durations are fixed constants (shared by both sides so the animation stays in
 * sync); only the server-side tuning (cap / dormant time / revive health) is codec-driven.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class BloodyBones extends GMMMonster {

    // phase ordinals (kept as raw ints for the synched accessor + the client model)
    public static final int PHASE_ALIVE = 0;
    public static final int PHASE_COLLAPSING = 1;
    public static final int PHASE_DOWNED = 2;
    public static final int PHASE_RISING = 3;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(BloodyBones.class, EntityDataSerializers.INT);

    // Animation timings — fixed constants so the client crumple and the server timer agree exactly.
    public static final int COLLAPSE_TICKS = 15;
    public static final int RISE_TICKS = 20;

    // Server-tunable knobs (codec-driven; see gmm:mob_config/bloody_bones.json).
    private static final int DEFAULT_MAX_RESURRECTS = 3;
    private static final int DEFAULT_DOWNED_TICKS = 100; // ~5s lying as a pile before it rises
    private static final double DEFAULT_REVIVE_HEALTH_PCT = 0.5D;

    /**
     * Consumer-supplied factory for the flung "limb" shrapnel (gmm registers no EntityTypes). DD wires
     * this to a {@code BoneShard} — the same projectile the Tainted Skeleton throws; if left null the
     * limbs fall back to a plain arrow.
     */
    public static BiFunction<LivingEntity, Level, AbstractArrow> shardFactory;

    // the four limbs (2 arms + 2 legs) that fly off when it collapses to a skull.
    private static final int LIMB_SHARDS = 4;
    // chunkier than a normal shard so they read as whole limb-bones rather than splinters.
    private static final float LIMB_SHARD_SCALE = 1.8F;

    // ticks elapsed within the current phase; ticked on BOTH sides, reset when the phase changes.
    private int phaseTicks;
    private int lastPhase = PHASE_ALIVE;
    // how many times it has already gone down (persisted). Once it hits maxResurrects the next
    // lethal blow is a real death.
    private int resurrectCount;

    public BloodyBones(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_ALIVE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    // --- codec accessors ------------------------------------------------------------------------

    private int maxResurrects() {
        return (int) MobConfigHelper.get(this).number("maxResurrects", DEFAULT_MAX_RESURRECTS);
    }

    private int downedTicks() {
        return (int) MobConfigHelper.get(this).number("downedTicks", DEFAULT_DOWNED_TICKS);
    }

    private double reviveHealthPct() {
        return MobConfigHelper.get(this).number("reviveHealthPct", DEFAULT_REVIVE_HEALTH_PCT);
    }

    private boolean selfResurrects() {
        return MobConfigHelper.get(this).flag("selfResurrect", true);
    }

    // --- phase state ----------------------------------------------------------------------------

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    public boolean isInert() {
        return getPhase() != PHASE_ALIVE;
    }

    private boolean canResurrect() {
        return selfResurrects() && resurrectCount < maxResurrects();
    }

    /**
     * A "true kill" bypasses resurrection entirely: fire/lava (burn the bones), or any damage type a
     * pack has added to {@code gmm:bloody_bones/true_kill} (holy/smite). Absent tag == matches nothing.
     */
    private boolean isTrueKill(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(GMMTags.DamageTypes.BLOODY_BONES_TRUE_KILL);
    }

    // --- death / damage interception ------------------------------------------------------------

    /**
     * The resurrection hook. While standing, a non-true-kill lethal blow does not kill it — it
     * collapses instead (death prevented by NOT calling {@code super.die}). Any death reached while
     * already inert (a finisher on the pile), out of resurrects, or from a true kill falls through to
     * a real death + loot.
     */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide
                && getPhase() == PHASE_ALIVE
                && canResurrect()
                && !isTrueKill(source)) {
            beginCollapse();
            return;
        }
        super.die(source);
    }

    /**
     * Gates what damage reaches the inert body. Mid-animation it is invulnerable except to a true
     * kill (you can still burn it as it goes down / gets up); as a downed pile it shrugs off
     * environmental damage but any attacker's hit (or fire) finishes it. Health is pinned to 1 while
     * inert, so any hit that gets through is lethal → {@link #die} → real death.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            int phase = getPhase();
            if (phase == PHASE_COLLAPSING || phase == PHASE_RISING) {
                if (!isTrueKill(source)) {
                    return false;
                }
            } else if (phase == PHASE_DOWNED) {
                boolean finisher = isTrueKill(source) || source.getEntity() instanceof LivingEntity;
                if (!finisher) {
                    return false;
                }
            }
        }
        return super.hurt(source, amount);
    }

    /**
     * Enter the collapse: pin to 1 HP, shut off AI, and burst apart — the arms/legs fly off as bone
     * shrapnel (Tainted-style) and only the skull is left to drop to the ground (handled by the model).
     * Server-side only.
     */
    private void beginCollapse() {
        this.resurrectCount++;
        setPhase(PHASE_COLLAPSING);
        this.phaseTicks = 0;
        this.setHealth(1.0F);
        this.setNoAi(true);
        this.setTarget(null);
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.getNavigation().stop();
        this.playSound(SoundEvents.SKELETON_DEATH, 1.0F, 0.8F);
        flingLimbs();
        spawnBoneBurst(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE)), 18);
    }

    /**
     * Tosses the four limbs (2 arms + 2 legs) a short way out as it comes apart — a gentle, harmless
     * scatter (no damage, low velocity) so they just flop down nearby rather than shooting off.
     */
    private void flingLimbs() {
        for (int i = 0; i < LIMB_SHARDS; i++) {
            AbstractArrow shard = (shardFactory != null)
                    ? shardFactory.apply(this, this.level()) : new Arrow(this.level(), this);
            double dx = this.random.nextGaussian() * 0.5D;
            double dy = this.random.nextDouble() * 0.4D + 0.2D; // a soft upward pop
            double dz = this.random.nextGaussian() * 0.5D;
            shard.setPos(this.getX(), this.getY(0.6D), this.getZ());
            shard.shoot(dx, dy, dz, 0.28F, 3.0F); // low velocity — they don't travel far
            shard.setBaseDamage(0.0D);            // purely cosmetic: hurts nothing
            shard.setCritArrow(false);
            shard.pickup = AbstractArrow.Pickup.DISALLOWED;
            if (shard instanceof mod.gottsch.neo.gmm.core.entity.projectile.BoneShard bone) {
                bone.setScale(LIMB_SHARD_SCALE);
            }
            this.level().addFreshEntity(shard);
        }
    }

    /** Stand back up: restore AI + a fraction of health. Server-side only. */
    private void beginRise() {
        setPhase(PHASE_RISING);
        this.phaseTicks = 0;
        this.playSound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.6F);
        spawnBoneBurst(ParticleTypes.CRIT, 10);
    }

    private void finishRise() {
        setPhase(PHASE_ALIVE);
        this.phaseTicks = 0;
        this.setNoAi(false);
        float revived = Math.max(1.0F, (float) (this.getMaxHealth() * reviveHealthPct()));
        this.setHealth(revived);
    }

    private void spawnBoneBurst(net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(particle,
                    this.getX(), this.getY(0.3D), this.getZ(),
                    count, 0.3D, 0.1D, 0.3D, 0.02D);
        }
    }

    // --- tick / phase timer ---------------------------------------------------------------------

    /**
     * {@code setNoAi(true)} suppresses {@code serverAiStep}/{@code customServerAiStep} entirely, so the
     * phase timer lives here in {@code tick()} (which always runs). {@code phaseTicks} is advanced on
     * both sides for the client crumple; the server also drives the transitions.
     */
    @Override
    public void tick() {
        super.tick();

        int phase = getPhase();
        if (phase != this.lastPhase) {
            this.phaseTicks = 0;
            this.lastPhase = phase;
        } else {
            this.phaseTicks++;
        }

        if (!this.level().isClientSide) {
            switch (phase) {
                case PHASE_COLLAPSING -> {
                    if (this.phaseTicks >= COLLAPSE_TICKS) {
                        setPhase(PHASE_DOWNED);
                    }
                }
                case PHASE_DOWNED -> {
                    if (this.phaseTicks >= downedTicks()) {
                        beginRise();
                    }
                }
                case PHASE_RISING -> {
                    if (this.phaseTicks >= RISE_TICKS) {
                        finishRise();
                    }
                }
                default -> { /* ALIVE: nothing to schedule */ }
            }
        }
    }

    /**
     * 0 = fully standing, 1 = fully collapsed heap. Interpolated with partialTicks for a smooth
     * crumple/rise; used by {@code BloodyBonesModel} and its renderer.
     */
    public float getCollapseAmount(float partialTicks) {
        float t = this.phaseTicks + partialTicks;
        return switch (getPhase()) {
            case PHASE_COLLAPSING -> Mth.clamp(t / COLLAPSE_TICKS, 0.0F, 1.0F);
            case PHASE_DOWNED -> 1.0F;
            case PHASE_RISING -> Mth.clamp(1.0F - t / RISE_TICKS, 0.0F, 1.0F);
            default -> 0.0F;
        };
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseTicks", this.phaseTicks);
        tag.putInt("ResurrectCount", this.resurrectCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(tag.getInt("Phase"));
        this.phaseTicks = tag.getInt("PhaseTicks");
        this.lastPhase = getPhase();
        this.resurrectCount = tag.getInt("ResurrectCount");
    }

    // --- flavour / sounds -----------------------------------------------------------------------

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void aiStep() {
        // sun-burn only while up and about; a downed pile must be burned deliberately (real fire),
        // otherwise it could never be seen to resurrect in daylight.
        if (getPhase() == PHASE_ALIVE && this.isSunBurnTick()) {
            this.setSecondsOnFire(8);
        }
        // weeps blood: an occasional droplet forms on a bone, then falls and splats (reuses the
        // acid-drip behaviour with a red sprite). Only while up and moving.
        if (this.level().isClientSide && getPhase() == PHASE_ALIVE && this.random.nextInt(12) == 0) {
            this.level().addParticle(GMMParticles.BLOOD_DRIP.get(),
                    this.getRandomX(0.6D),
                    this.getY() + 0.1D + this.random.nextDouble() * 1.6D,
                    this.getRandomZ(0.6D),
                    0.0D, 0.0D, 0.0D);
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
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

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.74F;
    }
}
