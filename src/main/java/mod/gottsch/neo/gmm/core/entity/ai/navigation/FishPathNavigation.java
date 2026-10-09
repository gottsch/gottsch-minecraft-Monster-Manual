package mod.gottsch.neo.gmm.core.entity.ai.navigation;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * @author by Mark Gottschling on 11/1/2025
 */
public class FishPathNavigation extends GroundPathNavigation {
    private final Mob fish;

    public FishPathNavigation(Mob mob, Level level) {
        super(mob, level);
        this.fish = mob;
    }

    // CRITICAL OVERRIDE: This method tells the navigator when to use "land" logic.
    // The mob.onGround() check in the parent class is typically what handles this.
    // We override the internal logic to ensure the mob uses land pathing when out of water.
    @Override
    protected boolean canMoveDirectly(Vec3 vec, Vec3 vec2) {
        // If the fish is NOT in water, assume it can move directly like a land mob.
        // This is the simplest way to force it to "walk" the short distance.
        if (!this.fish.isInWater()) {
            return true;
        }

        // Otherwise, defer to the standard GroundPathNavigation checks (which usually fail
        // because the mob's feet aren't on the ground when swimming).
        return super.canMoveDirectly(vec, vec2);
    }

    // Overriding isStableDestination is still good practice to confirm the path target.
    @Override
    public boolean isStableDestination(BlockPos p_218155_) {
        // A destination is stable if it's a water block.
        return this.level.getFluidState(p_218155_).is(net.minecraft.tags.FluidTags.WATER);
    }
}