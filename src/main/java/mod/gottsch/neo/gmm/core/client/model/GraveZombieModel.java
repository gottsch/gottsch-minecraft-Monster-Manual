package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.entity.monster.zombie.GraveZombie;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Grave Zombie rig: the shared {@link GMMZombieModel} shamble with a phase-driven "digging" pose
 * swapped in for the two transitions. The whole rig — every part, moved together as one rigid unit,
 * not via independent per-part rotations (which read as a broken/twisted pose) — sinks fully below
 * the ground plane while buried and rises back up as it surfaces, with a small uniform positional
 * jitter layered on top for a "struggling to dig out" shudder. Arms additionally blend from
 * thrust-straight-up to the normal shamble pose as it rises (and the reverse on the way back down).
 * {@code riseAmount} (0 = fully buried, 1 = fully risen) is fed in by {@code GraveZombieRenderer} each
 * frame, partial-tick interpolated — the same trick {@code BloodyBonesModel#collapse} uses for its own
 * rise animation.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class GraveZombieModel<T extends GraveZombie> extends GMMZombieModel<T> {

    // how far (model units; 16 = 1 block) the whole rig sinks below its standing position when fully
    // buried. Enough to put the WHOLE rig under the ground plane, which the old 26 was not: the
    // humanoid rig spans model y -8 (top of the head) to 24 (the feet, which sit on the ground
    // plane), so 32 is the exact depth that drops the crown of the head to ground level. The extra 2
    // is margin: the hat is the head cube grown by a 0.5 deformation, and armor layers grow it again.
    //
    // 26 was chosen on the reasoning that a fully buried zombie is invisible anyway, so the sink only
    // had to read as "emerging" partway through the transition. That held right up until the
    // invisibility went missing across a save (fixed in GraveZombie#readAdditionalSaveData) and the
    // remaining ~5 units showed as a zombie head stuck in the floor. Burying the rig properly means
    // that flag is no longer the only thing standing between a dormant zombie and a visible head.
    private static final float SINK_DEPTH = 34.0F;
    // small whole-body jitter (model units, applied as position, not rotation) while transitioning --
    // every part gets the identical offset so the rig stays rigid instead of flexing part-to-part.
    private static final float SHAKE_AMOUNT = 1.1F;

    // "reaching straight up out of the grave" arm pose (xRot 0 = hanging down; more negative swings
    // the arm forward and up, ~-172deg is just short of a full overhead reach).
    private static final float ARMS_UP_XROT = -3.0F;
    private static final float ARMS_UP_ZROT = 0.12F;

    // baked base (x, y, z) of each part, captured once so the sink/shake can be applied as a clean
    // offset each frame rather than accumulating.
    private final float baseHeadX, baseHeadY, baseHeadZ;
    private final float baseBodyX, baseBodyY, baseBodyZ;
    private final float baseRightArmX, baseRightArmY, baseRightArmZ;
    private final float baseLeftArmX, baseLeftArmY, baseLeftArmZ;
    private final float baseRightLegX, baseRightLegY, baseRightLegZ;
    private final float baseLeftLegX, baseLeftLegY, baseLeftLegZ;

    /** 0..1 rise amount, set by the renderer in {@code setupRotations} before {@code setupAnim} runs. */
    public float riseAmount = 1.0F;

    public GraveZombieModel(ModelPart root) {
        super(root);
        this.baseHeadX = this.head.x; this.baseHeadY = this.head.y; this.baseHeadZ = this.head.z;
        this.baseBodyX = this.body.x; this.baseBodyY = this.body.y; this.baseBodyZ = this.body.z;
        this.baseRightArmX = this.rightArm.x; this.baseRightArmY = this.rightArm.y; this.baseRightArmZ = this.rightArm.z;
        this.baseLeftArmX = this.leftArm.x; this.baseLeftArmY = this.leftArm.y; this.baseLeftArmZ = this.leftArm.z;
        this.baseRightLegX = this.rightLeg.x; this.baseRightLegY = this.rightLeg.y; this.baseRightLegZ = this.rightLeg.z;
        this.baseLeftLegX = this.leftLeg.x; this.baseLeftLegY = this.leftLeg.y; this.baseLeftLegZ = this.leftLeg.z;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // normal shamble pose first (from GMMZombieModel) -- the "risen" end of the arm blend below.
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int phase = entity.getPhase();
        boolean transitioning = phase == GraveZombie.PHASE_SURFACING || phase == GraveZombie.PHASE_REBURROWING;

        float sink = SINK_DEPTH * (1.0F - this.riseAmount);
        float shakeX = 0.0F;
        float shakeZ = 0.0F;

        if (transitioning) {
            float rise = this.riseAmount;
            this.rightArm.xRot = Mth.lerp(rise, ARMS_UP_XROT, this.rightArm.xRot);
            this.leftArm.xRot = Mth.lerp(rise, ARMS_UP_XROT, this.leftArm.xRot);
            this.rightArm.zRot = Mth.lerp(rise, -ARMS_UP_ZROT, this.rightArm.zRot);
            this.leftArm.zRot = Mth.lerp(rise, ARMS_UP_ZROT, this.leftArm.zRot);

            // two mismatched, slow-ish frequencies so it doesn't read as a mechanical wobble --
            // applied identically to every part below, so the whole rig moves as one rigid unit
            // rather than flexing at the joints.
            shakeX = Mth.sin(ageInTicks * 5.0F) * SHAKE_AMOUNT;
            shakeZ = Mth.cos(ageInTicks * 7.0F) * SHAKE_AMOUNT;
        }

        this.head.x = baseHeadX + shakeX; this.head.y = baseHeadY + sink; this.head.z = baseHeadZ + shakeZ;
        this.body.x = baseBodyX + shakeX; this.body.y = baseBodyY + sink; this.body.z = baseBodyZ + shakeZ;
        this.rightArm.x = baseRightArmX + shakeX; this.rightArm.y = baseRightArmY + sink; this.rightArm.z = baseRightArmZ + shakeZ;
        this.leftArm.x = baseLeftArmX + shakeX; this.leftArm.y = baseLeftArmY + sink; this.leftArm.z = baseLeftArmZ + shakeZ;
        this.rightLeg.x = baseRightLegX + shakeX; this.rightLeg.y = baseRightLegY + sink; this.rightLeg.z = baseRightLegZ + shakeZ;
        this.leftLeg.x = baseLeftLegX + shakeX; this.leftLeg.y = baseLeftLegY + sink; this.leftLeg.z = baseLeftLegZ + shakeZ;
        this.hat.copyFrom(this.head);
    }
}
