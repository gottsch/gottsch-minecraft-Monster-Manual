package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.Optional;

public class ReturnToWaterGoal extends Goal {
    private final WaterAnimal fish;
    private final double speedModifier;
    private Path path;

    // search range for finding water (in blocks)
    private static final int SEARCH_RANGE = 5;

    public ReturnToWaterGoal(WaterAnimal fish, double speedModifier) {
        this.fish = fish;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /**
     * determines if the goal should start running.
     * The fish must NOT be in water to run this goal.
     */
    @Override
    public boolean canUse() {
        if (this.fish.isInWater()) {
            return false;
        }

        // try to find the nearest water block
        Optional<BlockPos> nearestWater = this.findNearestWater();
        if (nearestWater.isEmpty()) {
            return false; // no water found nearby
        }

        // attempt to create a path to the water block
        BlockPos waterPos = nearestWater.get();
        this.path = this.fish.getNavigation().createPath(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 0);

        // only run if a valid path exists
        return this.path != null;
    }

    /**
     * determines if the goal should continue running.
     * The fish continues until it is back in water or the path fails.
     */
    @Override
    public boolean canContinueToUse() {
        return !this.fish.isInWater() && !this.fish.getNavigation().isDone();
    }

    /**
     * called when the goal is started.
     */
    @Override
    public void start() {
        // The custom FishPathNavigation now handles the land/water logic internally.
        if (this.path != null) {
            this.fish.getNavigation().moveTo(this.path, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        // Just stop the path, no need to swap navigators back and forth.
        this.fish.getNavigation().stop();
    }

    /**
     * called every tick while the goal is running.
     */
    @Override
    public void tick() {
        // if the fish's current target is reached, or the path is done,
        // the canContinueToUse() check will handle termination.
    }

    /**
     * searches for the nearest water block position.
     */
    private Optional<BlockPos> findNearestWater() {
        LevelReader level = this.fish.level();
        BlockPos mobPos = this.fish.blockPosition();

        // Define the search area (a box around the mob)
        int searchRadius = SEARCH_RANGE; // Using your constant of 20

        // We search a box centered on the mob, limited to 4 blocks high/low
        Iterable<BlockPos> searchArea = BlockPos.betweenClosed(
                mobPos.offset(-searchRadius, -4, -searchRadius),
                mobPos.offset(searchRadius, 4, searchRadius)
        );

        // Manually iterate through the nearby blocks
        for (BlockPos pos : searchArea) {
            // Check if the current block is a water fluid block
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                // Found water! Return its position immediately.
                return Optional.of(pos.immutable());
            }
        }

        // No water found within the search area
        return Optional.empty();
    }
}