package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A slow-moving ooze that, unlike vanilla {@code Slime}, splits <em>on being struck by a melee
 * weapon</em> rather than only on death — deliberately different from Slime's death/size-based split
 * (see the {@code MobIdeasCatalog}'s "Ochre Jelly" entry: "differentiate via split-on-hit + fire
 * counter"). A hit only counts as a splitting blow if it's a direct melee strike (the attacker itself,
 * not a thrown/shot projectile, an explosion, or magic (spells/potions) — see
 * {@link #isMeleeWeaponHit}); fire instead deals bonus damage and never triggers a split.
 * <p>
 * Each split spawns one new, full-size jelly at a fraction of this jelly's max health (config
 * {@code splitHealthFraction}, default 3/4). Only a jelly with {@link #splitsRemaining} still above
 * zero can split — a jelly spawned <em>by</em> a split always starts at zero and can never itself
 * split, regardless of config, so growth is strictly linear rather than exponential: one jelly caps
 * out at {@code 1 + maxSplits} total (default {@code maxSplits} 2 → up to 3). It shares
 * {@link GelatinousCube}'s non-bouncing, non-legged ground movement (reuses vanilla's
 * {@code SlimeModel}) but has none of its engulf ability — splitting is this mob's whole gimmick.
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class OchreJelly extends GMMMonster {

    // a distinct yellow-ochre recolor from GelatinousCube's tan/amber — see OchreJellyRenderer
    private static final float TRAIL_R = 0.68F;
    private static final float TRAIL_G = 0.72F;
    private static final float TRAIL_B = 0.24F;

    private static final float DEFAULT_FIRE_DAMAGE_MULTIPLIER = 1.5F;
    private static final int DEFAULT_MAX_SPLITS = 2;
    private static final double DEFAULT_SPLIT_HEALTH_FRACTION = 0.75D;
    // twice vanilla MeleeAttackGoal's own 20-tick cadence — see GelatinousCube for why this is needed
    private static final int DEFAULT_ATTACK_COOLDOWN = 40;

    // server-only bookkeeping: never rendered, never touched client-side, so no synced data needed.
    // only ever nonzero for a naturally-spawned/spawn-egg jelly (see finalizeSpawn) — a split-spawned
    // child is always created with this at 0, so it can never itself split.
    private int splitsRemaining = DEFAULT_MAX_SPLITS;
    private int lastAttackTick = -1000;

    public OchreJelly(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    /** Reads the optional {@code maxSplits} config once at (natural/spawner/summon-goal) spawn. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        int maxSplits = (int) MobConfigHelper.get(this).number("maxSplits", DEFAULT_MAX_SPLITS);
        this.splitsRemaining = Math.max(0, maxSplits);
        return spawnGroupData;
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
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
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

    /** Same self-imposed cooldown gate GelatinousCube uses — see that class for why. */
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
        }
        return hurt;
    }

    /**
     * Fire deals bonus damage and never splits; a direct melee strike (see {@link #isMeleeWeaponHit})
     * that this jelly survives, with splits still available, triggers {@link #split()}.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            float multiplier = (float) MobConfigHelper.get(this).number("fireDamageMultiplier", DEFAULT_FIRE_DAMAGE_MULTIPLIER);
            return super.hurt(source, amount * multiplier);
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && this.isAlive() && this.splitsRemaining > 0
                && MobConfigHelper.get(this).flag("split", true) && isMeleeWeaponHit(source)) {
            split();
        }
        return hurt;
    }

    /** A direct hand-to-hand strike — sword, axe, shovel, bare fist, etc — as opposed to a thrown/shot
     * projectile, an explosion, or magic (arrows, TNT, and spells/potions don't trigger a split). */
    private boolean isMeleeWeaponHit(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        Entity causing = source.getEntity();
        return causing instanceof LivingEntity && direct == causing;
    }

    /**
     * Spawns one new, full-size jelly alongside this one (which is otherwise unaffected beyond the
     * damage it just took) at a fraction of this jelly's max health. The new jelly's own
     * {@code splitsRemaining} is always 0 — it can never split itself, no matter what hits it.
     */
    private void split() {
        if (this.level().isClientSide) {
            return;
        }
        this.splitsRemaining--;
        double healthFraction = MobConfigHelper.get(this).number("splitHealthFraction", DEFAULT_SPLIT_HEALTH_FRACTION);

        Entity spawned = this.getType().create(this.level());
        if (spawned instanceof OchreJelly child) {
            child.splitsRemaining = 0;
            double newMaxHealth = Math.max(4.0D, this.getMaxHealth() * healthFraction);
            AttributeInstance healthAttribute = child.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(newMaxHealth);
            }
            child.setHealth((float) newMaxHealth);
            double offsetX = (this.random.nextDouble() - 0.5D) * 0.8D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.8D;
            child.moveTo(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ, this.random.nextFloat() * 360.0F, 0.0F);
            if (this.getTarget() != null) {
                child.setTarget(this.getTarget());
            }
            this.level().addFreshEntity(child);
        }

        this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        ((ServerLevel) this.level()).sendParticles(ParticleTypes.ITEM_SLIME,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                12, this.getBbWidth() * 0.3D, this.getBbHeight() * 0.3D, this.getBbWidth() * 0.3D, 0.05D);
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

    /** See {@code GelatinousCube#isMoving} — same client-safe walkAnimation-based check. */
    private boolean isMoving() {
        return this.onGround() && this.walkAnimation.isMoving();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SplitsRemaining", this.splitsRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SplitsRemaining")) {
            this.splitsRemaining = tag.getInt("SplitsRemaining");
        }
    }
}
