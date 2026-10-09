package mod.gottsch.neo.gmm.core.entity.monster.mimic;

import mod.gottsch.neo.gmm.core.entity.ai.goal.GatedGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Ported from Treasure2's {@code Mimic} — an ambush "monster" disguised as loot furniture, reworked
 * to stay disguised indefinitely (Treasure2's original auto-opened its lid a fixed delay after spawn
 * regardless of whether anyone was around). This version stays fully closed and pinned in place until
 * a player either hits it ({@link #hurt}) or interacts with it as if it were the container it's
 * disguised as ({@link #mobInteract}) — {@link #activate()} then flips {@link #isActive()}
 * *instantly* (same "getting struck should never feel unresponsive" philosophy as Gray Ooze's reveal),
 * but the lid itself still plays out a ~1s opening transition rather than popping straight to fully
 * open — see {@link #getOpenProgress()} and {@code MimicModel}.
 * <p>
 * Every AI goal — including look-around and float, not just movement/attack — is wrapped with
 * {@link #gated(Goal)} so it flat-out cannot run while {@link #isActive()} is false. Earlier this only
 * gated the stroll/attack goals, which left {@code RandomLookAroundGoal} free to swivel the head (and,
 * via vanilla's body-follows-head catch-up, the whole model, since this rig has no separate head part)
 * and {@code FloatGoal} free to bob it in any water at its feet — both broke the "sits like an inert
 * block" illusion. {@link #hasTarget()} is exposed purely for the model's idle "chomp" animation (wider
 * when it has a target) — see {@code MimicModel}.
 * <p>
 * Also added (not in Treasure2's original) so the disguise actually reads as a placed block rather than
 * a mob that happens to be closed: {@link #finalizeSpawn} centers it in its block and snaps its facing
 * to a cardinal direction (reusing Gray Ooze's {@code snapToCardinalFacing} idea — a real chest/barrel
 * is always axis-aligned and block-centered, never at an arbitrary sub-block position or angle).
 * <p>
 * {@link #setLootTable} lets a consumer assign an individual spawned Mimic its own loot table — the
 * same idea as Treasure2's {@code Mimic.setLootTable}, which existed so the code swapping a specific
 * placed chest/barrel for its mimic could hand it that container's own loot rather than one fixed table
 * per entity class (see {@link BarrelMimic}'s class doc on rarity-tiered stats for the same underlying
 * reason). Treasure2's version needed reflection (a {@code Mob.lootTable} SRG hack pinned to 1.18.2's
 * field name, likely broken by 1.20.1) because that field is {@code private} with no vanilla setter.
 * This reimplements it without reflection: {@code Mob.getLootTable()} is {@code public final} but falls
 * back to {@code getDefaultLootTable()} (protected, overridable) whenever the private field is unset —
 * which it always is here, since nothing ever touches it — so overriding that hook is enough.
 * <p>
 * Treasure2's original also carried a commented-out gradual open/close "amount" system, which wasn't
 * live code either, so it's dropped here rather than ported forward.
 *
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's Mimic
 */
public abstract class Mimic extends GMMMonster {

    /** ~1s at 20 tps: how long the lid takes to swing fully open once activated. */
    private static final float OPEN_STEP = 0.05F;

    /**
     * Ambient sound: defaults to GMM's own {@link GMMSounds#MIMIC_AMBIENT} (shipped with the mob),
     * overridable by a consumer that wants its own sound; set to null to silence.
     */
    public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.MIMIC_AMBIENT.get();

    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(Mimic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_TARGET =
            SynchedEntityData.defineId(Mimic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_OPEN_PROGRESS =
            SynchedEntityData.defineId(Mimic.class, EntityDataSerializers.FLOAT);

    private static final String TAG_LOOT_TABLE = "MimicLootTable";

    /** Server-only, per-instance loot table override — see this class's doc for why. Null = use the
     * entity type's normal registered table ({@code data/<namespace>/loot_tables/entities/<id>.json}). */
    @Nullable
    private ResourceLocation lootTable;

    protected Mimic(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Assigns this specific Mimic instance its own loot table, overriding the entity type's normally
     * registered one. Pass {@code null} to clear the override and go back to the type default.
     */
    public void setLootTable(@Nullable ResourceLocation lootTable) {
        this.lootTable = lootTable;
    }

    /** The raw override, or null if none was set — for reading back what was assigned. To get the
     * *effective* loot table (override or type default), use the inherited {@code getLootTable()}. */
    @Nullable
    public ResourceLocation getLootTableOverride() {
        return this.lootTable;
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return this.lootTable != null ? this.lootTable : super.getDefaultLootTable();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, gated(new FloatGoal(this)));
        this.goalSelector.addGoal(1, gated(new MeleeAttackGoal(this, 1.0D, false)));
        this.goalSelector.addGoal(5, gated(new WaterAvoidingRandomStrollGoal(this, 1.0D)));
        this.goalSelector.addGoal(6, gated(new RandomLookAroundGoal(this)));

        this.targetSelector.addGoal(1, gated(new SummonedOwnerTargetGoal(this)));
        this.targetSelector.addGoal(2, gated(new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, gated(new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner)));
    }

    /** Wraps any goal so it can't {@code canUse()}/{@code canContinueToUse()} while still disguised —
     * a thin convenience around the shared {@link GatedGoal} (extracted from this class once
     * {@code AnimatedArmor} needed the identical "every goal is inert until a flag flips" shape). */
    private Goal gated(Goal delegate) {
        return new GatedGoal(delegate, this::isActive);
    }

    /**
     * Centers this Mimic in its block and snaps its facing to a cardinal direction (0/90/180/270), so
     * it sits exactly like the chest/barrel/etc it's disguised as before its lid ever opens. Runs on
     * every spawn path (natural, spawn egg, {@code /summon}) since they all call finalizeSpawn.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        snapToBlockPose();
        return spawnGroupData;
    }

    private void snapToBlockPose() {
        BlockPos pos = this.blockPosition();
        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

        float cardinal = Math.round(this.getYRot() / 90.0F) * 90.0F;
        this.setYRot(cardinal);
        this.setYBodyRot(cardinal);
        this.setYHeadRot(cardinal);
        this.yRotO = cardinal;
        this.yBodyRotO = cardinal;
        this.yHeadRotO = cardinal;
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE, false);
        this.entityData.define(DATA_HAS_TARGET, false);
        this.entityData.define(DATA_OPEN_PROGRESS, 0.0F);
    }

    /** Persists the loot table override (unrelated to vanilla's own "DeathLootTable" tag, which this
     * class never uses since the private field it targets is intentionally left untouched). */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.lootTable != null) {
            tag.putString(TAG_LOOT_TABLE, this.lootTable.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_LOOT_TABLE)) {
            this.lootTable = ResourceLocation.tryParse(tag.getString(TAG_LOOT_TABLE));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setHasTarget(this.getTarget() != null);
        if (isActive() && getOpenProgress() < 1.0F) {
            setOpenProgress(Math.min(1.0F, getOpenProgress() + OPEN_STEP));
        }
    }

    /** Whether this Mimic has blown its cover — closed/pinned/immobile while false. */
    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    /**
     * 0 (still fully closed) to 1 (lid fully open) — ramps up over {@link #OPEN_STEP} once
     * {@link #isActive()}, driving {@code MimicModel}'s opening transition.
     */
    public float getOpenProgress() {
        return this.entityData.get(DATA_OPEN_PROGRESS);
    }

    public void setOpenProgress(float progress) {
        this.entityData.set(DATA_OPEN_PROGRESS, progress);
    }

    /**
     * Blows this Mimic's cover the instant it's called — it starts moving/attacking and its lid
     * begins swinging open right away, but the open swing itself still plays out over time rather
     * than snapping straight to fully open, see {@link #getOpenProgress()}.
     */
    public void activate() {
        setActive(true);
    }

    /** A punch/attack always blows its cover — a disguise shouldn't feel unresponsive. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            activate();
        }
        return hurt;
    }

    /** Right-clicking it like the container it's disguised as also blows its cover. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isActive()) {
            if (!this.level().isClientSide) {
                activate();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Drives the idle "chomp" width in {@code MimicModel#activeAnim} — cosmetic only. */
    public boolean hasTarget() {
        return this.entityData.get(DATA_HAS_TARGET);
    }

    public void setHasTarget(boolean hasTarget) {
        this.entityData.set(DATA_HAS_TARGET, hasTarget);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WOODEN_DOOR_OPEN;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHEST_CLOSE;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ambientSound != null ? ambientSound.get() : null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHEST_LOCKED, 0.15F, 1.0F);
    }

    @Override
    public void playAmbientSound() {
        SoundEvent sound = getAmbientSound();
        if (sound != null) {
            this.playSound(sound, 0.10F, 0.80F);
        }
    }

}
