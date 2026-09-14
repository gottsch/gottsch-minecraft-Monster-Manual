package mod.gottsch.forge.gmm.core.entity.monster;

/**
 * A mob that takes a wall down with a single wound-up punch, and whose <em>animation</em> the client
 * has to draw.
 *
 * <h2>Why this is not {@link ISlammingMob}</h2>
 * <p>The timing contract is identical &mdash; a synched flag, a server that counts its own ticks,
 * and a client that starts its own clock on the rising edge (that whole argument lives in
 * {@link ISlammingMob} and is not repeated here). What differs is the <strong>pose</strong>, and it
 * differs in the one way that matters: a slam drives both fists into the FLOOR, a burst drives one
 * fist HORIZONTALLY into something at chest height. Reusing the slam's flag would have cost nothing
 * to write and shown a seven-block wall falling while the mob punched the ground in front of it.</p>
 *
 * <p>A mob may implement both. They must not run at once, and the model resolves that rather than
 * the goals: see {@code StoneColossusModel}, where the slam wins.</p>
 *
 * <h2>One fist, and which one</h2>
 * <p>The striking fist is <strong>synched</strong>, not derived. Deriving it from the animation
 * start tick would have been free and would have let two players watching the same blow see
 * different arms, because each client's clock starts when the entity loaded on that client. The
 * server picks, everyone sees the same punch.</p>
 *
 * @author Mark Gottschling on 9/14/2026
 */
public interface IBurstingMob {

    /** No burst animation running on this side. */
    int NOT_BURSTING = -1;

    /** Synched: is a wall burst in progress right now. */
    boolean isBursting();

    void setBursting(boolean bursting);

    /** Synched: which fist is thrown. Left when true. */
    boolean isBurstLeftFisted();

    void setBurstLeftFisted(boolean leftFisted);

    /**
     * The tick THIS SIDE first saw the burst begin, or {@link #NOT_BURSTING}. Client-local and not
     * synched &mdash; see {@link ISlammingMob} for why syncing it would be the bug.
     */
    int getBurstAnimStartTick();

    void setBurstAnimStartTick(int tick);

    /**
     * Ticks from the start of the burst to the moment the wall goes. The server breaks the blocks on
     * the tick this ends and the model has the fist arriving by exactly then, so it is one number
     * shared by the animation and the blow.
     */
    int getBurstWindupTicks();

    /** Ticks after the blocks go during which the mob is still committed to the punch. */
    int getBurstRecoveryTicks();
}
