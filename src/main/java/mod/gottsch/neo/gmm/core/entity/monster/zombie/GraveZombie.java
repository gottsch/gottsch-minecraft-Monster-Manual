package mod.gottsch.neo.gmm.core.entity.monster.zombie;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * "Grave Zombie" (a.k.a. "Clawer") — an ambush zombie that never walks up on you. It spawns
 * <b>burrowed</b> (fully invisible, AI off, pinned in place) and lies dormant until it senses a
 * player, at which point it <b>digs up</b> within a short radius of that target in a burst of dirt
 * and joins the fight. If it loses its target's trail for too long it sinks back into the ground and
 * repositions, ready to ambush again rather than trudging around in the open like a normal zombie.
 *
 * <p>Runs on the shared vanilla zombie rig (dirt-caked recolor); "burrowed" is plain invisibility (no
 * geometry change needed there), but the two transitions — surfacing and reburrowing — get a dedicated
 * {@code GraveZombieModel} pose (arms thrust straight up, fanning down to/from the normal shamble as it
 * emerges/sinks, plus a shudder) fed by {@link #getRiseAmount(float)}, the same
 * feed-a-0..1-float-to-the-renderer trick {@code BloodyBonesModel} uses for its collapse/rise. The
 * phase machine (dormant → telegraph → active → telegraph → dormant) otherwise mirrors
 * {@code BloodyBones}'s collapse/rise handling: {@code setNoAi(true)} suppresses
 * {@code serverAiStep}/{@code customServerAiStep} entirely while inert, so the phase timer lives in
 * {@code tick()} (which always runs) rather than in a goal.
 *
 * <p>Only ever digs into actual dirt ({@code minecraft:dirt} — never stone or other terrain), which as
 * a side effect makes it a de facto surface mob. A consumer can anchor a specific instance to a spot
 * with vanilla's own {@code Mob#restrictTo(BlockPos, int)} (the same mechanism villagers/iron golems
 * use) — a restricted instance never repositions (always rises exactly where it's buried) and never
 * despawns, so a mapmaker can hand-place a graveyard of zombies that rise from their own graves. The
 * restriction isn't persisted by vanilla {@code Mob}, so this class saves/restores it itself. Same
 * goes for the burrowed invisibility, for a different reason &mdash; the flag is saved by nothing at
 * all (see {@link #readAdditionalSaveData}).
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class GraveZombie extends GMMMonster {

    // phase ordinals (kept as raw ints for the synched accessor)
    public static final int PHASE_BURROWED = 0;
    public static final int PHASE_SURFACING = 1;
    public static final int PHASE_ACTIVE = 2;
    public static final int PHASE_REBURROWING = 3;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(GraveZombie.class, EntityDataSerializers.INT);

    // how often (ticks) a dormant zombie checks for a nearby player worth surfacing for.
    private static final int SCAN_INTERVAL = 10;
    // how many candidate spots it tries before giving up on surfacing this scan.
    private static final int SURFACE_ATTEMPTS = 12;

    // Server-tunable knobs (codec-driven; see gmm:mob_config/grave_zombie.json).
    private static final double DEFAULT_DETECT_RANGE = 16.0D;
    private static final double DEFAULT_AMBUSH_MIN_RADIUS = 2.0D;
    private static final double DEFAULT_AMBUSH_MAX_RADIUS = 6.0D;
    private static final int DEFAULT_SURFACE_TICKS = 50;    // "digging up" telegraph, arms-up -> shamble (~2.5s)
    private static final int DEFAULT_REBURROW_TICKS = 50;   // "digging down" telegraph, shamble -> arms-up (~2.5s)
    private static final int DEFAULT_LOSE_SIGHT_TICKS = 100; // ~5s with no target/LOS before it reburrows

    // ticks elapsed within the current phase; reset when the phase changes.
    private int phaseTicks;
    private int lastPhase = PHASE_BURROWED;
    // ticks with no target or no line of sight to it, accumulated only while PHASE_ACTIVE.
    private int sightlessTicks;
    // the target to re-acquire once a surface/telegraph finishes; not persisted (transient hand-off).
    @Nullable
    private LivingEntity pendingTarget;
    // scratch coordinates for a surface spot found by trySurfaceNear, consumed by beginSurfacing.
    private double pendingSurfaceX, pendingSurfaceY, pendingSurfaceZ;

    public GraveZombie(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_BURROWED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    // --- codec accessors ------------------------------------------------------------------------

    private boolean ambushEnabled() {
        return MobConfigHelper.get(this).flag("ambush", true);
    }

    private double detectRange() {
        return MobConfigHelper.get(this).number("detectRange", DEFAULT_DETECT_RANGE);
    }

    private double ambushMinRadius() {
        return MobConfigHelper.get(this).number("ambushMinRadius", DEFAULT_AMBUSH_MIN_RADIUS);
    }

    private double ambushMaxRadius() {
        return MobConfigHelper.get(this).number("ambushMaxRadius", DEFAULT_AMBUSH_MAX_RADIUS);
    }

    private int surfaceTicks() {
        return (int) MobConfigHelper.get(this).number("surfaceTicks", DEFAULT_SURFACE_TICKS);
    }

    private int reburrowTicks() {
        return (int) MobConfigHelper.get(this).number("reburrowTicks", DEFAULT_REBURROW_TICKS);
    }

    private int loseSightTicks() {
        return (int) MobConfigHelper.get(this).number("loseSightTicks", DEFAULT_LOSE_SIGHT_TICKS);
    }

    // --- phase state ----------------------------------------------------------------------------

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    /** Spawns already burrowed (invisible, AI off) unless the ambush gimmick is disabled. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        if (ambushEnabled()) {
            this.setNoAi(true);
            this.setInvisible(true);
        }
        return spawnGroupData;
    }

    /**
     * Being struck always blows its cover immediately — an ambush shouldn't feel unresponsive. Fire is
     * excluded: it's not an attack that should interrupt an in-progress reburrow, and a fire tick lands
     * every ~second, so without this exclusion a burning zombie could never finish sinking back into
     * the ground (see {@link #beginReburrow()}, which snuffs the fire immediately instead).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !source.is(DamageTypeTags.IS_FIRE)) {
            int phase = getPhase();
            if (phase == PHASE_BURROWED || phase == PHASE_REBURROWING) {
                beginSurfacingInPlace(source.getEntity() instanceof LivingEntity living ? living : null);
            }
        }
        return hurt;
    }

    // --- tick / phase timer ---------------------------------------------------------------------

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

        if (this.level().isClientSide) {
            return;
        }

        switch (phase) {
            case PHASE_BURROWED -> tickBurrowed();
            case PHASE_SURFACING -> {
                if (this.phaseTicks >= surfaceTicks()) {
                    finishSurfacing();
                }
            }
            case PHASE_ACTIVE -> tickActive();
            case PHASE_REBURROWING -> {
                if (this.phaseTicks >= reburrowTicks()) {
                    finishReburrow();
                }
            }
        }
    }

    /** Dormant: periodically scans for a nearby player and, if one is found, digs up near them. */
    private void tickBurrowed() {
        if (!ambushEnabled()) {
            // ambush disabled: behave like a plain always-active zombie, no gimmick.
            this.setNoAi(false);
            this.setInvisible(false);
            setPhase(PHASE_ACTIVE);
            return;
        }
        if (this.tickCount % SCAN_INTERVAL != 0) {
            return;
        }
        // a creative (or spectator) player is never a valid ambush target -- it should stay buried
        // rather than surface for someone who isn't actually playing. To preview the rise/reburrow
        // animation without switching to survival, hit it instead: hurt() always surfaces it in place
        // regardless of the attacker's game mode.
        Player player = this.level().getNearestPlayer(
                this.getX(), this.getY(), this.getZ(), detectRange(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player == null) {
            return;
        }
        // structure-placed (a grave with restrictTo() set by a consumer -- see the class doc): a
        // preset grave never relocates, it only ever rises exactly where it's buried.
        if (this.hasRestriction()) {
            beginSurfacingInPlace(player);
            return;
        }
        // already close enough to ambush from right here -- no need to relocate to a "better" spot.
        double maxRadius = ambushMaxRadius();
        if (this.distanceToSqr(player) <= maxRadius * maxRadius) {
            beginSurfacingInPlace(player);
        } else if (trySurfaceNear(player)) {
            beginSurfacing(player);
        }
    }

    /** Active: tracks how long it's gone without seeing its target, and reburrows if it's too long. */
    private void tickActive() {
        LivingEntity target = this.getTarget();
        boolean seen = target != null && target.isAlive() && this.getSensing().hasLineOfSight(target);
        this.sightlessTicks = seen ? 0 : this.sightlessTicks + 1;
        if (this.sightlessTicks >= loseSightTicks()) {
            beginReburrow();
        }
    }

    /**
     * Looks for a spot within {@code [ambushMinRadius, ambushMaxRadius]} of the target that's clear
     * to stand on <b>and</b> is actual diggable ground ({@code minecraft:dirt} — dirt/grass/podzol/
     * coarse dirt/mycelium/rooted dirt; never stone or other non-dirt terrain, see the class doc). As a
     * side effect this makes it a de facto surface mob: dirt is rare deep underground. Stashes the
     * coordinates on success for {@link #beginSurfacing} to consume.
     */
    private boolean trySurfaceNear(LivingEntity target) {
        double minRadius = ambushMinRadius();
        double maxRadius = ambushMaxRadius();
        for (int i = 0; i < SURFACE_ATTEMPTS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = minRadius + this.random.nextDouble() * (maxRadius - minRadius);
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            BlockPos ground = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, target.getY(), z));
            double y = ground.getY();
            AABB box = this.getDimensions(Pose.STANDING).makeBoundingBox(x, y, z);
            if (this.level().noCollision(this, box)
                    && this.level().getBlockState(ground.below()).is(BlockTags.DIRT)
                    && this.level().getBlockState(ground.below()).isFaceSturdy(this.level(), ground.below(), Direction.UP)) {
                this.pendingSurfaceX = x;
                this.pendingSurfaceY = y;
                this.pendingSurfaceZ = z;
                return true;
            }
        }
        return false;
    }

    /** Teleports to the spot found by {@link #trySurfaceNear} and begins the surfacing telegraph. */
    private void beginSurfacing(LivingEntity target) {
        this.moveTo(this.pendingSurfaceX, this.pendingSurfaceY, this.pendingSurfaceZ, this.getYRot(), this.getXRot());
        beginSurfacingInPlace(target);
    }

    /** Begins the surfacing telegraph without repositioning — used by the hit-while-hidden interrupt. */
    private void beginSurfacingInPlace(@Nullable LivingEntity target) {
        this.pendingTarget = target;
        this.setInvisible(false);
        setPhase(PHASE_SURFACING);
        burstDirt();
    }

    /** Restores AI and hands off the pending target once the "digging up" telegraph finishes. */
    private void finishSurfacing() {
        this.setNoAi(false);
        if (this.pendingTarget != null && this.pendingTarget.isAlive()) {
            this.setTarget(this.pendingTarget);
        }
        this.pendingTarget = null;
        setPhase(PHASE_ACTIVE);
    }

    /** Loses its target's trail — sinks back down so it can dig up somewhere better next time. */
    private void beginReburrow() {
        this.setNoAi(true);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        // digging into the cool earth smothers any fire immediately -- without this a zombie that
        // caught fire in daylight could only ever reburrow after it finished burning out on its own.
        this.clearFire();
        setPhase(PHASE_REBURROWING);
        burstDirt();
    }

    private void finishReburrow() {
        this.setInvisible(true);
        this.sightlessTicks = 0;
        setPhase(PHASE_BURROWED);
    }

    private void burstDirt() {
        this.playSound(SoundEvents.GRAVEL_BREAK, 1.0F, 0.7F + this.random.nextFloat() * 0.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    this.getX(), this.getY() + 0.1D, this.getZ(),
                    20, this.getBbWidth() * 0.4D, 0.2D, this.getBbWidth() * 0.4D, 0.05D);
        }
    }

    /**
     * A steady trickle of dirt kicked up at ground level for the whole surfacing/reburrowing
     * telegraph — the one-shot {@link #burstDirt()} pop at the start/end of each isn't enough on its
     * own to read as ongoing digging over a multi-second animation.
     */
    private void spawnDiggingParticles() {
        for (int i = 0; i < 3; i++) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    this.getRandomX(0.6D), this.getY() + 0.05D, this.getRandomZ(0.6D),
                    (this.random.nextDouble() - 0.5D) * 0.1D, this.random.nextDouble() * 0.15D, (this.random.nextDouble() - 0.5D) * 0.1D);
        }
    }

    /**
     * 0 = fully buried (arms thrust straight up), 1 = fully risen/normal shamble pose. Interpolated
     * with partialTicks for a smooth client-side animation; used by {@code GraveZombieModel} via
     * {@code GraveZombieRenderer}, the same feed-in trick {@code BloodyBonesModel} uses for its
     * collapse/rise.
     */
    public float getRiseAmount(float partialTicks) {
        float t = this.phaseTicks + partialTicks;
        return switch (getPhase()) {
            case PHASE_SURFACING -> Mth.clamp(t / surfaceTicks(), 0.0F, 1.0F);
            case PHASE_REBURROWING -> Mth.clamp(1.0F - t / reburrowTicks(), 0.0F, 1.0F);
            case PHASE_ACTIVE -> 1.0F;
            default -> 0.0F; // BURROWED -- irrelevant while invisible
        };
    }

    /**
     * The grave zombie's anchor holds until it actually rises: a buried grave is meant to stay
     * exactly where it was placed, so "engaged" here is the active PHASE, not merely having noticed
     * someone. Falls through to the base rule (having a target) so a zombie that is somehow active
     * without a phase change is not leashed either. See {@code Anchor}.
     */
    @Override
    protected boolean isAnchorSuspended() {
        return getPhase() == PHASE_ACTIVE || super.isAnchorSuspended();
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) {
            setPhase(tag.getInt("Phase"));
            this.lastPhase = getPhase();
            // Invisibility is the other HALF of what "burrowed" means, and unlike NoAi -- which Mob
            // persists for us -- vanilla never saves it. Entity#saveWithoutId writes Glowing,
            // Silent, NoGravity and Invulnerable, but the Invisible shared-flag bit is not among
            // them, because vanilla only ever gets invisibility from a potion effect that is itself
            // persisted. So a dormant grave zombie came back from a chunk unload/reload still AI
            // off and still PHASE_BURROWED -- and therefore still sunk by GraveZombieModel, which
            // sinks the rig 26 units and deliberately stops short of hiding it (it is supposed to be
            // invisible down there) -- but fully visible. What that reads as in game is a zombie
            // head stuck in the floor, permanently, since a burrowed zombie only rises for a
            // non-creative player inside its ambush radius.
            //
            // Only for BURROWED. A save caught mid-REBURROW resumes with phaseTicks back at 0, sinks
            // again from the top and ends in finishReburrow(), which sets the flag itself.
            if (getPhase() == PHASE_BURROWED && ambushEnabled()) {
                this.setInvisible(true);
            }
        }
    }

    // --- flavour / sounds -----------------------------------------------------------------------

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void aiStep() {
        // a buried corpse doesn't catch sun; only burns while up and visible.
        if (getPhase() == PHASE_ACTIVE && this.isSunBurnTick()) {
            this.setSecondsOnFire(8);
        }
        // a steady trickle of kicked-up dirt for the whole digging-up / digging-down animation.
        if (this.level().isClientSide) {
            int phase = getPhase();
            if ((phase == PHASE_SURFACING || phase == PHASE_REBURROWING) && this.random.nextInt(3) == 0) {
                spawnDiggingParticles();
            }
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
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
