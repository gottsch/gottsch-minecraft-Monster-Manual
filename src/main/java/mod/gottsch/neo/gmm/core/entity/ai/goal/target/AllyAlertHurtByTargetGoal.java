package mod.gottsch.neo.gmm.core.entity.ai.goal.target;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;

import java.util.EnumSet;

/**
 * Like vanilla HurtByTargetGoal, but its "alert allies" set is data-driven: any nearby mob
 * whose EntityType is in {@code allyTag} is alerted to the attacker (vanilla's version takes an
 * exclude-list of concrete classes; this takes an include-set via a tag). This lets the goal live
 * in the shared library without referencing any consumer mob class - consumers populate the tag.
 *
 * Generalized from Dungeon Denizens' Headless alert goal.
 *
 * @author Mark Gottschling
 */
public class AllyAlertHurtByTargetGoal extends TargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private final TagKey<EntityType<?>> allyTag;
    private final Class<?>[] toIgnoreDamage;
    private int timeStamp;

    public AllyAlertHurtByTargetGoal(PathfinderMob mob, TagKey<EntityType<?>> allyTag, Class<?>... toIgnoreDamage) {
        super(mob, true);
        this.allyTag = allyTag;
        this.toIgnoreDamage = toIgnoreDamage;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        int i = this.mob.getLastHurtByMobTimestamp();
        LivingEntity livingentity = this.mob.getLastHurtByMob();
        if (i != this.timeStamp && livingentity != null) {
            if (livingentity.getType() == EntityType.PLAYER && this.mob.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                return false;
            } else {
                for (Class<?> oclass : this.toIgnoreDamage) {
                    if (oclass.isAssignableFrom(livingentity.getClass())) {
                        return false;
                    }
                }
                return this.canAttack(livingentity, HURT_BY_TARGETING);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.timeStamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        this.alertOthers();
        super.start();
    }

    protected void alertOthers() {
        AllyAlertUtil.alertNearby(this.mob, this.mob.getLastHurtByMob(), this.getFollowDistance(), this.allyTag);
    }
}
