package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.ExtendedReachMeleeAttackGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.forge.gmm.core.particle.GMMParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The deadliest and fastest of the ooze family — where {@link GelatinousCube} is a snail-slow trap you
 * blunder into and {@link GrayOoze} is a stationary ambusher, the Black Pudding actively runs you down.
 * It hits harder than anything else in the family and moves faster than any of them (see
 * {@link #createAttributes}).
 * <p>
 * Two mechanics on top of the stat line, both bounded so they don't cancel each other's counterplay:
 * <ul>
 * <li><b>Tendril lash</b> — an adult strikes from {@code reachBonus} blocks further than a normal
 * melee mob, so simply backpedaling doesn't buy you the usual safety margin. The strike is drawn as an
 * arcing pseudopod by {@code TendrilRenderer}; the cuboid "tendrils" on the rig are unrelated
 * decorative slime. Note the reach is applied by {@link ExtendedReachMeleeAttackGoal}, not by
 * {@link #getMeleeAttackRangeSqr}, which vanilla's melee goal ignores.</li>
 * <li><b>Split on slashing/lightning</b> — the D&amp;D-canonical trigger, translated to MC as "struck by
 * a sword or an axe, or by lightning" ({@link #isSplittingBlow}). Deliberately <em>not</em> every melee
 * hit the way {@link OchreJelly} splits: the whole point is that it hands the player a discoverable out
 * (use anything else — a pickaxe, a fist, fire) instead of punishing them for fighting back at all.</li>
 * </ul>
 * Unlike {@link OchreJelly}'s strictly linear split (children can never split), a pudding's children
 * <em>can</em> split again. Two caps keep that terminating: each pudding has its own budget of
 * {@code maxSplits} splits (default 1, see {@link #splitsRemaining}), and {@link #getGeneration generation}
 * stops the chain at {@code maxGeneration} (default 3). Each generation also halves max health. With
 * {@code s = maxSplits} and {@code G = maxGeneration}, a single adult yields at most
 * {@code s + s^2 + ... + s^G} descendants: {@code G} when {@code s = 1} (default: 3), otherwise
 * {@code s(s^G - 1)/(s - 1)} (e.g. 14 for {@code s = 2}). Children are also strictly weaker in kind, not just in numbers: only generation 0 can
 * lash ({@link #hasLash}), so only the adult gets the extended reach, and children render visibly
 * smaller. That keeps a corridor full of puddings from becoming an unbroken wall of long-reach
 * attackers with no gap to retreat through.
 *
 * @author Mark Gottschling on 8/24/2026
 */
public class BlackPudding extends GMMMonster {

    // near-black with a faint violet cast, so the trail reads as distinct from the Cube's tan and the
    // Jelly's ochre without being invisible against a dark dungeon floor
    private static final float TRAIL_R = 0.13F;
    private static final float TRAIL_G = 0.09F;
    private static final float TRAIL_B = 0.16F;

    /** Extra melee reach in blocks, on top of the vanilla range. Adults only — see {@link #hasLash}. */
    private static final double DEFAULT_REACH_BONUS = 1.0D;
    private static final int DEFAULT_MAX_GENERATION = 3;
    private static final int DEFAULT_MAX_SPLITS = 1;
    private static final double DEFAULT_SPLIT_HEALTH_FRACTION = 0.5D;
    // same self-imposed gate the rest of the family uses — see GelatinousCube#doHurtTarget for why
    private static final int DEFAULT_ATTACK_COOLDOWN = 30;
    private static final float MIN_CHILD_HEALTH = 4.0F;

    /**
     * How many splits deep this pudding is: 0 = the original (has tendrils, has reach), 1+ = a child.
     * Synched because the client needs it — the model hides the tendril cubes and the renderer shrinks
     * the body for anything above 0.
     */
    private static final EntityDataAccessor<Integer> DATA_GENERATION =
            SynchedEntityData.defineId(BlackPudding.class, EntityDataSerializers.INT);

    /**
     * Bumped once per lash that actually lands, so the client can fire a one-shot strike animation.
     * A counter rather than a tick-stamp on purpose: {@code tickCount} runs independently on each
     * side and the two diverge, so a synced server tick is not comparable to the client's own.
     */
    private static final EntityDataAccessor<Integer> DATA_LASH_ID =
            SynchedEntityData.defineId(BlackPudding.class, EntityDataSerializers.INT);

    /**
     * Centre-to-centre distance to the victim at the moment the lash connected, so the client can
     * extend the pseudopod exactly that far instead of always snapping to maximum reach.
     */
    private static final EntityDataAccessor<Float> DATA_LASH_DISTANCE =
            SynchedEntityData.defineId(BlackPudding.class, EntityDataSerializers.FLOAT);

    /** How long the strike takes to recover, in ticks. Animation feel, so deliberately not config. */
    private static final int LASH_TICKS = 8;

    /**
     * How many more times <em>this</em> pudding may split. Without it, every sword hit a pudding survived
     * spawned another child, so one adult became dozens. Filled from {@code maxSplits} at spawn and for
     * each child in {@link #split}, decremented per split, and persisted so a chunk reload can't refill
     * it. Server-only bookkeeping, like {@link OchreJelly}'s, so not synched.
     */
    private int splitsRemaining = DEFAULT_MAX_SPLITS;
    private int lastAttackTick = -1000;
    /** Client-only animation state; never read or written server-side. */
    private int lastSeenLashId;
    private int clientLashTicks;

    public BlackPudding(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    /** Reads the optional {@code maxSplits} config once at (natural/spawner/summon-goal) spawn. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        this.splitsRemaining = maxSplits();
        return spawnGroupData;
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GENERATION, 0);
        this.entityData.define(DATA_LASH_ID, 0);
        this.entityData.define(DATA_LASH_DISTANCE, 0.0F);
    }

    /**
     * Watches for the lash counter changing and starts the client-side strike animation. Using the
     * counter (rather than the vanilla swing state the goal already sets) is what keeps the tell
     * honest: {@code MeleeAttackGoal} calls {@code swing()} before {@link #doHurtTarget}, and this
     * mob's cooldown gate rejects roughly half of those attempts, so animating off the swing would
     * show a strike on blows that deal no damage.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_LASH_ID.equals(key) && this.level().isClientSide) {
            int id = this.entityData.get(DATA_LASH_ID);
            if (id != this.lastSeenLashId) {
                this.lastSeenLashId = id;
                this.clientLashTicks = LASH_TICKS;
            }
        }
    }

    /**
     * Strike progress for the renderer: 1.0 on the frame the hit lands, easing to 0.0 as the tendrils
     * recover, and 0.0 whenever it isn't striking. Deliberately no wind-up — like a zombie, the swing
     * <em>is</em> the hit, so this is follow-through only and never telegraphs.
     * <p>
     * Client-side only; returns 0.0 on the server.
     */
    /** Centre-to-centre distance the last landed lash had to cover, in blocks. */
    public float getLashDistance() {
        return this.entityData.get(DATA_LASH_DISTANCE);
    }

    public float getLashProgress(float partialTick) {
        if (this.clientLashTicks <= 0) {
            return 0.0F;
        }
        float remaining = this.clientLashTicks - partialTick;
        return remaining <= 0.0F ? 0.0F : remaining / LASH_TICKS;
    }

    public int getGeneration() {
        return this.entityData.get(DATA_GENERATION);
    }

    public void setGeneration(int generation) {
        this.entityData.set(DATA_GENERATION, Math.max(0, generation));
        this.refreshDimensions();
    }

    /**
     * Only the original can lash — so only the original gets the extended reach. Named for the ability
     * rather than the geometry: the cuboid "tendrils" on the rig are decorative trailing slime that
     * every generation has, and are unrelated to reach (see {@code BlackPuddingModel}).
     */
    public boolean hasLash() {
        return getGeneration() == 0;
    }

    /** Children render (and collide) smaller, one step per generation, floored so they stay visible. */
    public float getSizeScale() {
        return Math.max(0.45F, 1.0F - 0.2F * getGeneration());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getSizeScale());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // NOT vanilla MeleeAttackGoal: that recomputes its reach inline from the hitbox and ignores
        // Mob#getMeleeAttackRangeSqr entirely, so the lash's extra reach never took effect.
        this.goalSelector.addGoal(2, new ExtendedReachMeleeAttackGoal(this, 1.0D, false, this::reachBonus));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // the family's toughest: Gray Ooze 25, Gelatinous Cube 24, Ochre Jelly 20
                .add(Attributes.MAX_HEALTH, 30.0D)
                // the family's fastest by a clear margin (Ochre Jelly 0.18 was the previous high),
                // but deliberately still under a vanilla Zombie's 0.23 — it should outpace the other
                // oozes, not outpace the player. Walking away from it stays a viable option.
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                // the family's hardest hit: Gelatinous Cube 4, Ochre Jelly 3
                .add(Attributes.ATTACK_DAMAGE, 7.0D);
    }

    /**
     * Extra reach the tendril lash currently grants, in blocks — zero for a split child, which has no
     * tendrils to lash with. Read live so a config change applies without a respawn.
     */
    private double reachBonus() {
        if (!hasLash() || !MobConfigHelper.get(this).flag("tendrilLash", true)) {
            return 0.0D;
        }
        return Math.max(0.0D, MobConfigHelper.get(this).number("reachBonus", DEFAULT_REACH_BONUS));
    }

    /**
     * Kept in step with {@link #reachBonus} for the vanilla code paths that <em>do</em> consult it
     * (targeting checks, other goals). {@link ExtendedReachMeleeAttackGoal} does not use this — see
     * that class for why a Mob-level override alone is not enough.
     */
    @Override
    public double getMeleeAttackRangeSqr(LivingEntity target) {
        double baseSqr = super.getMeleeAttackRangeSqr(target);
        double bonus = reachBonus();
        if (bonus <= 0.0D) {
            return baseSqr;
        }
        double reach = Math.sqrt(baseSqr) + bonus;
        return reach * reach;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15F, 0.8F);
    }

    /** Same self-imposed cooldown gate the rest of the family uses — see {@code GelatinousCube}. */
    @Override
    public boolean doHurtTarget(Entity target) {
        int cooldown = (int) MobConfigHelper.get(this).number("attackCooldown", DEFAULT_ATTACK_COOLDOWN);
        if (this.tickCount - this.lastAttackTick < cooldown) {
            // rejected by the cooldown: no strike happened at all, so nothing is shown
            return false;
        }
        // committed to a strike from here on, whether or not the victim ends up taking damage
        this.lastAttackTick = this.tickCount;

        // Fire the animation on the committed strike rather than on damage dealt. An invulnerable
        // target -- a creative-mode player, /effect resistance, a successful shield block -- makes
        // super.doHurtTarget return false, and gating the visual on that would mean the pudding
        // lashed at such a player with nothing rendered at all. Vanilla mobs still visibly swing at
        // creative players; this matches. The cooldown-rejected attempts above stay invisible, which
        // is what keeps the tell honest.
        // Distance first, so it is already in place when the id change starts the client animation.
        this.entityData.set(DATA_LASH_DISTANCE, (float) this.distanceTo(target));
        this.entityData.set(DATA_LASH_ID, this.entityData.get(DATA_LASH_ID) + 1);

        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.8F);
        }
        return hurt;
    }

    /**
     * A splitting blow that this pudding survives spawns a child, provided this pudding still has a
     * split left and the chain hasn't already hit {@code maxGeneration}.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.isAlive() && this.splitsRemaining > 0
                && MobConfigHelper.get(this).flag("split", true)
                && getGeneration() < maxGeneration()
                && isSplittingBlow(source)) {
            split();
        }
        return hurt;
    }

    private int maxGeneration() {
        return (int) MobConfigHelper.get(this).number("maxGeneration", DEFAULT_MAX_GENERATION);
    }

    private int maxSplits() {
        return Math.max(0, (int) MobConfigHelper.get(this).number("maxSplits", DEFAULT_MAX_SPLITS));
    }

    /**
     * D&amp;D's Black Pudding splits on slashing and lightning damage. Minecraft has no damage-type
     * system to read that off, so it's translated to the weapon actually used: a direct melee strike
     * with a sword or an axe (the game's two "slashing" tools), or any lightning damage.
     * <p>
     * Everything else — a pickaxe, a shovel, a bare fist, arrows, explosions, fire, magic — deals its
     * damage without splitting. That asymmetry is the mechanic's whole counterplay, so this stays
     * deliberately narrow.
     */
    private boolean isSplittingBlow(DamageSource source) {
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();
        if (!(causing instanceof LivingEntity attacker) || direct != causing) {
            return false;
        }
        ItemStack weapon = attacker.getMainHandItem();
        return weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES);
    }

    /**
     * Spawns one child alongside this one at the next generation down, with half the health, and spends
     * one of this pudding's splits. The child gets its own fresh {@code maxSplits} budget and can split
     * again in turn, but its higher generation brings the chain closer to the cap. See the class javadoc
     * for the resulting descendant bound.
     */
    private void split() {
        if (this.level().isClientSide) {
            return;
        }
        this.splitsRemaining--;
        double healthFraction = MobConfigHelper.get(this).number("splitHealthFraction", DEFAULT_SPLIT_HEALTH_FRACTION);

        Entity spawned = this.getType().create(this.level());
        if (spawned instanceof BlackPudding child) {
            child.setGeneration(this.getGeneration() + 1);
            // create() skips finalizeSpawn, so the child's budget has to be filled here
            child.splitsRemaining = maxSplits();
            double newMaxHealth = Math.max(MIN_CHILD_HEALTH, this.getMaxHealth() * healthFraction);
            AttributeInstance healthAttribute = child.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(newMaxHealth);
            }
            child.setHealth((float) newMaxHealth);
            double offsetX = (this.random.nextDouble() - 0.5D) * 0.8D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.8D;
            child.moveTo(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ, this.random.nextFloat() * 360.0F, 0.0F);
            if (this.getTarget() != null) {
                child.setTarget(this.getTarget());
            }
            this.level().addFreshEntity(child);
        }

        this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.6F + this.random.nextFloat() * 0.3F);
        ((ServerLevel) this.level()).sendParticles(ParticleTypes.ITEM_SLIME,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                12, this.getBbWidth() * 0.3D, this.getBbHeight() * 0.3D, this.getBbWidth() * 0.3D, 0.05D);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide && this.clientLashTicks > 0) {
            this.clientLashTicks--;
        }
        if (this.level().isClientSide && this.tickCount % 4 == 0 && isMoving()) {
            this.level().addParticle(GMMParticles.SLIME_TRAIL.get(),
                    this.getRandomX(0.4D),
                    this.getY() + 0.02D,
                    this.getRandomZ(0.4D),
                    TRAIL_R, TRAIL_G, TRAIL_B);
        }
        super.aiStep();
    }

    /** See {@code GelatinousCube#isMoving} — same client-safe walkAnimation-based check. */
    private boolean isMoving() {
        return this.onGround() && this.walkAnimation.isMoving();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Generation", this.getGeneration());
        tag.putInt("SplitsRemaining", this.splitsRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Generation")) {
            setGeneration(tag.getInt("Generation"));
        }
        if (tag.contains("SplitsRemaining")) {
            this.splitsRemaining = tag.getInt("SplitsRemaining");
        }
    }
}
