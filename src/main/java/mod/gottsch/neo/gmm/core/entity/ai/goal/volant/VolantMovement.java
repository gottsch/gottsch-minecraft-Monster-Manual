package mod.gottsch.neo.gmm.core.entity.ai.goal.volant;

/**
 * High-level locomotion state of a volant (flight-capable) mob: on the ground or airborne.
 * Synced to the client (as a byte ordinal) so it can drive walk-vs-flight animation.
 *
 * @author by Mark Gottschling on 7/27/2025
 */
public enum VolantMovement {
    WALK,
    FLY;

    private static final VolantMovement[] VALUES = values();

    public static VolantMovement byOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : WALK;
    }
}
