package mod.gottsch.neo.gmm.core.client.model;// Made with Blockbench 5.0.3

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
import net.minecraft.world.entity.Entity;

public class AlligatorGarModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "alligator_gar"), "main");

	private final ModelPart bodyFront;
	private final ModelPart bodyBack;
	private final ModelPart dorsalBack;
	private final ModelPart tailFin;
	private final ModelPart dorsalFront;
	private final ModelPart head;
	private final ModelPart snoutBottom;
	private final ModelPart leftFin;
	private final ModelPart rightFin;

	public AlligatorGarModel(ModelPart root) {
		this.bodyFront = root.getChild("body_front");
		this.bodyBack = this.bodyFront.getChild("body_back");
		this.dorsalBack = this.bodyBack.getChild("dorsal_back");
		this.tailFin = this.bodyBack.getChild("tailfin");
		this.dorsalFront = this.bodyFront.getChild("dorsal_front");
		this.head = this.bodyFront.getChild("head");
		this.snoutBottom = this.head.getChild("snoutBottom");
		this.leftFin = this.bodyFront.getChild("leftFin");
		this.rightFin = this.bodyFront.getChild("rightFin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body_front = partdefinition.addOrReplaceChild("body_front", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -8.5F, 0.0F, 3.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, -4.0F));
		PartDefinition body_back = body_front.addOrReplaceChild("body_back", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5F, -8.5F, 0.0F, 3.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 8.0F));
		PartDefinition dorsal_back = body_back.addOrReplaceChild("dorsal_back", CubeListBuilder.create().texOffs(2, 3).addBox(0.0F, -5.5F, 6.0F, 0.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, 0.0F));
		PartDefinition tailfin = body_back.addOrReplaceChild("tailfin", CubeListBuilder.create().texOffs(20, 10).addBox(0.0F, -8.5F, 0.0F, 0.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 8.0F));
		PartDefinition dorsal_front = body_front.addOrReplaceChild("dorsal_front", CubeListBuilder.create().texOffs(4, 2).addBox(0.0F, -5.5F, 6.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, 6.0F));
		PartDefinition head = body_front.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 27).addBox(-0.9F, -3.6F, -10.25F, 2.0F, 2.0F, 7.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, -3.0F, 0.0F));
		PartDefinition rightTooth3_r1 = head.addOrReplaceChild("rightTooth3_r1", CubeListBuilder.create().texOffs(22, 28).addBox(0.05F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(22, 28).addBox(1.65F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8F, -1.95F, -8.5F, -0.7854F, 0.0F, 0.0F));
		PartDefinition rightTooth1_r1 = head.addOrReplaceChild("rightTooth1_r1", CubeListBuilder.create().texOffs(22, 28).mirror().addBox(0.15F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(22, 28).addBox(1.75F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.9F, -1.95F, -6.0F, -0.7854F, 0.0F, 0.0F));
		PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(23, 3).addBox(-2.0F, -2.5F, -3.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.5F, -2.0F, -1.0F, 0.2182F, 0.0F, 0.0F));
		PartDefinition snoutBottom = head.addOrReplaceChild("snoutBottom", CubeListBuilder.create().texOffs(0, 39).addBox(-1.0F, -1.0F, -7.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.1F, -0.9F, -3.25F, 0.1309F, 0.0F, 0.0F));
		PartDefinition leftFin = body_front.addOrReplaceChild("leftFin", CubeListBuilder.create().texOffs(18, 23).addBox(0.0F, -1.0F, 0.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, -4.0F, 0.0F, -0.3491F, 0.3491F, 0.0F));
		PartDefinition rightFin = body_front.addOrReplaceChild("rightFin", CubeListBuilder.create().texOffs(18, 23).addBox(0.0F, -1.0F, 0.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -4.0F, 0.0F, -0.3491F, -0.3491F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		float rotationMultiplier = 1.0F;
		float speedMultiplier = 1.0F;
		if (!entity.isInWater()) {
			rotationMultiplier = 1.3F;
			speedMultiplier = 1.7F;
		}

		this.bodyBack.yRot = -rotationMultiplier * 0.25F * Mth.sin(speedMultiplier * 0.6F * ageInTicks);

		// reset mouth
		this.snoutBottom.xRot = 0.1309F;

		// bob mouth
		bobMouthPart(this.snoutBottom, ageInTicks, 0.15F, 0.174533F);
	}

	public void bobMouthPart(ModelPart mouth, float age, float speed, float radians) {
		mouth.xRot -= Mth.cos(age * speed) * radians + 0.05F;
	}
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bodyFront.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}