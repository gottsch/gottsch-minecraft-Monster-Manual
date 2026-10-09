package mod.gottsch.neo.gmm.core.entity.monster.gargoyle;

import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantCombatGoal;
import mod.gottsch.neo.gmm.core.entity.monster.WingedHumanoid;
import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantLandGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
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
 * Subterranean relative of the Gargoyle: it cannot really fly, only hover about a foot off the
 * ground, so it skims toward its target instead of launching skyward. Kept low so it fits under
 * tight sewer/dungeon ceilings.
 * <p>
 * No statue-disguise state either — see {@link Gargoyle}'s class doc for that (unbuilt) idea and its
 * {@code GatedGoal} pointer; applies equally here.
 *
 * @author Mark Gottschling on July 26, 2025
 */
public class Margoyle extends WingedHumanoid {
    /** Consumer-supplied ambient sound (GMM ships no sound events). Left null = silent. */
    public static Supplier<SoundEvent> ambientSound = () -> GMMSounds.WING_FLAP.get();

    public Margoyle(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        super.registerGoals();

        // shares the volant combat cycle; its low getMaxFlyHeight() keeps it skimming.
        this.goalSelector.addGoal(2, new VolantCombatGoal(this, 2.5D, 6.0D, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new VolantLandGoal(this, 0.8D));

        // add other goals like wandering, looking at the player, etc. with lower priorities
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.5)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.245D)
                .add(Attributes.FLYING_SPEED, 0.245D)
                .add(Attributes.FOLLOW_RANGE, 35D);
    }

    /**
     * Cannot really fly — only hovers ~1 block off the ground, so it skims toward its target.
     */
    @Override
    public double getMaxFlyHeight() {
        return 1.0D;
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

    protected float getStandingEyeHeight(Pose pose, EntityDimensions entityDimensions) {
        return 1.74F;
    }
}
