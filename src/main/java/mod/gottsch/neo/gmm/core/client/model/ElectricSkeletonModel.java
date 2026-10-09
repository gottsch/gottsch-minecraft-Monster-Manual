package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Electric Skeleton model (authored in Blockbench) — a bent/jointed-arm skeleton rig, each arm
 * split into an upper and a lower segment so the elbow can fold, with a dedicated
 * {@code leftArmElectricity}/{@code rightArmElectricity} child cuff on each forearm that spins
 * independently to sell a crackle of current wrapped around the hand. Single texture, single render
 * pass, same idiom as {@link SkeletonChampionModel}.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class ElectricSkeletonModel<T extends Mob> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "electric_skeleton_model"), "main");

    private final ModelPart main;
    private final ModelPart left_leg;
    private final ModelPart right_leg;
    private final ModelPart left_arm;
    private final ModelPart lowerLeftArm;
    private final ModelPart leftArmElectricity;
    private final ModelPart right_arm;
    private final ModelPart lowerRightArm;
    private final ModelPart rightArmElectricity;
    private final ModelPart body;
    private final ModelPart head;

    public ElectricSkeletonModel(ModelPart root) {
        this.main = root.getChild("main");
        this.left_leg = this.main.getChild("left_leg");
        this.right_leg = this.main.getChild("right_leg");
        this.left_arm = this.main.getChild("left_arm");
        this.lowerLeftArm = this.left_arm.getChild("lowerLeftArm");
        this.leftArmElectricity = this.lowerLeftArm.getChild("leftArmElectricity");
        this.right_arm = this.main.getChild("right_arm");
        this.lowerRightArm = this.right_arm.getChild("lowerRightArm");
        this.rightArmElectricity = this.lowerRightArm.getChild("rightArmElectricity");
        this.body = this.main.getChild("body");
        this.head = this.main.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition left_leg = main.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(2.0F, -12.0F, 0.0F));

        PartDefinition right_leg = main.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -12.0F, 0.0F));

        PartDefinition left_arm = main.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(5.0F, -22.0F, 0.0F, 0.0F, 0.0F, -0.0873F));

        PartDefinition lowerLeftArm = left_arm.addOrReplaceChild("lowerLeftArm", CubeListBuilder.create().texOffs(40, 22).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.5672F, 0.0F, 0.0F));

        lowerLeftArm.addOrReplaceChild("leftArmElectricity", CubeListBuilder.create().texOffs(49, 22).mirror().addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offset(0.0F, 3.0F, 0.0F));

        PartDefinition right_arm = main.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -22.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

        PartDefinition lowerRightArm = right_arm.addOrReplaceChild("lowerRightArm", CubeListBuilder.create().texOffs(40, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.5672F, 0.0F, 0.0F));

        lowerRightArm.addOrReplaceChild("rightArmElectricity", CubeListBuilder.create().texOffs(49, 22).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 3.0F, 0.0F));

        main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

        main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    // continuous spin for the electricity cuffs, independent of the arm's own swing/attack pose
    // (degrees/tick — 25 read as a blur, 4.5 read as too slow/sluggish; a full turn every ~1.6s
    // is the sweet spot between "spinning" and "crackling")
    private static final float ELECTRICITY_SPIN_SPEED = 9.0F;

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;

        this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        setupAttackAnimation();

        // the cuff spins in place around the forearm regardless of what the arm itself is doing
        this.leftArmElectricity.yRot = ageInTicks * (ELECTRICITY_SPIN_SPEED * ((float) Math.PI / 180F));
        this.rightArmElectricity.yRot = -ageInTicks * (ELECTRICITY_SPIN_SPEED * ((float) Math.PI / 180F));
    }

    /** Rotation-only bite/swipe, both arms (no weapon — this skeleton fights bare-handed). */
    private static final float ATTACK_JAB = 1.1F;

    private void setupAttackAnimation() {
        if (this.attackTime <= 0.0F) {
            return;
        }
        float jab = Mth.sin(this.attackTime * (float) Math.PI) * ATTACK_JAB;
        this.right_arm.xRot -= jab;
        this.left_arm.xRot -= jab;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
