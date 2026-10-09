package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.entity.ai.goal.ChargeAttackGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.TallMeleeAttackGoal;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * A bull-headed brute: slow, heavy, hard to stagger and hard to run from. Where an Orc is a
 * skirmisher that might throw instead of close, the Minotaur has exactly one plan -- walk at you
 * and hit you with whatever is in its fist. It trades the Orc's reach and variety for weight:
 * higher health and armor, real knockback resistance, and an attack that throws the target back.
 *
 * <p>Equipment is data-driven the same way the rest of GMM does it -- the weapon comes from the
 * consumer-populated {@code gmm:minotaur/weapons} item tag, so the library ships no item of its
 * own. An empty pool leaves the Minotaur bare-fisted, which is a legitimate look for it (see
 * {@link #populateDefaultEquipmentSlots}), unlike the Orc, whose empty hands mean "thrower".
 *
 * @author Mark Gottschling on 9/7/2026
 */
public class Minotaur extends GMMMonster implements IChargingMob {
    /**
     * Synched because the charge pose is a CLIENT-side read: {@code MinotaurModel} lowers the head
     * and flattens the body while this is set, and the client never runs the goal that sets it.
     */
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(Minotaur.class, EntityDataSerializers.BOOLEAN);
    private static final String CHARGING_TAG = "charging";

    // Charge tuning. Trigger band starts outside melee reach so the charge is a way to CLOSE
    // distance rather than a second melee attack, and stops before the mob would lose the target
    // during the run.
    //
    // The windup is the price of the hit and the player's window to move. It was 20 -- a full
    // second of standing still, which read as hesitation rather than as a wind-up. 12 on
    // 2026-09-09, still long enough to be a tell and to turn away from.
    //
    // NOTE THIS COMPOUNDS WITH CHARGE_SPEED, which went up the same day. Shorter windup and faster
    // run both cut the time the player has, so the next playtest of either is really a playtest of
    // the pair -- and the pair is walking back toward the version that killed an unarmored player
    // outright. See CHARGE_SPEED below.
    private static final double CHARGE_RANGE_MIN = 5.0D;
    private static final double CHARGE_RANGE_MAX = 16.0D;
    private static final double CHARGE_HIT_RANGE = 2.6D;
    private static final int CHARGE_WINDUP_TICKS = 12;
    private static final int CHARGE_MAX_TICKS = 60;
    private static final int CHARGE_COOLDOWN_TICKS = 160;
    // Retuned 2026-09-07 after playtesting: the charge was killing an unarmored player outright.
    // Navigation speed modifier and a MULTIPLY_BASE bonus on MOVEMENT_SPEED compose, so the effective
    // charge speed is (0.23 * (1 + bonus)) * CHARGE_SPEED. It was 0.77 -- about 3.4x the mob's own
    // walk, and comfortably faster than a sprinting player, i.e. not dodgeable and barely visible.
    // Now ~0.40, a little under twice its walk: still clearly a charge, still faster than you, but
    // something you can read and step out of.
    //
    // 2026-09-09: nudged 1.3 -> 1.5 (effective ~0.40 -> ~0.47, about 2.2x its walk) at Mark's
    // request -- the charge read as a shade too slow to feel like a charge. NOTE THE CEILING this
    // is walking back toward: 0.77 was the version that killed an unarmored player outright, so
    // this band is narrow and the next raise should be playtested against an unarmored player
    // rather than reasoned about.
    private static final double CHARGE_SPEED = 1.5D;
    private static final double CHARGE_SPEED_BONUS = 0.35D;
    /** FLAT extra damage on impact, not a multiplier -- see {@code ChargeAttackGoal#damageBonus}. */
    private static final double CHARGE_DAMAGE_BONUS = 4.0D;
    private static final float CHARGE_KNOCKBACK = 1.6F;
    private static final float CHARGE_PROBABILITY = 0.08F;


    public Minotaur(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // a minotaur carries its own weapon rather than scavenging; matches the Orc drop rate
        Arrays.fill(this.handDropChances, 0.25F);
    }

    /**
     * Ticks between melee swings, against vanilla's 20.
     *
     * <p>Same reasoning as the Stone Colossus's 45, scaled to a smaller mob: an axe blow worth 12
     * damage landing three times in two seconds reads as a flurry rather than as a heavy swing
     * (Mark, 2026-09-14: "the minotaur strikes a little too fast for such a powerful swing"). It
     * stays well under the Colossus's because the Minotaur is the fast bruiser of the pair — its
     * threat is the charge and the pursuit, not the weight of one blow.</p>
     */
    private static final int MELEE_INTERVAL_TICKS = 30;

    @Override
    protected void registerGoals() {
        // priority 3: above the melee goal, so a charge wins the MOVE/LOOK flags when it fires and
        // the mob commits to the run instead of drifting back into a normal approach.
        this.goalSelector.addGoal(3, new ChargeAttackGoal(this,
                CHARGE_RANGE_MIN, CHARGE_RANGE_MAX, CHARGE_HIT_RANGE,
                CHARGE_WINDUP_TICKS, CHARGE_MAX_TICKS, CHARGE_COOLDOWN_TICKS,
                CHARGE_SPEED, CHARGE_SPEED_BONUS, CHARGE_DAMAGE_BONUS,
                CHARGE_KNOCKBACK, CHARGE_PROBABILITY));
        // Tall enough for vanilla's feet-to-feet reach to matter: at 2.6 blocks a player on a
        // one-block step is already measured further away than they look. Milder than the
        // Stone Colossus's version of the same problem, same cause.
        this.goalSelector.addGoal(4, new TallMeleeAttackGoal(this, 1.0D, false, MELEE_INTERVAL_TICKS));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                // Low on purpose, the way vanilla arms a weapon-carrying mob: almost all of this
                // mob's damage comes from the axe it holds (an ADDITION modifier of +9), so the
                // attribute is the BARE-HANDED figure, not the total. Set to 7 originally, which
                // read as reasonable in isolation and made an armed Minotaur hit for 16.
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                // the signature of the mob: it does not chip you, it launches you
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                // slower than an Orc (0.25) -- the weight has to cost it something
                .add(Attributes.MOVEMENT_SPEED, 0.23D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        // super applies the gmm:mob_config attribute overrides and any companion pool
        groupData = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return groupData;
    }

    /**
     * Draws the mainhand weapon from {@code gmm:minotaur/weapons}. An empty or absent tag leaves
     * the hands empty on purpose -- a bare-fisted minotaur is still a minotaur, so there is no
     * fallback pool here.
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        Optional<Item> weapon = ForgeRegistries.ITEMS.tags()
                .getTag(GMMTags.Items.MINOTAUR_WEAPONS).getRandomElement(randomSource);
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon.map(ItemStack::new).orElse(ItemStack.EMPTY));
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(CHARGING_TAG, isCharging());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(CHARGING_TAG)) {
            setCharging(tag.getBoolean(CHARGING_TAG));
        }
    }

    @Override
    public void setCharging(boolean charging) {
        this.entityData.set(CHARGING, charging);
    }

    @Override
    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.15F, 1.0F);
    }
}
