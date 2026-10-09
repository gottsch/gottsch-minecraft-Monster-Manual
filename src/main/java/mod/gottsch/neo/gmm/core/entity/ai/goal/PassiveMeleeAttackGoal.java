package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A low-priority melee attack goal for a mob whose primary role is something else (e.g. a bow user):
 * it only swings when the target wanders into reach, on a cooldown, without pursuing.
 *
 * @author Mark Gottschling on Feb 5, 2024
 */
public class PassiveMeleeAttackGoal extends Goal {
    private static final int DEFAULT_COOLDOWN_TIME = 20;
    private Mob mob;
    private int cooldownCount;
    private int cooldownTime;

    public PassiveMeleeAttackGoal(Mob mob) {
        this(mob, DEFAULT_COOLDOWN_TIME);
    }

    public PassiveMeleeAttackGoal(Mob mob, int cooldownTime) {
        this.mob = mob;
        this.cooldownTime = cooldownTime;
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null;
    }

    @Override
    public void start() {
        // set to the threshold so the attack can be performed right away on first use.
        this.cooldownCount = cooldownTime;
    }

    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget((LivingEntity) null);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // cooldown regardless of criteria
        cooldownCount = Math.min(++this.cooldownCount, cooldownTime);
        if (cooldownCount >= cooldownTime) {
            if (this.getAttackReachSqr(mob.getTarget()) >= this.mob.distanceToSqr(mob.getTarget().getX(), mob.getTarget().getY(), mob.getTarget().getZ())) {
                doAttackAmin(mob);
                this.mob.doHurtTarget(mob.getTarget());
                this.cooldownCount = 0;
            }
        }
    }

    protected double getAttackReachSqr(LivingEntity entity) {
        return (double)(mob.getBbWidth() * 2.0F * mob.getBbWidth() * 2.0F + entity.getBbWidth());
    }

    public void doAttackAmin(Mob mob) {
        mob.swing(getMeleeHand(mob));
    }

    public InteractionHand getMeleeHand(Mob mob) {
        return InteractionHand.MAIN_HAND;
    }
}
