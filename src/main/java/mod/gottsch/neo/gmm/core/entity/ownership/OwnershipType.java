package mod.gottsch.neo.gmm.core.entity.ownership;

/**
 * The <em>kind</em> of relationship an owned mob has with its owner, layered on top of the owner UUID.
 * <p>
 * An "owner" in GMM is any entity that spawns, summons or conjures another entity. The type decides
 * the extra behavior the ownership carries:
 * <ul>
 *   <li>{@link #NONE} -- not owned (default for a natural spawn).</li>
 *   <li>{@link #REINFORCEMENT} -- owner is recorded for lineage/allegiance, but the mob is otherwise a
 *       normal, permanent spawn that targets whoever it likes (vanilla zombie-reinforcement style).</li>
 *   <li>{@link #SUMMONED} -- a temporary conjuration: it may assist its owner and it has a lifespan,
 *       after which it is "unsummoned" (vanishes). E.g. a wizard summoning a demon for a set time.</li>
 *   <li>{@link #THRALL} -- a mob under the owner's command that obeys issued orders (guard, follow,
 *       attack, stay).</li>
 * </ul>
 *
 * @author Mark Gottschling
 */
public enum OwnershipType {
    NONE,
    REINFORCEMENT,
    SUMMONED,
    THRALL;

    /** Name-based lookup used when loading from NBT; unknown/old names fall back to {@link #NONE}. */
    public static OwnershipType byName(String name) {
        if (name != null) {
            for (OwnershipType type : values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
        }
        return NONE;
    }
}
