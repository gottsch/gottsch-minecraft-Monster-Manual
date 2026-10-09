package mod.gottsch.neo.gmm.core.entity.monster.construct;

import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * D&amp;D-flavored "Wood Golem" -- a bark/root-bodied construct ported from Treasure2's
 * {@code WitherwoodGolem}, per the catalog's Family: Constructs entry. Unlike {@code AnimatedArmor}/
 * {@code Mimic} it isn't a disguised ambush prop -- it's always visible, standing guard over whatever
 * it was built to protect, and never natural-spawns (no {@code ModEntities.ALL_MOBS} entry, no
 * {@code biome_modifier} -- purely hand/structure-placed, same treatment as the mimic family).
 * <p>
 * A consumer anchors an instance with vanilla's own {@code Mob#restrictTo(BlockPos, int)} (the same
 * mechanism {@code GraveZombie}'s hand-placed graves and {@code AnimatedArmor}'s posed suits use). While
 * it has no target, {@code MoveTowardsRestrictionGoal} actively walks it back within the restricted
 * radius if something (a shove, a chase) carried it out -- this is the one golem-family mob so far that
 * really is meant to patrol/hold a post, not just avoid despawning near one. The instant it's fighting,
 * {@link #isWithinRestriction} reports every position as in-bounds so it can chase a target freely
 * without being leashed back mid-fight -- same "despawn/homing gate, not a combat leash" idiom
 * {@code AnimatedArmor}/{@code GraveZombie} already established, just keyed off "has a target" rather
 * than a phase/active flag since this mob has no dormant state to track. As with those two, vanilla
 * {@code Mob} never persists {@code restrictCenter}/{@code restrictRadius}, so this class saves/restores
 * it itself.
 * <p>
 * Neutral by default, not a hostile monster -- a golem is a <em>protector</em>, and its hostility is a
 * deployment decision, not an intrinsic trait the way it is for a zombie or skeleton. Two independent
 * config toggles decide who it fights: {@code attacksMonsters} (default {@code true}) targets whatever
 * matches {@link GMMTags.EntityTypes#CATEGORY_HOSTILE_MONSTERS} -- a shared, cross-cutting tag (gmm
 * ships a "common overworld/dungeon hostiles" default) so a consumer never has to enumerate a monster
 * roster just to make a golem defend its post -- and {@code attacksPlayers} (default {@code false}),
 * a separate binary policy call rather than a tag entry, since "is this guardian hostile to players at
 * all" isn't really a "which type" question the way monster hostility is. Treasure2's original also
 * hardcoded Iron Golem hostility ("the other faction's golem") -- dropped per the user's call: golems
 * shouldn't be fighting each other, and it's now subsumed by the same {@code attacksMonsters} toggle
 * anyway (iron golems aren't in the default hostile-monsters tag). Attack is still Treasure2's
 * iron-golem-style slam: a wide half-to-full random damage roll plus knockback, using iron golem sounds
 * as a stand-in until Wood Golem gets its own "wood creak" asset (flagged in the catalog, not built
 * this pass).
 *
 * @author Mark Gottschling
 */
public class WoodGolem extends GMMMonster {

    private static final boolean DEFAULT_ATTACKS_MONSTERS = true;
    private static final boolean DEFAULT_ATTACKS_PLAYERS = false;

    public WoodGolem(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        MobConfig config = MobConfigHelper.get(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.2D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        if (config.flag("attacksPlayers", DEFAULT_ATTACKS_PLAYERS)) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
        }
        if (config.flag("attacksMonsters", DEFAULT_ATTACKS_MONSTERS)) {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, true,
                    mob -> mob.getType().is(GMMTags.EntityTypes.CATEGORY_HOSTILE_MONSTERS)));
        }
    }

    /**
     * Golems don't fit the project's default zombie-anchored health baseline (MC 20 HP &#8596; D&amp;D 22 HP,
     * ~0.91x) -- they're an outlier the same way skeletons are, just in the opposite direction. Anchored
     * instead to vanilla Iron Golem (MC 100 HP / D&amp;D 210 HP &#8776; 0.476x, MC 15.0 ATK / D&amp;D single-slam
     * avg 20.5 &#8776; 0.732x) as the golem-family baseline (source: 5esrd.com for all D&amp;D 5e figures
     * below).
     * <p>
     * Literal D&amp;D 5e Wood Golem (52 HP, CR 3) converts to ~25 HP / ~6 ATK, ranking it below every other
     * golem (Flesh CR 5, Clay CR 9, Stone CR 10, Iron CR 16). Per the user's explicit call, GMM's own
     * family ranking deliberately diverges from that literal CR order -- keyed off <em>Minecraft's own</em>
     * material-accessibility intuition (the same logic vanilla's wood/stone/iron/diamond tool tiers
     * train players to expect) rather than the D&amp;D source material: zombie flesh is a trivial, always-
     * renewable mob drop, so Wood ranks <em>above</em> Flesh; clay is a locational, biome-bound resource,
     * so Wood still ranks <em>below</em> Clay. Checking where every golem's D&amp;D figure lands once
     * converted for reference: Flesh 93 HP/13 dmg &#8594; ~44/9.5; Clay 133 HP/16 dmg &#8594; ~63/11.7; Stone 178
     * HP/19 dmg &#8594; ~85/13.9; Iron 210 HP/20.5 dmg &#8594; 100/15 (the anchor). Final values (50 HP / 10 ATK)
     * sit squarely between Flesh and Clay, matching that MC-accessibility ordering. MOVEMENT_SPEED/
     * KNOCKBACK_RESISTANCE have no D&amp;D equivalent stat and stay at Iron Golem's own MC baseline.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    /**
     * Ported verbatim from {@code WitherwoodGolem}: the same wide, iron-golem-style damage roll (half
     * the attribute's value, plus a further random roll up to the full value) rather than a flat hit,
     * with knockback scaled against the target's own knockback resistance.
     */
    @Override
    public boolean doHurtTarget(Entity entity) {
        float attackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float calculatedAttackDamage = (int) attackDamage > 0
                ? attackDamage / 2.0F + (float) this.random.nextInt((int) attackDamage)
                : attackDamage;
        DamageSource damageSource = this.damageSources().mobAttack(this);
        boolean isTargetEntityHurt = entity.hurt(damageSource, calculatedAttackDamage);
        if (isTargetEntityHurt) {
            double targetKnockbackResistance = entity instanceof LivingEntity livingEntity
                    ? livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
                    : 0.0D;
            double knockback = Math.max(0.0D, 1.0D - targetKnockbackResistance);
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.4F * knockback, 0.0D));
            this.doEnchantDamageEffects(this, entity);
        }
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return isTargetEntityHurt;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
