package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.attribute.Rotation;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;

/**
 * @author Mark Gottschling on Jan 19, 2024
 */
public class SkeletonWarriorModel<T extends Mob> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "skeleton_warrior_model"), "main");

    protected Rotation rightArmRots;
    protected Rotation leftArmRots;

    public SkeletonWarriorModel(ModelPart root) {
        super(root);
        rightArmRots = new Rotation(this.rightArm);
        leftArmRots = new Rotation(this.leftArm);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // head
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);

        // arms
        // 0.5235988F = 30 degrees
        float armSpeed = 0.25F;
        float radians = 0.5235988F; // 30
        this.rightArm.xRot = -0.7417649F + Mth.cos(limbSwing * armSpeed) * radians * 1.4F * limbSwingAmount;
        this.leftArm.xRot = -0.567232F + Mth.cos(limbSwing * armSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount;

        // legs
        radians = 0.6F;
        float walkSpeed = 0.5F; // half speed = 0.5
        this.rightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * radians  * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount;

        // reset arm rotations before bobbing
        resetRotations(rightArm, rightArmRots);
        resetRotations(leftArm, leftArmRots);

        AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);

        setupAttackAnimation(entity, ageInTicks);

        // a raised shield (see RaiseShieldGoal) bends the offhand (left) arm up -- nothing in vanilla
        // does this for a Mob automatically, see GMMAnimationUtils#poseBlockingArm. This class fully
        // overrides setupAnim without calling super, so HumanoidModel's own private ArmPose dispatch
        // never runs -- can't just set leftArmPose here, it would be ignored.
        if (entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.OFF_HAND) {
            GMMAnimationUtils.poseBlockingArm(this.leftArm, this.head, false);
        }
    }

    public void resetRotations(ModelPart part, Rotation rotations) {
        part.yRot = rotations.y();
        part.zRot = rotations.z();
    }

    /**
     * Need to override this as the skeleton's arm is thinner than a normal humanoid,
     * else the held item won't be in the correct position.
     */
    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        float side = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        ModelPart modelpart = this.getArm(arm);
        modelpart.x += side;
        modelpart.translateAndRotate(poseStack);
        modelpart.x -= side;
    }
}
