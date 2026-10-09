package mod.gottsch.neo.gmm.core.entity.ai.goal;





import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import mod.gottsch.neo.gmm.core.entity.monster.IChargingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A wind up, a run, and one heavy hit: the bull rush. Meant for a big melee mob that should open a
 * fight by closing distance violently rather than walking into range like everything else.
 *
 * <p>The goal only fires from the middle distance -- too close and there is nothing to charge
 * across, too far and the target has time to simply walk aside. Between those it stops the mob dead
 * for {@code windupTicks} (the telegraph -- a mob implementing {@link IChargingMob} drops its head
 * here), locks the target's position at the moment the run starts, and drives at it. Locking the
 * point is deliberate: a charge that steers is a homing missile, and a player who dodges should be
 * rewarded with the sight of two tons of beef going past.
 *
 * <p>It moves by {@code PathNavigation} rather than by writing {@code deltaMovement}, so the charge
 * follows the floor, handles a step up, and does not bury the mob in a corridor wall. The speed
 * comes from the navigation's own speed modifier plus a temporary
 * {@link Attributes#MOVEMENT_SPEED} modifier, both removed in {@link #stop()}.
 *
 * <p>The impact reuses {@code doHurtTarget}, so armour, enchantments, damage events and knockback
 * resistance all behave normally; the extra bite is a temporary {@link Attributes#ATTACK_DAMAGE}
 * modifier applied for that single call, plus knockback along the charge line on top of whatever
 * {@code ATTACK_KNOCKBACK} already gave. Deal the damage by hand and every one of those would have
 * to be reimplemented.
 *
 * @author Mark Gottschling on 9/7/2026
 */
public class ChargeAttackGoal extends Goal {
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "charge_speed");
    private static final ResourceLocation DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "charge_damage");

    private final PathfinderMob mob;
    private final double triggerRangeMin;
    private final double triggerRangeMax;
    private final double hitRange;
    private final int windupTicks;
    private final int maxChargeTicks;
    private final int cooldownTicks;
    private final double chargeSpeed;
    /** Extra movement speed while charging, as a multiplier of the mob's base. */
    private final double speedBonus;
    /**
     * Extra attack damage on the impact, as a FLAT amount rather than a multiplier.
     *
     * <p>It was a {@code MULTIPLY_BASE} multiplier until 2026-09-07, which was wrong in a way that
     * only shows once the mob is armed: {@code AttributeInstance.calculateValue} applies
     * {@code MULTIPLY_BASE} <em>after</em> every {@code ADDITION} modifier, and a weapon's damage is
     * an {@code ADDITION}. So the multiplier compounded with whatever happened to be in the mob's
     * hand -- a Minotaur holding a +9 axe turned a "+80% charge" into +12.8 rather than the +5.6 the
     * bare-handed mob would have got. Arming it better would have made the charge better, without
     * limit and without anyone choosing that.
     *
     * <p>A charge's extra damage is the impact of a large animal hitting you. It should not care
     * what the animal is carrying.
     */
    private final double damageBonus;
    private final float knockback;
    private final float probability;

    private LivingEntity target;
    private Vec3 chargePoint;
    private int windup;
    private int charging;
    private long cooldownUntilTick;
    private boolean spent;

    public ChargeAttackGoal(PathfinderMob mob, double triggerRangeMin, double triggerRangeMax,
                            double hitRange, int windupTicks, int maxChargeTicks, int cooldownTicks,
                            double chargeSpeed, double speedBonus, double damageBonus,
                            float knockback, float probability) {
        this.mob = mob;
        this.triggerRangeMin = triggerRangeMin;
        this.triggerRangeMax = triggerRangeMax;
        this.hitRange = hitRange;
        this.windupTicks = windupTicks;
        this.maxChargeTicks = maxChargeTicks;
        this.cooldownTicks = cooldownTicks;
        this.chargeSpeed = chargeSpeed;
        this.speedBonus = speedBonus;
        this.damageBonus = damageBonus;
        this.knockback = knockback;
        this.probability = probability;
        // takes over movement and facing for its duration -- the melee goal must not steer at the
        // same time, which is exactly what these flags prevent.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity candidate = this.mob.getTarget();
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }
        if (this.mob.level().getGameTime() < this.cooldownUntilTick) {
            return false;
        }
        double distance = this.mob.distanceTo(candidate);
        if (distance < this.triggerRangeMin || distance > this.triggerRangeMax) {
            return false;
        }
        if (!this.mob.getSensing().hasLineOfSight(candidate)) {
            return false;
        }
        // rolled per attempt rather than per cooldown, so the mob does not charge on a metronome
        if (this.mob.getRandom().nextFloat() >= this.probability) {
            return false;
        }
        this.target = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.spent
                && this.target != null
                && this.target.isAlive()
                && (this.windup > 0 || this.charging < this.maxChargeTicks);
    }

    @Override
    public void start() {
        this.windup = this.windupTicks;
        this.charging = 0;
        this.spent = false;
        this.chargePoint = null;
        this.mob.getNavigation().stop();
        if (this.mob instanceof IChargingMob charger) {
            charger.setCharging(true);
        }
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (this.windup > 0) {
            // plant and stare. Nothing moves; this is the whole telegraph.
            this.mob.getNavigation().stop();
            if (--this.windup == 0) {
                this.chargePoint = this.target.position();
                applyModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, this.speedBonus);
                driveAtChargePoint();
            }
            return;
        }

        this.charging++;
        if (this.mob.getNavigation().isDone()) {
            driveAtChargePoint();
        }

        if (this.mob.distanceToSqr(this.target) <= this.hitRange * this.hitRange) {
            impact();
        }
    }

    private void driveAtChargePoint() {
        this.mob.getNavigation().moveTo(this.chargePoint.x, this.chargePoint.y, this.chargePoint.z, this.chargeSpeed);
    }

    /** One hit, then the goal is done -- a charge that could connect twice would not be a charge. */
    private void impact() {
        applyFlatModifier(Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID, this.damageBonus);
        try {
            this.mob.doHurtTarget(this.target);
        } finally {
            removeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID);
        }
        // knockback() pushes the victim AWAY from the (x, z) it is handed, so hand it the mob's
        // position -- the target goes down the charge line, not back along it.
        this.target.knockback(this.knockback,
                this.mob.getX() - this.target.getX(),
                this.mob.getZ() - this.target.getZ());
        this.spent = true;
    }

    @Override
    public void stop() {
        removeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
        removeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID);
        this.mob.getNavigation().stop();
        this.cooldownUntilTick = this.mob.level().getGameTime() + this.cooldownTicks;
        this.target = null;
        this.chargePoint = null;
        if (this.mob instanceof IChargingMob charger) {
            charger.setCharging(false);
        }
    }

    /** Flat {@code ADDITION}: does not compound with the mob's weapon. See {@link #damageBonus}. */
    private void applyFlatModifier(Holder<Attribute> attribute, ResourceLocation id, double amount) {
        applyModifier(attribute, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Proportional {@code MULTIPLY_BASE}: scales with the mob's own value, which is what speed wants. */
    private void applyModifier(Holder<Attribute> attribute, ResourceLocation id, double amount) {
        applyModifier(attribute, id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private void applyModifier(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = this.mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // a goal can be stopped mid-charge by something else claiming MOVE; never stack two
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private void removeModifier(Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = this.mob.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
