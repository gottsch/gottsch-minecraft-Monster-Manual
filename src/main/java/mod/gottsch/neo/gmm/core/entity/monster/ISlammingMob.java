package mod.gottsch.neo.gmm.core.entity.monster;

/**
 * A mob with a wind-up-then-impact ground attack whose <em>animation</em> the client has to draw.
 *
 * <h2>Why this syncs a FLAG and not a tick</h2>
 * <p>The first cut synched the server's {@code tickCount} at the moment the slam began and had the
 * model measure against its own {@code ageInTicks}. That is wrong, and subtly: <strong>
 * {@code tickCount} is per-side</strong>. The client's counter starts when the entity was created
 * on that client, so for anything that existed before the player loaded the chunk the two clocks
 * are an arbitrary distance apart — and the animation ran at an arbitrary offset from the blow.
 * The symptom was the attack landing while the fists were still overhead (Mark, 2026-09-13).
 *
 * <p>So: the server owns the timing and counts its own ticks inside the goal; a synched
 * <em>boolean</em> says a slam is happening; and each client starts its own clock the tick it first
 * sees that flag set. The two clocks are then at most network latency apart instead of arbitrarily
 * apart, and the animation self-corrects for a player who walks into view mid-slam.
 *
 * <p>The wind-up length is still published here, because it is <strong>one number shared by the
 * server and the animation</strong> — the server strikes on the tick it ends, and the model has to
 * have the fists overhead by exactly then.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public interface ISlammingMob {

    /** No slam animation running on this side. */
    int NOT_SLAMMING = -1;

    /** Synched: is a slam in progress right now. */
    boolean isSlamming();

    void setSlamming(boolean slamming);

    /**
     * The tick THIS SIDE first saw the slam begin, or {@link #NOT_SLAMMING}. Client-local and not
     * synched — see the class javadoc for why syncing it would be the bug.
     */
    int getSlamAnimStartTick();

    void setSlamAnimStartTick(int tick);

    /** Ticks from the start of the slam to the moment of impact. */
    int getSlamWindupTicks();

    /** Ticks after impact during which the mob is still committed to the blow. */
    int getSlamRecoveryTicks();
}
