package mod.gottsch.neo.gmm.core.entity.ai.goal;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gottschcore.random.RandomHelper;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * @author Mark Gottschling on Jan 11, 2024
 *
 */
public class WeightedChanceSummonGoal extends ChanceSummonGoal {
    private static final int DEFAULT_COOLDOWN_TIME = 1200;
    private static final double SUMMON_DISTANCE = 32;
    private static final double SUMMON_DISTSNCE_SQ = SUMMON_DISTANCE * SUMMON_DISTANCE;

    private final Mob mob;
    private final WeightedCollection<Double, EntityType<? extends Mob>> mobs;
    private int minMobs;
    private int maxMobs;

    public WeightedChanceSummonGoal(Mob summoner, WeightedCollection<Double, EntityType<? extends Mob>> mobs) {
        this(summoner, DEFAULT_COOLDOWN_TIME, 100, mobs, 1, 1);
    }

    public WeightedChanceSummonGoal(Mob summoner, EntityType<? extends Mob> mob) {
        this(summoner, new WeightedCollection<Double, EntityType<? extends Mob>>().add(1D, mob));
    }

    public WeightedChanceSummonGoal(Mob summoner, int cooldownTime, double chance, EntityType<? extends Mob> mob, int minMobs, int maxMobs) {
        this(summoner, cooldownTime, chance, new WeightedCollection<Double, EntityType<? extends Mob>>().add(1D, mob), minMobs, maxMobs);
    }

    public WeightedChanceSummonGoal(Mob summoner, int cooldownTime, double chance, WeightedCollection<Double, EntityType<? extends Mob>> mobs, int minMobs, int maxMobs) {
        super(cooldownTime, chance);
        this.mob = summoner;
        this.mobs = mobs;
        this.minMobs = minMobs;
        this.maxMobs = maxMobs;
    }

    @Override
    public void start() {
        this.cooldownCount = cooldownTime / 2;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            // increase cooldown count regardless is the mob can see the target or not.
            ++this.cooldownCount;
            if (target.distanceToSqr(this.mob) < SUMMON_DISTSNCE_SQ && this.mob.hasLineOfSight(target)) {
                Level level = this.mob.level();
                if (this.getCooldownCount() >= getCooldownTime()) {
                    if (RandomHelper.checkProbability(mob.getRandom(), getProbability())) {
                        int y = 0;
                        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mob.blockPosition().getX(), mob.blockPosition().getZ());
                        if (mob.blockPosition().getY() > height) {
                            // mob is above ground
                            y = height;
                        } else {
                            // find ground below mob. isAir() also covers cave air and void air, so this
                            // works underground (== Blocks.AIR did not).
                            y = mob.blockPosition().below().getY();
                            while (level.getBlockState(mob.blockPosition().atY(y)).isAir()) {
                                y--;
                                if (Math.abs(mob.blockPosition().getY() - y) > 15) {
                                    // no ground within range (e.g. hovering high above terrain); reset the
                                    // cooldown so we don't rescan every tick and never summon.
                                    this.cooldownCount = 0;
                                    return;
                                }
                            }
                        }
                        GMM.LOGGER.debug("spawning...");
                        int numSpawns = mob.getRandom().nextInt(minMobs, maxMobs + 1);
                        for (int i = 0; i < numSpawns; i++) {
                            EntityType<? extends Mob> mob;
                            mob = mobs.next();

                            boolean spawnSuccess = super.spawn((ServerLevel) level, level.random, this.mob, mob, Coords.of(this.mob.blockPosition().getX(), y + 1, this.mob.blockPosition().getZ()), target);
                            if (!level.isClientSide() && spawnSuccess) {
                                for (int p = 0; p < 20; p++) {
                                    double xSpeed = this.mob.getRandom().nextGaussian() * 0.02D;
                                    double ySpeed = this.mob.getRandom().nextGaussian() * 0.02D;
                                    double zSpeed = this.mob.getRandom().nextGaussian() * 0.02D;
                                    ((ServerLevel) level).sendParticles(ParticleTypes.POOF, this.mob.blockPosition().getX() + 0.5D, this.mob.blockPosition().getY(), this.mob.blockPosition().getZ() + 0.5D, 1, xSpeed, ySpeed, zSpeed, (double) 0.15F);
                                }
                            }
                        }
                    }
                    // cooldown count is reset regardless of chance success or failure
                    this.cooldownCount = 0;
                }
            }
        }
    }
}
