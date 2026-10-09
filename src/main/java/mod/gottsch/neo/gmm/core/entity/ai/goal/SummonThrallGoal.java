package mod.gottsch.neo.gmm.core.entity.ai.goal;

import mod.gottsch.neo.gmm.core.entity.monster.ICastingMob;
import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.event.ForgeEventFactory;
import net.neoforged.neoforge.registries.ForgeRegistries;

/**
 * The Summon-a-thrall ability: a charge-up-then-execute effect (like {@link EnthrallGoal}'s shape,
 * but <em>conjuring</em> a brand-new mob rather than dominating an existing one) cast near the caster
 * while it has an active combat target. Picks an {@link EntityType} at random from a consumer-tag-
 * driven pool, spawns it, then stamps it a permanent {@code THRALL} of the caster via
 * {@link Ownership#enthrall} -- the exact same permanent-ownership + goal-injection path a live
 * Enthrall cast uses, just arriving at a thrall by conjuring instead of dominating. Gated by its own
 * cooldown and a shared {@code maxThralls} cap checked against {@link IGMMMonster#getThralls()}, so a
 * caster running both this and {@link EnthrallGoal} side by side (e.g. Wight) can't exceed one
 * combined army-size limit between the two abilities.
 * <p>
 * Only usable by an {@link IGMMMonster} (needs somewhere to track the resulting thrall list). Runs
 * continuously alongside combat, mirroring how Beholder runs its own (Java-configured) minion-summon
 * goal alongside {@code EnthrallGoal} rather than tying either to a kill.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class SummonThrallGoal extends Goal {
    private static final int DEFAULT_CHARGE_TIME = 60;
    private static final double DEFAULT_SPAWN_RADIUS = 2.5D;

    private final Mob mob;
    private final IGMMMonster summoner;
    private final TagKey<EntityType<?>> poolTag;
    private final int chargeTime;
    private final int cooldownTime;
    private final int maxThralls;
    private final double spawnRadius;
    private int chargeTimeCount;
    private int cooldownCount;

    public SummonThrallGoal(Mob mob, TagKey<EntityType<?>> poolTag, int chargeTime, int cooldownTime, int maxThralls, double spawnRadius) {
        this.mob = mob;
        this.summoner = (IGMMMonster) mob;
        this.poolTag = poolTag;
        this.chargeTime = chargeTime;
        this.cooldownTime = cooldownTime;
        this.maxThralls = maxThralls;
        this.spawnRadius = spawnRadius;
    }

    public SummonThrallGoal(Mob mob, TagKey<EntityType<?>> poolTag) {
        this(mob, poolTag, DEFAULT_CHARGE_TIME, 600, 3, DEFAULT_SPAWN_RADIUS);
    }

    @Override
    public boolean canUse() {
        return cooldownCount <= 0 && summoner.getThralls().size() < maxThralls && mob.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return cooldownCount <= 0 && mob.getTarget() != null;
    }

    @Override
    public void start() {
        chargeTimeCount = 0;
        if (mob instanceof ICastingMob casting) {
            casting.setCasting(true);
        }
    }

    @Override
    public void stop() {
        chargeTimeCount = 0;
        cooldownCount = cooldownTime;
        if (mob instanceof ICastingMob casting) {
            casting.setCasting(false);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (cooldownCount > 0) {
            --cooldownCount;
            return;
        }
        if (mob.getTarget() == null) {
            chargeTimeCount = 0;
            return;
        }
        mob.getLookControl().setLookAt(mob.getTarget(), 30.0F, 30.0F);
        // charge-up telegraph: a few soul motes around the caster every few ticks, so the cast is
        // visible well before the new thrall actually appears.
        if (chargeTimeCount % 5 == 0 && mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.7D, mob.getZ(),
                    3, mob.getBbWidth() * 0.3D, 0.2D, mob.getBbWidth() * 0.3D, 0.01D);
        }
        if (++chargeTimeCount >= chargeTime) {
            raise();
            chargeTimeCount = 0;
            cooldownCount = cooldownTime;
        }
    }

    /** Conjures a fresh mob from {@code poolTag} (a zombie default is expected in it) near the caster. */
    private void raise() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.tags().getTag(poolTag)
                .getRandomElement(mob.getRandom()).orElse(EntityType.ZOMBIE);
        Entity spawned = type.create(serverLevel);
        if (!(spawned instanceof Mob raised)) {
            return;
        }
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double x = mob.getX() + Math.cos(angle) * spawnRadius;
        double z = mob.getZ() + Math.sin(angle) * spawnRadius;
        raised.moveTo(x, mob.getY(), z, mob.getYRot(), 0.0F);
        ForgeEventFactory.onFinalizeSpawn(raised, serverLevel, serverLevel.getCurrentDifficultyAt(raised.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        serverLevel.addFreshEntityWithPassengers(raised);
        if (mob.getTarget() != null) {
            raised.setTarget(mob.getTarget());
        }
        Ownership.enthrall(raised, mob);

        serverLevel.sendParticles(ParticleTypes.SOUL, raised.getX(), raised.getY() + raised.getBbHeight() * 0.5D, raised.getZ(),
                20, raised.getBbWidth() * 0.3D, 0.3D, raised.getBbWidth() * 0.3D, 0.02D);
        serverLevel.playSound(null, raised.getX(), raised.getY(), raised.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, mob.getSoundSource(), 1.0F, 0.8F);
    }
}
