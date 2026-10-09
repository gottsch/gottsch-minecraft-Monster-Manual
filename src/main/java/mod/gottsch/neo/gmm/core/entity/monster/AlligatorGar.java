package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.entity.ai.goal.ReturnToWaterGoal;
import mod.gottsch.neo.gmm.core.entity.ai.navigation.FishPathNavigation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import static net.neoforged.neoforge.common.ForgeMod.SWIM_SPEED;

/**
 * @author by Mark Gottschling on 10/31/2025
 */
public class AlligatorGar extends WaterAnimal {
    public static final float FISH_WIDTH = 0.6F;  // narrow
    public static final float FISH_HEIGHT = 0.4F; // short
    private static final float FISH_LENGTH = 1.0F; // used for effective swimming size

    public AlligatorGar(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);

        // update the pathfinding so that the fish doesn't attempt to walk on land
        this.setPathfindingMalus(BlockPathTypes.WALKABLE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.OPEN, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        // optional: ensure water is favorable
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 10.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (pose == Pose.SWIMMING) {
            // when swimming, we swap the width and height to represent
            // a long, horizontal object rather than a short, wide one.
            // use a generous width (for length) and the narrow height (for thickness).
            return EntityDimensions.scalable(FISH_LENGTH, FISH_HEIGHT); // 1.0F long (width), 0.4F high (height)
        }

        // Use the default dimensions for all other poses (like STANDING, when on land).
        return super.getDimensions(pose).scale(this.getScale());
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        // inject custom fish navigator here
        return new FishPathNavigation(this, pLevel);
    }

    @Override
    public float getPathfindingMalus(BlockPathTypes pathType) {
        // if the fish is on land, temporarily ignore the penalty for walking.
        // this allows the navigation system to pathfind across land to water.
        if (!this.isInWater() && pathType == BlockPathTypes.WALKABLE) {
            return 0.0F; // no penalty for walking to let it escape
        }

        // otherwise, use the standard penalties set in the constructor (e.g., -1.0F)
        return super.getPathfindingMalus(pathType);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                // we added a custom navigator that is based on GroundPathNavigation,
                // so the mob uses MOVEMENT_SPEED instead of SWIM_SPEED
                .add(Attributes.MOVEMENT_SPEED,1D)
                .add(SWIM_SPEED.get(), 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ReturnToWaterGoal(this, 0.25D)); // Use a faster speed (1.2D)

        // 1. custom Melee Attack Goal (checks if the mob is in water)
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1D, false) {
            @Override
            public boolean canUse() {
                // must be in water AND satisfy the base MeleeAttackGoal requirements
                return this.mob.isInWater()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                // stop if it leaves water OR if the base conditions fail (e.g., target dies/leaves range)
                return this.mob.isInWater() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource p_29795_) {
        return SoundEvents.SALMON_HURT;
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.SALMON_FLOP;
    }
}
