package mod.gottsch.neo.gmm.core.entity.ai.goal.volant;

import mod.gottsch.neo.gmm.core.entity.monster.WingedHumanoid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Brings a flying {@link WingedHumanoid} safely back to the ground when it has no
 * target — e.g. after its target dies or escapes while it is airborne. {@link
 * VolantCombatGoal} owns landing while a target exists, so this only fires for the
 * targetless case (and runs at a lower priority).
 *
 * @author Mark Gottschling on July 26, 2025 (reworked)
 */
public class VolantLandGoal extends Goal {
    private final WingedHumanoid mob;
    private final double speedModifier;

    public VolantLandGoal(WingedHumanoid mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.isFlying() && mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        // keep running until we are safely back on the ground (out of flight mode)
        return mob.isFlying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.mob.fallDistance = 0.0F;
    }

    @Override
    public void tick() {
        BlockPos groundPos = WingedHumanoid.findSafeGroundPos(mob.level(), mob.getOnPos(), 16);
        if (groundPos == null) {
            // nowhere safe directly below: hover in place rather than drift off
            mob.getNavigation().stop();
            return;
        }
        if (mob.tickDescentToward(groundPos, speedModifier)) {
            mob.setWalkingMovement();
        }
    }
}
