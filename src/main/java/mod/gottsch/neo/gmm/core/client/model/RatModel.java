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
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class RatModel<T extends Entity> extends EntityModel<T> implements IAnimalModel {
	public static final String MODEL_NAME = "rat_model";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

	private final ModelPart rat;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftFrontFoot;
	private final ModelPart leftBackFoot;
	private final ModelPart rightFrontFoot;
	private final ModelPart rightBackFoot;
	private final ModelPart tail;
	private final ModelPart part2;
	private final ModelPart part3;

	public RatModel(ModelPart root) {
		this.rat = root.getChild("rat");
		this.head = this.rat.getChild("head");
		this.body = root.getChild("body");
		this.leftFrontFoot = this.body.getChild("leftFrontFoot");
		this.leftBackFoot = this.body.getChild("leftBackFoot");
		this.rightFrontFoot = this.body.getChild("rightFrontFoot");
		this.rightBackFoot = this.body.getChild("rightBackFoot");
		this.tail = this.body.getChild("tail");
		this.part2 = this.tail.getChild("part2");
		this.part3 = this.part2.getChild("part3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition rat = partdefinition.addOrReplaceChild("rat", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = rat.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -2.75F, -8.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(22, 15).addBox(0.0F, -3.75F, -6.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(22, 15).mirror().addBox(-3.0F, -3.75F, -6.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(22, 12).addBox(-1.5F, -2.25F, -8.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.75F, -7.75F, 0.1745F, 0.0F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -5.0F, 5.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition leftFrontFoot = body.addOrReplaceChild("leftFrontFoot", CubeListBuilder.create(), PartPose.offset(1.0F, -0.5F, -3.0F));
		PartDefinition leftFrontFoot_r1 = leftFrontFoot.addOrReplaceChild("leftFrontFoot_r1", CubeListBuilder.create().texOffs(13, 12).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, -0.1745F, 0.0F));
		PartDefinition leftBackFoot = body.addOrReplaceChild("leftBackFoot", CubeListBuilder.create(), PartPose.offset(1.0F, -0.5F, 3.0F));
		PartDefinition leftBackFoot_r1 = leftBackFoot.addOrReplaceChild("leftBackFoot_r1", CubeListBuilder.create().texOffs(13, 12).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, -0.1745F, 0.0F));
		PartDefinition rightFrontFoot = body.addOrReplaceChild("rightFrontFoot", CubeListBuilder.create(), PartPose.offset(-2.0F, -0.5F, -3.0F));
		PartDefinition rightFrontFoot_r1 = rightFrontFoot.addOrReplaceChild("rightFrontFoot_r1", CubeListBuilder.create().texOffs(13, 12).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.5F, 0.0F, 0.0F, 0.1745F, 0.0F));
		PartDefinition rightBackFoot = body.addOrReplaceChild("rightBackFoot", CubeListBuilder.create(), PartPose.offset(-2.0F, -0.5F, 3.0F));
		PartDefinition rightBackFoot_r1 = rightBackFoot.addOrReplaceChild("rightBackFoot_r1", CubeListBuilder.create().texOffs(13, 12).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.5F, 0.0F, 0.0F, 0.1745F, 0.0F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(13, 17).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -2.0F, 2.0F, -0.0436F, 0.0F, 0.0F));
		PartDefinition part2 = tail.addOrReplaceChild("part2", CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, -0.1309F, 0.0F, 0.0F));
		PartDefinition part3 = part2.addOrReplaceChild("part3", CubeListBuilder.create().texOffs(9, 22).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// normalize the sine wave output from [-1, 1] to [0, 1]
		double normalizedSin = (Math.sin(limbSwing) + 1) / 2.0;

		// head
		swivelHead(netHeadYaw, headPitch, -20, 20, 0, 0);

		// legs
		float radians = 0.25F;
		float walkSpeed = 0.5F; // half speed = 0.5
		this.rightFrontFoot.xRot = Mth.sin(limbSwing * walkSpeed) * radians  * 1.4F * limbSwingAmount;
		this.leftFrontFoot.xRot = Mth.sin(limbSwing  * walkSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount;
		this.rightBackFoot.xRot = this.leftFrontFoot.xRot;
		this.leftBackFoot.xRot = this.rightFrontFoot.xRot;

		// bob tail
		getTail().ifPresent(tail -> tail.yRot = -0.25F * Mth.sin(0.25F * ageInTicks) );
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		rat.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void swivelHead(float netYaw, float pitch, float minYaw, float maxYaw, float minPitch, float maxPitch) {
		getHead().ifPresent(head -> {
			if (netYaw < 0) {
				head.yRot = Math.max(minYaw, netYaw) * ((float) Math.PI / 180F);
			} else {
				head.yRot = Math.min(maxYaw, netYaw) * ((float) Math.PI / 180F);
			}
		});
	}

	@Override
	public Optional<ModelPart> getHead() {
		return Optional.of(this.head);
	}

	@Override
	public Optional<ModelPart> getTail() {
		return Optional.of(this.tail);
	}
}