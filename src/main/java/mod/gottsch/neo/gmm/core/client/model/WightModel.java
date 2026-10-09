package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.entity.monster.zombie.Wight;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

/**
 * The Wight rig: the shared {@link GMMZombieModel} zombie rig, but without the shambling-zombie arm
 * pose — a Wight kept its wits (and its fighting stance), so it swings its arms like a normal
 * humanoid combatant (walking swing + attack swing, both already computed by
 * {@link net.minecraft.client.model.HumanoidModel#setupAnim} before {@link #animateArms(Wight, float)}
 * would otherwise override them) rather than lurching with its arms thrust forward.
 *
 * <p>While {@link Wight#isCasting()} (Summon/Enthrall charging up, synced from the goal via
 * {@code ICastingMob}), both arms instead raise into a channeling pose with a gentle waving
 * oscillation, so the cast is visibly telegraphed rather than the Wight just standing still.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class WightModel<T extends Wight> extends GMMZombieModel<T> {

    // Channeling pose: arms raised forward/up (more negative xRot = higher), with a slow opposed
    // wave so it doesn't read as a frozen statue while charging.
    private static final float CAST_XROT = -2.3F;
    private static final float CAST_ZROT = 0.3F;
    private static final float CAST_WAVE_AMOUNT = 0.15F;

    public WightModel(ModelPart root) {
        super(root);
    }

    @Override
    protected void animateArms(T entity, float ageInTicks) {
        if (entity.isCasting()) {
            float wave = Mth.sin(ageInTicks * 0.3F) * CAST_WAVE_AMOUNT;
            this.rightArm.xRot = CAST_XROT + wave;
            this.leftArm.xRot = CAST_XROT - wave;
            this.rightArm.zRot = -CAST_ZROT;
            this.leftArm.zRot = CAST_ZROT;
            return;
        }
        // a raised shield (see RaiseShieldGoal) bends the offhand (left) arm up -- nothing in vanilla
        // does this for a Mob automatically, see GMMAnimationUtils#poseBlockingArm. Runs here (not a
        // setupAnim override) since this method already fires after GMMZombieModel's super.setupAnim
        // call, guaranteeing it wins over whatever the normal walk/attack swing left the arm at.
        if (entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.OFF_HAND) {
            GMMAnimationUtils.poseBlockingArm(this.leftArm, this.head, false);
            return;
        }
        // no-op otherwise: skip the zombie arms-out lurch and keep HumanoidModel's own arm swing/attack pose.
    }
}
