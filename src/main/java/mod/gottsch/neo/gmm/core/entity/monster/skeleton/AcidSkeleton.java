package mod.gottsch.neo.gmm.core.entity.monster.skeleton;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.particle.GMMParticles;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A skeleton that oozes caustic acid: it fights in melee like a normal skeleton, but each hit also
 * corrodes one random damageable item the target has equipped (armor or held), on top of the extra
 * bite damage baked into its attack attribute. Trails a faint green ooze fleck so it's identifiable.
 * Corrosion durability / whether it corrodes at all are codec-driven ({@code gmm:mob_config}); the
 * gear damage is intentionally conservative and can be disabled by consumers.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class AcidSkeleton extends GMMMonster {

    // Durability points stripped from one random equipped item per hit (kept low on purpose —
    // players dislike gear loss; consumers can retune or disable via the codec).
    private static final int DEFAULT_CORROSION = 15;

    public AcidSkeleton(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RestrictSunGoal(this));
        this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // slightly higher than a vanilla skeleton's melee — the "extra damage" of a caustic bite
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    private int corrosionAmount() {
        return (int) MobConfigHelper.get(this).number("corrosion", DEFAULT_CORROSION);
    }

    private boolean corrodesGear() {
        return MobConfigHelper.get(this).flag("corrodeGear", true);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            corrodeEquipment(living);
        }
        return hurt;
    }

    /** Strips durability from one random damageable equipped item (armor or hand). */
    private void corrodeEquipment(LivingEntity target) {
        if (!corrodesGear()) {
            return;
        }
        // creative players are immune to gear damage
        if (target instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        List<EquipmentSlot> damageable = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            // skip empty, non-damageable, and corrosion-immune (e.g. diamond) gear
            if (!stack.isEmpty() && stack.isDamageableItem()
                    && !stack.is(GMMTags.Items.CORROSION_IMMUNE)) {
                damageable.add(slot);
            }
        }
        if (damageable.isEmpty()) {
            return;
        }
        EquipmentSlot slot = damageable.get(this.random.nextInt(damageable.size()));
        target.getItemBySlot(slot).hurtAndBreak(corrosionAmount(), target, slot);
    }

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
        // drip caustic acid: an occasional droplet that forms on a bone, then falls and splats
        if (this.level().isClientSide && this.random.nextInt(12) == 0) {
            this.level().addParticle(GMMParticles.ACID_DRIP.get(),
                    this.getRandomX(0.6D),
                    this.getY() + 0.1D + this.random.nextDouble() * 1.6D,
                    this.getRandomZ(0.6D),
                    0.0D, 0.0D, 0.0D);
        }
        super.aiStep();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
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

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(1.74F);
    }
}
