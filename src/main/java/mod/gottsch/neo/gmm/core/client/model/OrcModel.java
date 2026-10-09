package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.Orc;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HumanoidModel;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/**
 * Orc rig. Extends {@link HumanoidModel} for its animation rather than for its geometry: everything
 * a weapon-carrying humanoid needs -- the walk cycle, the attack swing off {@code attackTime}, the
 * bow/riding/swimming poses -- is already written in {@code HumanoidModel#setupAnim}, so the seven
 * vanilla part names are registered with NO cubes, {@code super.setupAnim} poses them, and the real
 * orc parts read the resulting rotations back out.
 *
 * <p><b>Refactored 2026-09-07.</b> This model used to register real vanilla cubes and hide them with
 * {@code visible = false}, and place a held item by hand-tuning the hidden {@code rightArm} to
 * {@code (-6.5, 4.0)} so that the inherited {@code translateToHand} happened to land in about the
 * right spot -- the original comment here said as much ("can't get the held item to rotate/swing
 * properly with the arm"). {@link #translateToHand} now walks the real arm chain instead, so the item
 * follows the actual forearm through the swing rather than an approximation of it. Two things fell
 * out of that:
 * <ul>
 *   <li>The hidden cubes are gone. They also overlapped real orc UV regions ({@code texOffs(0,0)} is
 *       the torso), which was harmless only because they never rendered.</li>
 *   <li>The <b>offhand</b> is fixed. The hidden {@code left_arm} was never tuned the way the right
 *       one was, so an offhand item rendered 1.5 across and 2 up from the actual left fist. The
 *       right hand's placement is unchanged to within 0.1 units.</li>
 * </ul>
 *
 * @author Mark Gottschling on Apr 28, 2022
 *
 * @param <T>
 */
public class OrcModel<T extends LivingEntity> extends HumanoidModel<T> implements ArmedModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "orc"), "main");
	private final ModelPart root;
	private final ModelPart orcHead;
	private final ModelPart orcBody;
	private final ModelPart orcLeftArm;
	private final ModelPart orcRightArm;
	private final ModelPart orcRightLowerArm;
	private final ModelPart orcLeftLowerArm;
	private final ModelPart orcLeftLeg;
	private final ModelPart orcRightLeg;
	private final ModelPart mouth;

	private final ModelPart rightShoulderPad;
	private final ModelPart leftShoulderPad;
	private final ModelPart hair;
	private final ModelPart leftBracer;
	private final ModelPart rightBracer;

	private float leftArmX;
	private float rightArmX;
	private float leftArmY;
	private float rightArmY;

	/**
	 *
	 * @param root
	 */
	public OrcModel(ModelPart root) {
		super(root);
		this.root = root;
		this.orcHead = root.getChild("orcHead");
		this.orcBody = root.getChild("orcBody");
		this.orcRightArm = orcBody.getChild("orcRightArm");
		this.orcLeftArm = orcBody.getChild("orcLeftArm");
		this.orcLeftLeg = root.getChild("orcLeftLeg");
		this.orcRightLeg = root.getChild("orcRightLeg");
		mouth = orcHead.getChild("jaw");
		hair = orcHead.getChild("hair");

		rightShoulderPad = orcBody.getChild("torso").getChild("rightShoulderPad");
		leftShoulderPad = orcBody.getChild("torso").getChild("leftShoulderPad");

		orcRightLowerArm = orcRightArm.getChild("orcRightLowerArm");
		orcLeftLowerArm = orcLeftArm.getChild("orcLeftLowerArm");
		rightBracer = orcRightLowerArm.getChild("orcRightBracer");
		leftBracer = orcLeftLowerArm.getChild("orcLeftBracer");

		// save orc arm original positions
		rightArmX = orcRightArm.x;
		leftArmX = orcLeftArm.x;
		rightArmY = orcRightArm.y;
		leftArmY = orcLeftArm.y;

	}

	/**
	 *
	 * @return
	 */
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		// TODO fix this - it is not needed - see SkeletonModel
		// MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		// the above creates the mesh from the humanoid.
		// a problem arises that the parts are all based from the root, and not in bones/sub-parts
		// should be able to work around this, as long as you set the Humanoid properties with the correct
		// path to the named part. So you could Humanoid without any of the original parts
		// (but the arms probably have to be in the same position for the item to place properly).

		// The seven part names HumanoidModel's constructor demands, registered with NO cubes. They
		// exist only so super.setupAnim() has somewhere to write the vanilla pose that setupAnim()
		// reads back out; nothing here ever renders. Before 2026-09-07 these carried real vanilla
		// cubes that were then hidden with visible = false -- see the class javadoc.
		partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
		partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

		/////	orc model parts	//////////////
		PartDefinition orcHead = partdefinition.addOrReplaceChild("orcHead", CubeListBuilder.create().texOffs(27, 21).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -3.0F));
		PartDefinition ear_r1 = orcHead.addOrReplaceChild("ear_r1", CubeListBuilder.create().texOffs(0, 29).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -2.0F, -1.0F, 0.4363F, -0.2618F, -0.2618F));
		PartDefinition ear_r2 = orcHead.addOrReplaceChild("ear_r2", CubeListBuilder.create().texOffs(29, 11).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -2.0F, -1.0F, 0.4363F, 0.2618F, 0.2618F));
		PartDefinition hair = orcHead.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 29).addBox(2.0F, -2.5F, -3.5F, 3.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -5.0F, -0.5F));
		PartDefinition jaw = orcHead.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(17, 56).addBox(-4.0F, -0.5F, -3.5F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(0.0F, 1.5F, -1.5F, 0.2618F, 0.0F, 0.0F));
		PartDefinition teeth4_r1 = jaw.addOrReplaceChild("teeth4_r1", CubeListBuilder.create().texOffs(15, 29).addBox(-0.2F, -1.5F, 4.5F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(15, 29).addBox(8.2F, -1.5F, 4.5F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.5F, -6.5F, -0.7854F, 0.0F, 0.0F));
		PartDefinition teeth2_r1 = jaw.addOrReplaceChild("teeth2_r1", CubeListBuilder.create().texOffs(20, 29).addBox(6.0F, -3.0F, 2.8F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(20, 29).addBox(3.0F, 0.0F, 2.8F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.5F, -6.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition orcBody = partdefinition.addOrReplaceChild("orcBody", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition torso = orcBody.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -26.0F, -3.0F, 20.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 11).addBox(-5.5F, -22.0F, -3.0F, 11.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition leftShoulderPad = torso.addOrReplaceChild("leftShoulderPad", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition spike2_r1 = leftShoulderPad.addOrReplaceChild("spike2_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.4619F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9619F, -27.0F, 0.5F, 0.0F, 0.0F, 0.5236F));
		PartDefinition spike1_r1 = leftShoulderPad.addOrReplaceChild("spike1_r1", CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5F, -25.0F, 0.5F, 0.0F, 0.0F, 0.6981F));
		PartDefinition left_should_pad_r1 = leftShoulderPad.addOrReplaceChild("left_should_pad_r1", CubeListBuilder.create().texOffs(35, 11).addBox(0.5F, -1.5F, -3.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(4.0F, -26.0F, 0.0F, 0.0F, 0.0F, 0.2618F));
		PartDefinition rightShoulderPad = torso.addOrReplaceChild("rightShoulderPad", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition rightSpike2_r1 = rightShoulderPad.addOrReplaceChild("rightSpike2_r1", CubeListBuilder.create().texOffs(0, 0).addBox(0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -26.0F, 0.5F, 0.0F, 0.0F, -0.5236F));
		PartDefinition rightSpike1_r1 = rightShoulderPad.addOrReplaceChild("rightSpike1_r1", CubeListBuilder.create().texOffs(0, 11).addBox(0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.5F, -25.0F, 0.5F, 0.0F, 0.0F, -0.6981F));
		PartDefinition right_should_pad_r1 = rightShoulderPad.addOrReplaceChild("right_should_pad_r1", CubeListBuilder.create().texOffs(35, 11).addBox(-6.0F, -1.5F, -3.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(-4.5F, -26.0F, 0.0F, 0.0F, 0.0F, -0.2618F));
		PartDefinition orcLeftArm = orcBody.addOrReplaceChild("orcLeftArm", CubeListBuilder.create().texOffs(60, 38).addBox(-1.5F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(7.0F, -23.0F, 0.0F, 0.2182F, 0.0F, -0.0873F));
		PartDefinition orcLeftLowerArm = orcLeftArm.addOrReplaceChild("orcLeftLowerArm", CubeListBuilder.create().texOffs(59, 52).addBox(-1.5F, 0.0F, -2.1F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.4363F, 0.0F, 0.0F));
		PartDefinition orcLeftBracer = orcLeftLowerArm.addOrReplaceChild("orcLeftBracer", CubeListBuilder.create().texOffs(17, 63).addBox(4.5F, -15.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.2F))
		.texOffs(15, 29).addBox(8.0F, -14.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(15, 29).addBox(6.0F, -14.0F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(15, 29).addBox(6.0F, -14.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 18.0F, 0.0F));
		PartDefinition orcRightArm = orcBody.addOrReplaceChild("orcRightArm", CubeListBuilder.create().texOffs(60, 26).addBox(-2.5F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-7.0F, -23.0F, 0.0F, 0.1309F, 0.0F, 0.0436F));
		PartDefinition orcRightLowerArm = orcRightArm.addOrReplaceChild("orcRightLowerArm", CubeListBuilder.create().texOffs(42, 56).addBox(-2.5F, 0.0F, -2.1F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.2182F, 0.0F, 0.0F));
		PartDefinition orcRightBracer = orcRightLowerArm.addOrReplaceChild("orcRightBracer", CubeListBuilder.create().texOffs(0, 61).addBox(3.5F, -15.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.2F))
		.texOffs(15, 29).addBox(3.0F, -14.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(15, 29).addBox(5.0F, -14.0F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(15, 29).addBox(5.0F, -14.0F, 1.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 18.0F, 0.0F));
		PartDefinition orcLeftLeg = partdefinition.addOrReplaceChild("orcLeftLeg", CubeListBuilder.create().texOffs(39, 39).addBox(-2.0F, 1.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 12.0F, 0.0F));
		PartDefinition left_boot = orcLeftLeg.addOrReplaceChild("left_boot", CubeListBuilder.create().texOffs(55, 15).addBox(0.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offset(-2.5F, 12.0F, 0.0F));
		PartDefinition orcRightLeg = partdefinition.addOrReplaceChild("orcRightLeg", CubeListBuilder.create().texOffs(18, 39).addBox(-3.0F, 1.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 12.0F, 0.0F));
		PartDefinition right_boot = orcRightLeg.addOrReplaceChild("right_boot", CubeListBuilder.create().texOffs(53, 0).addBox(-0.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offset(-2.5F, 12.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	//	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// let HumanoidModel pose the (empty) vanilla parts -- walk cycle, attack swing,
		// bow/riding poses -- then read the arm rotations back out below.
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		// reset orc arm positions
		orcRightArm.x = rightArmX;
		orcLeftArm.x = leftArmX;

		// get the orc entity and determine what parts are visible
		Orc orc = (Orc) entity;
		rightShoulderPad.visible = orc.hasRightShoulderPad();
		leftShoulderPad.visible = orc.hasLeftShoulderPad();
		hair.visible = orc.hasHair();
		rightBracer.visible = orc.hasBracers();
		leftBracer.visible = rightBracer.visible;

		// head
		this.orcHead.yRot = netHeadYaw * ((float)Math.PI / 180F);

		if (headPitch < 0) {
			this.orcHead.xRot = Math.max(-15, headPitch)  * ((float)Math.PI / 180F);
		}
		else {
			this.orcHead.xRot = Math.min(35, headPitch) * ((float)Math.PI / 180F);
		}

		/*
		 *  legs
		 */
		float radians = 0.6F;
		float walkSpeed = 0.9F; // half speed = 0.5
		this.orcRightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * radians  * 1.4F * limbSwingAmount;
		this.orcLeftLeg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount;


		/*
		 *  arms
		 */
		// take the arm swing HumanoidModel just computed and put it on the real arms
		this.orcRightArm.xRot = this.rightArm.xRot;
		this.orcLeftArm.xRot = this.leftArm.xRot;

		// bob the arms
		bobArmPart(this.orcRightArm, ageInTicks, 1.0F);
		bobArmPart(this.orcLeftArm, ageInTicks, -1.0F);

		orcRightArm.y = rightArmY + (Mth.cos(ageInTicks * 0.1F) * 0.5F + 0.05F);
		orcLeftArm.y = leftArmY + (Mth.cos(ageInTicks * 0.1F) * 0.5F + 0.05F);

		// bob mouth
		bobMouthPart(mouth, ageInTicks);
	}

	/**
	 *
	 * @param part
	 * @param age
	 * @param direction
	 */
	public static void bobArmPart(ModelPart part, float age, float direction) {
		part.zRot = direction * (0.08726646F + (Mth.cos(age * 0.15F) * 0.05F + 0.05F));
	}

	public static void bobMouthPart(ModelPart mouth, float age) {
		mouth.xRot = Math.max(0.08726646F, 0.08726646F + Mth.cos(age * 0.07F) * -0.2617994F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		orcHead.render(poseStack, buffer, packedLight, packedOverlay);
		orcBody.render(poseStack, buffer, packedLight, packedOverlay);
		orcLeftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		orcRightLeg.render(poseStack, buffer, packedLight, packedOverlay);
	}

	/**
	 * Positions the {@code PoseStack} at the mainhand/offhand fist so {@code ItemInHandLayer} can
	 * render the equipped weapon there.
	 *
	 * <p>The inherited {@code HumanoidModel} version applies only the vanilla arm part's own local
	 * transform. That works for vanilla, whose arm is a direct child of an unoffset root, but this
	 * rig's arm is three deep -- {@code orcBody > orc*Arm > orc*LowerArm} -- and the vanilla arm is
	 * now an empty stub. Walking the real chain reconstructs the same composed transform
	 * {@code orcBody.render()} builds, which is what makes a held item swing WITH the arm rather
	 * than approximately alongside it.
	 *
	 * <p>The trailing nudge lines the item up with the fist. {@code ItemInHandLayer} places the item,
	 * in the frame this method leaves behind, at {@code (-1, 10, -2)} for the right hand and
	 * {@code (+1, 10, -2)} for the left -- the bottom-front-centre of a <i>vanilla</i> 4x12x4 arm
	 * pivoting at the shoulder. This forearm is 8 long and 0.1 inflated, with its cube centred half
	 * a unit off the bone, so its bottom-front-centre is {@code (-+0.5, 8, -2.1)}. Hence
	 * {@code (+-0.5, -2, -0.1)}.
	 */
	@Override
	public void translateToHand(HumanoidArm side, PoseStack poseStack) {
		boolean right = side == HumanoidArm.RIGHT;
		this.orcBody.translateAndRotate(poseStack);
		(right ? this.orcRightArm : this.orcLeftArm).translateAndRotate(poseStack);
		(right ? this.orcRightLowerArm : this.orcLeftLowerArm).translateAndRotate(poseStack);
		poseStack.translate((right ? 0.5F : -0.5F) / 16.0F, -2.0F / 16.0F, -0.1F / 16.0F);
	}

	public ModelPart getAttackArm() {
		return getRightArm();
	}

	public ModelPart getRoot() {
		return root;
	}

	public ModelPart getHead() {
		return orcHead;
	}

	public ModelPart getBody() {
		return orcBody;
	}

	public ModelPart getRightArm() {
		return orcRightArm;
	}

	public ModelPart getLeftArm() {
		return orcLeftArm;
	}
}
