package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ai.control.FlyingGottschMonsterMoveControl;
import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantFlyAction;
import mod.gottsch.neo.gmm.core.entity.ai.goal.volant.VolantMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for a ground-walking humanoid that can launch into flight. Owns the dual
 * locomotion setup: walking ({@link MoveControl} + {@link GroundPathNavigation} +
 * gravity) and flying ({@link FlyingGottschMonsterMoveControl} +
 * {@link FlyingPathNavigation} + no gravity). The active set is swapped as a unit
 * in {@link #setMovementState(VolantMovement)}.
 *
 * @author by Mark Gottschling on 7/16/2025
 */
public class WingedHumanoid extends GMMMonster implements IWingedHumanoid {
    /** default airborne ceiling (blocks above launch position) for a true flyer. */
    public static final double DEFAULT_MAX_FLY_HEIGHT = 6.0D;

    /** default distance (blocks, from the target's center) at which the mob lands — beside the
     *  target rather than on top of it. Kept below the combat goal's melee reach so it can still bite. */
    public static final double DEFAULT_LAND_PROXIMITY = 2.0D;

    // synced (as a byte ordinal) so the client can drive walk vs. flight animation
    private static final EntityDataAccessor<Byte> DATA_MOVEMENT_STATE =
            SynchedEntityData.defineId(WingedHumanoid.class, EntityDataSerializers.BYTE);

    // locomotion controllers — swapped together as a unit
    protected final MoveControl walkingMoveControl;
    protected final FlyingGottschMonsterMoveControl flyingMoveControl;
    /** ground pathfinder, created by super via {@link #createNavigation(Level)}. */
    protected final PathNavigation groundNavigation;
    /** flying pathfinder, used while airborne. */
    protected final FlyingPathNavigation flyingNavigation;

    /** server-side fine-grained flight phase (launch/fly/land); not synced. */
    protected VolantFlyAction flyAction;
    protected boolean landAttackOnly;

    protected WingedHumanoid(EntityType<? extends Monster> mob, Level level) {
        super(mob, level);

        // setup move controls
        this.walkingMoveControl = new MoveControl(this);
        this.flyingMoveControl = new FlyingGottschMonsterMoveControl(this);

        // super() already created the ground navigation via createNavigation();
        // keep a handle to it and build the flying counterpart.
        this.groundNavigation = this.navigation;
        this.flyingNavigation = buildFlyingNavigation(level);

        // start grounded
        this.moveControl = this.walkingMoveControl;
        setDataMovementState(VolantMovement.WALK);
        this.flyAction = VolantFlyAction.IDLE;
    }

    /**
     * The default/initial navigation is the ground pathfinder (the mob starts walking).
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    protected FlyingPathNavigation buildFlyingNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOVEMENT_STATE, (byte) VolantMovement.WALK.ordinal());
    }

    // NOTE flight state is intentionally not persisted: the mob always reloads
    // grounded and re-evaluates whether to launch. This avoids reloading airborne
    // (gravity desync / fall damage) and keeps save data minimal.

    @Override
    public boolean isLaunching() {
        return this.flyAction == VolantFlyAction.LAUNCH;
    }

    public void setIsLaunching(boolean launching) {
        this.flyAction = launching ? VolantFlyAction.LAUNCH : VolantFlyAction.IDLE;
    }

    @Override
    public boolean isLanding() {
        return this.flyAction == VolantFlyAction.LAND;
    }

    public void setIsLanding(boolean landing) {
        this.flyAction = landing ? VolantFlyAction.LAND : VolantFlyAction.IDLE;
    }

    @Override
    public boolean isWalking() {
        return !isFlying();
    }

    @Override
    public boolean isFlying() {
        return getDataMovementState() == VolantMovement.FLY;
    }

    private VolantMovement getDataMovementState() {
        return VolantMovement.byOrdinal(this.entityData.get(DATA_MOVEMENT_STATE));
    }

    private void setDataMovementState(VolantMovement movement) {
        this.entityData.set(DATA_MOVEMENT_STATE, (byte) movement.ordinal());
    }

    /**
     * Swap the entire locomotion set (move control + navigation + gravity) to match
     * the requested movement state. Server-driven; the synced state propagates to
     * the client for animation.
     */
    @Override
    public void setMovementState(VolantMovement state) {
        if (getDataMovementState() == state) {
            return;
        }
        setDataMovementState(state);

        // abandon any in-progress path before swapping controllers
        this.navigation.stop();

        if (state == VolantMovement.WALK) {
            this.moveControl = this.walkingMoveControl;
            this.navigation = this.groundNavigation;
            this.flyAction = VolantFlyAction.IDLE;
            this.setNoGravity(false);
        } else {
            this.moveControl = this.flyingMoveControl;
            this.navigation = this.flyingNavigation;
            this.setNoGravity(true);
        }
    }

    /** convenience: enter active flight. */
    public void setFlyingMovement() {
        setMovementState(VolantMovement.FLY);
        this.flyAction = VolantFlyAction.FLY;
    }

    /** convenience: return to the ground. */
    public void setWalkingMovement() {
        setMovementState(VolantMovement.WALK);
        this.flyAction = VolantFlyAction.IDLE;
        // clear any fall distance accumulated during the controlled descent so the
        // final settle under gravity doesn't convert into fall damage.
        this.resetFallDistance();
    }

    /**
     * A controlled descent (or any active flight) never inflicts fall damage — the mob drives its
     * own vertical motion, which vanilla still counts as fall distance. Suppress it while airborne.
     */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (isFlying()) {
            this.resetFallDistance();
            return false;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public double getMaxFlyHeight() {
        return DEFAULT_MAX_FLY_HEIGHT;
    }

    /**
     * Distance (blocks, from the target's center) at which this mob lands when closing to melee — so
     * it touches down beside the target instead of on top of it. Data-driven via the {@code gmm:mob_config}
     * {@code landProximity} property, falling back to {@link #DEFAULT_LAND_PROXIMITY}.
     */
    public double getLandProximity() {
        return MobConfigHelper.get(this).number("landProximity", DEFAULT_LAND_PROXIMITY);
    }

    /**
     * Drive a controlled descent toward the given ground column using the (active)
     * flying move control at a reduced speed, zeroing fall distance as it nears the
     * ground so the transition back to gravity is damage-free. Returns {@code true}
     * once effectively touched down — the caller should then {@link #setWalkingMovement()}.
     *
     * <p>Velocity damping is delegated to the move control (which runs after goals
     * each tick); forcing delta in the goal would simply be overwritten.
     */
    public boolean tickDescentToward(BlockPos groundPos, double speedModifier) {
        double standY = groundPos.getY() + 1.0D;
        getMoveControl().setWantedPosition(groundPos.getX() + 0.5D, standY, groundPos.getZ() + 0.5D, speedModifier);

        double distanceToGround = position().y - standY;
        if (distanceToGround <= 1.0D) {
            // continuously clear accumulated fall distance through the final approach
            resetFallDistance();
        }
        return onGround() || distanceToGround < 0.3D;
    }

    @Override
    public boolean isLandAttackOnly() {
        return this.landAttackOnly;
    }

    public void setLandAttackOnly(boolean landAttackOnly) {
        this.landAttackOnly = landAttackOnly;
    }

    /** Snap the mob's body to face the target (only when reasonably close). */
    public void faceTarget(LivingEntity target) {
        if (target != null && target.distanceToSqr(this) < 4096.0D) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            setYRot(-((float) Mth.atan2(dx, dz)) * (180F / (float) Math.PI));
            yBodyRot = getYRot();
        }
    }

    /**
     * Finds a safe standing position at/near the ground below {@code startPos}: searches down for a
     * solid block, then up for two stacked air blocks. Returns the block an entity would stand on,
     * or null if none found within {@code maxSearchDistance}.
     */
    public static BlockPos findSafeGroundPos(Level level, BlockPos startPos, int maxSearchDistance) {
        BlockPos.MutableBlockPos currentPos = startPos.mutable();

        // 1. Search downwards for a solid block
        for (int i = 0; i < maxSearchDistance; i++) {
            BlockState state = level.getBlockState(currentPos);
            if (!state.isAir() && !state.canBeReplaced() && state.isCollisionShapeFullBlock(level, currentPos)) {
                break; // Found the ground level
            }
            currentPos.move(0, -1, 0);
            if (currentPos.getY() < level.getMinBuildHeight()) {
                return null; // Reached bottom of world without finding ground
            }
        }

        // 2. From the solid block, find the first spot with two stacked air blocks (where an entity stands)
        BlockPos groundLevel = currentPos.immutable();

        BlockPos posAboveGround = groundLevel.above();
        if (level.getBlockState(posAboveGround).isAir() && level.getBlockState(posAboveGround.above()).isAir()) {
            return posAboveGround;
        }

        BlockPos.MutableBlockPos safePos = groundLevel.mutable().move(0, 1, 0);
        for (int i = 0; i < maxSearchDistance; i++) {
            if (level.getBlockState(safePos).isAir() && level.getBlockState(safePos.above()).isAir()) {
                return safePos.immutable();
            }
            safePos.move(0, 1, 0);
            if (safePos.getY() > startPos.getY() + 10) {
                break;
            }
        }

        return null;
    }
}
