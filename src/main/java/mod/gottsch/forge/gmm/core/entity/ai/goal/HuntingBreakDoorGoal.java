package mod.gottsch.forge.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

import java.util.function.IntSupplier;

/**
 * A {@link BreakDoorGoal} that only runs while the mob has a target: it smashes through a wooden door
 * to reach someone, and leaves doors alone while it is wandering.
 *
 * <h2>The goal alone is not enough</h2>
 * <p>{@code DoorInteractGoal} only finds a door that lies on the mob's current path, and the
 * pathfinder routes through a closed wooden door only when the navigation's {@code canOpenDoors} is
 * set. Otherwise it treats the door as a wall and never plans a path through it. The owning mob must
 * call {@link #updateNavigation()} every server tick, which turns {@code canOpenDoors} on only while
 * there is a target. A mob that always had it on would plan its idle wandering through closed doors
 * and then stand against them, because this goal will not break a door without a target.</p>
 *
 * <h2>Differences from vanilla</h2>
 * <ul>
 *   <li><strong>Any difficulty.</strong> A vanilla zombie breaks doors only on Hard. Here the target is
 *       the only condition.</li>
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

    @Override
    public boolean canUse() {
        return hasTarget() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return hasTarget() && super.canContinueToUse();
    }

    @Override
    protected int getDoorBreakTime() {
        return Math.max(1, this.breakTimeSupplier.getAsInt());
    }

    /**
     * An open door counts as closed for a mob too wide to slip past its panel, so the inherited
     * {@code !isOpen()} checks let the goal break it. {@code super.isOpen()} is still called first
     * because it also notices when the door has gone.
     */
    @Override
    protected boolean isOpen() {
        boolean open = super.isOpen();
        return open && this.mob.getBbWidth() <= MAX_WIDTH_PAST_OPEN_DOOR;
    }

    private boolean hasTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }
}
