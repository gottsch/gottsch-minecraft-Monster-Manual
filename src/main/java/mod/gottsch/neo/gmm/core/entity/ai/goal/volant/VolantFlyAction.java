package mod.gottsch.neo.gmm.core.entity.ai.goal.volant;

/**
 * Server-side fine-grained flight phase of a volant mob (launch/cruise/land). Not synced.
 *
 * @author by Mark Gottschling on 7/26/2025
 */
public enum VolantFlyAction {
    IDLE,
    LAUNCH,
    FLY,
    LAND;
}
