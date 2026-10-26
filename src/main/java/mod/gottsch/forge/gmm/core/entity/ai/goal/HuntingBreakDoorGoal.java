package mod.gottsch.forge.gmm.core.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.ForgeHooks;

import java.util.function.IntSupplier;

/**
 * A {@link BreakDoorGoal} for a hunting mob: it smashes through a wooden door to reach its target,
 * and leaves doors alone while it is wandering.
 *
 * <h2>The goal alone is not enough</h2>
 * <p>The goal only finds a door that lies on the mob's current path, and the pathfinder routes
 * through a closed wooden door only when the navigation's {@code canOpenDoors} is set. Otherwise it
 * treats the door as a wall and never plans a path through it. The owning mob must call
 * {@link #updateNavigation()} every server tick, which turns {@code canOpenDoors} on only while
 * there is a target. A mob that always had it on would plan its idle wandering through closed doors
 * and then stand against them, because this goal will not start on a door without a target.</p>
 *
 * <h2>Differences from vanilla</h2>
 * <ul>
 *   <li><strong>Any difficulty.</strong> A vanilla zombie breaks doors only on Hard. Here the target is
 *       the only condition.</li>
 *   <li><strong>Once it starts, it finishes.</strong> A target is needed to start, not to carry on.
 *       A mob targets by sight, a shut door blocks sight, and a mob forgets an unseen target after 3
 *       seconds - so a goal that needed the target throughout gave up a quarter of the way through a
 *       12-second break, and no door was ever broken. It stops as vanilla's does: when the door
 *       opens or goes, or the mob is pulled away from it.</li>
 *   <li><strong>Any door, not just a {@link DoorBlock}.</strong> Vanilla finds doors by their class.
 *       Here a door is any block with an {@code open} property whose shut state the pathfinder reads
 *       as a shut wooden door ({@link BlockPathTypes#DOOR_WOOD_CLOSED}, through Forge's
 *       {@code getBlockPathType}) - so a modded door that is not a {@code DoorBlock}, such as a door
 *       three blocks tall, is found and broken too, provided it tells the pathfinder what it is.
 *       Breaking one block of it is the block's own business: a door that holds itself together
 *       comes down whole.</li>
 *   <li><strong>The break time is the mob's to choose.</strong> Vanilla's
 *       {@code getDoorBreakTime()} silently clamps to at least 240 ticks, so a shorter time passed to
 *       its constructor is ignored. This class reads the supplier instead, with no clamp.</li>
 *   <li><strong>Open doors are broken too, when the mob is too wide to get past one.</strong> An open
 *       door's panel takes 3px of its cell, and a mob walking the cell's centre clears it only at width
 *       {@value #MAX_WIDTH_PAST_OPEN_DOOR} or less. A wider mob (a 0.9 orc) jams against the panel,
 *       and vanilla's goal would not help, since it refuses to break an open door. The goal still
 *       starts only when the mob is actually colliding and the door is on its path.</li>
 * </ul>
 *
 * <p>Still gated by Forge's {@code canEntityDestroy}, so with the {@code mobGriefing} game rule off
 * no door is broken.</p>
 *
 * @author Mark Gottschling on 10/24/2026
 */
public class HuntingBreakDoorGoal extends BreakDoorGoal {
    /** Widest a mob can be and still walk the centre of a door cell past an open door's 3px panel. */
    public static final double MAX_WIDTH_PAST_OPEN_DOOR = 0.625D;

    private final IntSupplier breakTimeSupplier;

    /**
     * @param breakTimeSupplier ticks of hacking before the door gives way, read each time it is
     *                          needed so a mob's config can change it live
     */
    public HuntingBreakDoorGoal(Mob mob, IntSupplier breakTimeSupplier) {
        super(mob, difficulty -> true);
        this.breakTimeSupplier = breakTimeSupplier;
    }

    /**
     * Lets the pathfinder plan through closed wooden doors only while the mob is hunting. Call this
     * every server tick from the owning mob, e.g. in {@code customServerAiStep}.
     */
    public void updateNavigation() {
        if (this.mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(hasTarget());
        }
    }

    /**
     * Vanilla's {@code DoorInteractGoal} and {@code BreakDoorGoal} checks in one, with the door test
     * widened to {@link #isBreakableDoor}: a target, a ground navigator, the mob up against something,
     * a breakable door on the next stretch of its path (or in its own cell), griefing allowed, and the
     * door not open to it.
     */
    @Override
    public boolean canUse() {
        if (!hasTarget() || !GoalUtils.hasGroundPathNavigation(this.mob) || !this.mob.horizontalCollision) {
            return false;
        }
        if (!findDoor()) {
            return false;
        }
        return ForgeHooks.canEntityDestroy(this.mob.level(), this.doorPos, this.mob) && !this.isOpen();
    }

    /** No target needed to carry on; see the class notes. */
    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    protected int getDoorBreakTime() {
        return Math.max(1, this.breakTimeSupplier.getAsInt());
    }

    /**
     * Open, for this mob: open and narrow enough a gap for it, or gone - a door that is no longer
     * there counts as open, so the goal stops rather than hacking at air. A mob too wide to slip past
     * an open door's panel finds it shut, which is what lets the goal break it.
     */
    @Override
    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        }
        BlockState state = this.mob.level().getBlockState(this.doorPos);
        if (!isBreakableDoor(this.mob.level(), this.doorPos, state)) {
            this.hasDoor = false;
            return true;
        }
        return state.getValue(BlockStateProperties.OPEN) && this.mob.getBbWidth() <= MAX_WIDTH_PAST_OPEN_DOOR;
    }

    /** As vanilla's search: the next nodes of the path, then the mob's own cell, a block up. */
    private boolean findDoor() {
        GroundPathNavigation navigation = (GroundPathNavigation) this.mob.getNavigation();
        Path path = navigation.getPath();
        if (path != null && !path.isDone() && navigation.canOpenDoors()) {
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
                Node node = path.getNode(i);
                this.doorPos = new BlockPos(node.x, node.y + 1, node.z);
                if (this.mob.distanceToSqr(this.doorPos.getX(), this.mob.getY(), this.doorPos.getZ()) <= 2.25D) {
                    this.hasDoor = isBreakableDoor(this.mob.level(), this.doorPos, this.mob.level().getBlockState(this.doorPos));
                    if (this.hasDoor) {
                        return true;
                    }
                }
            }
        }
        this.doorPos = this.mob.blockPosition().above();
        this.hasDoor = isBreakableDoor(this.mob.level(), this.doorPos, this.mob.level().getBlockState(this.doorPos));
        return this.hasDoor;
    }

    /**
     * A wooden door, of any kind: a vanilla {@link DoorBlock} whose type opens by hand, or any block
     * with an {@code open} property whose shut state the pathfinder reads as a shut wooden door.
     * Trapdoors and fence gates have the property but no such path type, so they are left alone.
     */
    public static boolean isBreakableDoor(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock door) {
            return door.type().canOpenByHand();
        }
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return false;
        }
        return state.setValue(BlockStateProperties.OPEN, false).getBlockPathType(level, pos, null)
                == BlockPathTypes.DOOR_WOOD_CLOSED;
    }

    private boolean hasTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }
}
