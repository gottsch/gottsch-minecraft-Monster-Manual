package mod.gottsch.neo.gmm.core.entity.monster.gargoyle;

import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantCombatGoal;
import mod.gottsch.neo.gmm.core.entity.monster.WingedHumanoid;
import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantLandGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import mod.gottsch.neo.gmm.core.sound.GMMSounds;

/**
 * A winged humanoid that walks on the ground and launches into flight to close
 * on distant targets, then lands to melee. Flight behaviour lives in the
 * {@link WingedHumanoid} base + the volant goal package.
 * <p>
 * Currently always active — no "looks like an inert stone statue until approached" disguise (that's
 * the classic gargoyle trope, but it isn't built here yet). If that ever gets added, {@code Mimic}'s
 * {@code GatedGoal} wrapper (`core/entity/monster/mimic/Mimic.java`) is the tool for gating every goal
 * on a dormant/active flag without a bespoke subclass per goal — same shape as the Mimic ambush and
 * Gray Ooze's camouflage, just a new trigger condition (proximity, presumably, rather than hit/interact).
 *
 * @author Mark Gottschling on July 3, 2025
 */
public class Gargoyle extends WingedHumanoid {
    /** Consumer-supplied ambient sound (GMM ships no sound events). Left null = silent. */
    public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.WING_FLAP.get();

    public Gargoyle(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        super.registerGoals();

        // combat owns the launch -> fly -> land -> melee cycle; land goal covers the
        // targetless descent at a lower priority.
        this.goalSelector.addGoal(2, new VolantCombatGoal(this, 2.5D, 6.0D, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new VolantLandGoal(this, 0.8D));
        // wandering / looking goals at lower priorities
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FLYING_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 50D);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        // immune to Poison
        if (effectInstance.getEffect() == MobEffects.POISON) {
            return false;
        }
        // immune to strong Slowness (closest thing to "Petrified")
        if (effectInstance.getEffect() == MobEffects.MOVEMENT_SLOWDOWN && effectInstance.getAmplifier() >= 2) {
            return false;
        }
        return super.canBeAffected(effectInstance);
    }

    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public int getAmbientSoundInterval() {
        return 30;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ambientSound != null ? ambientSound.get() : null;
    }

    protected SoundEvent getStepSound() {
        return null;
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.UNDEFINED;
    }

    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.74F;
    }
}
