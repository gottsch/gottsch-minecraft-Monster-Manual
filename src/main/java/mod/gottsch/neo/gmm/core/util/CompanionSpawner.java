package mod.gottsch.neo.gmm.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;

/**
 * Public, imperative companion-placement API. {@code GMMMonster#getCompanionPool()} /
 * {@code GMMFlyingMonster#getCompanionPool()} use this internally to drive their own tag-driven,
 * spawn-time-triggered escorts, but it's equally meant to be called directly by a consumer that
 * already knows exactly which companion(s) it wants for one specific occasion — e.g. a custom
 * structure/room spawner that places a single mini-boss and wants a hand-picked escort alongside it,
 * with no general "this mob type always gets companions" rule involved at all.
 *
 * <p>Every companion is finalized with {@link MobSpawnType#MOB_SUMMONED} — deliberately not
 * {@code NATURAL}, so a companion can never itself trigger the tag-driven auto-spawn path (that check
 * only fires on natural/egg/command spawns, never {@code MOB_SUMMONED}). Calling this method directly
 * carries the same guarantee: whatever a caller spawns through here is inert as far as the auto-spawn
 * system is concerned, so mixing manual calls with the tag-driven system is always safe.
 *
 * @author Mark Gottschling on 7/14/2026
 */
public final class CompanionSpawner {

    private static final int MAX_PLACEMENT_ATTEMPTS = 8;
    private static final double MIN_RADIUS = 2.0D;
    private static final double RADIUS_SPREAD = 2.0D;

    private CompanionSpawner() {}

    /**
     * Places one companion of {@code type} near {@code owner} (bounded position search, checked
     * against the companion's own vanilla spawn-position rules). Returns the spawned {@link Mob}, or
     * {@code null} if {@code owner} isn't currently in a {@link ServerLevel}, {@code type} didn't
     * create a {@code Mob}, or no valid position was found within the attempt budget.
     */
    @Nullable
    public static Mob spawnNear(Mob owner, EntityType<?> type) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        RandomSource random = owner.getRandom();
        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = MIN_RADIUS + random.nextDouble() * RADIUS_SPREAD;
            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            BlockPos pos = BlockPos.containing(x, owner.getY(), z);

            if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
                continue;
            }
            Entity entity = type.create(level);
            if (!(entity instanceof Mob companion)) {
                return null;
            }
            companion.moveTo(x, owner.getY(), z, random.nextFloat() * 360.0F, 0.0F);
            EventHooks.finalizeMobSpawn(companion, level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntityWithPassengers(companion);
            return companion;
        }
        return null;
    }

    /** Convenience overload: spawns {@code count} companions, each independently picked at random from {@code pool}. */
    public static void spawnNear(Mob owner, TagKey<EntityType<?>> pool, int count) {
        RandomSource random = owner.getRandom();
        for (int i = 0; i < count; i++) {
            BuiltInRegistries.ENTITY_TYPE.getRandomElementOf(pool, random).ifPresent(type -> spawnNear(owner, type.value()));
        }
    }
}
