package mod.gottsch.neo.gmm.core.entity.ai.goal.target;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shared "alert nearby allies to a target" scan, factored out of {@link AllyAlertNearestAttackableTargetGoal}
 * and {@link AllyAlertHurtByTargetGoal} (which had near-identical copies of this logic) so a third,
 * non-goal caller -- {@code Shrieker}, which has no combat target of its own to acquire, just a proximity
 * pulse -- can reuse the exact same broadcast without a third duplicate.
 *
 * @author Mark Gottschling on 7/10/2026
 */
public final class AllyAlertUtil {
    private static final double DEFAULT_RANGE_Y = 10.0D;

    private AllyAlertUtil() {}

    /** Alerts nearby allies (in {@code allyTag}) to {@code target}, using a default vertical range. */
    public static void alertNearby(Mob source, @Nullable LivingEntity target, double horizontalRange, TagKey<EntityType<?>> allyTag) {
        alertNearby(source, target, horizontalRange, DEFAULT_RANGE_Y, allyTag);
    }

    public static void alertNearby(Mob source, @Nullable LivingEntity target, double horizontalRange, double verticalRange, TagKey<EntityType<?>> allyTag) {
        if (target == null) {
            return;
        }
        AABB aabb = AABB.unitCubeFromLowerCorner(source.position()).inflate(horizontalRange, verticalRange, horizontalRange);
        List<? extends Mob> nearby = source.level().getEntitiesOfClass(Mob.class, aabb, EntitySelector.NO_SPECTATORS);
        for (Mob otherMob : nearby) {
            if (source != otherMob && otherMob.getTarget() == null && otherMob.getType().is(allyTag)) {
                otherMob.setTarget(target);
            }
        }
    }
}
