package mod.gottsch.neo.gmm.core.entity.monster.plant;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Violet Fungus ("Grabweed"): D&amp;D 5e's rooted lasher, second mob in the catalog's "Family:
 * Plants" (see {@code Shrieker}, the family's first). Permanently rooted the same way (
 * {@code setNoAi(true)}, alert/attack cycle driven from an overridden {@code tick()} since
 * {@code setNoAi(true)} suppresses {@code customServerAiStep}), but unlike Shrieker it actually fights:
 * every {@code lashCooldownTicks} it scans for the nearest non-creative/non-spectator player within
 * {@code lashRange} (deliberately short -- "a bit past vanilla melee reach," not Reaper's long grapple
 * reach, see the catalog's Reaper entry for that contrast) and, if one is in range, calls
 * {@link #doHurtTarget(Entity)} on it directly -- bypassing the goal system entirely, the same way
 * Shrieker bypasses it for its own proximity scan, since there is no {@code MeleeAttackGoal} to drive a
 * rooted mob's attack (that goal assumes it can path toward its target).
 *
 * <p>The anti-heal/wither tick is a direct reuse of {@link GMMMobEffects#WITHERED} (Wight's own
 * max-HP-drain effect) via the identical stacking-amplifier idiom {@code Wight#applyWither} already
 * established -- not a new effect, just weaker defaults ({@code maxWitherStacks} lower,
 * {@code witherDurationTicks} shorter) so it reads as "a weak tick," per the catalog wording, rather than
 * Wight's own more punishing drain.
 *
 * <p><b>The reach is rendered as VFX only, and after an in-game test round, deliberately stays that
 * way</b> -- no model tentacles at all (a real limb is still Reaper's job, where reach is long enough to
 * be the actual headline mechanic). In-game, the particle streak alone already read as a clear
 * differentiator from Shrieker mid-fight, which made a later "reveal tentacles on first hit" plan
 * unnecessary -- the streak already does that job. The streak reuses {@code GMMParticles.SLIME_TRAIL}
 * (the ooze family's tintable ground-smear particle, whose spawn velocity args are already repurposed as
 * an RGB tint) rather than Electric Skeleton's spark sprite verbatim -- a green tint reads as "vine," a
 * lightning-yellow spark sprite would have read as electricity. The zig-zag jitter technique itself
 * (points walked from self to target, nudged off the straight line, tapering to zero at both ends) is
 * lifted directly from {@code ElectricSkeleton#arc}. One real difference from that precedent:
 * {@code SLIME_TRAIL}'s RGB-tint trick only round-trips correctly through the client-only
 * {@code Level#addParticle(options,x,y,z,dx,dy,dz)} call (confirmed by how {@code GelatinousCube}/
 * {@code GrayOoze}/{@code OchreJelly} all spawn it, client-side only, from their own {@code aiStep()}) --
 * broadcasting it via {@code ServerLevel#sendParticles}' count/offset/speed packing is not confirmed to
 * preserve exact tint values, so rather than risk a broken-looking streak this mob syncs a lightweight
 * {@code DATA_LASH_TARGET_ID} (the struck entity's id, or -1) for a couple of ticks after a landed hit;
 * every nearby client already has both entities' positions and independently draws the same streak
 * locally, the same "client resolves its own copy" trick {@code BurningSkeleton}'s aura-wave telegraph
 * already uses, just keyed off a synced id instead of a tick-modulo interval. (A round-2 attempt to
 * extend this into a several-second tracking streak -- re-tracking the target's live position instead of
 * a one-off burst -- was reverted: in practice it read as a trail of particles chasing the player around
 * well after the hit landed, not a reach telegraph. Back to a brief flash on the moment of impact.)
 *
 * <p>Sways on a continuous proximity trigger (not a one-shot pulse like Shrieker's alert) -- any
 * non-creative/non-spectator player within {@code lashRange} keeps {@link #getSwayIntensity()} pinned at
 * full while they stay close, easing back to rest the same way once they leave, per the user's ask that
 * it "sway just like the Shrieker when you are near." Reuses {@code ShriekerModel#applySway} directly
 * (see {@code VioletFungusModel}) rather than duplicating the oscillation math.
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class VioletFungus extends GMMMonster {

    private static final double DEFAULT_MAX_HEALTH = 14.0D;
    private static final double DEFAULT_LASH_DAMAGE = 3.0D;

    private static final double DEFAULT_LASH_RANGE = 3.0D;
    private static final int DEFAULT_LASH_COOLDOWN_TICKS = 30; // ~1.5s between lashes

    private static final int DEFAULT_WITHER_DURATION_TICKS = 100; // ~5s -- shorter than Wight's own drain
    private static final int DEFAULT_MAX_WITHER_STACKS = 2;       // lower cap -- reads as "weak," not Wight's punish

    private static final int LASH_FLASH_TICKS = 2; // how long DATA_LASH_TARGET_ID stays set after a hit

    // Proximity sway -- pure animation-feel constants (same category as ShriekerModel's own
    // SWAY_SPEED/SWAY_AMOUNT/ease-out), not gmm:mob_config-driven; see feedback_default_to_configurable.
    private static final int SWAY_SCAN_INTERVAL_TICKS = 5;
    private static final int SWAY_HOLD_TICKS = 10; // re-armed every scan while a player stays in lashRange
    private static final int SWAY_EASE_OUT_TICKS = 15;

    private static final EntityDataAccessor<Integer> DATA_LASH_TARGET_ID =
            SynchedEntityData.defineId(VioletFungus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SWAY_INTENSITY =
            SynchedEntityData.defineId(VioletFungus.class, EntityDataSerializers.FLOAT);

    private int lashCooldownTicks;
    private int lashFlashTicksRemaining;
    private int swayHoldTicksRemaining;
    private int swayEaseTicksRemaining;

    public VioletFungus(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LASH_TARGET_ID, -1);
        this.entityData.define(DATA_SWAY_INTENSITY, 0.0F);
    }

    /** Permanently rooted -- no goals, no pathing, ever (same as Shrieker). */
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
                .add(Attributes.ATTACK_DAMAGE, DEFAULT_LASH_DAMAGE);
    }

    // --- codec accessors -------------------------------------------------------------------------

    private double lashRange() {
        return MobConfigHelper.get(this).number("lashRange", DEFAULT_LASH_RANGE);
    }

    private int lashCooldownTicksConfig() {
        return (int) MobConfigHelper.get(this).number("lashCooldownTicks", DEFAULT_LASH_COOLDOWN_TICKS);
    }

    private int witherDurationTicks() {
        return (int) MobConfigHelper.get(this).number("witherDurationTicks", DEFAULT_WITHER_DURATION_TICKS);
    }

    private int maxWitherStacks() {
        return (int) MobConfigHelper.get(this).number("maxWitherStacks", DEFAULT_MAX_WITHER_STACKS);
    }

    private boolean appliesWither() {
        return MobConfigHelper.get(this).flag("appliesWither", true);
    }

    // --- sway state (synced -- read by VioletFungusModel, mirrors Shrieker#getSwayIntensity) --------

    public float getSwayIntensity() {
        return this.entityData.get(DATA_SWAY_INTENSITY);
    }

    private void setSwayIntensity(float intensity) {
        this.entityData.set(DATA_SWAY_INTENSITY, intensity);
    }

    // --- lash cycle --------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (lashFlashTicksRemaining > 0 && --lashFlashTicksRemaining == 0) {
            this.entityData.set(DATA_LASH_TARGET_ID, -1);
        }
        tickSway();
        if (lashCooldownTicks > 0) {
            lashCooldownTicks--;
            return;
        }
        Player player = this.level().getNearestPlayer(
                this.getX(), this.getY(), this.getZ(), lashRange(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player != null) {
            lashCooldownTicks = Math.max(1, lashCooldownTicksConfig());
            this.doHurtTarget(player);
        }
    }

    /**
     * Continuous proximity sway, independent of the attack cooldown -- re-arms {@code swayHoldTicksRemaining}
     * every {@code SWAY_SCAN_INTERVAL_TICKS} while a player stays within {@code lashRange}, so the
     * stalks stay swaying the whole time someone's nearby; once nobody's found, it eases back to rest
     * over {@code SWAY_EASE_OUT_TICKS} instead of snapping, the same idiom {@code Shrieker}'s own
     * post-pulse ease-out already established.
     */
    private void tickSway() {
        if (this.tickCount % SWAY_SCAN_INTERVAL_TICKS == 0) {
            Player nearby = this.level().getNearestPlayer(
                    this.getX(), this.getY(), this.getZ(), lashRange(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            if (nearby != null) {
                swayHoldTicksRemaining = SWAY_HOLD_TICKS + SWAY_SCAN_INTERVAL_TICKS;
            }
        }
        if (swayHoldTicksRemaining > 0) {
            swayHoldTicksRemaining--;
            setSwayIntensity(1.0F);
            if (swayHoldTicksRemaining == 0) {
                swayEaseTicksRemaining = SWAY_EASE_OUT_TICKS;
            }
        } else if (swayEaseTicksRemaining > 0) {
            swayEaseTicksRemaining--;
            setSwayIntensity(swayEaseTicksRemaining / (float) SWAY_EASE_OUT_TICKS);
        }
    }

    /** Layers the wither tick + lash-streak telegraph on top of a landed vanilla hit. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            if (appliesWither()) {
                applyWither(living);
            }
            this.entityData.set(DATA_LASH_TARGET_ID, target.getId());
            lashFlashTicksRemaining = LASH_FLASH_TICKS;
            // reuses the ooze family's own wet-impact sound -- fits the green vine-lash particle better
            // than any of the "generic melee hit" sounds, and keeps this within the same plumbing reuse
            // idiom as GMMParticles.SLIME_TRAIL above.
            this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, 0.7F);
        }
        return hurt;
    }

    /** Stacking, temporary max-health drain -- identical idiom to {@code Wight#applyWither}, weaker defaults. */
    private void applyWither(LivingEntity target) {
        MobEffectInstance existing = target.getEffect(GMMMobEffects.WITHERED.get());
        int amplifier = existing == null ? 0 : Math.min(maxWitherStacks() - 1, existing.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(GMMMobEffects.WITHERED.get(), witherDurationTicks(), amplifier, false, true, false), this);
    }

    /**
     * Client-side only: draws a jittered green streak from this mob to whatever {@code DATA_LASH_TARGET_ID}
     * currently points at (see the class doc for why this is synced-id-driven rather than a server
     * broadcast). Every nearby client resolves the same target entity locally and draws the identical
     * line, the same "client re-derives its own copy" trick {@code BurningSkeleton}'s aura wave uses.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            return;
        }
        int targetId = this.entityData.get(DATA_LASH_TARGET_ID);
        if (targetId < 0) {
            return;
        }
        Entity target = this.level().getEntity(targetId);
        if (target == null) {
            return;
        }
        Vec3 a = this.position().add(0.0D, this.getBbHeight() * 0.4D, 0.0D);
        Vec3 b = target.getEyePosition();
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double jitter = 0.3D * Mth.sin((float) (Math.PI * t));
            this.level().addParticle(GMMParticles.SLIME_TRAIL.get(),
                    a.x + (b.x - a.x) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    a.y + (b.y - a.y) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    a.z + (b.z - a.z) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    0.35D, 0.75D, 0.30D);
        }
    }
}
