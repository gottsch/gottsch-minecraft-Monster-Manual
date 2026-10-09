package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * A skeleton crackling with stored charge: it fights in melee like a normal skeleton, but every
 * landed hit discharges a shock that arcs from the struck target to a few nearby creatures (chain
 * lightning), and any target that is wet — standing in water or out in the rain — takes bonus
 * damage. Trails a faint electric spark so it's identifiable. Chain reach / hop count / damage and
 * the wet bonus are codec-driven ({@code gmm:mob_config}); the chaining can be disabled by consumers.
 *
 * @author Mark Gottschling on 7/4/2026
 */
public class ElectricSkeleton extends GMMMonster {

    // How far the arc can jump from the struck target, how many extra creatures it hops to, and the
    // damage of each hop. Kept modest so a lone hit is fine but a crowd (or a wet fight) is dangerous.
    private static final double DEFAULT_CHAIN_RANGE = 4.0D;
    private static final int DEFAULT_CHAIN_TARGETS = 3;
    private static final float DEFAULT_CHAIN_DAMAGE = 3.0F;
    // Extra damage dealt to a wet target (water conducts) — applied to the melee target and each hop.
    private static final float DEFAULT_WET_BONUS = 3.0F;

    public ElectricSkeleton(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
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
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                // the shock/chain is the real threat, so the base bite is a touch lower than a vanilla skeleton's
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    private double chainRange() {
        return MobConfigHelper.get(this).number("chainRange", DEFAULT_CHAIN_RANGE);
    }

    private int chainTargets() {
        return (int) MobConfigHelper.get(this).number("chainTargets", DEFAULT_CHAIN_TARGETS);
    }

    private float chainDamage() {
        return (float) MobConfigHelper.get(this).number("chainDamage", DEFAULT_CHAIN_DAMAGE);
    }

    private float wetBonus() {
        return (float) MobConfigHelper.get(this).number("wetBonus", DEFAULT_WET_BONUS);
    }

    private boolean chainsLightning() {
        return MobConfigHelper.get(this).flag("chainLightning", true);
    }

    private boolean bonusVsWet() {
        return MobConfigHelper.get(this).flag("bonusVsWet", true);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            shock(living);
        }
        return hurt;
    }

    /** A creature is "wet" (and so conducts extra) when it's in water or standing out in the rain. */
    private static boolean isWet(LivingEntity entity) {
        return entity.isInWaterRainOrBubble();
    }

    /**
     * Discharge on a landed melee hit: apply the wet bonus to the struck target, then (unless the
     * codec disables it) arc the charge from the target to the nearest few creatures, each hop dealing
     * {@link #chainDamage()} (plus the wet bonus to any wet hop). Server-side only; the arc is drawn
     * with electric-spark particles.
     */
    private void shock(LivingEntity primary) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // water conducts: an extra jolt to the target we just bit
        if (bonusVsWet() && isWet(primary)) {
            primary.hurt(this.damageSources().lightningBolt(), wetBonus());
        }
        arc(serverLevel, this, primary);
        burst(serverLevel, primary);  // a scatter flare at the point of impact

        if (!chainsLightning()) {
            return;
        }
        // gather the nearest living creatures around the struck target (skip self, the target, our
        // summoner, and anything allied to us) and hop the arc outward through them
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class,
                primary.getBoundingBox().inflate(chainRange()),
                e -> e != this && e != primary && e != this.getSummonedOwner() && e.isAlive() && !this.isAlliedTo(e));
        nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));

        int hops = Math.min(chainTargets(), nearby.size());
        LivingEntity from = primary;
        for (int i = 0; i < hops; i++) {
            LivingEntity next = nearby.get(i);
            float damage = (bonusVsWet() && isWet(next)) ? chainDamage() + wetBonus() : chainDamage();
            next.hurt(this.damageSources().lightningBolt(), damage);
            arc(serverLevel, from, next);
            from = next;
        }
    }

    /**
     * Draw a jagged string of custom electric-spark particles between two entities to sell the arc.
     * Each node is nudged off the straight line so the bolt zig-zags (strongest in the middle, pinned
     * at both ends).
     */
    private void arc(ServerLevel level, Entity from, Entity to) {
        Vec3 a = from.getEyePosition();
        Vec3 b = to.getEyePosition();
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            // zig-zag amount tapers to 0 at the endpoints
            double jitter = 0.35D * Math.sin(Math.PI * t);
            level.sendParticles(GMMParticles.ELECTRIC_SPARK.get(),
                    a.x + (b.x - a.x) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    a.y + (b.y - a.y) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    a.z + (b.z - a.z) * t + (this.random.nextDouble() - 0.5D) * jitter,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** A tight scatter of sparks around a struck creature — the flash of the shock landing. */
    private void burst(ServerLevel level, Entity at) {
        Vec3 c = at.getEyePosition();
        for (int i = 0; i < 6; i++) {
            level.sendParticles(GMMParticles.ELECTRIC_SPARK.get(),
                    c.x + (this.random.nextDouble() - 0.5D) * 0.6D,
                    c.y + (this.random.nextDouble() - 0.5D) * 0.6D,
                    c.z + (this.random.nextDouble() - 0.5D) * 0.6D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void aiStep() {
        if (this.isSunBurnTick()) {
            this.setSecondsOnFire(8);
        }
        // a faint crackle of stored charge so it's identifiable while alive
        if (this.level().isClientSide && this.random.nextInt(10) == 0) {
            this.level().addParticle(GMMParticles.ELECTRIC_SPARK.get(),
                    this.getRandomX(0.6D),
                    this.getY() + 0.1D + this.random.nextDouble() * 1.6D,
                    this.getRandomZ(0.6D),
                    0.0D, 0.0D, 0.0D);
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
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.74F;
    }
}
