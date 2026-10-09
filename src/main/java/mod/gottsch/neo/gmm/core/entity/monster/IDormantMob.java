package mod.gottsch.neo.gmm.core.entity.monster;

/**
 * A mob that spends its first life as scenery: no AI, no movement, no nameplate, until something
 * brings it round.
 *
 * <p>Modelled on {@link IChargingMob} and for the same reason &mdash; the flag is a
 * <strong>client-side read</strong>. The statue pose lives in the model, and the client never runs
 * the goal that clears the flag, so the state has to be synched and reachable through an interface
 * the model can test without depending on any one mob class.
 *
 * <p>Waking is deliberately two states rather than one. {@code dormant} is the statue; the wake
 * window that follows is time during which the mob is awake but still owns no goals &mdash; that
 * gap is what stops a player being hit by something that was a rock a tick ago.
 *
 * <h2>A FLAG and a per-side clock, not a synched tick</h2>
 * <p>Two earlier cuts were both wrong, and the second only subtly. The first synched a counter and
 * decremented it server-side every tick: the client saw twenty discrete values a second with
 * nothing between them, so the rise stepped rather than moved (Mark, 2026-09-13: "the wake up
 * animation is really choppy"). The second synched the server's {@code tickCount} at the moment
 * the rise began and had the model measure it against its own {@code ageInTicks} &mdash; but
 * <strong>{@code tickCount} is per-side</strong>. The client's counter starts when the entity was
 * created on that client, so for a colossus woken out of view, or after a chunk reload, the two
 * clocks are an arbitrary distance apart; a client reading {@code start} far in its own future
 * blends permanently toward the statue pose and the mob never finishes rising.
 *
 * <p>So the shape is {@link ISlammingMob}'s, which is the one the other three poses already use:
 * the <strong>server</strong> owns the timing and counts its own ticks inside {@link
 * mod.gottsch.neo.gmm.core.entity.ai.goal.DormancyGoal}; a synched <em>boolean</em> says a rise
 * is happening; and each client starts its own clock the tick it first sees that flag set. The
 * animation is then smooth at any frame rate, costs one packet instead of one per tick, and a
 * player who walks into view mid-rise sees a rise rather than a frozen statue.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public interface IDormantMob {

    /** No rise animation running on this side. */
    int NOT_WAKING = -1;

    boolean isDormant();

    void setDormant(boolean dormant);

    /** Synched: is the rise playing right now. Cleared by the goal when the window expires. */
    boolean isWaking();

    void setWaking(boolean waking);

    /**
     * The tick THIS SIDE first saw the rise begin, or {@link #NOT_WAKING}. Client-local and not
     * synched &mdash; see the class javadoc for why syncing it was the bug.
     */
    int getWakeAnimStartTick();

    void setWakeAnimStartTick(int tick);

    /** How long the wake-up animation runs. Constant per mob, so the model can normalise against it. */
    int getWakeDuration();

    /**
     * Brings the mob round if it is still a statue. Safe to call every tick and from a damage
     * handler; it does nothing once the mob is awake.
     */
    default void wake() {
        if (isDormant()) {
            setDormant(false);
            setWaking(true);
        }
    }

    /** Dormant, or mid wake-up: either way the mob is not yet fighting. */
    default boolean isInert() {
        return isDormant() || isWaking();
    }
}
