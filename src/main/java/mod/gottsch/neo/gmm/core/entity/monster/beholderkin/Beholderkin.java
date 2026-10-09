package mod.gottsch.neo.gmm.core.entity.monster.beholderkin;

import mod.gottsch.neo.gmm.core.entity.monster.GMMFlyingMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Random;

/**
 * Shared base for the Beholder-kin family (Beholder, DeathTyrant, Gazer, Spectator): a hovering,
 * eye-stalked flyer that bites in melee range and randomly floats above the ground.
 *
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public abstract class Beholderkin extends GMMFlyingMonster {

    public Beholderkin(EntityType<? extends FlyingMob> entityType, Level level) {
        super(entityType, level);
    }

    /*
     *
     */
    public static class BeholderkinMoveControl extends MoveControl {
        private final Beholderkin beholderkin;
        private int floatDuration;

        public BeholderkinMoveControl(Beholderkin beholderkin) {
            super(beholderkin);
            this.beholderkin = beholderkin;
        }

        public void tick() {
            if (operation == Operation.MOVE_TO) {
                if (floatDuration-- <= 0) {
                    floatDuration += beholderkin.getRandom().nextInt(5) + 2;
                    Vec3 vec3 = new Vec3(wantedX - beholderkin.getX(), wantedY - beholderkin.getY(), wantedZ - beholderkin.getZ());
                    double distance = vec3.length();
                    vec3 = vec3.normalize();
                    if (canReach(vec3, Mth.ceil(distance))) {
                        beholderkin.setDeltaMovement(beholderkin.getDeltaMovement().add(vec3.scale(0.1D)));
                    } else {
                        operation = Operation.WAIT;
                    }
                }

            }
        }

        private boolean canReach(Vec3 vec3, int distance) {
            AABB aabb = beholderkin.getBoundingBox();

            for(int i = 1; i < distance; ++i) {
                aabb = aabb.move(vec3);
                if (!beholderkin.level().noCollision(beholderkin, aabb)) {
                    return false;
                }
            }
            return true;
        }
    }

    /*
     *
     */
    public static class BeholderkinBiteGoal extends Goal {
        private static final int DEFAULT_COOLDOWN_TIME = 40;
        private Beholderkin beholderkin;
        private int cooldownCount;
        private int cooldownTime;

        public BeholderkinBiteGoal(Beholderkin beholderkin) {
            this(beholderkin, DEFAULT_COOLDOWN_TIME);
        }

        public BeholderkinBiteGoal(Beholderkin beholderkin, int cooldownTime) {
            this.beholderkin = beholderkin;
            this.cooldownTime = cooldownTime;
        }

        @Override
        public boolean canUse() {
            return beholderkin.getTarget() != null;
        }

        @Override
        public void start() {
            // NOTE since cooldownCount counts up (instead of down),
            // set to the threshold so the bite can be performed right away
            // on first use.
            this.cooldownCount = cooldownTime ;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            // cooldown regardless of criteria
            cooldownCount = Math.min(++this.cooldownCount, cooldownTime);
            if (cooldownCount >= cooldownTime) {
                if (this.getAttackReachSqr(beholderkin.getTarget()) >= this.beholderkin.distanceToSqr(beholderkin.getTarget().getX(), beholderkin.getTarget().getY(), beholderkin.getTarget().getZ())
                        && this.beholderkin.hasLineOfSight(beholderkin.getTarget())) {
                    this.beholderkin.doHurtTarget(beholderkin.getTarget());
                    this.cooldownCount = 0;
                }
            }
        }

        protected double getAttackReachSqr(LivingEntity entity) {
            return (double)(this.beholderkin.getBbWidth() * 2.0F * this.beholderkin.getBbWidth() * 2.0F + entity.getBbWidth());
        }
    }

    /*
     *
     */
    public static class BeholderkinRandomFloatAroundGoal extends Goal {
        private final Beholderkin beholderkin;
        // relative float height above ground in blocks
        private final int maxFloatHeight;
        private Random random = new Random();

        public BeholderkinRandomFloatAroundGoal(Beholderkin beholderkin, int maxFloatHeight) {
            this.beholderkin = beholderkin;
            this.maxFloatHeight = maxFloatHeight;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl moveControl = this.beholderkin.getMoveControl();
            if (!moveControl.hasWanted()) {
                return true;
            } else {
                double deltaX = moveControl.getWantedX() - this.beholderkin.getX();
                double deltaY = moveControl.getWantedY() - this.beholderkin.getY();
                double detlaZ = moveControl.getWantedZ() - this.beholderkin.getZ();
                double delta = deltaX * deltaX + deltaY * deltaY + detlaZ * detlaZ;
                return delta < 1.0D || delta > 3600.0D;
            }
        }

        public boolean canContinueToUse() {
            return false;
        }

        public void start() {
            double x = this.beholderkin.getX() + (double)((random.nextFloat() * 2.0F - 1.0F) * 8.0F);
            double y = this.beholderkin.getY() + (double)((random.nextFloat() * 2.0F - 1.0F) * 8.0F);
            double z = this.beholderkin.getZ() + (double)((random.nextFloat() * 2.0F - 1.0F) * 8.0F);

            BlockPos targetPos = new BlockPos((int)x, (int)y, (int)z);

            // Skip targets inside a fluid: picking one would steer the mob into water/lava.
            if (this.beholderkin.level().isFluidAtPosition(targetPos, (fluidState) -> {
                return fluidState.isSourceOfType(Fluids.WATER) || fluidState.isSourceOfType(Fluids.LAVA);
            })) {
                return;
            }

            // Skip targets inside a solid block: the ground scan below would treat that block as the
            // "ground" and hand back a target embedded in terrain, which MoveControl.canReach can never
            // reach -- so the mob parks in WAIT and hovers stuck. Only float toward open air.
            if (!this.beholderkin.level().getBlockState(targetPos).isAir()) {
                return;
            }

            // find ground below mob. isAir() also covers cave air and void air, so this works
            // underground (== Blocks.AIR did not), and the minBuildHeight guard bounds the loop.
            double groundY = y;
            int minY = beholderkin.level().getMinBuildHeight();
            while (groundY > minY && beholderkin.level().getBlockState(new BlockPos((int)x, (int)groundY, (int)z)).isAir()) {
                groundY--;
            }

            // Keep the target within [ground+1, ground+maxFloatHeight]: near the ground but never
            // inside it. (x/y/z is guaranteed air here, so groundY+1 is always <= the original y.)
            y = Mth.clamp(y, groundY + 1, groundY + getMaxFloatHeight());
            this.beholderkin.getMoveControl().setWantedPosition(x, y, z, 1.0D);
        }

        public int getMaxFloatHeight() {
            return maxFloatHeight;
        }
    }

    /*
     *
     */
    public static class BeholderkinLookGoal extends Goal {
        private final Beholderkin beholderkin;

        public BeholderkinLookGoal(Beholderkin beholderkin) {
            this.beholderkin = beholderkin;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        public boolean canUse() {
            return true;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (beholderkin.getTarget() == null) {
                Vec3 vec3 = beholderkin.getDeltaMovement();
                beholderkin.setYRot(-((float)Mth.atan2(vec3.x, vec3.z)) * (180F / (float)Math.PI));
                beholderkin.yBodyRot = beholderkin.getYRot();
            } else {
                LivingEntity livingEntity = this.beholderkin.getTarget();
                if (livingEntity.distanceToSqr(this.beholderkin) < 4096.0D) {
                    double deltaX = livingEntity.getX() - beholderkin.getX();
                    double deltaZ = livingEntity.getZ() - beholderkin.getZ();
                    beholderkin.setYRot(-((float)Mth.atan2(deltaX, deltaZ)) * (180F / (float)Math.PI));
                    beholderkin.yBodyRot = beholderkin.getYRot();
                }
            }
        }
    }
}
