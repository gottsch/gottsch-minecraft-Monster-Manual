package mod.gottsch.neo.gmm.core.entity.monster.zombie;



import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.resources.ResourceLocation;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The Bodak: a gaunt, ashen undead whose gaze -- not its fists -- is the real threat. D&amp;D 5e's
 * Death Gaze relies on a per-turn "avert your eyes to avoid the save" choice that has no MC
 * equivalent, so it's reworked as a continuous windup instead of an instant roll: {@link #isLookingAtMe}
 * reimplements the spirit of vanilla {@code EnderMan#isLookingAtMe}'s look-vector/line-of-sight math
 * (that method is package-private, so this is a fresh implementation, not a call into it) but with a
 * wide, flat cone rather than Enderman's own razor-thin distance-scaled one -- see {@link #isLookingAtMe}
 * for why. A nearby (non-creative, non-spectator) player who keeps looking at the Bodak charges a
 * {@code gazeChargeTicks} counter that bleeds off gradually (not an instant reset) when they look away,
 * break line of sight, leave range, or wear a warding helmet ({@link GMMTags.Items#BODAK_GAZE_WARD},
 * defaulting to a carved pumpkin -- the same item vanilla already uses to ward off Enderman aggro).
 * Reaching full charge deals heavy damage, with a rare "near-lethal" branch that drops the target to 1 HP
 * -- never a true instant kill by default, unlike the literal 5e text, unless {@code nearLethalAllowKill}
 * opts a server into that harsher behavior.
 *
 * <p>Sunlight is far worse for a Bodak than a plain zombie: on top of the normal sunburn fire tick it
 * takes a direct bonus hit of damage and, while burning, gets a short-lived movement-speed burst (a
 * toned-down Enderman-speed-boost idiom -- see {@code SUN_PANIC_SPEED_MODIFIER}) so it visibly panics
 * and scrambles for shade via the same vanilla {@code FleeSunGoal} every other GMM zombie-family sun-
 * sensitive mob already uses, just faster while it's active. This is an original addition, not in the
 * 5e stat block -- added so the sunlight weakness reads as a real behavior, not just a bigger DoT.
 *
 * <p>Texture is the {@code ash_zombie} palette originally authored for Wight, then reworked pale for
 * Wight's own final look -- the ashen-grey original was preserved rather than discarded and is reused
 * here verbatim ({@code textures/entity/bodak.png}, a copy of the leftover {@code ash_zombie.png}), on
 * the vanilla zombie rig via {@link mod.gottsch.neo.gmm.core.client.model.BodakModel} -- no new layer
 * definition, but a thin model subclass overlays a stepped head-roll jerk (see that class's doc) while
 * a gaze charge is building, the actual D&amp;D-described "jerky, puppet-like" movement in place of the
 * particle-based telegraph an earlier pass tried and the user asked to remove. An audio cue (vanilla
 * Enderman's own "creepy stare" sound) was tried and then removed again -- even played only once per
 * stare (a fix for an earlier stacking/desync bug), it still read as confusing rather than clarifying,
 * since it's the exact sound a real Enderman makes and players reasonably assumed one was nearby. The
 * head-jerk + ambient smoke are the sole telegraph now.
 *
 * <p>Withering Gaze is the ranged companion to melee-range Death Gaze: a plain {@link CastSpellGoal}
 * layered on top (declares no {@code Goal.Flag}s, so it never fights the other goals for control), no
 * look-direction requirement at all -- just line-of-sight + range, gated by {@code spellMinRange} so it
 * won't fire point-blank -- punishing a player who kites at distance instead of standing and staring it
 * down. The shared library owns no concrete spell projectile, so the consumer supplies
 * {@link #spellCaster} (e.g. wired to GMM's own {@code WitheringGazeSpell}), the same
 * consumer-supplied-static pattern {@code OrcShaman.spellCaster} already established. Uses
 * {@code CastSpellGoal}'s full constructor (added for this mob) to override its default charge
 * telegraph particle (plain {@code ParticleTypes.SMOKE} to match Bodak's own ashen visual language,
 * not the goal's default purple {@code WITCH} sparkle) and to give it a real {@code spellCooldownTime}
 * after each cast -- the goal has no cooldown by default (it can start recharging the instant a cast
 * completes), which read as firing too rapidly.
 *
 * <p>Aura of Annihilation (built 2026-07-12) folds directly into the Death Gaze windup rather than
 * running as its own toggle-driven ability: while a charge is building, any non-warded player within
 * {@link #auraRadius()} takes a small necrotic trickle scaling with {@link #getGazeCharge()} (see
 * {@link #tickAuraOfAnnihilation()}), telegraphed client-side by the same black smoke used at the
 * mouth/eyes now also rising from the ground in a ring around the Bodak (see {@link #aiStep()}) --
 * the ring sits at the aura's actual radius, so it doubles as a readable boundary rather than an
 * invisible damage source, and only appears while a charge is actually building (never at rest).
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class Bodak extends GMMMonster {

    // D&D 5e Bodak stat block (CR6): 58 HP, scaled via the project's zombie-baseline ratio
    // (D&D 22 HP <-> MC 20 HP, ~0.91x -- same conversion Wight/Mimic use): 58 * 20/22 ~ 53.
    private static final double DEFAULT_MAX_HEALTH = 53.0D;
    private static final double DEFAULT_MOVEMENT_SPEED = 0.23D;
    private static final double DEFAULT_ATTACK_DAMAGE = 4.0D;

    // Death Gaze: range, how long a continuous look takes to fully charge, the cooldown after a
    // successful gaze, the damage dealt, and the odds of the rare near-lethal branch.
    private static final double DEFAULT_GAZE_RANGE = 8.0D;
    private static final int DEFAULT_GAZE_CHARGE_TICKS = 60;      // ~3s of sustained looking
    private static final int DEFAULT_GAZE_COOLDOWN_TICKS = 100;   // ~5s before it can charge again
    private static final double DEFAULT_GAZE_DAMAGE = 14.0D;
    private static final double DEFAULT_NEAR_LETHAL_CHANCE = 0.2D;
    // How wide a cone counts as "looking at it" (half-angle, degrees) -- a flat, generous cone rather
    // than vanilla EnderMan's distance-scaled one (see isLookingAtMe's doc for why that was too tight
    // to actually hold for a real player). And how fast the charge bleeds off per tick you're NOT
    // looking, rather than a hard reset -- see decayGazeCharge.
    private static final double DEFAULT_GAZE_CONE_DEGREES = 20.0D;
    private static final int GAZE_DECAY_PER_MISS_TICK = 2;

    // Sunlight: bonus instant damage per sunburn proc, and the movement-speed burst while burning.
    private static final double DEFAULT_SUN_BONUS_DAMAGE = 2.0D;
    private static final double DEFAULT_SUN_PANIC_SPEED_BOOST = 0.12D; // ~+50% of DEFAULT_MOVEMENT_SPEED

    // Aura of Annihilation: folded into the Death Gaze windup (see class doc) rather than an independent
    // toggle -- radius the ring telegraph/damage applies at, damage per pulse at FULL charge (scaled down
    // by current charge otherwise), and how often a pulse can land while a charge is building.
    private static final double DEFAULT_AURA_RADIUS = 3.0D;
    private static final double DEFAULT_AURA_DAMAGE = 1.0D;
    private static final int DEFAULT_AURA_INTERVAL_TICKS = 20; // ~1s between pulses

    // Head-jerk telegraph (BodakModel, client-side only) -- angle range (degrees) scales with gaze
    // charge; the snap cadence (ticks) is fixed, not charge-scaled -- see BodakModel's class doc for why.
    private static final double DEFAULT_JERK_MIN_ANGLE_DEGREES = 6.0D;
    private static final double DEFAULT_JERK_MAX_ANGLE_DEGREES = 12.0D;
    private static final int DEFAULT_JERK_PERIOD_TICKS = 16;

    private static final ResourceLocation SUN_PANIC_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bodak_sun_panic");

    // Withering Gaze: how long the cast takes to charge, the "don't cast this close" range buffer
    // (the melee goal handles point-blank instead), and the cooldown after a completed cast --
    // CastSpellGoal has no cooldown by default (it can start charging again the instant a cast
    // finishes), which read as firing too rapidly; see registerGoals().
    private static final int DEFAULT_SPELL_CHARGE_TIME = 50;
    private static final double DEFAULT_SPELL_MIN_RANGE = 4.0D;
    private static final int DEFAULT_SPELL_COOLDOWN_TIME = 100;

    /**
     * Consumer-supplied Withering Gaze projectile (e.g. wired to GMM's own
     * {@code WitheringGazeSpell}). The shared library owns no concrete spell; left null, the Bodak
     * simply never casts one -- same pattern as {@code OrcShaman.spellCaster}.
     */
    public static CastSpellGoal.SpellLauncher spellCaster;

    private static final EntityDataAccessor<Float> DATA_GAZE_CHARGE =
            SynchedEntityData.defineId(Bodak.class, EntityDataSerializers.FLOAT);

    // Death Gaze windup state -- server-side only (DATA_GAZE_CHARGE mirrors the 0..1 progress to the
    // client purely for the smoke-intensify telegraph, see BodakRenderer/aiStep below).
    @Nullable
    private UUID gazeTargetId;
    private int gazeChargeTicks;
    private int gazeCooldownTicks;

    public Bodak(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GAZE_CHARGE, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        if (spellCaster != null) {
            int chargeTime = (int) MobConfigHelper.get(this).number("spellChargeTime", DEFAULT_SPELL_CHARGE_TIME);
            double minRange = MobConfigHelper.get(this).number("spellMinRange", DEFAULT_SPELL_MIN_RANGE);
            int cooldownTime = (int) MobConfigHelper.get(this).number("spellCooldownTime", DEFAULT_SPELL_COOLDOWN_TIME);
            // null particle: CastSpellGoal's own charge telegraph is disabled outright, not recolored --
            // Death Gaze already has its own dedicated smoke telegraph, and layering a second one (even
            // matched to the same palette) on top of it read as redundant rather than reinforcing it.
            this.goalSelector.addGoal(4, new CastSpellGoal(this, chargeTime, minRange * minRange,
                    cooldownTime, null, spellCaster));
        }
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, DEFAULT_ATTACK_DAMAGE);
    }

    // --- codec accessors -------------------------------------------------------------------------

    private boolean deathGazeEnabled() {
        return MobConfigHelper.get(this).flag("deathGaze", true);
    }

    private double gazeRange() {
        return MobConfigHelper.get(this).number("gazeRange", DEFAULT_GAZE_RANGE);
    }

    private int gazeChargeTicksRequired() {
        return (int) MobConfigHelper.get(this).number("gazeChargeTicks", DEFAULT_GAZE_CHARGE_TICKS);
    }

    private int gazeCooldownTicksConfig() {
        return (int) MobConfigHelper.get(this).number("gazeCooldownTicks", DEFAULT_GAZE_COOLDOWN_TICKS);
    }

    private double gazeDamage() {
        return MobConfigHelper.get(this).number("gazeDamage", DEFAULT_GAZE_DAMAGE);
    }

    private double nearLethalChance() {
        return MobConfigHelper.get(this).number("nearLethalChance", DEFAULT_NEAR_LETHAL_CHANCE);
    }

    private double gazeConeDegrees() {
        return MobConfigHelper.get(this).number("gazeConeDegrees", DEFAULT_GAZE_CONE_DEGREES);
    }

    private boolean nearLethalAllowKill() {
        return MobConfigHelper.get(this).flag("nearLethalGazeAllowKill", false);
    }

    private double sunBonusDamage() {
        return MobConfigHelper.get(this).number("sunBonusDamage", DEFAULT_SUN_BONUS_DAMAGE);
    }

    private boolean sunPanicEnabled() {
        return MobConfigHelper.get(this).flag("sunPanic", true);
    }

    private double sunPanicSpeedBoost() {
        return MobConfigHelper.get(this).number("sunPanicSpeedBoost", DEFAULT_SUN_PANIC_SPEED_BOOST);
    }

    private boolean auraEnabled() {
        return MobConfigHelper.get(this).flag("auraOfAnnihilation", true);
    }

    /** Radius the Aura of Annihilation's damage/ring telegraph applies at -- public, read client-side by {@link #aiStep()}. */
    public double auraRadius() {
        return MobConfigHelper.get(this).number("auraRadius", DEFAULT_AURA_RADIUS);
    }

    private double auraDamage() {
        return MobConfigHelper.get(this).number("auraDamage", DEFAULT_AURA_DAMAGE);
    }

    private int auraIntervalTicks() {
        return (int) MobConfigHelper.get(this).number("auraIntervalTicks", DEFAULT_AURA_INTERVAL_TICKS);
    }

    // Public (not the usual private-codec-accessor style) since these are read cross-package by
    // BodakModel during client-side rendering, not just from within this class -- the same reason
    // getGazeCharge() below is public. Safe to read client-side: gmm:mob_config is registered with a
    // network codec (see GMMRegistries#onNewDataPackRegistry), so it's synced to the client on join.

    /** Head-jerk minimum roll angle (degrees, at zero gaze charge) -- read by {@code BodakModel}. */
    public double getJerkMinAngleDegrees() {
        return MobConfigHelper.get(this).number("jerkMinAngleDegrees", DEFAULT_JERK_MIN_ANGLE_DEGREES);
    }

    /** Head-jerk maximum roll angle (degrees, at full gaze charge) -- read by {@code BodakModel}. */
    public double getJerkMaxAngleDegrees() {
        return MobConfigHelper.get(this).number("jerkMaxAngleDegrees", DEFAULT_JERK_MAX_ANGLE_DEGREES);
    }

    /** Fixed head-jerk snap cadence (ticks) -- not charge-scaled -- read by {@code BodakModel}. */
    public int getJerkPeriodTicks() {
        return (int) MobConfigHelper.get(this).number("jerkPeriodTicks", DEFAULT_JERK_PERIOD_TICKS);
    }

    // --- Death Gaze --------------------------------------------------------------------------------

    /** 0..1 windup progress toward the next Death Gaze -- used client-side to intensify the smoke telegraph. */
    public float getGazeCharge() {
        return this.entityData.get(DATA_GAZE_CHARGE);
    }

    private void setGazeCharge(float charge) {
        this.entityData.set(DATA_GAZE_CHARGE, charge);
    }

    private void tickDeathGaze() {
        if (!deathGazeEnabled()) {
            return;
        }
        if (gazeCooldownTicks > 0) {
            gazeCooldownTicks--;
            return;
        }
        Player looker = findLooker();
        if (looker == null) {
            decayGazeCharge();
            return;
        }
        if (!looker.getUUID().equals(gazeTargetId)) {
            gazeTargetId = looker.getUUID();
            gazeChargeTicks = 0;
        }
        int required = Math.max(1, gazeChargeTicksRequired());
        gazeChargeTicks = Math.min(required, gazeChargeTicks + 1);
        setGazeCharge(Mth.clamp((float) gazeChargeTicks / required, 0.0F, 1.0F));
        if (gazeChargeTicks >= required) {
            fireDeathGaze(looker);
            resetGazeCharge();
            gazeCooldownTicks = Math.max(0, gazeCooldownTicksConfig());
        }
    }

    private void resetGazeCharge() {
        gazeChargeTicks = 0;
        gazeTargetId = null;
        setGazeCharge(0.0F);
    }

    /**
     * A missed tick bleeds the charge off gradually ({@link #GAZE_DECAY_PER_MISS_TICK} per tick)
     * instead of zeroing it outright -- a hard reset meant ordinary mouse jitter mid-stare (a pixel of
     * drift for one tick) could cancel a multi-second charge that was otherwise almost complete, which
     * is exactly why staring felt like it "did nothing": the charge kept getting wiped before it could
     * ever finish. Deliberately looking away for a sustained moment still fully clears it (decay is
     * faster than the build rate), so it's still a real "look away to escape" counterplay -- just not a
     * hair-trigger one.
     */
    private void decayGazeCharge() {
        if (gazeChargeTicks <= 0) {
            gazeTargetId = null;
            return;
        }
        gazeChargeTicks = Math.max(0, gazeChargeTicks - GAZE_DECAY_PER_MISS_TICK);
        setGazeCharge(Mth.clamp((float) gazeChargeTicks / Math.max(1, gazeChargeTicksRequired()), 0.0F, 1.0F));
        if (gazeChargeTicks == 0) {
            gazeTargetId = null;
        }
    }

    /**
     * The nearest non-creative/non-spectator player within {@link #gazeRange()} that's actually
     * looking at this Bodak (see {@link #isLookingAtMe}) -- creative testers are deliberately excluded
     * from triggering it, the same "a tester shouldn't get punished for looking" precedent
     * {@code GraveZombie}'s ambush detection established (this mob's own first in-game test round
     * confirmed the mechanic needs to be verified in Survival, not creative -- see the class doc).
     */
    @Nullable
    private Player findLooker() {
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(gazeRange()), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        Player nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (Player player : nearby) {
            if (!isLookingAtMe(player)) {
                continue;
            }
            double distSqr = this.distanceToSqr(player);
            if (distSqr < nearestDistSqr) {
                nearestDistSqr = distSqr;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * Whether {@code player} is looking roughly at this Bodak: a flat, generous cone
     * ({@link #gazeConeDegrees()}, default 20 degrees half-angle) around the direction from the
     * player's eyes to this entity's eye position, plus a line-of-sight check. The <em>first</em> pass
     * of this method copied vanilla {@code EnderMan#isLookingAtMe(Player)}'s distance-scaled threshold
     * verbatim (dot-product against a threshold that tightens with range -- at this mob's 8-block gaze
     * range that works out to roughly a 4.5-degree cone), which turned out to be far too punishing for
     * a real player to hold: the crosshair has to be almost pixel-perfectly centered, and combined with
     * the old hard-reset-on-miss charge logic, ordinary aim jitter meant the charge effectively never
     * completed in practice ("I stare but nothing happens" -- round-3 in-game feedback). A fixed, wide
     * cone reads as "roughly looking toward it," which is what "staring at it" actually means to a
     * player, rather than a raycast-precision test. Short-circuits to {@code false} if the player is
     * wearing a {@link GMMTags.Items#BODAK_GAZE_WARD} helmet, mirroring the carved-pumpkin short-circuit
     * in vanilla's own {@code ForgeHooks.shouldSuppressEnderManAnger}.
     */
    private boolean isLookingAtMe(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(GMMTags.Items.BODAK_GAZE_WARD)) {
            return false;
        }
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double dist = toMe.length();
        if (dist < 1.0E-4D) {
            return false;
        }
        toMe = toMe.normalize();
        double dot = view.dot(toMe);
        double cosThreshold = Math.cos(Math.toRadians(gazeConeDegrees()));
        return dot > cosThreshold && player.hasLineOfSight(this);
    }

    /**
     * Applies the fully-charged Death Gaze. The near-lethal branch bypasses normal damage/armor
     * entirely by design -- 5e's version is a saving throw, not an attack roll, so it was never
     * subject to AC in the first place: the default (non-kill) case requests exactly enough damage to
     * reach 1 HP, which armor/resistance can only reduce further (landing above 1, never below); the
     * opt-in {@code nearLethalGazeAllowKill} case calls {@link LivingEntity#die} directly, which skips
     * {@code hurt()}'s totem-of-undying check entirely -- a deliberate, documented trade-off for
     * servers that want the literal 5e "reduced to 0 hit points" behavior back.
     */
    private void fireDeathGaze(Player target) {
        DamageSource source = GMMDamageTypes.source(this.level(), GMMDamageTypes.DEATH_GAZE);
        if (this.random.nextDouble() < nearLethalChance()) {
            if (nearLethalAllowKill()) {
                target.die(source);
            } else {
                float toOne = target.getHealth() - 1.0F;
                if (toOne > 0.0F) {
                    target.hurt(source, toOne);
                }
            }
        } else {
            target.hurt(source, (float) gazeDamage());
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, target.getX(), target.getEyeY(), target.getZ(),
                    12, 0.3D, 0.4D, 0.3D, 0.02D);
        }
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 0.8F);
    }

    /**
     * A point roughly on the face, offset forward from eye height along the entity's current view
     * direction -- an approximation (no access to the actual head-cuboid transform from here) used to
     * anchor the mouth/eye particle tells so they read as coming from the face and track head turns,
     * rather than a single random point floating near the whole body.
     */
    private Vec3 facePosition(double heightOffset, double forwardDist) {
        Vec3 forward = this.getViewVector(1.0F);
        return new Vec3(this.getX(), this.getEyeY() + heightOffset, this.getZ()).add(forward.scale(forwardDist));
    }

    // --- sunlight: bonus damage + panic speed burst -------------------------------------------------

    private void updateSunPanic() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(SUN_PANIC_SPEED_MODIFIER_ID,
                sunPanicSpeedBoost(), AttributeModifier.Operation.ADD_VALUE);
        boolean panicking = sunPanicEnabled() && this.isOnFire();
        if (panicking) {
            if (!speed.hasModifier(SUN_PANIC_SPEED_MODIFIER_ID)) {
                speed.addTransientModifier(modifier);
            }
        } else if (speed.hasModifier(SUN_PANIC_SPEED_MODIFIER_ID)) {
            speed.removeModifier(SUN_PANIC_SPEED_MODIFIER_ID);
        }
    }

    // --- flavour / sounds -----------------------------------------------------------------------

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        tickDeathGaze();
        tickAuraOfAnnihilation();
    }

    /**
     * Aura of Annihilation, folded into the Death Gaze windup rather than run as its own toggle (see
     * class doc) -- only ever active while {@link #getGazeCharge()} is above zero, i.e. exactly the
     * window the ground-ring smoke telegraph in {@link #aiStep()} is visible for. Pulses on a fixed
     * cadence ({@link #auraIntervalTicks()}) rather than every tick, so it reads as a "trickle," not a
     * single big continuous drain -- each pulse's damage scales with the current charge (0..1), so it
     * ramps up right alongside the ring's own growing intensity. Reuses {@code GMMDamageTypes.DEATH_GAZE}
     * rather than a dedicated damage type -- it's explicitly part of Death Gaze's own windup, not a
     * separate ability with its own death-message flavor. Same non-creative/non-spectator player scope as
     * Death Gaze itself ({@link #findLooker()}); does not require line of sight or looking at the Bodak
     * at all, only proximity.
     */
    private void tickAuraOfAnnihilation() {
        if (!auraEnabled()) {
            return;
        }
        float charge = getGazeCharge();
        if (charge <= 0.0F) {
            return;
        }
        if (this.tickCount % Math.max(1, auraIntervalTicks()) != 0) {
            return;
        }
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(auraRadius()), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (nearby.isEmpty()) {
            return;
        }
        DamageSource source = GMMDamageTypes.source(this.level(), GMMDamageTypes.DEATH_GAZE);
        float damage = (float) (auraDamage() * charge);
        for (Player player : nearby) {
            player.hurt(source, damage);
        }
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide) {
            // ambient mouth + eye smoke -- two anchors (not one shared random point), each with its own
            // roll so both are visible independently. Base rate bumped up from the original pass (which
            // read as too sparse to notice) -- roughly a particle every second or so per anchor at rest.
            float charge = getGazeCharge();
            int baseChance = 6;
            int chance = Math.max(1, (int) (baseChance - charge * (baseChance - 1)));
            if (this.random.nextInt(chance) == 0) {
                Vec3 mouth = facePosition(-0.15D, 0.32D);
                this.level().addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 0.0D, 0.02D, 0.0D);
            }
            if (this.random.nextInt(chance) == 0) {
                Vec3 eyes = facePosition(0.03D, 0.30D);
                this.level().addParticle(ParticleTypes.SMOKE, eyes.x, eyes.y, eyes.z, 0.0D, 0.02D, 0.0D);
            }
            // Aura of Annihilation telegraph: the same black smoke rising from the ground in a ring at
            // the aura's own radius -- only while a gaze charge is actually building (never at rest,
            // unlike the mouth/eye wisps above). Unlike those two single-particle-per-roll anchors, this
            // spawns a full spread of points around the ring every tick -- a single random dot per roll
            // (the first pass) was too sparse to read as a "ring" at all against a 3+ block circle;
            // multiple evenly-spaced points (with light per-point jitter so it doesn't look like a rigid
            // polygon) plus a per-tick rotating base angle read immediately as a boiling ring instead.
            // Point count scales 4..10 with charge (denser as it builds); radius itself stays fixed (not
            // charge-scaled) so the ring is always an honest boundary cue -- see
            // Bodak#tickAuraOfAnnihilation() for the matching server-side damage.
            if (charge > 0.0F) {
                int ringPoints = 4 + (int) (charge * 6.0F);
                double ringRadius = auraRadius();
                double baseAngle = this.random.nextDouble() * Math.PI * 2.0D;
                for (int i = 0; i < ringPoints; i++) {
                    double angle = baseAngle + (Math.PI * 2.0D * i / ringPoints) + (this.random.nextDouble() - 0.5D) * 0.3D;
                    double px = this.getX() + Math.cos(angle) * ringRadius;
                    double pz = this.getZ() + Math.sin(angle) * ringRadius;
                    this.level().addParticle(ParticleTypes.SMOKE, px, this.getY() + 0.05D, pz,
                            0.0D, 0.02D + charge * 0.03D, 0.0D);
                }
            }
            // full-body fire + white ash-smoke burst while sun-exposed -- deliberately FLAME/
            // CAMPFIRE_COSY_SMOKE (orange fire, pale-white smoke), not the black SMOKE the ambient
            // mouth/eye wisps use above, so "burning in the sun" reads as visually distinct from the
            // gaze telegraph rather than blurring into "more of the same grey smoke."
            if (this.isOnFire()) {
                if (this.random.nextInt(2) == 0) {
                    this.level().addParticle(ParticleTypes.FLAME,
                            this.getRandomX(0.6D), this.getRandomY(), this.getRandomZ(0.6D),
                            0.0D, 0.04D, 0.0D);
                }
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getRandomX(0.6D), this.getRandomY(), this.getRandomZ(0.6D),
                            0.0D, 0.05D, 0.0D);
                }
            }
        } else {
            if (this.isSunBurnTick()) {
                this.igniteForSeconds(8);
                this.hurt(this.damageSources().onFire(), (float) sunBonusDamage());
            }
            updateSunPanic();
        }
        super.aiStep();
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
}
