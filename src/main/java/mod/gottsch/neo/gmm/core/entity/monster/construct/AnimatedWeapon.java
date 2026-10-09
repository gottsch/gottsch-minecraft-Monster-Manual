package mod.gottsch.neo.gmm.core.entity.monster.construct;

import mod.gottsch.neo.gmm.core.entity.ai.goal.target.MobHurtByTargetGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMFlyingMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;

/**
 * D&D 5e's "Animated Weapon" construct: a masterless sword/axe that drifts through the air and
 * attacks on its own. Unlike {@link AnimatedArmor} this natural-spawns like any other hostile — it has
 * no wielder to give it away as a decoy, so there's no ambush/dormant state to gate. Rendered with
 * {@code AnimatedWeaponModel}, a body-less rig whose only real part is an invisible pivot the equipped
 * weapon hangs off of (see that class's doc) — the item itself is the entire visual identity.
 * <p>
 * Hover/attack shape mirrors {@code Beholderkin}'s (float-around + line-of-sight bite) rather than
 * vanilla's ground-pathing {@code MeleeAttackGoal}, since {@link FlyingMob} isn't a
 * {@code PathfinderMob} and can't use it. Kept as private nested classes local to this mob rather than
 * generalizing {@code Beholderkin}'s own nested goals — same "don't extract until a second consumer
 * actually needs the identical shape, not just a similar one" convention as {@code GatedGoal}'s history;
 * revisit if a third hovering construct shows up wanting the exact same float/bite pair.
 * <p>
 * {@link #isWindingUp()} drives a real telegraph (see {@code BiteGoal}): unlike vanilla's own
 * ~0.3s {@code swing()}/{@code attackAnim} (too short to read, and {@code getCurrentSwingDuration()}
 * isn't overridable to lengthen it), the strike is held back behind its own synced flag for a fixed
 * windup the client model poses distinctly (raised/pulled-back) — same "custom telegraph, not vanilla's
 * built-in swing" idiom {@code CastSpellGoal}'s charge tell and Bodak's Death Gaze windup already use.
 * {@code HoverMoveControl} also now turns the whole entity to face its target every tick (the same
 * {@code setYRot}/{@code yBodyRot} trick vanilla's own {@code Vex} uses) — previously nothing rotated
 * the entity at all, so it could land a hit facing any direction.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class AnimatedWeapon extends GMMFlyingMonster {

    private static final EntityDataAccessor<Boolean> DATA_WINDING_UP =
            SynchedEntityData.defineId(AnimatedWeapon.class, EntityDataSerializers.BOOLEAN);

    /** How long the windup pose takes to fully extend, in ticks — shared by {@code BiteGoal} (which
     * owns the actual timing/damage) and {@link #getWindupProgress} (which turns elapsed time into a
     * 0..1 animation blend for the model). */
    static final int WINDUP_DURATION = 15;

    // ticks elapsed since isWindingUp() most recently became true -- deliberately NOT synced data.
    // Entity#tick() runs identically on both logical sides, and this counter only resets off the
    // *edge* of the already-synced isWindingUp() flag, so both sides stay in lockstep without any
    // extra network traffic -- same trick GraveZombie's phaseTicks uses for its rise/reburrow blend.
    private int windupAnimTicks;
    private boolean wasWindingUp;

    public AnimatedWeapon(EntityType<? extends FlyingMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new HoverMoveControl(this);
        this.xpReward = 8;
    }

    @Override
    public void tick() {
        super.tick();
        if (isWindingUp()) {
            this.windupAnimTicks = this.wasWindingUp ? this.windupAnimTicks + 1 : 0;
            this.wasWindingUp = true;
        } else {
            this.windupAnimTicks = 0;
            this.wasWindingUp = false;
        }
    }

    /** 0 (just started winding up) to 1 (fully wound, about to strike) — partial-tick-interpolated for
     * a smooth client animation, same {@code getRiseAmount(float)} idiom {@code GraveZombie} uses.
     * {@code AnimatedWeaponRenderer} feeds this to the model each frame. */
    public float getWindupProgress(float partialTicks) {
        if (!isWindingUp()) {
            return 0.0F;
        }
        return Mth.clamp((this.windupAnimTicks + partialTicks) / (float) WINDUP_DURATION, 0.0F, 1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.3D)
                // was 6.0 -- read as far too hard-hitting for how easily/often it could land a hit
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                // a light floating object with 0 resistance got flung wildly on every hit, making
                // follow-up swings unpredictable on top of its already-small hitbox
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WINDING_UP, false);
    }

    /** Whether the weapon is mid-telegraph, about to strike -- see {@code BiteGoal} and the class doc.
     * Read by {@code AnimatedWeaponModel} to hold a distinct raised/pulled-back pose. */
    public boolean isWindingUp() {
        return this.entityData.get(DATA_WINDING_UP);
    }

    private void setWindingUp(boolean windingUp) {
        this.entityData.set(DATA_WINDING_UP, windingUp);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BiteGoal(this));
        this.goalSelector.addGoal(2, new FloatAroundGoal(this, 6));

        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new MobHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    /**
     * No shared base class calls {@code populateDefaultEquipmentSlots} for us -- vanilla's own leaf
     * mobs (Zombie, AbstractSkeleton, ...) each call it explicitly from their own {@code finalizeSpawn}
     * override, same idiom {@code SkeletonWarrior}/{@code Wight} already follow. Without this override
     * the mainhand roll below is dead code and the mob spawns with nothing to hold -- its entire visual
     * identity, since {@code AnimatedWeaponModel} has no body of its own.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return spawnGroupData;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        Optional<Item> weapon = ForgeRegistries.ITEMS.tags()
                .getTag(GMMTags.Items.ANIMATED_WEAPON_WEAPONS).getRandomElement(random);
        weapon.ifPresent(item -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item, 1)));
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F); // always drops its own weapon
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TRIDENT_RIPTIDE_1;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ITEM_BREAK;
    }

    /**
     * Smooth hover physics identical in shape to {@code Beholderkin.BeholderkinMoveControl} (nudges
     * toward the wanted point a few times a second rather than snapping), plus one addition Beholder's
     * version doesn't need: every tick it also turns the whole entity to face its target (or its
     * direction of travel if it has none) — the exact {@code setYRot}/{@code yBodyRot} trick vanilla's
     * own {@code Vex} uses in {@code VexMoveControl}. Without this the entity's rotation never changed
     * at all, so a swing could land facing any direction.
     */
    private static class HoverMoveControl extends MoveControl {
        private final AnimatedWeapon weapon;
        private int floatDuration;

        HoverMoveControl(AnimatedWeapon weapon) {
            super(weapon);
            this.weapon = weapon;
        }

        @Override
        public void tick() {
            LivingEntity target = weapon.getTarget();
            if (target != null) {
                double dx = target.getX() - weapon.getX();
                double dz = target.getZ() - weapon.getZ();
                weapon.setYRot(-((float) Mth.atan2(dx, dz)) * (180F / (float) Math.PI));
                weapon.yBodyRot = weapon.getYRot();
            }

            if (operation == Operation.MOVE_TO) {
                if (floatDuration-- <= 0) {
                    floatDuration += weapon.getRandom().nextInt(5) + 2;
                    Vec3 vec = new Vec3(wantedX - weapon.getX(), wantedY - weapon.getY(), wantedZ - weapon.getZ());
                    double distance = vec.length();
                    vec = vec.normalize();
                    if (canReach(vec, Mth.ceil(distance))) {
                        weapon.setDeltaMovement(weapon.getDeltaMovement().add(vec.scale(0.1D)));
                        if (target == null) {
                            // no target to face -- fall back to facing the direction of travel
                            weapon.setYRot(-((float) Mth.atan2(vec.x, vec.z)) * (180F / (float) Math.PI));
                            weapon.yBodyRot = weapon.getYRot();
                        }
                    } else {
                        operation = Operation.WAIT;
                    }
                }
            }
        }

        private boolean canReach(Vec3 vec, int distance) {
            AABB aabb = weapon.getBoundingBox();
            for (int i = 1; i < distance; ++i) {
                aabb = aabb.move(vec);
                if (!weapon.level().noCollision(weapon, aabb)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Line-of-sight melee with a real telegraph: once in range and off cooldown, the weapon holds a
     * {@link AnimatedWeapon#WINDUP_DURATION}-tick windup (synced via {@code setWindingUp}, posed
     * distinctly by {@code AnimatedWeaponModel} with a smooth partial-tick blend from
     * {@code getWindupProgress}, plus a couple of {@code ParticleTypes.CRIT} sparks each windup tick so
     * it reads from a distance too) before the hit actually lands — see the class doc for why this is
     * custom rather than vanilla's own (too-short, non-lengthenable) {@code swing()} animation.
     */
    private static class BiteGoal extends Goal {
        private static final int COOLDOWN = 50;
        // a normal-ish melee reach, independent of the weapon's own (tiny) hitbox size
        private static final double ATTACK_RANGE = 2.5D;
        private final AnimatedWeapon weapon;
        private int cooldownCount;
        private int windupTicks;

        BiteGoal(AnimatedWeapon weapon) {
            this.weapon = weapon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return weapon.getTarget() != null;
        }

        @Override
        public void start() {
            this.cooldownCount = COOLDOWN;
            this.windupTicks = 0;
        }

        @Override
        public void stop() {
            this.windupTicks = 0;
            this.weapon.setWindingUp(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = weapon.getTarget();
            if (target == null) {
                return;
            }
            // close the distance while a target is set (still true mid-windup, so it doesn't go inert)
            weapon.getMoveControl().setWantedPosition(target.getX(), target.getEyeY(), target.getZ(), 1.0D);

            if (weapon.isWindingUp()) {
                if (weapon.level() instanceof ServerLevel serverLevel && weapon.tickCount % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            weapon.getX(), weapon.getY() + weapon.getBbHeight() * 0.5D, weapon.getZ(),
                            2, 0.15D, 0.15D, 0.15D, 0.01D);
                }
                if (--windupTicks <= 0) {
                    weapon.setWindingUp(false);
                    if (getAttackReachSqr(target) >= weapon.distanceToSqr(target.getX(), target.getY(), target.getZ())
                            && weapon.hasLineOfSight(target)) {
                        weapon.doHurtTarget(target);
                    }
                    cooldownCount = 0;
                }
                return;
            }

            cooldownCount = Math.min(++cooldownCount, COOLDOWN);
            if (cooldownCount >= COOLDOWN
                    && getAttackReachSqr(target) >= weapon.distanceToSqr(target.getX(), target.getY(), target.getZ())
                    && weapon.hasLineOfSight(target)) {
                windupTicks = AnimatedWeapon.WINDUP_DURATION;
                weapon.setWindingUp(true);
            }
        }

        private double getAttackReachSqr(LivingEntity target) {
            double reach = ATTACK_RANGE + target.getBbWidth();
            return reach * reach;
        }
    }

    /** Idle wander: picks a nearby open-air point to drift toward. Identical shape to
     * {@code Beholderkin.BeholderkinRandomFloatAroundGoal}. */
    private static class FloatAroundGoal extends Goal {
        private final AnimatedWeapon weapon;
        private final int maxFloatHeight;
        private final Random random = new Random();

        FloatAroundGoal(AnimatedWeapon weapon, int maxFloatHeight) {
            this.weapon = weapon;
            this.maxFloatHeight = maxFloatHeight;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl moveControl = weapon.getMoveControl();
            if (!moveControl.hasWanted()) {
                return true;
            }
            double dx = moveControl.getWantedX() - weapon.getX();
            double dy = moveControl.getWantedY() - weapon.getY();
            double dz = moveControl.getWantedZ() - weapon.getZ();
            double delta = dx * dx + dy * dy + dz * dz;
            return delta < 1.0D || delta > 3600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            double x = weapon.getX() + (random.nextFloat() * 2.0F - 1.0F) * 8.0F;
            double y = weapon.getY() + (random.nextFloat() * 2.0F - 1.0F) * 8.0F;
            double z = weapon.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 8.0F;
            BlockPos targetPos = new BlockPos((int) x, (int) y, (int) z);

            if (weapon.level().isFluidAtPosition(targetPos,
                    fluidState -> fluidState.isSourceOfType(Fluids.WATER) || fluidState.isSourceOfType(Fluids.LAVA))) {
                return;
            }
            if (!weapon.level().getBlockState(targetPos).isAir()) {
                return;
            }

            double groundY = y;
            int minY = weapon.level().getMinBuildHeight();
            while (groundY > minY && weapon.level().getBlockState(new BlockPos((int) x, (int) groundY, (int) z)).isAir()) {
                groundY--;
            }
            y = Mth.clamp(y, groundY + 1, groundY + maxFloatHeight);
            weapon.getMoveControl().setWantedPosition(x, y, z, 1.0D);
        }
    }
}
