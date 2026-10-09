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
 * @author by Mark Gottschling on 11/6/2025
 */
public class SewerGhoulModel<T extends Entity> extends HumanlikeModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "sewer_ghoul"), "main");

	private final ModelPart ghoul;
	private final ModelPart body;
	private final ModelPart torso;
	private final ModelPart upperTorso;
	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart jawZAxis;
	private final ModelPart leftArm;
	private final ModelPart left_lower_arm;
	private final ModelPart left_hand;
	private final ModelPart left_top_hand;
	private final ModelPart finger1;
	private final ModelPart finger2;
	private final ModelPart rightArm;
	private final ModelPart rightLowerArm;
	private final ModelPart rightHand;
	private final ModelPart rightTopHand;
	private final ModelPart rightFinger1;
	private final ModelPart rightFinger2;
	private final ModelPart stomach;
	private final ModelPart pelvis;
	private final ModelPart leftLeg;
	private final ModelPart leftLowerLeg;
	private final ModelPart leftToes;
	private final ModelPart rightLeg;
	private final ModelPart rightLowerLeg;
	private final ModelPart rightToes;

	private Position chestPos;
	private Position rightArmPos;
	private Position leftArmPos;

	private Rotation leftArmRot;
	private Rotation rightArmRot;


	public SewerGhoulModel(ModelPart root) {
        this.ghoul = root.getChild("ghoul");
		this.body = this.ghoul.getChild("body");
		this.torso = this.body.getChild("torso");
		this.upperTorso = this.torso.getChild("upperTorso");
		this.chest = this.upperTorso.getChild("chest");
		this.head = this.chest.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.jawZAxis = this.jaw.getChild("jawZAxis");
		this.leftArm = this.chest.getChild("leftArm");
		this.left_lower_arm = this.leftArm.getChild("left_lower_arm");
		this.left_hand = this.left_lower_arm.getChild("left_hand");
		this.left_top_hand = this.left_hand.getChild("left_top_hand");
		this.finger1 = this.left_top_hand.getChild("finger1");
		this.finger2 = this.left_top_hand.getChild("finger2");
		this.rightArm = this.chest.getChild("rightArm");
		this.rightLowerArm = this.rightArm.getChild("rightLowerArm");
		this.rightHand = this.rightLowerArm.getChild("rightHand");
		this.rightTopHand = this.rightHand.getChild("rightTopHand");
		this.rightFinger1 = this.rightTopHand.getChild("rightFinger1");
		this.rightFinger2 = this.rightTopHand.getChild("rightFinger2");
		this.stomach = this.upperTorso.getChild("stomach");
		this.pelvis = this.torso.getChild("pelvis");
		this.leftLeg = this.ghoul.getChild("leftLeg");
		this.leftLowerLeg = this.leftLeg.getChild("leftLowerLeg");
		this.leftToes = this.leftLowerLeg.getChild("leftToes");
		this.rightLeg = this.ghoul.getChild("rightLeg");
		this.rightLowerLeg = this.rightLeg.getChild("rightLowerLeg");
		this.rightToes = this.rightLowerLeg.getChild("rightToes");

		// part states
		chestPos = new Position(chest);
		rightArmPos = new Position(rightArm);
		leftArmPos = new Position(leftArm);

		rightArmRot = new Rotation(this.rightArm);
		leftArmRot = new Rotation(this.leftArm);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition ghoul = partdefinition.addOrReplaceChild("ghoul", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, -4.0F));
		PartDefinition body = ghoul.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 12.0F, 5.0F, 0.3054F, 0.0F, 0.0F));
		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, -1.0F));
		PartDefinition upperTorso = torso.addOrReplaceChild("upperTorso", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 4.0F, 1.0F, 0.1745F, 0.0F, 0.0F));
		PartDefinition chest = upperTorso.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, -4.0F, -2.25F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(25, 0).addBox(-3.5F, -4.0F, -2.25F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(19, 28).addBox(2.5F, -4.0F, -2.25F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(25, 9).addBox(-0.5F, -3.0F, 1.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(25, 9).addBox(-0.5F, -1.0F, 1.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.25F, 0.1745F, 0.0F, 0.0F));

		PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -5.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -2.25F, -0.3491F, 0.0F, 0.0F));
		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(30, 28).addBox(-0.2F, 0.0F, -1.5F, 0.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9F, -6.0F, -0.5F, 0.0F, 0.0F, 0.0873F));
		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(9, 30).addBox(0.0F, 0.0F, -1.5F, 0.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.1F, -6.0F, -0.5F, 0.0F, 0.0F, -0.0873F));
		PartDefinition hairBack2_r1 = head.addOrReplaceChild("hairBack2_r1", CubeListBuilder.create().texOffs(37, 27).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.5F, 2.0F, 1.5708F, 0.0F, 0.0F));
		PartDefinition hairBack1_r1 = head.addOrReplaceChild("hairBack1_r1", CubeListBuilder.create().texOffs(16, 37).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.0F, 1.0F, 0.2618F, 0.0F, 0.0F));
		PartDefinition frontTooth1_r1 = head.addOrReplaceChild("frontTooth1_r1", CubeListBuilder.create().texOffs(21, 18).addBox(9.1924F, 9.1924F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -14.75F, -4.75F, 0.0F, 0.0F, 0.7854F));
		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.25F, -2.0F, 0.3491F, 0.0F, 0.0F));
		PartDefinition jawZAxis = jaw.addOrReplaceChild("jawZAxis", CubeListBuilder.create().texOffs(21, 12).addBox(-3.9848F, -0.0986F, -3.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -0.25F, 0.0F, 0.0F, 0.0F, -0.0873F));
		PartDefinition tooth3_r1 = jawZAxis.addOrReplaceChild("tooth3_r1", CubeListBuilder.create().texOffs(21, 18).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, 0.2F, -1.8F, 0.0F, 0.0F, 0.7854F));
		PartDefinition tooth2_r1 = jawZAxis.addOrReplaceChild("tooth2_r1", CubeListBuilder.create().texOffs(21, 18).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, -0.05F, -1.8F, 0.0F, 0.0F, 0.7854F));
		PartDefinition leftArm = chest.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(32, 18).addBox(-1.0F, -1.0434F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, -3.0F, -0.25F, -1.1781F, -0.3054F, 0.0F));
		PartDefinition left_lower_arm = leftArm.addOrReplaceChild("left_lower_arm", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 1.2066F, -1.1F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(37, 30).addBox(-0.5F, -1.0434F, -1.1F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(-0.5F, 5.0F, 0.1F, -0.9163F, 0.0F, 0.0F));
		PartDefinition left_hand = left_lower_arm.addOrReplaceChild("left_hand", CubeListBuilder.create(), PartPose.offset(-5.5F, 17.9566F, 0.995F));
		PartDefinition left_top_hand = left_hand.addOrReplaceChild("left_top_hand", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition cube_r3 = left_top_hand.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(29, 40).addBox(-0.4F, -0.3566F, -0.495F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(5.3F, -11.2F, -0.6F, 0.0F, 0.0F, 0.5236F));
		PartDefinition finger1 = left_top_hand.addOrReplaceChild("finger1", CubeListBuilder.create().texOffs(34, 40).addBox(-0.35F, -0.5F, -0.375F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F))
				.texOffs(30, 9).addBox(-0.35F, 1.5F, 0.375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(5.35F, -11.2066F, -1.72F, -0.48F, 0.0F, 0.0873F));
		PartDefinition finger2 = left_top_hand.addOrReplaceChild("finger2", CubeListBuilder.create().texOffs(34, 40).addBox(-0.35F, -0.5F, -0.375F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F))
				.texOffs(30, 9).addBox(-0.35F, 1.5F, 0.375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.35F, -11.2066F, -1.72F, -0.3491F, 0.0F, -0.0873F));
		PartDefinition rightArm = chest.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(32, 18).mirror().addBox(-1.0F, -1.0434F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.5F, -3.0F, -0.25F, -1.1781F, 0.3054F, 0.0F));
		PartDefinition rightLowerArm = rightArm.addOrReplaceChild("rightLowerArm", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(-1.0F, 1.25F, -1.095F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(37, 30).mirror().addBox(-1.0F, -1.0566F, -1.095F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F)).mirror(false), PartPose.offsetAndRotation(0.0F, 4.9566F, 0.095F, -0.9163F, 0.0F, 0.0F));
		PartDefinition rightHand = rightLowerArm.addOrReplaceChild("rightHand", CubeListBuilder.create(), PartPose.offset(-6.0F, 18.0F, 1.0F));
		PartDefinition rightTopHand = rightHand.addOrReplaceChild("rightTopHand", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition right_arm_r1 = rightTopHand.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(29, 40).addBox(-0.4F, -0.3566F, -0.495F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.3F, -11.2F, -0.6F, 0.0F, 0.0F, -0.5236F));
		PartDefinition rightFinger1 = rightTopHand.addOrReplaceChild("rightFinger1", CubeListBuilder.create().texOffs(34, 40).addBox(-0.35F, -0.5F, -0.375F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F))
				.texOffs(30, 9).addBox(-0.35F, 1.5F, 0.375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(5.35F, -11.2066F, -1.72F, -0.3491F, 0.0F, 0.0F));
		PartDefinition rightFinger2 = rightTopHand.addOrReplaceChild("rightFinger2", CubeListBuilder.create().texOffs(34, 40).addBox(-0.35F, -0.5F, -0.375F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F))
				.texOffs(30, 9).addBox(-0.35F, 1.5F, 0.375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.35F, -11.2066F, -1.72F, -0.3491F, 0.0F, 0.0F));
		PartDefinition stomach = upperTorso.addOrReplaceChild("stomach", CubeListBuilder.create().texOffs(19, 20).addBox(-2.0F, -4.0F, -1.25F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(25, 9).addBox(-1.0F, -3.5F, 1.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(25, 9).addBox(-1.0F, -1.5F, 1.25F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.5F, -0.25F));
		PartDefinition pelvis = torso.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, -1.5F, -2.0F, 6.0F, 3.0F, 4.0F, new CubeDeformation(-0.2F))
				.texOffs(23, 52).addBox(-3.0F, -1.0F, -2.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 5.5F, 1.0F));
		PartDefinition leftLeg = ghoul.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.1F)), PartPose.offset(2.25F, 12.0F, 5.0F));
		PartDefinition leftLowerLeg = leftLeg.addOrReplaceChild("leftLowerLeg", CubeListBuilder.create().texOffs(37, 30).addBox(-1.0F, -1.15F, -0.95F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F))
				.texOffs(36, 9).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 1.1345F, 0.0F, 0.0F));
		PartDefinition leftToes = leftLowerLeg.addOrReplaceChild("leftToes", CubeListBuilder.create().texOffs(37, 36).addBox(-1.15F, -0.15F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F))
				.texOffs(37, 36).addBox(0.1F, -0.15F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 6.25F, -0.75F, -0.3491F, 0.0F, 0.0F));
		PartDefinition rightLeg = ghoul.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(-2.25F, 12.0F, 5.0F, -0.6109F, 0.0F, 0.0F));
		PartDefinition rightLowerLeg = rightLeg.addOrReplaceChild("rightLowerLeg", CubeListBuilder.create().texOffs(37, 30).mirror().addBox(-1.0F, -1.15F, -0.95F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F)).mirror(false)
				.texOffs(36, 9).mirror().addBox(-1.0F, 1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 1.1345F, 0.0F, 0.0F));
		PartDefinition rightToes = rightLowerLeg.addOrReplaceChild("rightToes", CubeListBuilder.create().texOffs(37, 36).addBox(-1.15F, -0.15F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F))
				.texOffs(37, 36).addBox(0.1F, -0.15F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 6.25F, -0.75F, -0.3491F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

		swivelHead(netHeadYaw, headPitch, ageInTicks);
		swingLegs(limbSwing, limbSwingAmount, ageInTicks, 0.5F, 0.5F);
		swingArms(limbSwing, limbSwingAmount, ageInTicks, 0.25F, 0.5235988F);

		setupAttackAnimation(entity, ageInTicks);

		// reset parts before bobbing

		// bob chest
		bobUpperBody(this.chest, chestPos, ageInTicks, 1.0F);

		// bob arms
		bobArm(rightArm, ageInTicks, 1F);
		bobArm(leftArm, ageInTicks, -1F);
	}

	@Override
	public void swivelHead(float netHeadYaw, float headPitch, float ageInTicks) {
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F) - 0.349066F; // +20 degrees because has a slight down angle
	}

	@Override
	public void swingLegs(float limbSwing, float limbSwingAmount, float age, float walkSpeed, float maxAngle) {
		// 0.610865 = 35 degrees. sets the legs under the ghoul properly.
		this.rightLeg.xRot = -0.610865F + Mth.cos(limbSwing * walkSpeed) * maxAngle  * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
		this.leftLeg.xRot = -0.610865F + Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
	}

	@Override
	public void swingArms(float limbSwing, float limbSwingAmount, float age, float speed, float maxAngle)	{
		// 1.178097 = 67.5
		this.rightArm.xRot = -1.178097F + Mth.cos(limbSwing * speed) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
		this.leftArm.xRot = -1.178097F + Mth.cos(limbSwing * speed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
	}

	public void bobArm(ModelPart part, float age, float direction) {
		part.zRot += direction * (Mth.cos(age * /*0.09F*/ 0.15F) * 0.05F + 0.05F);

	}

	@Override
	public void resetSwing(T entity) {
		body.yRot = 0;
		rightArm.x = rightArmPos.x();
		rightArm.xRot = rightArmRot.x();
		rightArm.yRot = rightArmRot.y();
		rightArm.zRot = 0;
		leftArm.x = leftArmPos.x();
		leftArm.xRot = leftArmRot.x();
		leftArm.yRot = leftArmRot.y();
		leftArm.zRot = 0;
	}

	@Override
	public Optional<ModelPart> getHead() {
		return Optional.of(this.head);
	}

	@Override
	public Optional<ModelPart> getLeftLeg() {
		return Optional.of(this.leftLeg);
	}

	@Override
	public Optional<ModelPart> getRightLeg() {
		return Optional.of(this.rightLeg);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		ghoul.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}