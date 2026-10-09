package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.forge.gmm.core.particle.GMMParticles;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An ambush ooze, "camouflaged as wet stone" until disturbed (see the {@code MobIdeasCatalog}'s "Gray
 * Ooze" entry). It spawns fully disguised — a synced {@link #revealProgress} float (0 = hidden, 1 =
 * revealed) drives its render ({@code GrayOozeRenderer}: squashed to barely poke above the ground
 * (less than a pressure plate) and textured with a real crop of vanilla's own {@code stone.png} while
 * hidden; normal ooze proportions/texture once revealed) and which goals are even allowed to run
 * ({@link #isAwake()} gates movement/attack, mirroring {@link Boulder}'s dormant/active goal-gating,
 * though the visual approach here is a scale/texture interpolation rather than Boulder's full model
 * morph, since {@code SlimeModel}'s simple box-in-a-box geometry doesn't have limbs to fold). It also
 * snaps its own facing to the nearest cardinal direction whenever it (re-)settles into the fully hidden
 * state ({@link #snapToCardinalFacing()}) — a real stone block is always axis-aligned, so sitting at an
 * arbitrary angle would give it away even with the right texture and squash. It reveals the moment a
 * player gets within {@code revealRange} or it's hit (instantly, not gradually — getting struck should
 * never feel unresponsive), and re-hides after {@code quietTicks} of nobody nearby and no target.
 * <p>
 * Its attack is unlike every other ooze in the roster: <strong>it deals zero HP damage</strong>. It
 * exists purely to corrode gear — a single heavy durability hit (config {@code corrosion}, default 40 —
 * much larger than Acid Skeleton/Gelatinous Cube's incidental nibble, since here it's the whole point)
 * to one random damageable, non-{@link GMMTags.Items#CORROSION_IMMUNE} equipped item, throttled by its
 * own {@code attackCooldown} the same way {@link GelatinousCube}/{@link OchreJelly} throttle theirs.
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class GrayOoze extends GMMMonster {

    private static final float DEFAULT_REVEAL_RANGE = 3.0F;
    private static final int DEFAULT_QUIET_TICKS = 100; // 5s of nothing nearby before it re-hides
    private static final float REVEAL_STEP = 0.05F;      // ~1s for a full reveal/re-hide transition

    private static final int DEFAULT_CORROSION = 40;
    // slower than the other oozes' 40-tick throttle — a single heavy, deliberate strike, not a flurry
    private static final int DEFAULT_ATTACK_COOLDOWN = 60;

    private static final EntityDataAccessor<Float> DATA_REVEAL_PROGRESS =
            SynchedEntityData.defineId(GrayOoze.class, EntityDataSerializers.FLOAT);

    // server-only bookkeeping
    private int quietTicks = 0;
    private int lastAttackTick = -1000;

    public GrayOoze(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REVEAL_PROGRESS, 0.0F);
    }

    /** Spawns already hidden, so it should already be facing a cardinal direction — see {@link #snapToCardinalFacing()}. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        snapToCardinalFacing();
        return spawnGroupData;
    }

    /**
     * Rounds this entity's yaw to the nearest cardinal direction (0/90/180/270) and resets the
     * previous-tick rotation fields to match, so it doesn't visibly spin into place. A real stone block
     * is always axis-aligned; sitting at an arbitrary angle would be an obvious tell even with the
     * right texture and squash.
     */
    private void snapToCardinalFacing() {
        float cardinal = Math.round(this.getYRot() / 90.0F) * 90.0F;
        this.setYRot(cardinal);
        this.setYBodyRot(cardinal);
        this.setYHeadRot(cardinal);
        this.yRotO = cardinal;
        this.yBodyRotO = cardinal;
        this.yHeadRotO = cardinal;
    }

    public float getRevealProgress() {
        return this.entityData.get(DATA_REVEAL_PROGRESS);
    }

    public void setRevealProgress(float progress) {
        this.entityData.set(DATA_REVEAL_PROGRESS, progress);
    }

    /** Fully revealed and allowed to move/attack — see the movement/attack goal wrappers below. */
    public boolean isAwake() {
        return getRevealProgress() >= 1.0F;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!MobConfigHelper.get(this).flag("camouflage", true)) {
            setRevealProgress(1.0F);
            return;
        }

        float revealRange = (float) MobConfigHelper.get(this).number("revealRange", DEFAULT_REVEAL_RANGE);
        boolean playerNearby = this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), revealRange);
        boolean shouldReveal = this.getTarget() != null || playerNearby;

        if (shouldReveal) {
            this.quietTicks = 0;
        } else {
            this.quietTicks++;
        }

        int quietThreshold = (int) MobConfigHelper.get(this).number("quietTicks", DEFAULT_QUIET_TICKS);
        float targetProgress = (shouldReveal || this.quietTicks < quietThreshold) ? 1.0F : 0.0F;
        float current = getRevealProgress();
        if (current < targetProgress) {
            setRevealProgress(Math.min(targetProgress, current + REVEAL_STEP));
        } else if (current > targetProgress) {
            float next = Math.max(targetProgress, current - REVEAL_STEP);
            setRevealProgress(next);
            // just finished re-hiding — square back up to look like a placed block again
            if (next <= 0.0F) {
                snapToCardinalFacing();
            }
        }
    }

    /** Being struck always blows its cover immediately — a disguise shouldn't feel unresponsive. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.quietTicks = 0;
            setRevealProgress(1.0F);
        }
        return hurt;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GrayOozeMeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new GrayOozeStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                // Still the slowest of the ooze family, and still an ambusher rather than a chaser
                // -- but 0.12 made it IGNORABLE (raised 0.12 -> 0.14, 2026-09-19). This mob deals
                // zero hp damage; its entire threat is the one corrosion hit. A player who simply
                // keeps walking never pays that cost, so the ambush lands once and the mob is
                // scenery. The family ladder is the constraint on how far this can go: Gelatinous
                // Cube 0.15, Ochre Jelly 0.18, Black Pudding 0.22 (under a Zombie's 0.23 on
                // purpose). The Gray Ooze has to stay under the Cube to keep "slowest, an ambusher"
                // true, so 0.14 is the whole budget available without re-ordering the roster.
                .add(Attributes.MOVEMENT_SPEED, 0.14D);
        // no ATTACK_DAMAGE override: its attack deals zero HP damage, see doHurtTarget
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
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15F, 1.0F);
    }

    /**
     * Unlike every other ooze here, this never calls {@code target.hurt(...)} — the attack is purely a
     * heavy, throttled gear-corrosion touch with zero HP damage. Since that means no vanilla hurt
     * sound/red-flash ever happens, a successful corrosion instead gets its own sound + a burst of
     * stone-crumble particles at the target — otherwise there'd be no feedback at all that anything
     * happened.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        int cooldown = (int) MobConfigHelper.get(this).number("attackCooldown", DEFAULT_ATTACK_COOLDOWN);
        if (this.tickCount - this.lastAttackTick < cooldown) {
            return false;
        }
        this.lastAttackTick = this.tickCount;
        this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, 0.6F + this.random.nextFloat() * 0.2F);
        if (target instanceof LivingEntity living && corrodeEquipment(living)) {
            this.playSound(SoundEvents.STONE_BREAK, 0.6F, 0.8F + this.random.nextFloat() * 0.2F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        12, living.getBbWidth() * 0.3D, living.getBbHeight() * 0.3D, living.getBbWidth() * 0.3D, 0.05D);
            }
        }
        return true;
    }

    /** Strips heavy durability from one random damageable equipped item — no HP damage alongside it.
     * Returns whether it actually found something to corrode. */
    private boolean corrodeEquipment(LivingEntity target) {
        // creative players are immune to gear damage
        if (target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        List<EquipmentSlot> damageable = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem() && !stack.is(GMMTags.Items.CORROSION_IMMUNE)) {
                damageable.add(slot);
            }
        }
        if (damageable.isEmpty()) {
            return false;
        }
        EquipmentSlot slot = damageable.get(this.random.nextInt(damageable.size()));
        int corrosion = (int) MobConfigHelper.get(this).number("corrosion", DEFAULT_CORROSION);
        target.getItemBySlot(slot).hurtAndBreak(corrosion, target, e -> e.broadcastBreakEvent(slot));
        return true;
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide && this.tickCount % 5 == 0 && isMoving()) {
            this.level().addParticle(GMMParticles.SLIME_TRAIL.get(),
                    this.getRandomX(0.4D),
                    this.getY() + 0.02D,
                    this.getRandomZ(0.4D),
                    0.55F, 0.55F, 0.55F);
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
        tag.putFloat("RevealProgress", getRevealProgress());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RevealProgress")) {
            setRevealProgress(tag.getFloat("RevealProgress"));
        }
    }

    /** Only wanders once revealed — wandering while "dormant" would give the disguise away. */
    public static class GrayOozeStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final GrayOoze grayOoze;

        public GrayOozeStrollGoal(GrayOoze grayOoze, double speedModifier) {
            super(grayOoze, speedModifier);
            this.grayOoze = grayOoze;
        }

        @Override
        public boolean canUse() {
            return this.grayOoze.isAwake() && super.canUse();
        }
    }

    /** Only attacks once revealed — see {@link GrayOoze#isAwake()}. */
    public static class GrayOozeMeleeAttackGoal extends MeleeAttackGoal {
        private final GrayOoze grayOoze;

        public GrayOozeMeleeAttackGoal(GrayOoze grayOoze, double speedModifier, boolean followEvenIfNotSeen) {
            super(grayOoze, speedModifier, followEvenIfNotSeen);
            this.grayOoze = grayOoze;
        }

        @Override
        public boolean canUse() {
            return this.grayOoze.isAwake() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.grayOoze.isAwake() && super.canContinueToUse();
        }
    }
}
