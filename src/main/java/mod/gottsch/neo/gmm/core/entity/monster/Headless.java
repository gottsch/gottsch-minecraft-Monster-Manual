package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.entity.ai.goal.target.AllyAlertHurtByTargetGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.AllyAlertNearestAttackableTargetGoal;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Headless are about 20% better at everything than Zombies and are smart enough to avoid Creepers.
 * Alerts its allies (data-driven via the HEADLESS_*_ALLIES entity-type tags) when hurt or when it
 * acquires a target.
 *
 * @author Mark Gottschling on Apr 1, 2022
 */
public class Headless extends GMMMonster {

    public Headless(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Creeper.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // alerts only mobs whose EntityType is in the (consumer-populated) ally tags
        this.targetSelector.addGoal(1, new AllyAlertHurtByTargetGoal(this, GMMTags.EntityTypes.HEADLESS_HURT_ALLIES));
        this.targetSelector.addGoal(2, new AllyAlertNearestAttackableTargetGoal<>(this, Player.class, true, GMMTags.EntityTypes.HEADLESS_TARGET_ALLIES));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.5)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D)
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26F);
    }
}
