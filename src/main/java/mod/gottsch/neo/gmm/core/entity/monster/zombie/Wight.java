package mod.gottsch.neo.gmm.core.entity.monster.zombie;





import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import mod.gottsch.neo.gmm.core.entity.ai.goal.EnthrallGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.RaiseShieldGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.SummonThrallGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.VariantPowerRangedBowAttackGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.entity.monster.ICastingMob;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The Wight: an elite undead that kept its wits (fights like a person -- see {@code WightModel}, no
 * shambling zombie arm pose) and carries a real weapon, drawn from the {@code wight/weapons} item tag
 * (gmm ships a sword + bow default; see {@link #reassessWeaponGoal()} for the melee/ranged switch,
 * mirroring {@code BowSkeleton}).
 *
 * <p>Its bite drains life: every landed hit applies a stacking, temporary max-health reduction
 * ({@link GMMMobEffects#WITHERED}) to the target and heals the Wight for a fraction of the damage
 * dealt. Alongside combat it runs two independent, continuously-cast abilities -- the same shape
 * Beholder uses for its own Enthrall + minion-summon kit, each with its own charge/cooldown and a
 * shared {@code maxThralls} cap: <b>Summon</b> ({@link SummonThrallGoal}, conjures a fresh mob from
 * the {@code wight/summon_allies} entity-type tag, then stamps it a permanent thrall the same way
 * {@link Ownership#enthrall} does) and <b>Enthrall</b> ({@link EnthrallGoal}, dominates an existing
 * nearby live mob from the {@code wight/enthrall_candidates} tag). Both tags ship a
 * {@code minecraft:zombie} default; consumers add their own zombie-family mobs additively. Both
 * goals emit their own charge-up particle telegraph (so the cast is visible before it lands) and
 * flip {@link #isCasting()} (via {@link ICastingMob}) for the duration, which {@code WightModel}
 * reads to swap in a channeling arm pose instead of the normal walk/attack swing.
 *
 * <p>Also spawns with an escort (see {@link #getCompanionPool()} / {@code gmm:wight/companions},
 * Companion Spawning in {@code GMMMonster}) -- a lone Wight is just an easy-to-kite caster, so a
 * small guard at spawn reads as an actual undead leader rather than a solitary target. Distinct from
 * the two thrall-raising tags above, which are combat-time abilities, not a spawn-time escort.
 *
 * <p>Flesh is a pale, bloodless recolor of the vanilla zombie base ({@code textures/entity/wight.png}
 * -- matches the corpse-white look of most D&amp;D depictions, not an ashen grey); the "faint armor
 * tint" from the catalog entry is a real equipped, dyed leather chestplate rendered by
 * {@code HumanoidArmorLayer} -- the recolor script only ever touches the flesh texture, never this
 * equipped "clothes" layer. The original ashen-grey ramp is preserved in the script as an
 * {@code ash_zombie} candidate rather than discarded.
 *
 * <p>A melee-rolled Wight may also carry a shield (from {@code wight/shields}, a configurable-chance
 * roll like SkeletonWarrior's) -- {@link RaiseShieldGoal} raises it whenever a target closes to melee
 * range, giving real vanilla shield-block damage reduction. A bow-rolled Wight never gets one: both
 * hands are already spoken for by the bow, and a ranged Wight never melees anyway (see
 * {@link #reassessWeaponGoal()}).
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class Wight extends GMMMonster implements RangedAttackMob, ICastingMob {

    private static final EntityDataAccessor<Boolean> DATA_CASTING =
            SynchedEntityData.defineId(Wight.class, EntityDataSerializers.BOOLEAN);

    // D&D 5e Wight stat block (CR3): 45 HP, scaled via the project's zombie-baseline ratio
    // (D&D 22 HP <-> MC 20 HP, ~0.91x -- same conversion the Mimic stat block uses): 45 * 20/22 ~ 41.
    private static final double DEFAULT_MAX_HEALTH = 41.0D;
    private static final double DEFAULT_MOVEMENT_SPEED = 0.25D;
    private static final double DEFAULT_ATTACK_DAMAGE = 4.0D;
    private static final double DEFAULT_ARMOR = 2.0D;

    // Withered (life-drain): duration per stack refresh, stack cap, and lifesteal fraction of damage dealt.
    private static final int DEFAULT_WITHER_DURATION = 200;   // 10s per refresh
    private static final int DEFAULT_MAX_WITHER_STACKS = 5;   // -10 HP at full stacks
    private static final double DEFAULT_LIFESTEAL = 0.5D;     // heals half the damage it deals

    // Summon / Enthrall: charge time (how long the cast takes), cooldown after casting, and a cap on
    // live thralls shared between both abilities.
    private static final int DEFAULT_SUMMON_CHARGE_TIME = 60;
    private static final int DEFAULT_SUMMON_COOLDOWN_TIME = 600;
    private static final double DEFAULT_SUMMON_SPAWN_RADIUS = 2.5D;
    private static final int DEFAULT_ENTHRALL_CHARGE_TIME = 100;
    private static final int DEFAULT_ENTHRALL_COOLDOWN_TIME = 600;
    private static final double DEFAULT_ENTHRALL_RANGE = 16.0D;
    private static final int DEFAULT_MAX_THRALLS = 3;

    // Shield (see RaiseShieldGoal): a configurable-chance roll, melee-only (see
    // populateDefaultEquipmentSlots) -- a bow-rolled Wight never carries one.
    private static final double DEFAULT_SHIELD_PROBABILITY = 0.35D;
    private static final double DEFAULT_SHIELD_BLOCK_RANGE = 4.0D;
    private static final int DEFAULT_SHIELD_BLOCK_COOLDOWN = 40;
    private static final int DEFAULT_SHIELD_MAX_BLOCK_TICKS = 100;

    // Ash-grey tattered burial wrap -- the equipped "clothes" (see class doc); never touched by the
    // flesh recolor script.
    private static final int ROBE_COLOR = 0x4A4A50;

    protected Goal meleeGoal = new MeleeAttackGoal(this, 1.0D, false);
    protected final VariantPowerRangedBowAttackGoal<Wight> bowGoal =
            new VariantPowerRangedBowAttackGoal<>(this, 1.0D, 20, 15.0F, 1.0F);

    public Wight(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // defaults to melee until equipment is set (finalizeSpawn, or setItemSlot during an NBT
        // load); mirrors BowSkeleton's own defensive call in its constructor.
        this.reassessWeaponGoal();
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING, false);
    }

    /** True while {@code SummonThrallGoal}/{@code EnthrallGoal} is charging up a cast (see {@code WightModel}). */
    public boolean isCasting() {
        return this.entityData.get(DATA_CASTING);
    }

    @Override
    public void setCasting(boolean casting) {
        this.entityData.set(DATA_CASTING, casting);
    }

    /** Spawns an escort alongside a natural/egg/summon spawn (see {@code GMMTags.EntityTypes.WIGHT_COMPANIONS}). */
    @Override
    protected TagKey<EntityType<?>> getCompanionPool() {
        return GMMTags.EntityTypes.WIGHT_COMPANIONS;
    }

    @Override
    protected void registerGoals() {
        MobConfig config = MobConfigHelper.get(this);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new SummonThrallGoal(this, GMMTags.EntityTypes.WIGHT_SUMMON_ALLIES,
                (int) config.number("summonChargeTime", DEFAULT_SUMMON_CHARGE_TIME),
                (int) config.number("summonCooldownTime", DEFAULT_SUMMON_COOLDOWN_TIME),
                (int) config.number("maxThralls", DEFAULT_MAX_THRALLS),
                config.number("summonSpawnRadius", DEFAULT_SUMMON_SPAWN_RADIUS)));
        this.goalSelector.addGoal(4, new EnthrallGoal(this, GMMTags.EntityTypes.WIGHT_ENTHRALL_CANDIDATES,
                (int) config.number("enthrallChargeTime", DEFAULT_ENTHRALL_CHARGE_TIME),
                (int) config.number("enthrallCooldownTime", DEFAULT_ENTHRALL_COOLDOWN_TIME),
                (int) config.number("maxThralls", DEFAULT_MAX_THRALLS),
                config.number("enthrallRange", DEFAULT_ENTHRALL_RANGE)));
        if (config.flag("shieldBlocking", true)) {
            this.goalSelector.addGoal(4, new RaiseShieldGoal(this,
                    config.number("shieldBlockRange", DEFAULT_SHIELD_BLOCK_RANGE),
                    (int) config.number("shieldBlockCooldown", DEFAULT_SHIELD_BLOCK_COOLDOWN),
                    (int) config.number("shieldMaxBlockTicks", DEFAULT_SHIELD_MAX_BLOCK_TICKS)));
        }
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
                .add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, DEFAULT_ATTACK_DAMAGE)
                .add(Attributes.ARMOR, DEFAULT_ARMOR);
    }

    // --- codec accessors -------------------------------------------------------------------------

    private boolean witherEnabled() {
        return MobConfigHelper.get(this).flag("wither", true);
    }

    private int witherDuration() {
        return (int) MobConfigHelper.get(this).number("witherDuration", DEFAULT_WITHER_DURATION);
    }

    private int maxWitherStacks() {
        return (int) MobConfigHelper.get(this).number("maxWitherStacks", DEFAULT_MAX_WITHER_STACKS);
    }

    private double lifestealFraction() {
        return MobConfigHelper.get(this).number("lifesteal", DEFAULT_LIFESTEAL);
    }

    private double shieldProbability() {
        return MobConfigHelper.get(this).number("shieldProbability", DEFAULT_SHIELD_PROBABILITY);
    }

    // --- equipment --------------------------------------------------------------------------------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficultyInstance,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        groupData = super.finalizeSpawn(levelAccessor, difficultyInstance, spawnType, groupData);
        this.populateDefaultEquipmentSlots(levelAccessor.getRandom(), difficultyInstance);
        this.reassessWeaponGoal();
        return groupData;
    }

    /**
     * Arms the Wight from the {@code wight/weapons} tag (sword or bow) and dons its tattered wrap.
     * A melee-rolled Wight may also roll a shield (see {@link #shieldProbability()}) from
     * {@code wight/shields} -- a bow-rolled one never does, both hands are already spoken for.
     */
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        Optional<Item> weapon = BuiltInRegistries.ITEM.getRandomElementOf(GMMTags.Items.WIGHT_WEAPONS, randomSource).map(Holder::value);
        weapon.ifPresent(item -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item)));

        if (!(this.getMainHandItem().getItem() instanceof BowItem) && randomSource.nextDouble() < shieldProbability()) {
            Optional<Item> shield = BuiltInRegistries.ITEM.getRandomElementOf(GMMTags.Items.WIGHT_SHIELDS, randomSource).map(Holder::value);
            shield.ifPresent(item -> {
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item));
                // flavor only, matches the robe/weapon below -- neither piece drops
                this.handDropChances[EquipmentSlot.OFFHAND.getIndex()] = 0.0F;
            });
        }

        ItemStack robe = new ItemStack(Items.LEATHER_CHESTPLATE);
        robe.set(DataComponents.DYED_COLOR, new DyedItemColor(ROBE_COLOR, true));
        this.setItemSlot(EquipmentSlot.CHEST, robe);
        // flavor only, not loot -- neither piece drops
        this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 0.0F;
        this.armorDropChances[EquipmentSlot.CHEST.getIndex()] = 0.0F;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (slot == EquipmentSlot.MAINHAND && this.level() != null && !this.level().isClientSide) {
            reassessWeaponGoal();
        }
    }

    /** Switches between melee and bow AI based on the currently held weapon (mirrors BowSkeleton). */
    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide) {
            return;
        }
        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            this.goalSelector.addGoal(4, this.bowGoal);
        } else {
            this.goalSelector.addGoal(4, this.meleeGoal);
        }
    }

    // --- ranged attack (bow) ----------------------------------------------------------------------

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack itemStack = this.getProjectile(weapon);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, itemStack, power, weapon);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dist * 0.2F, dz, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return weapon == Items.BOW;
    }

    // --- life drain -------------------------------------------------------------------------------

    @Override
    public boolean doHurtTarget(Entity target) {
        float healthBefore = (target instanceof LivingEntity living) ? living.getHealth() : 0.0F;
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            applyWither(living);
            float damageDealt = Math.max(0.0F, healthBefore - living.getHealth());
            double lifesteal = lifestealFraction();
            if (damageDealt > 0.0F && lifesteal > 0.0D) {
                this.heal((float) (damageDealt * lifesteal));
            }
        }
        return hurt;
    }

    /** Stacking, temporary max-health drain (see {@link GMMMobEffects#WITHERED}). */
    private void applyWither(LivingEntity target) {
        if (!witherEnabled()) {
            return;
        }
        MobEffectInstance existing = target.getEffect(GMMMobEffects.WITHERED);
        int amplifier = existing == null ? 0 : Math.min(maxWitherStacks() - 1, existing.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(GMMMobEffects.WITHERED, witherDuration(), amplifier, false, true, false), this);
        if (target.getHealth() > target.getMaxHealth()) {
            target.setHealth(target.getMaxHealth());
        }
    }

    // --- flavour / sounds -----------------------------------------------------------------------

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    public void aiStep() {
        if (this.isSunBurnTick()) {
            this.igniteForSeconds(8);
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
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
