package mod.gottsch.neo.gmm.core.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Small reusable animation helpers that don't have a natural home on any single model class.
 *
 * @author Mark Gottschling
 */
public class GMMAnimationUtils {

    /**
     * Bends an arm up into a shield-blocking stance, following the target's head look direction --
     * the exact formula vanilla's own {@code HumanoidModel#poseBlockingArm} uses for a real player.
     * That method is private, and {@code HumanoidModel.ArmPose.BLOCK} (the field it's gated on) is
     * only ever assigned by {@code PlayerRenderer} -- nothing in vanilla sets it for a generic
     * {@code Mob}, so an offhand-shielded mob using {@code RaiseShieldGoal} would otherwise keep
     * whatever arm rotation its normal walk/attack animation left it in, and the shield (whose own
     * model geometry assumes the blocking pose) renders at the wrong angle. GMM's own model base
     * classes don't route through vanilla's private {@code ArmPose} dispatch uniformly enough to rely
     * on setting that field instead (some fully override {@code setupAnim} without calling
     * {@code super}), so this is called directly by any model whose entity might be blocking.
     */
    public static void poseBlockingArm(ModelPart arm, ModelPart head, boolean isRightArm) {
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yRot = (isRightArm ? -30.0F : 30.0F) * ((float) Math.PI / 180.0F) + Mth.clamp(head.yRot, (float) (-Math.PI / 6.0), (float) (Math.PI / 6.0));
    }
}
