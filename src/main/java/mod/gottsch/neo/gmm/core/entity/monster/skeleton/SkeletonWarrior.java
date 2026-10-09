package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.RaiseShieldGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import mod.gottsch.neo.gmm.core.util.EquipmentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Is not extended from Skeleton/AbstractSkeleton but most of the functionality is the same.
 * Equipment is data-driven: each slot is filled from a consumer-populated item tag
 * (SKELETON_WARRIOR_WEAPONS / _HELMETS / _CHESTPLATES / _LEGGINGS / _BOOTS / _SHIELDS).
 * <p>
 * Unlike the always-armed weapon/armor rolls, a shield (from {@code SKELETON_WARRIOR_SHIELDS}) is a
 * configurable-chance roll ({@code shieldProbability}) — not every warrior carries one. When equipped,
 * {@link RaiseShieldGoal} raises it whenever a target closes to melee range, giving real vanilla
 * shield-block damage reduction (see that goal's own doc for why this works on a {@code Mob} at all).
 *
 * @author Mark Gottschling on Jan 19, 2024
 */
public class SkeletonWarrior extends GMMMonster {

    // Shield (see RaiseShieldGoal): a configurable-chance roll, unlike the always-armed weapon --
    // a rank-and-file warrior isn't guaranteed to be carrying one.
    private static final double DEFAULT_SHIELD_PROBABILITY = 0.35D;
    private static final double DEFAULT_SHIELD_BLOCK_RANGE = 4.0D;
    private static final int DEFAULT_SHIELD_BLOCK_COOLDOWN = 40;
    private static final int DEFAULT_SHIELD_MAX_BLOCK_TICKS = 100;

    public SkeletonWarrior(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        MobConfig config = MobConfigHelper.get(this);
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        if (config.flag("shieldBlocking", true)) {
            this.goalSelector.addGoal(4, new RaiseShieldGoal(this,
                    config.number("shieldBlockRange", DEFAULT_SHIELD_BLOCK_RANGE),
                    (int) config.number("shieldBlockCooldown", DEFAULT_SHIELD_BLOCK_COOLDOWN),
                    (int) config.number("shieldMaxBlockTicks", DEFAULT_SHIELD_MAX_BLOCK_TICKS)));
        }
        this.goalSelector.addGoal(5, new MoveThroughVillageGoal(this, 1.0D, true, 4, () -> true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    private double shieldProbability() {
        return MobConfigHelper.get(this).number("shieldProbability", DEFAULT_SHIELD_PROBABILITY);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficultyInstance) {
        super.populateDefaultEquipmentSlots(randomSource, difficultyInstance);

        // randomize armor over a random subset of slots
        List<EquipmentSlot> availableSlots = new ArrayList<>(List.of(EquipmentUtil.ARMOR_SLOTS));
        for (int num = 0; num < random.nextInt(availableSlots.size()); num++) {
            EquipmentSlot slot = availableSlots.get(random.nextInt(availableSlots.size()));
            setRandomEquipment(slot);
            availableSlots.remove(slot);
        }

        // always randomize the weapon
        setRandomEquipment(EquipmentSlot.MAINHAND);

        // shield: a configurable-chance roll (see RaiseShieldGoal), separate from the always-armed
        // weapon above -- not every rank-and-file warrior carries one.
        if (this.random.nextDouble() < shieldProbability()) {
            setRandomEquipment(EquipmentSlot.OFFHAND);
        }
    }

    protected void setRandomEquipment(EquipmentSlot slot) {
        /* NOTE setItemSlotAndDropWhenKilled() makes the entity persistent (not despawnable),
         * which we don't want for spawned equipment - so set drop chances directly. */
        if (slot.isArmor()) {
            this.armorDropChances[slot.getIndex()] = 0.75F;
        } else {
            this.handDropChances[slot.getIndex()] = 0.75F;
        }
        this.setItemSlot(slot, selectRandomEquipment(slot));
    }

    protected ItemStack selectRandomEquipment(EquipmentSlot slot) {
        Optional<Item> equipment = ForgeRegistries.ITEMS.tags().getTag(tagForSlot(slot)).getRandomElement(this.random);
        if (equipment.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack equipmentStack = new ItemStack(equipment.get(), 1);
        // spawn with worn (randomly damaged) gear
        if (equipmentStack.isDamageableItem()) {
            equipmentStack.setDamageValue(this.random.nextInt(equipmentStack.getMaxDamage() - (int) (equipmentStack.getMaxDamage() * 0.1)));
        }
        return equipmentStack;
    }

    private static TagKey<Item> tagForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> GMMTags.Items.SKELETON_WARRIOR_HELMETS;
            case CHEST -> GMMTags.Items.SKELETON_WARRIOR_CHESTPLATES;
            case LEGS -> GMMTags.Items.SKELETON_WARRIOR_LEGGINGS;
            case FEET -> GMMTags.Items.SKELETON_WARRIOR_BOOTS;
            case OFFHAND -> GMMTags.Items.SKELETON_WARRIOR_SHIELDS;
            default -> GMMTags.Items.SKELETON_WARRIOR_WEAPONS; // MAINHAND
        };
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        groupData = super.finalizeSpawn(levelAccessor, difficultyInstance, spawnType, groupData, tag);

        RandomSource randomSource = levelAccessor.getRandom();
        this.populateDefaultEquipmentSlots(randomSource, difficultyInstance);
        this.populateDefaultEquipmentEnchantments(randomSource, difficultyInstance);

        this.setCanPickUpLoot(true);

        return groupData;
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        // any armor item, or a sword/axe
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return EquipmentUtil.ARMOR_LIST.contains(slot)
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem;
    }

    @Override
    public @NotNull ItemStack equipItemIfPossible(@NotNull ItemStack pickedUpStack) {
        EquipmentSlot slot = getEquipmentSlotForItem(pickedUpStack);
        ItemStack currentEquipmentStack = this.getItemBySlot(slot);
        if (currentEquipmentStack.isEmpty()) {
            ItemStack pickedUpStackCopy = pickedUpStack.copyWithCount(1);
            this.setItemSlotAndDropWhenKilled(slot, pickedUpStackCopy);
            return pickedUpStackCopy;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
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

    protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.UNDEAD;
    }

    // NOTE helmet protects from sun
    @Override
    public void aiStep() {
        if (this.isSunBurnTick()) {
            boolean flag = this.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            if (flag) {
                this.setSecondsOnFire(8);
            }
        }
        super.aiStep();
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.74F;
    }
}
