package mod.gottsch.neo.gmm.core.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;

/**
 * Retaliates against whatever last hurt this mob -- vanilla's {@code HurtByTargetGoal} equivalent, but
 * usable by any {@link Mob} rather than just {@link net.minecraft.world.entity.PathfinderMob}. Vanilla's
 * constructor is narrowed to PathfinderMob for no reason internal to its own logic (which only ever
 * touches Mob/LivingEntity methods), so a {@code FlyingMob}-based caster like Beholder can't use it
 * directly. Same shape as {@link AllyAlertHurtByTargetGoal} minus the ally-alerting.
 *
 * @author Mark Gottschling
 */
public class MobHurtByTargetGoal extends TargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private int timeStamp;

    public MobHurtByTargetGoal(Mob mob) {
        super(mob, true);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        int i = this.mob.getLastHurtByMobTimestamp();
        LivingEntity attacker = this.mob.getLastHurtByMob();
        if (i == this.timeStamp || attacker == null) {
            return false;
        }
        return this.canAttack(attacker, HURT_BY_TARGETING);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.timeStamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        super.start();
    }
}
