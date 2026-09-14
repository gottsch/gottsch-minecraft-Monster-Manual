package mod.gottsch.forge.gmm.core.entity.monster;

/**
 * A mob that heaves something heavy overhead and throws it, and whose <em>animation</em> the client
 * has to draw.
 *
 * <p>The third of the same family, and the timing contract is the one {@link ISlammingMob} argues
 * for: a synched flag, a server that counts its own ticks inside the goal, and a client that starts
 * its own clock on the rising edge. What differs is the pose and what happens at the end of it &mdash;
 * a slam ends at the floor, a burst ends in a wall, and a hurl ends with the mob's hands EMPTY and
 * something in the air.
 *
 * <p>The wind-up here is doing more work than in the other two. A thrown rock is the answer to a
 * player who has backed out of reach, and the only warning that player gets is the mob winding up,
 * so this is the number to raise if the boulder ever feels unfair rather than the damage to lower.
 *
 * @author Mark Gottschling on 9/14/2026
 */
public interface IHurlingMob {

    /** No hurl animation running on this side. */
    int NOT_HURLING = -1;

    /** Synched: is a throw in progress right now. */
    boolean isHurling();

    void setHurling(boolean hurling);

    /**
     * The tick THIS SIDE first saw the hurl begin, or {@link #NOT_HURLING}. Client-local and not
     * synched &mdash; see {@link ISlammingMob} for why syncing it would be the bug.
     */
    int getHurlAnimStartTick();

    void setHurlAnimStartTick(int tick);

    /** Ticks from the start of the heave to the moment the projectile leaves the mob's hands. */
    int getHurlWindupTicks();

    /** Ticks after the throw during which the mob is still following through. */
    int getHurlRecoveryTicks();
}
