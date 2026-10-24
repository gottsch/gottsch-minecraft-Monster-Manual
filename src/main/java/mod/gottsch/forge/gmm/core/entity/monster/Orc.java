package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.HuntingBreakDoorGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.ThrowProjectileGoal;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * A burly humanoid that fights in melee or, sometimes, as a ranged thrower (its hand is left empty
 * and it lobs a projectile). Cosmetic variety -- shoulder pads, hair, bracers -- is rolled at spawn
 * and synced for the model.
 * <p>
 * Equipment is data-driven: the weapon is chosen from the consumer-populated {@code gmm:orc/weapons}
 * item tag. The thrown projectile is supplied entirely by the consumer via {@link #rockLauncher}, so
 * this class has no knowledge of any concrete projectile -- it stays in the shared library.
 *
 * <h2>Registering it: keep the width UNDER 1.0 if it has to use doorways</h2>
 * <p>GMM registers no entity types, so the hitbox is the consumer's call -- and the obvious 1.0 is
 * the wrong one. Vanilla's ground pathfinder sizes a mob as {@code Mth.floor(bbWidth + 1.0F)} blocks
 * ({@code NodeEvaluator#prepare}), so a width of exactly 1.0 counts as <em>two</em> blocks and the
 * orc stalls at every 1-wide doorway or corridor. Register it at {@code sized(0.9F, 1.99F)}: that
 * floors to one block, and nothing in GMM reads the width in a way that changes how the orc looks or
 * fights (checked 2026-10-24):</p>
 * <ul>
 *   <li>Melee reach is vanilla's {@code (2w)^2 + targetWidth}: about 2.14 blocks centre-to-centre
 *       against a player at 1.0, 1.96 at 0.9. The gap between the two hitboxes at the moment of
 *       the swing shrinks by only 0.13 of a block, and a player's 3-block reach out-ranges the orc
 *       either way. The ranged orc's point-blank fallback ({@link ThrowProjectileGoal}) uses the
 *       same formula and moves by the same amount.</li>
 *   <li>{@code OrcRenderer} uses a fixed 0.8 shadow and {@code OrcModel} never reads the hitbox. The
 *       rig's shoulders are already 1.25 blocks across, wider than either hitbox.</li>
 * </ul>
 * <p>Two caveats. A <em>door block</em> is narrower than its cell: an open door's panel takes 3px,
 * and a mob walking the cell centre clears it only at width 0.625 or less (a vanilla zombie's 0.6
 * just fits). 0.9 passes an empty 1-wide opening, not one with a door in it -- worked out from the
 * collision shapes, not measured in game. And height counts too: 1.99 fits a 2-high doorway.</p>
 *
 * <h2>Doors</h2>
 * <p>Opt-in per entity id: with the {@code gmm:mob_config} flag {@code breakDoors} set, an orc that
 * has a target breaks wooden doors in its way (closed ones, and open ones it is too wide to get
 * past) via {@link HuntingBreakDoorGoal}, on any difficulty. Without a target it treats doors as
 * walls. Off by default, because door-breaking is griefing and a consumer's overworld orcs should
 * not start smashing village doors on a library update; a dungeon consumer turns it on for its own
 * orcs:</p>
 * <pre>{@code
 * // data/<consumer>/gmm/mob_config/orc.json
 * { "flags": { "breakDoors": true }, "properties": { "doorBreakTime": 240 } }
 * }</pre>
 * <p>{@code doorBreakTime} is in ticks (default 240, vanilla's zombie time). Each subclass reads its
 * own entity id, so the Shaman and Warlord need their own files. It needs the {@code mobGriefing}
 * game rule on, and iron doors stop it.</p>
 *
 * @author Mark Gottschling on Apr 27, 2022
 */
public class Orc extends GMMMonster {
    private static final EntityDataAccessor<Byte> DATA = SynchedEntityData.defineId(Orc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> IS_RANGED = SynchedEntityData.defineId(Orc.class, EntityDataSerializers.BOOLEAN);
    private static final String DATA_TAG = "data";
    private static final String IS_RANGED_TAG = "isRanged";
    protected static final byte RIGHT_SHOULDER_PAD = 0;
    protected static final byte LEFT_SHOULDER_PAD = 1;
    protected static final byte HAIR = 2;
    protected static final byte BRACERS = 3;

    /**
     * Every apparel bit set: both shoulder pads, the hair and the bracers.
     *
     * <p>Composed from the bit positions rather than written as a literal, so a fifth piece added to
     * the model is worn by whoever asks for the full set without anyone remembering to widen a
     * magic number. {@link OrcWarlord} is the one that asks &mdash; a chief in a random half of his
     * kit reads as a grunt with a better weapon.</p>
     *
     * <p>Note it includes HAIR, which is not armour. The bits are one model-part field and the full
     * set is the whole rig, which is the useful thing to be able to name; a subclass wanting the
     * armour only can compose its own from the positions above.</p>
     */
    protected static final byte ALL_APPAREL = (byte) ((1 << RIGHT_SHOULDER_PAD)
            | (1 << LEFT_SHOULDER_PAD) | (1 << HAIR) | (1 << BRACERS));

    /**
     * Consumer-supplied launcher for the Orc's ranged throw. The shared library owns no projectile,
     * so a consuming mod sets this (e.g. in its common setup) to create + launch its own projectile.
     * Left null, a ranged Orc simply throws nothing (it still postures and melees at point-blank).
     */
    public static ThrowProjectileGoal.ProjectileLauncher projectileLauncher;

    /** Vanilla's zombie door-break time: 12 seconds of hacking before the door gives way. */
    private static final int DEFAULT_DOOR_BREAK_TIME = 240;

    /**
     * Set in {@link #registerGoals()}, which the {@code Mob} constructor calls before this class's
     * field initializers run -- so this field must NOT have an initializer, or it would wipe the goal
     * out again. Null when the {@code breakDoors} config flag is off.
     */
    private HuntingBreakDoorGoal breakDoorGoal;

    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.0D, false);
    private final ThrowProjectileGoal throwGoal =
            new ThrowProjectileGoal(this, 1.0D, 40, 16F, mob -> ((Orc) mob).isRanged(), projectileLauncher);

    public Orc(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        Arrays.fill(this.handDropChances, 0.25F);
        this.reassessWeaponGoal();
    }

    @Override
    protected void registerGoals() {
        // NOTE Boulder-avoidance is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
        // Smash through wooden doors to reach a target. OFF by default: it is griefing, so a consumer
        // opts in per entity id (Dungeons2's dungeon orcs do; Dungeon Denizens' overworld ones don't).
        // Declares no flags, so it runs alongside the attack goal rather than competing with it.
        if (MobConfigHelper.get(this).flag("breakDoors", false)) {
            this.breakDoorGoal = new HuntingBreakDoorGoal(this,
                    () -> (int) MobConfigHelper.get(this).number("doorBreakTime", DEFAULT_DOOR_BREAK_TIME));
            this.goalSelector.addGoal(1, this.breakDoorGoal);
        }
        // the priority-4 attack goal (melee or throwing) is added by reassessWeaponGoal() based on isRanged()
        this.goalSelector.addGoal(5, new MoveThroughVillageGoal(this, 1.0D, true, 4, () -> true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.25D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.2D)
                .add(Attributes.ARMOR, (double) 2.0F)
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    /** Lets the pathfinder plan through closed doors only while hunting; see {@link HuntingBreakDoorGoal}. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.breakDoorGoal != null) {
            this.breakDoorGoal.updateNavigation();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData, CompoundTag tag) {
        double healthBonus = round(random.nextDouble() * 10D, 2);
        this.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", healthBonus, AttributeModifier.Operation.ADDITION));

        double attackBonus = round(random.nextDouble() * 2.5D, 2);
        this.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", attackBonus, AttributeModifier.Operation.ADDITION));

        double armorBonus = round(random.nextDouble() * 2D, 2);
        this.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", armorBonus, AttributeModifier.Operation.ADDITION));

        double armorToughBonus = round(random.nextDouble() * 5D, 2);
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", armorToughBonus, AttributeModifier.Operation.ADDITION));

        double knockbackResistanceBonus = random.nextDouble() * 0.1D;
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", knockbackResistanceBonus, AttributeModifier.Operation.ADDITION));

        double attackKnockbackBonus = round(random.nextDouble() * 1.5D, 2);
        this.getAttribute(Attributes.ATTACK_KNOCKBACK).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", attackKnockbackBonus, AttributeModifier.Operation.ADDITION));

        double chance = this.random.nextDouble() * 1.5D * (double) difficulty.getSpecialMultiplier();
        if (chance > 1.0D) {
            this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("random orc-spawn bonus", chance, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // arm the orc
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);

        // chance to be a ranged thrower instead of a melee fighter (gmm:mob_config datapack)
        double rangedProbability = MobConfigHelper.get(level, EntityType.getKey(this.getType())).number("rangedProbability", 0.15);
        if (this.random.nextDouble() < rangedProbability) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.setRanged(true);
        }
        this.reassessWeaponGoal();

        Random random = new Random();
        byte data = 0;
        if (random.nextBoolean()) {
            data |= 1 << RIGHT_SHOULDER_PAD; // 0 = right shoulder pad
        }
        if (random.nextBoolean()) {
            data |= 1 << LEFT_SHOULDER_PAD;
        }
        if (random.nextBoolean()) {
            data |= 1 << HAIR;
        }
        if (random.nextBoolean()) {
            data |= 1 << BRACERS;
        }
        setData(data);

        return groupData;
    }

    /**
     * Swaps the priority-4 attack goal between melee and ranged throwing based on {@link #isRanged()},
     * unless a subclass supplies its own {@link #getCombatGoalOverride()}.
     */
    public void reassessWeaponGoal() {
        if (this.level() != null && !this.level().isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.throwGoal);
            Goal override = getCombatGoalOverride();
            if (override != null) {
                this.goalSelector.removeGoal(override);
                this.goalSelector.addGoal(4, override);
            } else if (this.isRanged()) {
                this.goalSelector.addGoal(4, this.throwGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }
        }
    }

    /**
     * Hook for a subclass to fully replace the priority-4 melee/throw switch with its own combat
     * goal (e.g. a caster that stands off and only melees at point-blank). Returning {@code null}
     * (the default) keeps the normal melee/throw behavior driven by {@link #isRanged()}.
     */
    protected Goal getCombatGoalOverride() {
        return null;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        // 1-in-5 chance of empty hands (a ranged-thrower candidate); otherwise a random weapon from the tag
        if (this.random.nextInt(5) == 4) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        } else {
            Optional<Item> weapon = ForgeRegistries.ITEMS.tags().getTag(GMMTags.Items.ORC_WEAPONS).getRandomElement(this.random);
            this.setItemSlot(EquipmentSlot.MAINHAND, weapon.map(item -> new ItemStack(item)).orElse(ItemStack.EMPTY));
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        // setup armor
        if (this.random.nextInt(5) == 0) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA, (byte) 0);
        this.entityData.define(IS_RANGED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte(DATA_TAG, getData());
        tag.putBoolean(IS_RANGED_TAG, isRanged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DATA_TAG)) {
            this.setData(tag.getByte(DATA_TAG));
        }
        if (tag.contains(IS_RANGED_TAG)) {
            this.setRanged(tag.getBoolean(IS_RANGED_TAG));
        }
        this.reassessWeaponGoal();
    }

    public byte getDataBit(int position) {
        return (byte) ((getData() >> position) & 1);
    }

    public static double round(double x, long fraction) {
        return (double) Math.round(x * fraction) / fraction;
    }

    public boolean hasRightShoulderPad() {
        return getDataBit(RIGHT_SHOULDER_PAD) == 1;
    }

    public boolean hasLeftShoulderPad() {
        return getDataBit(LEFT_SHOULDER_PAD) == 1;
    }

    public boolean hasHair() {
        return getDataBit(HAIR) == 1;
    }

    public boolean hasBracers() {
        return getDataBit(BRACERS) == 1;
    }

    public byte getData() {
        return this.entityData.get(DATA);
    }

    public void setData(byte data) {
        this.entityData.set(DATA, data);
    }

    public void setData(int position, boolean value) {
        byte data = this.entityData.get(DATA);
        if (value) {
            data |= 1 << position;
        } else {
            data &= ~(1 << position);
        }
        setData(data);
    }

    public boolean isRanged() {
        return this.entityData.get(IS_RANGED);
    }

    public void setRanged(boolean isRanged) {
        this.entityData.set(IS_RANGED, isRanged);
    }
}
