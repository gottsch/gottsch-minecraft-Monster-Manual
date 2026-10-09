package mod.gottsch.neo.gmm.core.entity.ai.goal.target;

import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Makes a summoned mob assist its summoner: it targets whatever its owner is fighting.
 * The owner is resolved from {@link IGMMMonster#getSummonedOwner()} (player or mob), so this
 * works for any GMM mob base (e.g. both {@code GMMMonster} and {@code GMMFlyingMonster}).
 *
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on Jan 14, 2024
 */
public class SummonedOwnerTargetGoal extends TargetGoal {
    // TargetGoal already provides a protected Mob "mob" field; this holds the same instance
    // typed as IGMMMonster so getSummonedOwner() is reachable regardless of the GMM base class.
    private final IGMMMonster summonable;

    public SummonedOwnerTargetGoal(Mob mob) {
        super(mob, false);
        this.summonable = (IGMMMonster) mob;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // only mobs that are actually beholden to their owner assist it; a REINFORCEMENT targets freely.
        OwnershipType type = this.summonable.getOwnershipType();
        if (type != OwnershipType.SUMMONED && type != OwnershipType.THRALL) {
            return false;
        }
        // a THRALL under any order other than FOLLOW has its own dedicated behavior (see
        // ThrallStayGoal/ThrallGuardGoal/ThrallAttackOrderGoal) and shouldn't have the owner's combat
        // target mirrored onto it.
        if (type == OwnershipType.THRALL && this.summonable.getThrallOrder() != ThrallOrder.FOLLOW) {
            return false;
        }
        LivingEntity owner = this.summonable.getSummonedOwner();
        LivingEntity target = this.mob.getTarget();
        if (owner == null || (target != null && !target.equals(owner))) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        LivingEntity owner = this.summonable.getSummonedOwner();
        if (owner instanceof Player player) {
            if (player.getLastHurtByMob() != null) {
                this.mob.setTarget(player.getLastHurtByMob());
            } else if (player.getLastHurtMob() != null) {
                this.mob.setTarget(player.getLastHurtMob());
            }
        } else if (owner instanceof Mob ownerMob) {
            this.mob.setTarget(ownerMob.getTarget());
        }
        super.start();
    }
}
