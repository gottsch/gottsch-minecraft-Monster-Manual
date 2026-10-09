package mod.gottsch.neo.gmm.core.entity.monster.plant;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.AllyAlertUtil;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * The Shrieker: D&amp;D 5e's immobile fungus alarm. It never fights -- it's a pure force multiplier,
 * per the catalog's "Family: Plants" entry (the first mob built there; {@code VioletFungus} and
 * {@code Reaper} follow). Permanently rooted ({@code setNoAi(true)} at spawn, the same "drive periodic
 * logic from {@code tick()}, not {@code customServerAiStep()}" idiom {@code GraveZombie}/{@code BloodyBones}
 * established, since {@code setNoAi(true)} suppresses {@code customServerAiStep} entirely), it periodically
 * scans for a nearby non-creative/non-spectator player and, on finding one (subject to a cooldown and an
 * optional lifetime cap), pulses: alerts nearby GMM allies to the intruder via the shared
 * {@link AllyAlertUtil} (new this session, factored out of {@code AllyAlertNearestAttackableTargetGoal}/
 * {@code AllyAlertHurtByTargetGoal} so this non-goal, non-combat caller doesn't need a third copy of the
 * same scan), applies a capped-intensity darkness to whoever triggered it (see
 * {@link GMMMobEffects#SHRIEKER_DARKNESS} -- a marker effect, not vanilla {@code MobEffects.DARKNESS},
 * because vanilla Darkness always peaks at full screen opacity with no partial-intensity control; the
 * marker lets {@code core.client.ShriekerDarknessOverlay} cap the actual drawn opacity instead), and
 * plays vanilla's own Sculk Shrieker shriek ({@code SoundEvents.SCULK_SHRIEKER_SHRIEK} -- a literal,
 * zero-new-asset "shrieker" sound already in the game). The pulse also drives a client-visible tell:
 * {@link #isAlerting()} flips on for
 * {@code swayDurationTicks}, which {@code ShriekerModel} reads to sway its stalks (pivoted at their base,
 * per the design discussion this session) and which drives a {@code SPORE_BLOSSOM_AIR} particle puff from
 * {@link #aiStep()} -- both purely cosmetic, zero new particle assets.
 *
 * <p>Every part of the alert cycle is {@code gmm:mob_config}-driven, per the user's explicit ask:
 * {@code alertRange} (proximity that triggers detection), {@code broadcastRange} (how far the alert
 * reaches allies -- deliberately separate from {@code alertRange}, since "how close must a player get"
 * and "how far does the alarm carry" are different questions), {@code alertCooldownTicks} (minimum gap
 * between pulses -- the "frequency" ask), {@code maxAlerts} (lifetime pulse cap; negative = unlimited,
 * the default -- a permanent dungeon alarm should keep alarming unless a consumer opts to silence it after
 * N triggers), {@code swayDurationTicks} (how long the visual tell plays per pulse), and
 * {@code darknessSeconds}/{@code darknessOpacity} (the latter a 0..1 fraction, encoded into the applied
 * effect's amplifier so {@code ShriekerDarknessOverlay} can decode it client-side -- see
 * {@link #triggerAlert}). {@code alertsAllies} (flag, default true) lets a consumer disable the
 * ally-broadcast half and use a Shrieker as pure atmosphere/telegraph with no mechanical effect.
 *
 * <p>Fragile by design, matching the real 5e stat block (13 HP, AC 5, speed 0, no attack of its own) --
 * scaled down slightly for MC's lower baseline rather than the zombie-family 20/22 ratio, which is a
 * conversion specific to that family's own stat blocks, not a universal D&amp;D-to-MC factor.
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class Shrieker extends GMMMonster {

    private static final double DEFAULT_MAX_HEALTH = 8.0D;

    private static final double DEFAULT_ALERT_RANGE = 6.0D;
    private static final double DEFAULT_BROADCAST_RANGE = 16.0D;
    private static final int DEFAULT_ALERT_COOLDOWN_TICKS = 200;   // ~10s between pulses
    private static final int DEFAULT_MAX_ALERTS = -1;              // negative = unlimited
    private static final int DEFAULT_SWAY_DURATION_TICKS = 40;     // ~2s of stalk sway + spores
    // Pure animation-feel constant, not gmm:mob_config-driven -- same category as ShriekerModel's own
    // SWAY_SPEED/SWAY_AMOUNT (client-side rendering polish, not a value a consumer would rebalance
    // gameplay around). See feedback_default_to_configurable memory for the line this project draws
    // between "gameplay-affecting" (config-driven) and "pure animation feel" (hardcoded) constants.
    private static final int SWAY_EASE_OUT_TICKS = 15;              // ~0.75s to settle back to rest
    private static final int DEFAULT_DARKNESS_SECONDS = 5;
    private static final double DEFAULT_DARKNESS_OPACITY = 0.92D;  // "90-95% dark," never a true blackout

    private static final int SCAN_INTERVAL_TICKS = 10;

    private static final EntityDataAccessor<Boolean> DATA_ALERTING =
            SynchedEntityData.defineId(Shrieker.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SWAY_INTENSITY =
            SynchedEntityData.defineId(Shrieker.class, EntityDataSerializers.FLOAT);

    private static final String TAG_ALERT_COUNT = "AlertCount";

    private int alertCount;
    private int alertCooldownTicks;
    private int swayTicksRemaining;
    private int swayEaseTicksRemaining;

    public Shrieker(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ALERTING, false);
        this.entityData.define(DATA_SWAY_INTENSITY, 0.0F);
    }

    /** Permanently rooted -- no goals, no pathing, ever. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        this.setNoAi(true);
        return spawnGroupData;
    }


    /**
     * Rooted means rooted: nothing shoves it off its square.
     *
     * <p>{@code setNoAi(true)} and {@code MOVEMENT_SPEED 0} between them only stop it moving itself.
     * Neither touches <em>physics</em>: entity-vs-entity collision runs through
     * {@code EntitySelector.pushableBy}, which asks {@link #isPushable()}, so without this a player
     * walking into one slides it across the floor like a boat. Vanilla's {@code ArmorStand}
     * overrides exactly this, for exactly this reason.</p>
     *
     * <p>Deliberately not datapack-driven, unlike the attributes: "is this thing a plant" is a
     * property of the mob rather than a tuning knob, and a pushable plant is a bug in any pack.</p>
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                // The other half of "cannot be moved". isPushable stops something walking into it;
                // this stops a HIT sliding it, which is the same visual bug arriving by a different
                // route -- and the likelier one, since these are monsters and get attacked.
                // A knob rather than a rule, unlike isPushable: GMMMonster#applyConfigAttributes
                // overwrites this from a mob_config's "knockbackResistance" when a pack sets one,
                // so a pack that wants its fungi to skid can have them.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    // --- codec accessors -------------------------------------------------------------------------

    private double alertRange() {
        return MobConfigHelper.get(this).number("alertRange", DEFAULT_ALERT_RANGE);
    }

    private double broadcastRange() {
        return MobConfigHelper.get(this).number("broadcastRange", DEFAULT_BROADCAST_RANGE);
    }

    private int alertCooldownTicksConfig() {
        return (int) MobConfigHelper.get(this).number("alertCooldownTicks", DEFAULT_ALERT_COOLDOWN_TICKS);
    }

    private int maxAlerts() {
        return (int) MobConfigHelper.get(this).number("maxAlerts", DEFAULT_MAX_ALERTS);
    }

    private int swayDurationTicks() {
        return (int) MobConfigHelper.get(this).number("swayDurationTicks", DEFAULT_SWAY_DURATION_TICKS);
    }

    private int darknessSeconds() {
        return (int) MobConfigHelper.get(this).number("darknessSeconds", DEFAULT_DARKNESS_SECONDS);
    }

    /** 0..1 fraction, encoded into the applied effect's amplifier -- see {@link #triggerAlert}. */
    private double darknessOpacity() {
        return MobConfigHelper.get(this).number("darknessOpacity", DEFAULT_DARKNESS_OPACITY);
    }

    private boolean alertsAllies() {
        return MobConfigHelper.get(this).flag("alertsAllies", true);
    }

    // --- alerting state (synced -- read by ShriekerModel for the stalk-sway telegraph) -------------

    /** True for the active pulse window ({@code swayDurationTicks}) -- gates the spore particle puff. */
    public boolean isAlerting() {
        return this.entityData.get(DATA_ALERTING);
    }

    private void setAlerting(boolean alerting) {
        this.entityData.set(DATA_ALERTING, alerting);
    }

    /**
     * 0..1 sway amplitude scale -- 1.0 for the whole active pulse window, then eased linearly down to 0
     * over {@code SWAY_EASE_OUT_TICKS} once the pulse ends, instead of snapping straight back to the base
     * pose. {@code ShriekerModel} multiplies its oscillation by this value every frame (an unconditional
     * {@code base + oscillation * intensity}, no branch needed) so the stalks visibly settle to rest
     * rather than cutting instantly -- the "decay, not hard reset" idiom {@code Bodak}'s own gaze-charge
     * fix already established, applied here to a return-to-rest instead of a charge-up.
     */
    public float getSwayIntensity() {
        return this.entityData.get(DATA_SWAY_INTENSITY);
    }

    private void setSwayIntensity(float intensity) {
        this.entityData.set(DATA_SWAY_INTENSITY, intensity);
    }

    // --- alert cycle -------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        // server-authoritative only: entityData must never be written from the client-side copy of this
        // entity, and swayTicksRemaining/alertCooldownTicks are plain (unsynced) fields that only the
        // server ever advances -- the client just reads isAlerting() each frame via the synced boolean.
        if (this.level().isClientSide) {
            return;
        }
        if (swayTicksRemaining > 0) {
            swayTicksRemaining--;
            setSwayIntensity(1.0F);
            if (swayTicksRemaining == 0) {
                setAlerting(false);
                swayEaseTicksRemaining = SWAY_EASE_OUT_TICKS;
            }
        } else if (swayEaseTicksRemaining > 0) {
            swayEaseTicksRemaining--;
            setSwayIntensity(swayEaseTicksRemaining / (float) SWAY_EASE_OUT_TICKS);
        }
        if (alertCooldownTicks > 0) {
            alertCooldownTicks--;
            return;
        }
        if (this.tickCount % SCAN_INTERVAL_TICKS != 0 || !canAlert()) {
            return;
        }
        Player player = this.level().getNearestPlayer(
                this.getX(), this.getY(), this.getZ(), alertRange(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player != null) {
            triggerAlert(player);
        }
    }

    private boolean canAlert() {
        int max = maxAlerts();
        return max < 0 || alertCount < max;
    }

    private void triggerAlert(Player target) {
        alertCount++;
        alertCooldownTicks = Math.max(1, alertCooldownTicksConfig());
        swayTicksRemaining = Math.max(1, swayDurationTicks());
        swayEaseTicksRemaining = 0; // any in-progress ease-out is superseded by the fresh full-intensity pulse
        setAlerting(true);
        setSwayIntensity(1.0F);

        if (alertsAllies()) {
            AllyAlertUtil.alertNearby(this, target, broadcastRange(), GMMTags.EntityTypes.SHRIEKER_ALLIES);
        }
        // GMMMobEffects.SHRIEKER_DARKNESS, not vanilla MobEffects.DARKNESS -- vanilla Darkness has no
        // partial-intensity control (its client shader always peaks at full opacity while active); a
        // dedicated marker effect lets ShriekerDarknessOverlay cap the actual screen opacity instead.
        // The desired opacity (mob_config-driven) rides along as the effect's amplifier -- 0..100,
        // decoded back to a 0..1 fraction client-side -- the same "repurpose an existing numeric field
        // to carry data" trick GMMParticles.SLIME_TRAIL already uses for its own RGB tint.
        int opacityAmplifier = Mth.clamp((int) Math.round(darknessOpacity() * 100.0D), 0, 100);
        target.addEffect(new MobEffectInstance(GMMMobEffects.SHRIEKER_DARKNESS.get(), darknessSeconds() * 20, opacityAmplifier));
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 4.0F, 1.0F);
    }

    /** Client-side spore puff while alerting -- purely cosmetic, mirrors {@code isAlerting()}. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && isAlerting()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                        this.getRandomX(0.7D), this.getY(0.7D) + this.random.nextDouble() * 0.5D, this.getRandomZ(0.7D),
                        0.0D, 0.01D, 0.0D);
            }
        }
    }

    // --- persistence (maxAlerts must survive a save/reload, same lesson GraveZombie's restrictTo hit) --

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_ALERT_COUNT, alertCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ALERT_COUNT)) {
            alertCount = tag.getInt(TAG_ALERT_COUNT);
        }
    }
}
