package mod.gottsch.neo.gmm.core.entity.ai.goal.ghoul;

import mod.gottsch.neo.gmm.core.entity.monster.ghoul.AbstractGhoul;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/**
 * @author by Mark Gottschling on 11/6/2025
 */
public class GhoulHealGoal extends Goal {
    // TODO add a cooldown period
    private AbstractGhoul ghoul;

    public GhoulHealGoal(AbstractGhoul ghoul) {
        this.ghoul = ghoul;
    }

    @Override
    public boolean canUse() {
        // check if the ghoul is alive AND on the ground (short-circuit on failure)
        if (!getGhoul().isAlive() || !getGhoul().onGround()) {
            return false;
        }

        // check if health is less than or equal to 50% AND it has food in inventory
        return getGhoul().getHealth() <= (0.5F * getGhoul().getMaxHealth()) && getGhoul().getFoodInventory().isPresent();
    }

    @Override
    public void start() {
        getGhoul().getFoodInventory().ifPresent(slot -> {
            ItemStack foodStack = getGhoul().getInventory().getStackInSlot(slot);
            if (!foodStack.isEmpty()) {
                foodStack.shrink(1);
                // heal self
                getGhoul().heal(getGhoul().getHealAmount());
                // visual feedback: scatter heart particles around the ghoul's upper body
                spawnHealParticles();
                // check if hunting goals can be re-enabled (if disabled).
                getGhoul().reassessHuntingGoal();
            }
        });
        super.start();
    }

    // TODO future decrement cooldown
    @Override
    public void tick() {
        super.tick();
    }

    /**
     * Broadcasts heart particles around the ghoul's upper body. Runs server-side
     * (the goal ticks on the server), so it uses sendParticles to reach all clients.
     */
    private void spawnHealParticles() {
        if (getGhoul().level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    getGhoul().getX(),
                    getGhoul().getY() + getGhoul().getBbHeight() * 0.7D,
                    getGhoul().getZ(),
                    7,          // particle count
                    0.4D, 0.5D, 0.4D, // x/y/z spread around the body
                    0.0D);      // speed
        }
    }

    public AbstractGhoul getGhoul() {
        return ghoul;
    }
}
