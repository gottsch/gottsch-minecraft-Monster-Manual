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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

import java.util.function.IntSupplier;

/**
 * A {@link BreakDoorGoal} for a hunting mob: it smashes through a wooden door to reach its quarry,
 * and leaves doors alone while it is wandering.
 *
 * <h2>Hunting, and the door in the way</h2>
 * <p>A mob is hunting while it has a target, and for {@link #QUARRY_MEMORY_TICKS} after it loses
 * one - it remembers where the quarry went. While hunting, it breaks a door when it has stopped (up
 * against something, or with nowhere left to walk) and the door stands close by, between it and the
 * quarry. It does not need the door on its path, as vanilla's goal does.</p>
 *
 * <p>The owning mob must call {@link #updateNavigation()} every server tick. That keeps the memory,
 * and turns the navigation's {@code canOpenDoors} on only while hunting, so the pathfinder routes
 * through a shut wooden door to the quarry but not on idle wandering - a mob that always had it on
 * would plan its strolls through doors and then stand against them.</p>
 *
 * <h2>Differences from vanilla</h2>
 * <ul>
 *   <li><strong>Any difficulty.</strong> A vanilla zombie breaks doors only on Hard. Here the target is
 *       the only condition.</li>
 *   <li><strong>Once it starts, it finishes.</strong> Hunting is needed to start, not to carry on.
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
 *       and vanilla's goal would not help, since it refuses to break an open door.</li>
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

    /**
     * How long the mob keeps hunting a quarry it has lost: ten seconds. A mob targets by sight, a shut
     * door blocks sight, and vanilla forgets an unseen target in three - before the mob has even
     * reached the door it was chasing someone through.
     */
    public static final int QUARRY_MEMORY_TICKS = 200;

    /** How near a door must be to be the one in the way: a block and a half, centre to centre. */
    private static final double REACH_SQR = 1.5D * 1.5D;

    private final IntSupplier breakTimeSupplier;

    // where the quarry was last known to be, and when; null until the mob first has a target
    private Vec3 quarryPos;
    private int quarrySeenTick;

    /**
     * @param breakTimeSupplier ticks of hacking before the door gives way, read each time it is
     *                          needed so a mob's config can change it live
     */
    public HuntingBreakDoorGoal(Mob mob, IntSupplier breakTimeSupplier) {
        super(mob, difficulty -> true);
        this.breakTimeSupplier = breakTimeSupplier;
    }

    /**
     * Remembers where the quarry is, and lets the pathfinder plan through closed wooden doors only
     * while the mob is hunting. Call this every server tick from the owning mob, e.g. in
     * {@code customServerAiStep}.
     */
    public void updateNavigation() {
        if (hasTarget()) {
            this.quarryPos = this.mob.getTarget().position();
            this.quarrySeenTick = this.mob.tickCount;
        }
        if (this.mob.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(isHunting());
        }
    }

    /**
     * The mob is hunting (see {@link #QUARRY_MEMORY_TICKS}), it is stopped - up against something, or
     * with nowhere left to walk - and a breakable door stands between it and its quarry, close by.
     * Griefing must be allowed, and the door must not be open to it.
     *
     * <p>Not vanilla's test, which looks for the door on the mob's path: that left it to chance
     * (Mark, 2026-10-26: "hit or miss"). An orc that saw its quarry through an open door it was too
     * wide to pass had no path through it to put the door on, so it stood there; and once the door
     * shut, the quarry was out of sight and forgotten before the path was ever re-planned.</p>
     */
    @Override
    public boolean canUse() {
        if (!isHunting() || !GoalUtils.hasGroundPathNavigation(this.mob)
                || !(this.mob.horizontalCollision || this.mob.getNavigation().isDone())) {
            return false;
        }
        Vec3 quarry = hasTarget() ? this.mob.getTarget().position() : this.quarryPos;
        if (!findDoor(quarry)) {
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

    /**
     * The breakable door, if any, standing between the mob and where its quarry is: within a block
     * and a half of it, from its feet to two blocks up, and on the quarry's side of it. The nearest,
     * if there are two.
     */
    private boolean findDoor(Vec3 quarry) {
        BlockPos feet = this.mob.blockPosition();
        double toQuarryX = quarry.x - this.mob.getX();
        double toQuarryZ = quarry.z - this.mob.getZ();
        double quarrySqr = toQuarryX * toQuarryX + toQuarryZ * toQuarryZ;
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 2, 1))) {
            if (!isBreakableDoor(this.mob.level(), pos, this.mob.level().getBlockState(pos))) {
                continue;
            }
            double toDoorX = pos.getX() + 0.5D - this.mob.getX();
            double toDoorZ = pos.getZ() + 0.5D - this.mob.getZ();
            double doorSqr = toDoorX * toDoorX + toDoorZ * toDoorZ;
            boolean between = toDoorX * toQuarryX + toDoorZ * toQuarryZ > 0.0D && quarrySqr > doorSqr;
            if (between && doorSqr <= REACH_SQR && doorSqr < bestSqr) {
                best = pos.immutable();
                bestSqr = doorSqr;
            }
        }
        if (best == null) {
            return false;
        }
        this.doorPos = best;
        this.hasDoor = true;
        return true;
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

    /** It has a target, or had one within {@link #QUARRY_MEMORY_TICKS}. */
    private boolean isHunting() {
        return hasTarget() || (this.quarryPos != null
                && this.mob.tickCount - this.quarrySeenTick <= QUARRY_MEMORY_TICKS);
    }
}
