package mod.gottsch.neo.gmm.core.entity.ownership;

/**
 * The standing order given to a {@link OwnershipType#THRALL} mob by its owner. Only meaningful when
 * {@code getOwnershipType() == THRALL}; ignored for every other ownership type.
 * <p>
 * Orders are issued by the <b>owner</b>, which in GMM is always a mob (e.g. a Beholder enthralling a
 * zombie) -- there is no player-facing command UI. A consumer mob wires its own "decide what my
 * thralls should do" goal and calls {@link Ownership#issueOrder}/{@link Ownership#issueGuardOrder}/
 * {@link Ownership#issueAttackOrder} on the mobs it owns.
 *
 * @author Mark Gottschling
 */
public enum ThrallOrder {
    /** Default: stay near the owner; assists the owner's combat target (see SummonedOwnerTargetGoal). */
    FOLLOW,
    /** Hold the current position; still fights back if attacked, but never approaches or wanders. */
    STAY,
    /** Hold a specific bound position ({@link IOwnable#getGuardPos()}), returning to it once idle. */
    GUARD,
    /** Actively pursue and fight a specific owner-designated target, ignoring the owner's own target. */
    ATTACK;

    /** Name-based lookup used when loading from NBT; unknown/old names fall back to {@link #FOLLOW}. */
    public static ThrallOrder byName(String name) {
        if (name != null) {
            for (ThrallOrder order : values()) {
                if (order.name().equals(name)) {
                    return order;
                }
            }
        }
        return FOLLOW;
    }
}
