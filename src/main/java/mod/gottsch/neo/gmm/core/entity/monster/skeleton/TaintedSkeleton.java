package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/**
 * A skeleton corrupted by an unstable "taint" shard fused into its ribcage (inspired by Torchlight
 * 2's Ember Blight). It fights in melee like a normal skeleton but the shard **detonates when it
 * dies**, throwing a burst of bone shrapnel — the threat is killing it at close range. Trails a
 * faint unstable wisp. Explosion radius / shrapnel count / block-destruction are codec-driven
 * ({@code gmm:mob_config}); by default the blast damages entities but not terrain.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class TaintedSkeleton extends GMMMonster {

    // The shard-burst is the signature attack; the blast is only a small punctuation (leave the
    // big explosion to the Creeper).
    private static final float DEFAULT_EXPLOSION_RADIUS = 1.5F;
    private static final int DEFAULT_SHRAPNEL_COUNT = 14;

    /**
     * Consumer-supplied factory for the shrapnel projectile (gmm registers no EntityTypes). DD wires
     * this to a {@code BoneShard}; if left null the shrapnel falls back to a vanilla arrow.
     */
    public static BiFunction<LivingEntity, Level, AbstractArrow> shardFactory;

    private boolean hasExploded;

    public TaintedSkeleton(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // pursue at 1.2x so it quickly closes on the target (the threat is dying at close range)
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                // a touch quicker than a vanilla skeleton — it rushes to close the distance and detonate
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    private float explosionRadius() {
        return (float) MobConfigHelper.get(this).number("explosionRadius", DEFAULT_EXPLOSION_RADIUS);
    }

    private int shrapnelCount() {
        return (int) MobConfigHelper.get(this).number("shrapnelCount", DEFAULT_SHRAPNEL_COUNT);
    }

    private boolean destroysBlocks() {
        return MobConfigHelper.get(this).flag("destroyBlocks", false);
    }

    /** Detonate when killed (the whole point). Idempotent. */
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.explode();
    }

    private void explode() {
        if (this.level().isClientSide || this.hasExploded) {
            return;
        }
        this.hasExploded = true;
        Level.ExplosionInteraction interaction = this.destroysBlocks()
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius(), interaction);
        this.spawnShrapnel();
        this.discard();
    }

    /** Flings a handful of bone arrows radially — the "shrapnel". */
    private void spawnShrapnel() {
        int count = shrapnelCount();
        for (int i = 0; i < count; i++) {
            AbstractArrow shard = (shardFactory != null)
                    ? shardFactory.apply(this, this.level()) : new Arrow(this.level(), this);
            // full spherical scatter (slight upward bias so shards fan out rather than straight down)
            double dx = this.random.nextGaussian();
            double dy = this.random.nextGaussian() * 0.6D + 0.25D;
            double dz = this.random.nextGaussian();
            shard.setPos(this.getX(), this.getY(0.5D), this.getZ());
            shard.shoot(dx, dy, dz, 0.9F, 12.0F);
            shard.setBaseDamage(2.0D);
            shard.pickup = AbstractArrow.Pickup.DISALLOWED;
            this.level().addFreshEntity(shard);
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
        // a faint 'unstable' wisp so it's identifiable while alive
        if (this.level().isClientSide && this.random.nextInt(8) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE,
                    this.getRandomX(0.5D), this.getY() + this.random.nextDouble() * 1.6D, this.getRandomZ(0.5D),
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
