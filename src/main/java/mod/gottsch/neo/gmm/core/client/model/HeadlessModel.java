package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * @author Mark Gottschling on Apr 6, 2022
 * @param <T>
 */
public class HeadlessModel<T extends Entity> extends HumanlikeModel<T> {
	public static final String MODEL_NAME = "headless_model";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart body;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart loinCloth;
	private final ModelPart frontCloth;
	private final ModelPart backCloth;

	private final ModelPart rightLowerArm;
	private final ModelPart leftLowerArm;

	/*
	 * created solely to fulfill the contract of IHumanlikeModel
	 */
	private final ModelPart head;

	private float leftArmX;
	private float rightArmX;

	private ModelPart attackArm;
	private boolean changeAttackArm;
	private long changeAttackArmTime;

	public HeadlessModel(ModelPart root) {
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
		this.body = root.getChild("body");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.loinCloth = root.getChild("loin_cloth");
		this.frontCloth = loinCloth.getChild("front_cloth");
		this.backCloth = loinCloth.getChild("back_cloth");
		this.leftLowerArm = leftArm.getChild("left_lower_arm");
		this.rightLowerArm = rightArm.getChild("right_lower_arm");

		head = root.getChild("head");
		head.visible = false;

		rightArmX = rightArm.x;
		leftArmX = leftArm.x;

		attackArm = this.leftArm;
		changeAttackArm = false;
		changeAttackArmTime = 0;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 39).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 12.0F, 0.0F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(34, 9).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 12.0F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -24.0F, -3.0F, 12.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(29, 31).addBox(-4.0F, -18.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(34, 42).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.8727F));
		PartDefinition left_lower_arm = left_arm.addOrReplaceChild("left_lower_arm", CubeListBuilder.create().texOffs(17, 18).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(17, 42).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.8727F));
		PartDefinition right_lower_arm = right_arm.addOrReplaceChild("right_lower_arm", CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition loin_cloth = partdefinition.addOrReplaceChild("loin_cloth", CubeListBuilder.create().texOffs(0, 31).addBox(-4.5F, -14.0F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition front_cloth = loin_cloth.addOrReplaceChild("front_cloth", CubeListBuilder.create().texOffs(44, 0).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition back_cloth = loin_cloth.addOrReplaceChild("back_cloth", CubeListBuilder.create().texOffs(31, 0).addBox(-2.5F, 0.0F, 1.5F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().addBox(0F, 0F, 0F, 0F, 0F, 0F, new CubeDeformation(0.0F)), PartPose.offset(0F, 0F, 0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// legs
		float f = 1.0F;
		float radians = 0.6662F;
		float walkSpeed = 1.0F; // half speed = 0.5
		this.rightLeg.xRot = Mth.cos(limbSwing * radians * walkSpeed) * 1.4F * limbSwingAmount / f;
		this.leftLeg.xRot = Mth.cos(limbSwing * radians  * walkSpeed + (float)Math.PI) * 1.4F * limbSwingAmount / f;
		this.rightLeg.yRot = 0.0F;
		this.leftLeg.yRot = 0.0F;
		this.rightLeg.zRot = 0.0F;
		this.leftLeg.zRot = 0.0F;

		// loin cloth
		this.backCloth.xRot = Math.max(Math.max(0,  this.rightLeg.xRot), this.leftLeg.xRot);
		this.frontCloth.xRot = -this.backCloth.xRot;

		// fore-arms
		// 0.5235988F = 30 degrees
		float armSpeed = 0.25F;
		radians = 0.5235988F;
		this.rightLowerArm.xRot = Mth.cos(limbSwing * armSpeed) * radians * 1.4F * limbSwingAmount;
		this.leftLowerArm.xRot = Mth.cos(limbSwing * armSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount;

		// reset arm rotations before bobbing, because bobbing is an addition to current rotation
		this.leftArm.xRot = 0F;
		this.leftArm.zRot = -0.8726646F;

		this.rightArm.xRot = 0F;
		this.rightArm.zRot = 0.8726646F;

		setupAttackAnimation(entity, ageInTicks);

		// bob the arms
		bobModelPart(this.rightArm, ageInTicks, 1.0F);
		bobModelPart(this.leftArm, ageInTicks, -1.0F);
	}

	public static void bobModelPart(ModelPart part, float age, float direction) {
		part.zRot += direction * (Mth.cos(age * /*0.09F*/ 0.15F) * 0.05F + 0.05F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		leftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		rightLeg.render(poseStack, buffer, packedLight, packedOverlay);
		body.render(poseStack, buffer, packedLight, packedOverlay);
		leftArm.render(poseStack, buffer, packedLight, packedOverlay);
		rightArm.render(poseStack, buffer, packedLight, packedOverlay);
		loinCloth.render(poseStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public void resetSwing(T entity) {
		this.body.yRot = 0;
		this.rightArm.x = rightArmX;
		this.rightArm.zRot = 0.8726646F;
		this.rightArm.yRot = 0;
		this.leftArm.x = leftArmX;
		this.leftArm.yRot = 0;
		this.leftArm.zRot = -this.rightArm.zRot;
		if (this.attackTime > 0) {
			if (changeAttackArm && entity.level().getGameTime() - changeAttackArmTime > 20) {
				attackArm = (attackArm == this.rightArm) ? this.leftArm : this.rightArm;
				changeAttackArm = false;
			}
		}
		else {
			if (!changeAttackArm) {
				changeAttackArm = true;
				changeAttackArmTime = entity.level().getGameTime();
			}
		}
	}

	@Override
	public Optional<ModelPart> getAttackArm() {
		return Optional.of(attackArm);
	}

	@Override
	public Optional<ModelPart> getHead() {
		return Optional.of(head);
	}

	@Override
	public Optional<ModelPart> getBody() {
		return Optional.of(body);
	}

	@Override
	public Optional<ModelPart> getRightArm() {
		return Optional.of(rightArm);
	}

	@Override
	public Optional<ModelPart> getLeftArm() {
		return Optional.of(leftArm);
	}

	@Override
	public Optional<ModelPart> getRightLeg() {
		return Optional.of(this.rightLeg);
	}

	@Override
	public Optional<ModelPart> getLeftLeg() {
		return Optional.of(this.leftLeg);
	}
}
