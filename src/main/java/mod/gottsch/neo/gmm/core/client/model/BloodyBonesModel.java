package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BloodyBones;
import net.minecraft.client.model.geom.ModelPart;

/**
 * The Bloody Bones rig: the shared {@link SkeletonWarriorModel} (reuses its baked layer) with a
 * phase-driven "come apart" trick. When it collapses, the arms/legs/body are <b>hidden</b> (the entity
 * flings them off as bone shrapnel) and only the <b>skull</b> is left, which drops to the ground; on
 * resurrect the whole skeleton is made visible again and its y-position is raised back up from the
 * ground to standing (no scaling). {@code collapse} (0 = standing, 1 = fully down) is fed in by
 * {@code BloodyBonesRenderer} each frame.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class BloodyBonesModel<T extends BloodyBones> extends SkeletonWarriorModel<T> {

    // how far (model units) parts sink toward the ground: the lone skull's drop while collapsing, and
    // the whole skeleton's starting depth while it rises back up (kept equal so it rises from exactly
    // where the skull lay).
    private static final float DROP = 22.0F;

    // baked base Y of each part, captured so we can offset for the drop/rise and restore cleanly.
    private final float baseHeadY;
    private final float baseBodyY;
    private final float baseArmY;
    private final float baseLegY;

    /** 0..1 collapse amount, set by the renderer in {@code setupRotations} before {@code setupAnim} runs. */
    public float collapse;

    public BloodyBonesModel(ModelPart root) {
        super(root);
        this.baseHeadY = this.head.y;
        this.baseBodyY = this.body.y;
        this.baseArmY = this.rightArm.y;
        this.baseLegY = this.rightLeg.y;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // standing pose first (walk / attack / head-look)
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int phase = entity.getPhase();
        // "skull only" while collapsing (limbs have flown off) and while downed on the ground.
        boolean skullOnly = phase == BloodyBones.PHASE_COLLAPSING || phase == BloodyBones.PHASE_DOWNED;

        this.body.visible = !skullOnly;
        this.rightArm.visible = !skullOnly;
        this.leftArm.visible = !skullOnly;
        this.rightLeg.visible = !skullOnly;
        this.leftLeg.visible = !skullOnly;
        this.head.visible = true;
        this.hat.visible = true;

        if (skullOnly) {
            // only the skull remains; drop it from standing height to the ground (collapse 0..1),
            // resting it neutrally (no head-look rotation). Limbs stay at base (hidden).
            this.head.y = baseHeadY + DROP * this.collapse;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.zRot = 0.0F;
            restoreLimbY();
        } else if (phase == BloodyBones.PHASE_RISING) {
            // whole skeleton is back; raise it up from the ground to standing (collapse runs 1 -> 0,
            // so the sink shrinks from DROP to 0). No scaling.
            float sink = DROP * this.collapse;
            this.head.y = baseHeadY + sink;
            this.body.y = baseBodyY + sink;
            this.rightArm.y = baseArmY + sink;
            this.leftArm.y = baseArmY + sink;
            this.rightLeg.y = baseLegY + sink;
            this.leftLeg.y = baseLegY + sink;
        } else {
            // ALIVE: everything at its base position.
            this.head.y = baseHeadY;
            restoreLimbY();
        }
        this.hat.copyFrom(this.head);
    }

    private void restoreLimbY() {
        this.body.y = baseBodyY;
        this.rightArm.y = baseArmY;
        this.leftArm.y = baseArmY;
        this.rightLeg.y = baseLegY;
        this.leftLeg.y = baseLegY;
    }
}
