package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;

import mod.gottsch.neo.gmm.core.entity.ai.goal.VariantPowerRangedBowAttackGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.temporal.ChronoField;

/**
 * Base for a bow-wielding skeleton: reassesses melee vs. ranged based on its held item, strafes and
 * fires arrows (via {@link VariantPowerRangedBowAttackGoal}), flees the sun, and targets players /
 * iron golems / baby turtles. Not extended from vanilla AbstractSkeleton but most behaviour matches.
 *
 * @author Mark Gottschling on Feb 3, 2024
 */
public abstract class BowSkeleton extends GMMMonster implements RangedAttackMob {

    protected final VariantPowerRangedBowAttackGoal<AbstractSkeleton> bowGoal = new VariantPowerRangedBowAttackGoal<>(this, 1.0D, 20, 15.0F, 1.0F);
    protected Goal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
        public void stop() {
            super.stop();
            BowSkeleton.this.setAggressive(false);
        }

        public void start() {
            super.start();
            BowSkeleton.this.setAggressive(true);
        }
    };
    protected final FleeSunGoal fleeSunGoal = new FleeSunGoal(this, 1.0D);

    protected BowSkeleton(EntityType<? extends BowSkeleton> entityType, Level level) {
        super(entityType, level);
        this.reassessWeaponGoal();
        this.goalSelector.addGoal(3, fleeSunGoal);
    }

    protected void registerGoals() {
        // NOTE Boulder-avoidance is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    protected void playStepSound(BlockPos p_32159_, BlockState p_32160_) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

     protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }

    public void aiStep() {
        boolean flag = this.isSunBurnTick();
        if (flag && getBurnsInSun()) {
            if (sunDamagesHat()) {
                ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
                if (!itemstack.isEmpty()) {
                    if (itemstack.isDamageableItem()) {
                        itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
                        if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                            this.onEquippedItemBroken(itemstack.getItem(), EquipmentSlot.HEAD);
                            this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        }
                    }
                    flag = false;
                }
            }
            if (flag) {
                this.igniteForSeconds(8);
            }
        }

        super.aiStep();
    }

    public void rideTick() {
        super.rideTick();
        Entity entity = this.getControlledVehicle();
        if (entity instanceof PathfinderMob pathfindermob) {
            this.yBodyRot = pathfindermob.yBodyRot;
        }

    }

    protected void populateDefaultEquipmentSlots(RandomSource p_218949_, DifficultyInstance p_218950_) {
        super.populateDefaultEquipmentSlots(p_218949_, p_218950_);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficultyInstance, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(levelAccessor, difficultyInstance, spawnType, spawnGroupData);
        RandomSource randomsource = levelAccessor.getRandom();
        this.populateDefaultEquipmentSlots(randomsource, difficultyInstance);
        this.populateDefaultEquipmentEnchantments(levelAccessor, randomsource, difficultyInstance);
        this.reassessWeaponGoal();
        this.setCanPickUpLoot(randomsource.nextFloat() < 0.55F * difficultyInstance.getSpecialMultiplier());
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            LocalDate localdate = LocalDate.now();
            int i = localdate.get(ChronoField.DAY_OF_MONTH);
            int j = localdate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && randomsource.nextFloat() < 0.25F) {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(randomsource.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EquipmentSlot.HEAD.getIndex()] = 0.0F;
            }
        }

        return spawnGroupData;
    }

    public void reassessWeaponGoal() {
        if (this.level() != null && !this.level().isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem));
            if (itemstack.is(Items.BOW)) {
                int i = 20;
                if (this.level().getDifficulty() != Difficulty.HARD) {
                    i = 40;
                }

                this.bowGoal.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.bowGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }

        }
    }

    public void performRangedAttack(LivingEntity entity, float power) {
        ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem));
        ItemStack itemStack = this.getProjectile(weapon);
        AbstractArrow abstractarrow = this.getArrow(itemStack, power, weapon);
        if (weapon.getItem() instanceof ProjectileWeaponItem weaponItem)
            abstractarrow = weaponItem.customArrow(abstractarrow, itemStack, weapon);
        double d0 = entity.getX() - this.getX();
        double d1 = entity.getY(0.3333333333333333D) - abstractarrow.getY();
        double d2 = entity.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        abstractarrow.shoot(d0, d1 + d3 * (double)0.2F, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(abstractarrow);
    }

    protected AbstractArrow getArrow(ItemStack itemStack, float power, @Nullable ItemStack weapon) {
        return ProjectileUtil.getMobArrow(this, itemStack, power, weapon);
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem p_32144_) {
        return p_32144_ == Items.BOW;
    }

    public void readAdditionalSaveData(CompoundTag p_32152_) {
        super.readAdditionalSaveData(p_32152_);
        this.reassessWeaponGoal();
    }

    public void setItemSlot(EquipmentSlot p_32138_, ItemStack p_32139_) {
        super.setItemSlot(p_32138_, p_32139_);
        if (!this.level().isClientSide) {
            this.reassessWeaponGoal();
        }

    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(1.74F);
    }

    public boolean getBurnsInSun() {
        return true;
    }

    public boolean sunDamagesHat() {
        return true;
    }

    public Goal getMeleeAttackGoal() {
        return this.meleeGoal;
    }

    public void setMeleeAttackGoal(Goal goal) {
        this.meleeGoal = goal;
    }

    public FleeSunGoal getFleeSunGoal() {
        return fleeSunGoal;
    }
}
