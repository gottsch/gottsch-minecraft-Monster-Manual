package mod.gottsch.neo.gmm.core.entity.monster.ghoul;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.ghoul.GhoulHealGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Slightly faster than zombie.
 * Burns in sun but for shorter time.
 * Attempts to shelter from sun.
 * Attacks all farm (chicken, cow, pig, sheep) animals
 * Picks up meats.
 * Heals self if standing still and holding meat.
 *
 * @author by Mark Gottschling on 11/6/2025
 */
public abstract class AbstractGhoul extends GMMMonster implements IGhoul {
    private final ItemStackHandler inventory = new ItemStackHandler(getInventoryMaxSize()); // example: 5 inventory slots

    public static final int INVENTORY_MAX_SIZE = 5;
    public static final String INVENTORY_TAG = "mobInventory";

    private boolean canOpenDoors;

    // hunting goals
    private final NearestAttackableTargetGoal<Chicken> attackChickenGoal = new NearestAttackableTargetGoal<>(this, Chicken.class, true);
    private final NearestAttackableTargetGoal<Cow> attackCowGoal = new NearestAttackableTargetGoal<>(this, Cow.class, true);
    private final NearestAttackableTargetGoal<Horse> attackHorseGoal = new NearestAttackableTargetGoal<>(this, Horse.class, true);
    private final NearestAttackableTargetGoal<Pig> attackPigGoal = new NearestAttackableTargetGoal<>(this, Pig.class, true);
    private final NearestAttackableTargetGoal<Sheep> attackSheepGoal = new NearestAttackableTargetGoal<>(this, Sheep.class, true);
    private final NearestAttackableTargetGoal<Turtle> attackTurtleGoal = new NearestAttackableTargetGoal<>(this, Turtle.class, true);

    /*
     *
     */
    protected AbstractGhoul(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(true);
        this.reassessHuntingGoal();
    }

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }

    @Override
    protected void playStepSound(BlockPos p_34316_, BlockState p_34317_) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));

        this.goalSelector.addGoal(3, new GhoulHealGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    public void reassessHuntingGoal() {
        if (this.level() == null || this.level().isClientSide) {
            return;
        }
        
        // remove goals
        this.targetSelector.removeGoal(this.attackChickenGoal);
        this.targetSelector.removeGoal(this.attackCowGoal);
        this.targetSelector.removeGoal(this.attackHorseGoal);
        this.targetSelector.removeGoal(this.attackPigGoal);
        this.targetSelector.removeGoal(this.attackSheepGoal);
        this.targetSelector.removeGoal(this.attackTurtleGoal);
        
        // add goals
        if (!isInventoryFull()) {
            this.targetSelector.addGoal(3, this.attackChickenGoal);
            this.targetSelector.addGoal(3, this.attackCowGoal);
            this.targetSelector.addGoal(3, this.attackHorseGoal);
            this.targetSelector.addGoal(3, this.attackPigGoal);
            this.targetSelector.addGoal(3, this.attackSheepGoal);
            this.targetSelector.addGoal(4, this.attackTurtleGoal);
        }        
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData) {
        // vanilla
        groupData =  super.finalizeSpawn(level, difficulty, spawnType, groupData);

        // tuning comes from the gmm:mob_config datapack registry, keyed by this mob's type id
        this.setCanOpenDoors(MobConfigHelper.get(level, EntityType.getKey(this.getType())).flag("canOpenDoors", true));
        if (this.canOpenDoors()) {
            // update navigation
            ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
            // update goals
            this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        }

        // determine if the ghoul should continue to hunt
        this.reassessHuntingGoal();

        double chance = this.random.nextDouble() * 1.5D * (double)difficulty.getSpecialMultiplier();
        if (chance > 1.0D) {
            this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier(GMMMonster.RANDOM_SPAWN_BONUS_ID, chance, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (this.random.nextFloat() < chance * 0.05F) {
            this.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(GMMMonster.RANDOM_SPAWN_BONUS_ID, this.random.nextDouble() * 3.0D + 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return groupData;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.put(INVENTORY_TAG, inventory.serializeNBT(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains(INVENTORY_TAG)) {
            inventory.deserializeNBT(this.registryAccess(), tag.getCompound(INVENTORY_TAG));
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        // Check if the effect is Poison
        if (effectInstance.getEffect() == MobEffects.POISON ||
            effectInstance.getEffect() == MobEffects.WITHER) {
            return false; // mob is immune to poison and wither
        }
        // for all other effects, use the default logic
        return super.canBeAffected(effectInstance);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        super.dropAllDeathLoot(level, damageSource);

        if (!this.level().isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack itemstack = inventory.getStackInSlot(i);
                if (!itemstack.isEmpty()) {
                    this.spawnAtLocation(itemstack);
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * wants to pick up meats, skins, furs, bones, etc
     */
    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return stack.is(GMMTags.Items.GHOUL_FOOD) && !isInventoryFull();
    }

    /**
     * override to ensure that the ghoul does not attempt to equip item
     */
    @Override
    public ItemStack equipItemIfPossible(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public int getOnFireDuration() {
        return 4;
    }

    @Override
    public boolean burnsInSun() {
        return true;
    }

    @Override
    public void aiStep() {
        // set on fire if in sun
        if(burnsInSun() && this.isSunBurnTick()) {
            this.igniteForSeconds(getOnFireDuration());
        }
        super.aiStep();
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        if (this.level().isClientSide || !itemEntity.isAlive()) {
            return;
        }

        getFirstEmptySlot().ifPresent(slot -> {
            ItemStack itemStackToInsert = itemEntity.getItem().copy();

            getInventory().insertItem(slot, itemStackToInsert, false);
            // call vanilla method to set last item pickup time/sound/etc.
            this.onItemPickup(itemEntity);
            itemEntity.discard();
        });
    }

    @Override
    public OptionalInt getFoodInventory() {
        return IntStream.range(0, this.inventory.getSlots())
                .filter(slot -> inventory.getStackInSlot(slot).is(GMMTags.Items.GHOUL_FOOD))
                .findFirst();
    }

    @Override
    public float getHealAmount() {
        return (float) MobConfigHelper.get(this).number("healAmount", 4.0);
    }

    public int getInventoryMaxSize() {
        return INVENTORY_MAX_SIZE;
    }

    /*
     * doesn't check if there is room in the slots, only if there is
     * an empty slot.
     */
    public boolean isInventoryFull() {
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public OptionalInt getFirstEmptySlot() {
        return IntStream.range(0, this.inventory.getSlots())
                .filter(slot -> inventory.getStackInSlot(slot).isEmpty())
                .findFirst();
    }

    @Override
    public boolean canOpenDoors() {
        return canOpenDoors;
    }

    @Override
    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }
}
