package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A slow-moving ooze. Unlike vanilla {@code Slime}, it never overrides {@code moveControl} with a
 * jump-based controller and adds no jump goals, so it glides along the ground on normal pathfinding
 * navigation instead of hopping. It also doesn't override {@code remove()} to spawn smaller copies of
 * itself on death, so it never splits. It has a fixed size (smaller than a full-sized/"Big" vanilla
 * Slime) rather than Slime's variable size mechanic, since there's nothing for it to split into — but
 * that fixed size is scaled by an optional {@code gmm:mob_config} {@code properties.size} multiplier
 * (default {@code 1.0}, baked in once at spawn and synced — see {@link #finalizeSpawn}), the same
 * opt-in-override pattern as {@link GMMMonster}'s vanilla-attribute overrides.
 * <p>
 * On a successful hit it "engulfs" the target: an instant yank to a stop (zeroed horizontal velocity),
 * then a genuine brief hold — {@link GMMMobEffects#PARALYZED} (the same full-movement-root effect
 * {@code ParalysisSpell} uses, see {@code holdDuration}) — so the target actually can't move for a
 * moment, not just a single-tick zero. Vanilla Slowness (a longer, lingering sluggishness) and vanilla
 * Poison (standing in for an acid damage-over-time, the same vanilla-effect-as-flavor approach
 * {@code ParalysisSpell}/{@code Bloater} already use) both start on the same hit but outlast the hold,
 * so once {@code PARALYZED} expires the target reads as "wriggling free" — slowed, not fully stuck —
 * for the remainder. A conservative gear-durability nibble is a separate, independently toggleable
 * flourish on top, reusing {@code AcidSkeleton}'s corrosion pattern (and its shared
 * {@link GMMTags.Items#CORROSION_IMMUNE} tag) almost verbatim.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class GelatinousCube extends GMMMonster {

    // matches the amber/gold hue-shifted recolor used by GelatinousCubeRenderer's texture
    private static final float TRAIL_R = 0.71F;
    private static final float TRAIL_G = 0.59F;
    private static final float TRAIL_B = 0.39F;

    private static final int DEFAULT_HOLD_DURATION = 20;       // 1s — the genuine "held" window
    private static final int DEFAULT_SLOWNESS_DURATION = 100;  // 5s — outlasts the hold ("wriggling free")
    private static final int DEFAULT_SLOWNESS_AMPLIFIER = 1;   // Slowness II
    private static final int DEFAULT_ACID_DURATION = 60;       // 3s
    private static final int DEFAULT_ACID_AMPLIFIER = 0;       // Poison I
    private static final int DEFAULT_CORROSION = 8;            // conservative — half AcidSkeleton's
    // twice vanilla MeleeAttackGoal's own 20-tick cadence — a lumbering ooze shouldn't hit every second
    private static final int DEFAULT_ATTACK_COOLDOWN = 40;

    private static final EntityDataAccessor<Float> DATA_SIZE_SCALE =
            SynchedEntityData.defineId(GelatinousCube.class, EntityDataSerializers.FLOAT);

    // sentinel comfortably before tick 0, so the very first attack is never blocked by the cooldown
    private int lastAttackTick = -1000;

    public GelatinousCube(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SIZE_SCALE, 1.0F);
    }

    /** Reads the optional {@code size} config multiplier once at spawn, applied on top of the code default. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        double size = MobConfigHelper.get(this).number("size", 1.0D);
        setSizeScale(size > 0.0D ? (float) size : 1.0F);
        this.refreshDimensions();
        return spawnGroupData;
    }

    public float getSizeScale() {
        return this.entityData.get(DATA_SIZE_SCALE);
    }

    public void setSizeScale(float scale) {
        this.entityData.set(DATA_SIZE_SCALE, scale);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getSizeScale());
    }

    @Override
    public void refreshDimensions() {
        // preserve position across the dimension change (a taller/wider hitbox otherwise re-centers
        // and can shift the entity) — same fix vanilla's own Slime applies for its size mechanic.
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        super.refreshDimensions();
        this.setPos(x, y, z);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SizeScale", getSizeScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SizeScale")) {
            setSizeScale(tag.getFloat("SizeScale"));
        }
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide && this.tickCount % 5 == 0 && isMoving()) {
            this.level().addParticle(GMMParticles.SLIME_TRAIL.get(),
                    this.getRandomX(0.4D),
                    this.getY() + 0.02D,
                    this.getRandomZ(0.4D),
                    TRAIL_R, TRAIL_G, TRAIL_B);
        }
        super.aiStep();
    }

    /**
     * Only lays a trail while actually oozing along — not while standing still or falling.
     * {@code getDeltaMovement()} isn't reliable here: this entity type opts out of velocity sync
     * ({@code setShouldReceiveVelocityUpdates(false)} in {@code ModEntities}), so on the client (where
     * this is checked — see {@link #aiStep()}) it stays near zero. {@code walkAnimation} is computed
     * from actual position deltas each tick on both sides (see {@code LivingEntity#travel}), so it's
     * the correct client-safe "is this mob visibly moving" signal.
     */
    private boolean isMoving() {
        return this.onGround() && this.walkAnimation.isMoving();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                // snail-slow — noticeably slower than the roster's other melee mobs
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
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
        // a soft squelch instead of a footstep clatter
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15F, 1.0F);
    }

    /**
     * Vanilla {@code MeleeAttackGoal} hardcodes its own cooldown to 20 ticks (its {@code
     * ticksUntilNextAttack} field is private, so subclassing it can't change that) and calls
     * {@code swing()} + {@code doHurtTarget()} unconditionally every time it's off that internal
     * cooldown. Rather than reimplementing the whole goal, this gates here instead: an attempt within
     * {@code attackCooldown} ticks of the last actual hit is simply a no-op (no damage, no engulf, no
     * sound) — invisible for this mob since {@code SlimeModel} has no arm to swing anyway.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        int cooldown = (int) MobConfigHelper.get(this).number("attackCooldown", DEFAULT_ATTACK_COOLDOWN);
        if (this.tickCount - this.lastAttackTick < cooldown) {
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            this.lastAttackTick = this.tickCount;
            this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            if (target instanceof LivingEntity living) {
                engulf(living);
            }
        }
        return hurt;
    }

    /**
     * "Engulfs" a hit target: an instant yank to a stop, a brief genuine hold ({@code holdDuration}
     * ticks of {@link GMMMobEffects#PARALYZED}), then a longer, lingering Slowness + acid DoT that
     * outlasts the hold, and an optional gear nibble.
     */
    private void engulf(LivingEntity target) {
        if (!MobConfigHelper.get(this).flag("engulf", true)) {
            return;
        }
        // an instant yank to a stop, so no residual momentum carries the target out of the hold below
        target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);

        int holdDuration = (int) MobConfigHelper.get(this).number("holdDuration", DEFAULT_HOLD_DURATION);
        if (holdDuration > 0) {
            target.addEffect(new MobEffectInstance(GMMMobEffects.PARALYZED.get(), holdDuration, 0, false, true, true), this);
        }

        int slownessDuration = (int) MobConfigHelper.get(this).number("slownessDuration", DEFAULT_SLOWNESS_DURATION);
        int slownessAmplifier = (int) MobConfigHelper.get(this).number("slownessAmplifier", DEFAULT_SLOWNESS_AMPLIFIER);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slownessDuration, slownessAmplifier), this);

        int acidDuration = (int) MobConfigHelper.get(this).number("acidDuration", DEFAULT_ACID_DURATION);
        int acidAmplifier = (int) MobConfigHelper.get(this).number("acidAmplifier", DEFAULT_ACID_AMPLIFIER);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, acidDuration, acidAmplifier), this);

        corrodeEquipment(target);
    }

    /** Strips durability from one random damageable equipped item (armor or hand) — same pattern as AcidSkeleton. */
    private void corrodeEquipment(LivingEntity target) {
        if (!MobConfigHelper.get(this).flag("corrodeGear", true)) {
            return;
        }
        // creative players are immune to gear damage
        if (target instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        List<EquipmentSlot> damageable = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            // skip empty, non-damageable, and corrosion-immune (e.g. diamond) gear
            if (!stack.isEmpty() && stack.isDamageableItem()
                    && !stack.is(GMMTags.Items.CORROSION_IMMUNE)) {
                damageable.add(slot);
            }
        }
        if (damageable.isEmpty()) {
            return;
        }
        EquipmentSlot slot = damageable.get(this.random.nextInt(damageable.size()));
        int corrosion = (int) MobConfigHelper.get(this).number("corrosion", DEFAULT_CORROSION);
        target.getItemBySlot(slot).hurtAndBreak(corrosion, target, e -> e.broadcastBreakEvent(slot));
    }
}
