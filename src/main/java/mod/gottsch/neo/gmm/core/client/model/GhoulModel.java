package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.attribute.Position;
import mod.gottsch.neo.gmm.core.client.model.attribute.Rotation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * 
 * @author Mark Gottschling on Apr 7, 2022
 *
 * @param <T>
 */
public class GhoulModel<T extends Entity> extends HumanlikeModel<T> {
	public static final String MODEL_NAME = "ghoul_model";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

	private final ModelPart torso;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	private Position rightArmPos;
	private Position leftArmPos;
	private Rotation rightArmRot;
	private Rotation leftArmRot;

	/**
	 * 
	 * @param root
	 */
	public GhoulModel(ModelPart root) {
		this.head = root.getChild("head");
		this.torso = root.getChild("torso");		
		this.body = torso.getChild("body");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");

		// part states
		rightArmPos = new Position(rightArm);
		leftArmPos = new Position(leftArm);

		rightArmRot = new Rotation(this.rightArm);
		leftArmRot = new Rotation(this.leftArm);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		
		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, -6.0F, 0.3054F, 0.0F, 0.0F));
		PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.3054F, 0.0F, 0.0F));
		PartDefinition lower_body = torso.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(21, 24).addBox(-3.0F, -2.5F, -2.0F, 6.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, -1.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, 0.5F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.5F, 1.0F));
		PartDefinition body = torso.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, -3.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, -1.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, 1.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.5F, 3.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3491F, 0.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(13, 34).addBox(-1.5F, -1.75F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(26, 34).addBox(-1.0F, 3.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(5.5F, 4.75F, -0.5F, -0.5672F, 0.0F, 0.0F));
		PartDefinition left_lower_arm = left_arm.addOrReplaceChild("left_lower_arm", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, 1.25F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.25F, 0.0F, -0.8727F, 0.0F, 0.0F));
		PartDefinition left_hand = left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create(), PartPose.offset(-5.5F, 22.25F, 0.5F));
		PartDefinition left_top_hand = left_hand.addOrReplaceChild("left_top_hand", CubeListBuilder.create().texOffs(35, 18).addBox(5.9F, -14.75F, -8.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 3).addBox(5.9F, -14.0F, -8.75F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(35, 18).addBox(4.1F, -14.75F, -8.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 3).addBox(4.1F, -14.0F, -8.75F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition left_thumb_r1 = left_top_hand.addOrReplaceChild("left_thumb_r1", CubeListBuilder.create().texOffs(35, 8).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -12.35F, -5.25F, 0.3491F, 0.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(13, 34).addBox(-1.5F, -1.75F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(26, 34).addBox(-1.0F, 3.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-5.5F, 4.75F, -0.5F, -0.7418F, 0.0F, 0.0F));
		PartDefinition right_lower_arm = right_arm.addOrReplaceChild("right_lower_arm", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, 1.25F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.25F, 0.0F, -0.8727F, 0.0F, 0.0F));
		PartDefinition right_hand = right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.offset(-5.5F, 22.25F, 0.5F));
		PartDefinition right_top_hand = right_hand.addOrReplaceChild("right_top_hand", CubeListBuilder.create().texOffs(35, 18).addBox(5.9F, -14.75F, -8.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 3).addBox(5.9F, -14.0F, -8.75F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(35, 18).addBox(4.1F, -14.75F, -8.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 3).addBox(4.1F, -14.0F, -8.75F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition right_thumb_r1 = right_top_hand.addOrReplaceChild("right_thumb_r1", CubeListBuilder.create().texOffs(35, 8).addBox(2.9F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -12.35F, -5.25F, 0.3491F, 0.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(35, 34).addBox(-1.0F, 3.8285F, -5.0601F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F)), PartPose.offset(2.25F, 12.5215F, 2.8601F));
		PartDefinition left_lower_leg_r1 = left_leg.addOrReplaceChild("left_lower_leg_r1", CubeListBuilder.create().texOffs(25, 11).addBox(-1.75F, -5.0F, -4.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 8.4785F, 0.6399F, 0.48F, 0.0F, 0.0F));
		PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(25, 0).addBox(-1.75F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.25F, -0.5215F, 0.6399F, -0.7854F, 0.0F, 0.0F));
		PartDefinition left_toes = left_leg.addOrReplaceChild("left_toes", CubeListBuilder.create().texOffs(13, 28).addBox(-1.35F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(13, 28).addBox(0.35F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.9785F, -2.6101F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(35, 34).addBox(-1.5F, 3.8285F, -5.0601F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F)), PartPose.offset(-1.75F, 12.5215F, 2.8601F));
		PartDefinition right_lower_leg_r1 = right_leg.addOrReplaceChild("right_lower_leg_r1", CubeListBuilder.create().texOffs(25, 11).addBox(-2.25F, -5.0F, -4.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 8.4785F, 0.6399F, 0.48F, 0.0F, 0.0F));
		PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(25, 0).addBox(-2.25F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.25F, -0.5215F, 0.6399F, -0.7854F, 0.0F, 0.0F));
		PartDefinition right_toes = right_leg.addOrReplaceChild("right_toes", CubeListBuilder.create().texOffs(13, 28).addBox(-1.85F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(13, 28).addBox(-0.15F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.9785F, -2.6101F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// head
		swivelHead(netHeadYaw, headPitch, ageInTicks);

		// legs
		swingLegs(limbSwing, limbSwingAmount, ageInTicks, 0.5F, 0.6F);

		// arms
		// 0.5235988F = 30 degrees
		swingArms(limbSwing, limbSwingAmount, ageInTicks, 0.25F, 0.5235988F);

		setupAttackAnimation(entity, ageInTicks);

		// bob the arms
		bobArm(this.rightArm, ageInTicks, 1.0F);
		bobArm(this.leftArm, ageInTicks, -1.0F);
	}

	@Override
	public void swivelHead(float netHeadYaw, float headPitch, float ageInTicks) {
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F) + 0.3054326F; // +20 degrees because has a slight down angle
	}

	@Override
	public void swingLegs(float limbSwing, float limbSwingAmount, float age, float walkSpeed, float maxAngle) {
		// 0.610865 = 35 degrees. sets the legs under the ghoul properly.
		this.rightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * maxAngle  * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
		this.leftLeg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
	}

	@Override
	public void swingArms(float limbSwing, float limbSwingAmount, float age, float speed, float maxAngle)	{
		this.rightArm.xRot = -0.7417649F + Mth.cos(limbSwing * speed) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
		this.leftArm.xRot = -0.567232F + Mth.cos(limbSwing * speed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
	}
	/**
	 * 
	 * @param part
	 * @param age
	 * @param direction
	 */
	public static void bobArm(ModelPart part, float age, float direction) {
		part.zRot += direction * (Mth.cos(age * 0.15F) * 0.05F + 0.05F);
	}
	
	@Override
	public void resetSwing(T entity) {
		body.yRot = 0;
		rightArm.x = rightArmPos.x();
		rightArm.xRot = rightArmRot.x();
		rightArm.zRot = 0;
		rightArm.yRot = 0;
		leftArm.x = leftArmPos.x();
		leftArm.xRot = leftArmRot.x();
		leftArm.yRot = 0;
		leftArm.zRot = 0;
	}
	
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		head.render(poseStack, buffer, packedLight, packedOverlay);
		torso.render(poseStack, buffer, packedLight, packedOverlay);
		leftArm.render(poseStack, buffer, packedLight, packedOverlay);
		rightArm.render(poseStack, buffer, packedLight, packedOverlay);
		leftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		rightLeg.render(poseStack, buffer, packedLight, packedOverlay);
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
	public Optional<ModelPart> getHead() {
		return Optional.of(this.head);
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