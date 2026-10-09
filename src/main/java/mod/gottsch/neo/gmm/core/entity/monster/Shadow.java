package mod.gottsch.neo.gmm.core.entity.monster;



import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import mod.gottsch.neo.gottschcore.random.RandomHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A nimble, sun-fearing undead that flees when struck and weakens its victims (blindness/weakness).
 * Defining trait: it RESISTS metal -- higher tiers do progressively less damage -- so players are
 * pushed toward gold or the special "shadow-bane" weapons. The metal resistance + gold handling are
 * intrinsic (default GMM behavior); consumer mods add their own bane weapons via the {@code gmm:shadow/bane}
 * and {@code gmm:shadow/minor_bane} item tags, so this class references no consumer items.
 *
 * @author Mark Gottschling on Apr 12, 2022
 */
public class Shadow extends GMMMonster {

    /**
     * Ambient sound: defaults to GMM's own {@link GMMSounds#SHADOW_AMBIENT} (shipped with the mob),
     * overridable by a consumer that wants its own sound; set to null to silence.
     */
    public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.SHADOW_AMBIENT.get();

    private boolean flee;

    public Shadow(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // NOTE Boulder-avoidance is injected consumer-side (gmm owns no Boulder); see DD CommonSetup.
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ShadowFleeGoal<>(this, Player.class, 6.0F, 1.2D, 1.2D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.28F);
    }

    /** Undead: Healing hurts, Harming heals, immune to Poison/Regeneration. See {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isUndead() {
        return true;
    }
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData groupData) {
        groupData = super.finalizeSpawn(level, difficulty, spawnType, groupData);

        RandomSource randomSource = level.getRandom();
        this.populateDefaultEquipmentSlots(randomSource, difficulty);

        return groupData;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficultyInstance) {
        this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 0.75F;
        Optional<Item> weapon = BuiltInRegistries.ITEM.getRandomElementOf(GMMTags.Items.SHADOW_WEAPONS, this.random).map(Holder::value);
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon.map(item -> new ItemStack(item)).orElse(ItemStack.EMPTY));
    }

    @Override
    public void aiStep() {
        // set on fire if in sun
        if (this.isSunBurnTick()) {
            this.igniteForSeconds(4);
        }
        super.aiStep();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            if (target instanceof Player) {
                // inflict blindness and/or weakness (tuning from the gmm:mob_config datapack)
                MobConfig config = MobConfigHelper.get(this);
                ItemStack helmetStack = ((Player) target).getItemBySlot(EquipmentSlot.HEAD);
                if (helmetStack.isEmpty() || helmetStack.getItem() != Items.GOLDEN_HELMET) {
                    if (RandomHelper.checkProbability(this.random, config.number("blindnessProbability", 75.0))) {
                        int blindnessDuration = (int) config.number("blindnessDuration", 60);
                        if (blindnessDuration > 0) {
                            ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindnessDuration, 0), this);
                        }
                    }
                }
                if (RandomHelper.checkProbability(this.random, config.number("weaknessProbability", 20.0))) {
                    int weaknessDuration = (int) config.number("weaknessDuration", 100);
                    if (weaknessDuration > 0) {
                        ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessDuration, 0), this);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * The Shadow's signature defense: it resists metal weapons (higher tier -> less damage), takes
     * full damage + a bonus from gold and the consumer-defined bane weapons, and a guaranteed minimum
     * from non-weapon strikes. Bane weapons also negate the attacker's WEAKNESS penalty. Being struck
     * makes it flee. (Shadows have no armor, so adjusting the raw amount here is the final word.)
     */
    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (this.level().isClientSide()) {
            return false;
        }

        // cause the shadow to flee when hurt
        this.flee = true;

        if (damageSource.getEntity() instanceof Player player) {
            ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!heldStack.isEmpty()) {
                if (heldStack.is(Items.GOLDEN_SWORD) || heldStack.is(GMMTags.Items.SHADOW_BANE)) {
                    // full damage + gold-tier bane bonus
                    amount += 2.0F + weaknessNegation(player);
                } else if (heldStack.is(GMMTags.Items.SHADOW_MINOR_BANE)) {
                    amount += 1.0F + weaknessNegation(player);
                } else if (heldStack.getItem() instanceof TieredItem tieredItem) {
                    // metal resistance: higher tiers are absorbed more
                    Tier tier = tieredItem.getTier();
                    if (tier == Tiers.NETHERITE) {
                        amount += -3;
                    } else if (tier == Tiers.DIAMOND) {
                        amount += -2;
                    } else if (tier == Tiers.IRON) {
                        amount += -1;
                    }
                    // stone / wood: no penalty
                } else {
                    // all other items (incl. fists): a guaranteed minimum (stone-sword equivalent)
                    amount = Math.max(amount, 4.0f);
                }
                GMM.LOGGER.debug("strike with [{}] -> new amount {}", heldStack.getHoverName().getString(), amount);
            }
        }
        return super.hurt(damageSource, amount);
    }

    /** Bonus that cancels out the attacker's WEAKNESS penalty so bane weapons always land full force. */
    private static float weaknessNegation(Player player) {
        if (player.hasEffect(MobEffects.WEAKNESS)) {
            return 4.0F * (player.getEffect(MobEffects.WEAKNESS).getAmplifier() + 1);
        }
        return 0.0F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ambientSound != null ? ambientSound.get() : null;
    }

    /**
     * Flees a target only after the Shadow has been hurt (its {@code flee} flag is set), then clears
     * the flag when it stops.
     *
     * @author Mark Gottschling on Apr 13, 2022
     */
    public static class ShadowFleeGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {

        public ShadowFleeGoal(PathfinderMob mob, Class<T> target, float maxDistance, double walkSpeedModifier,
                              double sprintSpeedModifier) {
            super(mob, target, maxDistance, walkSpeedModifier, sprintSpeedModifier);
        }

        @Override
        public boolean canUse() {
            if (((Shadow) mob).flee) {
                return super.canUse();
            }
            return false;
        }

        @Override
        public void stop() {
            ((Shadow) mob).flee = false;
            this.toAvoid = null;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.pathNav.isDone();
        }
    }
}
