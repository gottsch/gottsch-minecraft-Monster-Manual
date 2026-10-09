package mod.gottsch.neo.gmm.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Anchoring: a mob posted to guard a position rather than roam.
 *
 * <h2>What this is for</h2>
 * <p>A consumer calls vanilla's own {@code Mob#restrictTo(BlockPos, int)} on an instance it has just
 * placed &mdash; a boss in a dungeon room, a guardian in an alcove, a zombie in its grave &mdash; and
 * that instance then stays where it was put and stops despawning. An unanchored instance is untouched
 * and behaves exactly as it always has, so this is opt-in per <em>instance</em>, not per mob type.</p>
 *
 * <h2>The two things vanilla does not do</h2>
 * <ol>
 *   <li><strong>{@code restrictTo} does not stop a mob despawning.</strong> {@code Mob#checkDespawn}
 *       never looks at the restriction, so a posted guardian wanders off the despawn cliff like any
 *       natural spawn. {@code GMMMonster}/{@code GMMFlyingMonster} override {@code checkDespawn} to
 *       return early when {@code hasRestriction()}.</li>
 *   <li><strong>{@code restrictTo} is not persisted.</strong> {@code Entity#saveWithoutId} writes
 *       {@code Glowing}, {@code Silent}, {@code NoGravity} and {@code Invulnerable}; the restrict
 *       centre and radius are written by nothing at all, because vanilla only ever sets them on
 *       villagers, whose brain re-derives a home from a bed. So a posted guardian came back from a
 *       chunk unload with no restriction, and therefore free to wander and free to despawn. That is
 *       what {@link #save}/{@link #load} are for.</li>
 * </ol>
 *
 * <p>The tag names are the ones {@code GraveZombie}, {@code AnimatedArmor} and {@code WoodGolem}
 * already wrote before this class existed, and are <strong>kept verbatim</strong>: changing them
 * would silently un-anchor every guardian already standing in an existing world.</p>
 *
 * <h2>Anchoring is not a leash</h2>
 * <p>The radius gates despawn and, via {@code Mob#isWithinRestriction}, the positions the wander
 * goals will pick &mdash; {@code WaterAvoidingRandomStrollGoal} runs its candidates through that
 * method, which is what keeps an idle guardian in its room without any extra goal. It was never
 * meant to haul a mob home mid-fight, and left unqualified it does something worse than leash: a
 * guardian that chased an intruder out of its room spends the rest of its life trying to wander back
 * even while the fight is still going. So both base classes suspend the restriction while the mob is
 * engaged &mdash; see {@code GMMMonster#isAnchorSuspended}. A consumer that genuinely wants the mob
 * to walk back to its post when idle adds vanilla's {@code MoveTowardsRestrictionGoal} to that mob's
 * goal list; this class deliberately installs no goals, since a goal list belongs to its mob.</p>
 *
 * @author Mark Gottschling on Sep 3, 2026
 */
public final class Anchor {

    private static final String TAG_HOME_X = "HomePosX";
    private static final String TAG_HOME_Y = "HomePosY";
    private static final String TAG_HOME_Z = "HomePosZ";
    private static final String TAG_HOME_RADIUS = "HomeRadius";

    private Anchor() {}

    /** Posts {@code mob} to guard {@code pos}. Convenience for a consumer placing a mob. */
    public static void guard(Mob mob, BlockPos pos, int radius) {
        mob.restrictTo(pos, radius);
    }

    /** Writes the restriction, if there is one. Nothing is written for an unanchored mob. */
    public static void save(CompoundTag tag, Mob mob) {
        if (!mob.hasRestriction()) {
            return;
        }
        BlockPos home = mob.getRestrictCenter();
        tag.putInt(TAG_HOME_X, home.getX());
        tag.putInt(TAG_HOME_Y, home.getY());
        tag.putInt(TAG_HOME_Z, home.getZ());
        tag.putInt(TAG_HOME_RADIUS, (int) mob.getRestrictRadius());
    }

    /** Restores a saved restriction. A mob saved without one is left unanchored. */
    public static void load(CompoundTag tag, Mob mob) {
        if (!tag.contains(TAG_HOME_X)) {
            return;
        }
        mob.restrictTo(new BlockPos(tag.getInt(TAG_HOME_X), tag.getInt(TAG_HOME_Y),
                tag.getInt(TAG_HOME_Z)), tag.getInt(TAG_HOME_RADIUS));
    }
}
