package mod.gottsch.neo.gmm.core.entity.ai.control;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Flying move control: steers the mob straight toward its wanted position at a
 * speed derived from {@link Attributes#FLYING_SPEED}, driving the vertical axis
 * directly so it can actually climb and descend. Pure locomotion — height limits,
 * landing and pathing decisions live in the volant goals.
 *
 * @author by Mark Gottschling on 7/17/2025
 */
public class FlyingGottschMonsterMoveControl extends MoveControl {

    public FlyingGottschMonsterMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            return;
        }

        Vec3 toWanted = new Vec3(wantedX - this.mob.getX(), wantedY - this.mob.getY(), wantedZ - this.mob.getZ()).normalize();
        double speed = this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED);
        this.mob.setDeltaMovement(toWanted.scale(speed));

        // drive the vertical directly so the mob climbs/descends to wantedY
        double dy = this.wantedY - this.mob.getY();
        if (Math.abs(dy) > 0.1D) {
            Vec3 delta = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(delta.x, Math.signum(dy) * speed, delta.z);
        }
    }
}
