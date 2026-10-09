package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A skeleton wreathed in perpetual flame: it fights in melee like a normal skeleton, but the fire
 * clinging to its bones sets nearby creatures alight (a flame aura), its bite ignites the struck
 * target for longer, and when it finally dies the fire ruptures outward in a burst that ignites
 * everything close and scatters short-lived flames on the ground. Being made of fire, it is fire-immune
 * (so it wades through its own blaze and lava unharmed) and never burns in sunlight — its "aflame" look
 * is a client-side wreath of flame + smoke particles, the same idiom as the Daemon.
 *
 * <p>Distinct from the {@code MagmaSkeleton}, which owns the Nether / lava-float / dual-wield-archer
 * niche and only carries fire as flavour; the Burning Skeleton is an overworld melee mob whose whole
 * kit is <em>actively</em> setting things on fire.
 *
 * <p>Aura reach / ignite durations / death-burst radius and whether it places ground fire are all
 * codec-driven ({@code gmm:mob_config}); every part can be tuned or disabled by consumers.
 *
 * @author Mark Gottschling on 7/6/2026
 */
public class BurningSkeleton extends GMMMonster {

    // How far the flame aura reaches, how long it sets a caught creature on fire, and how often it
    // pulses (kept modest — a passing brush shouldn't be as bad as taking a bite).
    private static final double DEFAULT_AURA_RANGE = 2.5D;
    private static final int DEFAULT_AURA_FIRE_SECONDS = 4;
    private static final int DEFAULT_AURA_INTERVAL = 30;
    // A landed bite ignites longer than the passing aura.
    private static final int DEFAULT_MELEE_FIRE_SECONDS = 6;
    // The death rupture: how far it ignites and for how long.
    private static final double DEFAULT_DEATH_FIRE_RADIUS = 3.0D;
    private static final int DEFAULT_DEATH_FIRE_SECONDS = 5;

    // The aura telegraph: on each pulse a ring of custom gmm:burning_aura flame "runners" is spawned
    // client-side (one per compass direction). Each runner travels flat out along the ground to the
    // aura's reach and then bends upward — the flat-then-up motion lives in BurningAuraFlameParticle.
    private static final int AURA_WAVE_SPOKES = 16;
    // Flat outward speed of a runner (blocks/tick). The particle derives its own flat-travel duration
    // from this + the reach, so a runner always lands right at the rim — no timing to keep in sync.
    private static final double AURA_WAVE_OUT_SPEED = 0.18D;

    private boolean hasBurst;

    public BurningSkeleton(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
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
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // the fire is the real threat, so the base bite matches a caustic skeleton's, no more
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    private double auraRange() {
        return MobConfigHelper.get(this).number("auraRange", DEFAULT_AURA_RANGE);
    }

    private int auraFireSeconds() {
        return (int) MobConfigHelper.get(this).number("auraFireSeconds", DEFAULT_AURA_FIRE_SECONDS);
    }

    private int auraInterval() {
        return (int) MobConfigHelper.get(this).number("auraInterval", DEFAULT_AURA_INTERVAL);
    }

    private int meleeFireSeconds() {
        return (int) MobConfigHelper.get(this).number("meleeFireSeconds", DEFAULT_MELEE_FIRE_SECONDS);
    }

    private double deathFireRadius() {
        return MobConfigHelper.get(this).number("deathFireRadius", DEFAULT_DEATH_FIRE_RADIUS);
    }

    private int deathFireSeconds() {
        return (int) MobConfigHelper.get(this).number("deathFireSeconds", DEFAULT_DEATH_FIRE_SECONDS);
    }

    private boolean hasFlameAura() {
        return MobConfigHelper.get(this).flag("flameAura", true);
    }

    private boolean ignitesOnHit() {
        return MobConfigHelper.get(this).flag("igniteOnHit", true);
    }

    private boolean burstsOnDeath() {
        return MobConfigHelper.get(this).flag("deathBurst", true);
    }

    private boolean placesDeathFire() {
        return MobConfigHelper.get(this).flag("deathFireBlocks", true);
    }

    /**
     * A caught creature is anyone but us, our summoner, our allies, and anything tagged
     * {@link GMMTags.EntityTypes#BURNING_SKELETON_IGNITE_IMMUNE}. {@code isAlliedTo} alone doesn't
     * cover "don't burn fellow undead standing next to you": vanilla hostile mobs aren't on a
     * scoreboard team by default, so two independently-spawned skeletons are never "allied" by that
     * check even though thematically they're on the same side — the aura/bite/death-burst were
     * igniting other skeletons (and any other undead, e.g. Wight/Bodak) standing nearby. gmm ships the
     * tag empty; consumers populate it with their skeleton/undead roster (see e.g.
     * {@code gmm:skeleton_champion/rally_allies} for the same "consumer owns the roster" idiom).
     */
    private boolean canIgnite(LivingEntity entity) {
        return entity != this && entity.isAlive() && entity != this.getSummonedOwner()
                && !this.isAlliedTo(entity) && !entity.getType().is(GMMTags.EntityTypes.BURNING_SKELETON_IGNITE_IMMUNE);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && ignitesOnHit() && target instanceof LivingEntity living) {
            living.igniteForSeconds(meleeFireSeconds());
        }
        return hurt;
    }

    /** Set nearby creatures alight — the flame aura pulse. Server-side only. */
    private void radiateFlame(ServerLevel level) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(auraRange()), this::canIgnite);
        for (LivingEntity target : nearby) {
            target.igniteForSeconds(auraFireSeconds());
        }
    }

    /**
     * The aura telegraph, spawned client-side once per pulse: a ring of custom {@code gmm:burning_aura}
     * flame runners flung flat out from the mob, one per compass direction. Each runner carries its
     * outward velocity in (dx,dz) and the aura reach in the dy slot; the particle itself travels flat to
     * that reach and then bends upward (see {@link mod.gottsch.neo.gmm.core.client.particle.BurningAuraFlameParticle}).
     * Purely cosmetic, so it's client-only — no server particle traffic.
     */
    private void spawnAuraWave() {
        double reach = auraRange();
        double y = this.getY() + 0.1D;
        for (int i = 0; i < AURA_WAVE_SPOKES; i++) {
            double angle = (Math.PI * 2.0D * i) / AURA_WAVE_SPOKES;
            this.level().addParticle(GMMParticles.BURNING_AURA.get(),
                    this.getX(), y, this.getZ(),
                    Math.cos(angle) * AURA_WAVE_OUT_SPEED, reach, Math.sin(angle) * AURA_WAVE_OUT_SPEED);
        }
    }

    /** Rupture when killed (the whole point): ignite everything close and scatter ground fire. Idempotent. */
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        burst();
    }

    private void burst() {
        if (this.hasBurst || !(this.level() instanceof ServerLevel level) || !burstsOnDeath()) {
            return;
        }
        this.hasBurst = true;

        // ignite everything close
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(deathFireRadius()), this::canIgnite);
        for (LivingEntity target : nearby) {
            target.igniteForSeconds(deathFireSeconds());
        }

        // a flash of flame + smoke at the death point
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5D), this.getZ(),
                40, 0.4D, 0.5D, 0.4D, 0.05D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5D), this.getZ(),
                12, 0.3D, 0.4D, 0.3D, 0.02D);
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.9F);

        // scatter a few short-lived flames on the ground (fire naturally burns out / spreads)
        if (placesDeathFire()) {
            scatterFire(level);
        }
    }

    /** Try to drop a handful of fire blocks on valid ground around the death point. */
    private void scatterFire(ServerLevel level) {
        int radius = Math.max(1, (int) Math.round(deathFireRadius()));
        BlockPos origin = this.blockPosition();
        for (int i = 0; i < 6; i++) {
            BlockPos pos = origin.offset(
                    this.random.nextInt(radius * 2 + 1) - radius,
                    0,
                    this.random.nextInt(radius * 2 + 1) - radius);
            // settle onto the ground: step down through air (up to 3 blocks) to find a surface
            for (int drop = 0; drop < 3 && level.getBlockState(pos.below()).isAir(); drop++) {
                pos = pos.below();
            }
            if (level.getBlockState(pos).isAir()) {
                BlockState fire = BaseFireBlock.getState(level, pos);
                if (fire.canSurvive(level, pos)) {
                    level.setBlock(pos, fire, Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // ignite nearby creatures on the pulse interval (the matching visual telegraph is spawned
        // client-side in aiStep — see there)
        if (hasFlameAura() && this.tickCount % Math.max(1, auraInterval()) == 0
                && this.level() instanceof ServerLevel serverLevel) {
            radiateFlame(serverLevel);
        }
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide) {
            // "aflame" wreath: flame + smoke clinging to the bones (Daemon idiom, but thinner — a
            // skeleton is a smaller frame than a Daemon). No real fire overlay — it's fire-immune and
            // never burns in sun, so the body texture stays visible.
            this.level().addParticle(ParticleTypes.FLAME,
                    this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
                    0.0D, 0.0D, 0.0D);
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE,
                        this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
                        0.0D, 0.0D, 0.0D);
            }
            // aura telegraph: on the pulse interval, fling the flat-then-up flame runners (client-only,
            // no server traffic). Client tickCount tracks the server's closely enough for a cosmetic.
            if (hasFlameAura() && this.tickCount % Math.max(1, auraInterval()) == 0) {
                spawnAuraWave();
            }
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
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
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(1.74F);
    }
}
