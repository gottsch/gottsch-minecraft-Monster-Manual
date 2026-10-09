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
 * Dedicated Orc Shaman rig -- a Blockbench-authored variant of {@link OrcModel} with a cowl (an
 * inflated outer head cube with a genuine alpha cutout for the face, same idiom as vanilla's hat
 * layer) and a full robe (shoulders + torso + independently-posed front/back flaps, merged into
 * {@code orcBody}). Ported from the Blockbench export at
 * {@code Blockbench/Dungeon Denizens/forge/orc shaman/OrcShaman.bbmodel}; follows the same pattern
 * {@link OrcModel} does -- extend {@code HumanoidModel} for its animation, register the seven vanilla
 * part names with no cubes, and drive the real geometry from the pose {@code super.setupAnim}
 * computes -- so vanilla's attack/walk animation keeps working.
 * <p>
 * Refactored alongside {@link OrcModel} on 2026-09-07 off the hidden-cubes version; see that class
 * for what changed and why. The staff below is baked geometry, so the mainhand fix does not show on
 * this rig -- but {@link #translateToHand} is correct here too, which matters for the offhand and for
 * any consumer that arms a Shaman.
 * <p>
 * The staff is baked directly into {@code orcRightLowerArm}'s geometry ({@code staffShaft} /
 * {@code staffHead}) rather than an equippable item -- {@code OrcShaman} keeps its main hand empty
 * by design, see {@code OrcShaman#populateDefaultEquipmentSlots}.
 * <p>
 * NOTE: the source .bbmodel also defines {@code leftShoulderPad}/{@code rightShoulderPad} groups,
 * excluded from export on purpose (confirmed with the user) -- this rig deliberately has no shoulder
 * pads, a robed caster doesn't wear an orc's spiked pauldrons. The rig also has no hair part (the
 * cowl covers the head entirely), so unlike {@link OrcModel} this model does not toggle hair
 * visibility off {@link Orc#hasHair()}.
 *
 * @author Mark Gottschling on Jul 10, 2026
 */
public class OrcShamanModel<T extends LivingEntity> extends HumanoidModel<T> implements ArmedModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "orc_shaman"), "main");
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
	private final ModelPart leftBracer;
	private final ModelPart rightBracer;

	private float leftArmX;
	private float rightArmX;
	private float leftArmY;
	private float rightArmY;

	public OrcShamanModel(ModelPart root) {
		super(root);
		this.root = root;
		this.orcHead = root.getChild("orcHead");
		this.orcBody = root.getChild("orcBody");
		this.orcRightArm = orcBody.getChild("orcRightArm");
		this.orcLeftArm = orcBody.getChild("orcLeftArm");
		this.orcLeftLeg = root.getChild("orcLeftLeg");
		this.orcRightLeg = root.getChild("orcRightLeg");
		mouth = orcHead.getChild("jaw");

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

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

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

		///// orc shaman model parts //////////////
		// orcHead: skull "ears", eye plate, and an inflated cowl shell -- all merged into one cube list,
		// same idiom the old torso used. The cowl carries a genuine alpha cutout around the jaw so the
		// face shows through; DO NOT solid-fill that region when repainting the texture.
		PartDefinition orcHead = partdefinition.addOrReplaceChild("orcHead", CubeListBuilder.create().texOffs(34, 83).addBox(-4.1F, -6.0F, -6.0F, 0.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(34, 83).addBox(4.1F, -6.0F, -6.0F, 0.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(70, 48).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 7.0F, 2.0F, new CubeDeformation(-0.01F))
				.texOffs(57, 0).addBox(-4.0F, -6.0F, -5.5F, 8.0F, 9.0F, 10.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -2.0F, -3.0F));

		PartDefinition jaw = orcHead.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(56, 66).addBox(-4.0F, -0.5F, -3.6F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 1.5F, -1.5F, 0.2618F, 0.0F, 0.0F));
		PartDefinition teeth2_r1 = jaw.addOrReplaceChild("teeth2_r1", CubeListBuilder.create().texOffs(53, 37).addBox(6.0F, -3.0F, 2.8F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.0F, -6.2F, 0.0F, 0.0F, 0.7854F));
		PartDefinition teeth1_r1 = jaw.addOrReplaceChild("teeth1_r1", CubeListBuilder.create().texOffs(53, 37).addBox(3.0F, 0.0F, 2.8F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -4.2F, -6.3F, 0.0F, 0.0F, 0.7854F));

		PartDefinition orcBody = partdefinition.addOrReplaceChild("orcBody", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		// robe: shoulders + main torso box, plus front/back flaps posed independently of the torso so
		// they can hang/swing on their own (no shoulder pads -- excluded from export on purpose)
		PartDefinition robe = orcBody.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(0, 29).addBox(-10.0F, -26.0F, -3.0F, 20.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 43).addBox(-5.5F, -26.0F, -3.0F, 11.0F, 17.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition frontRobe = robe.addOrReplaceChild("frontRobe", CubeListBuilder.create().texOffs(0, 43).addBox(-5.5F, -1.0F, 0.0F, 11.0F, 17.0F, 6.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -25.0F, -3.0F, -0.0873F, 0.0F, 0.0F));
		PartDefinition backRobe = robe.addOrReplaceChild("backRobe", CubeListBuilder.create().texOffs(35, 43).addBox(-5.5F, 0.0F, -6.0F, 11.0F, 16.0F, 6.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -25.0F, 3.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition orcLeftArm = orcBody.addOrReplaceChild("orcLeftArm", CubeListBuilder.create().texOffs(0, 78).addBox(-1.5F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(7.0F, -23.0F, 0.0F, 0.2182F, 0.0F, -0.0873F));
		PartDefinition orcLeftLowerArm = orcLeftArm.addOrReplaceChild("orcLeftLowerArm", CubeListBuilder.create().texOffs(56, 73).addBox(-1.5F, 0.0F, -2.1F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.4363F, 0.0F, 0.0F));
		PartDefinition orcLeftBracer = orcLeftLowerArm.addOrReplaceChild("orcLeftBracer", CubeListBuilder.create().texOffs(70, 58).addBox(4.5F, -14.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(-6.0F, 18.0F, 0.0F));

		PartDefinition orcRightArm = orcBody.addOrReplaceChild("orcRightArm", CubeListBuilder.create().texOffs(17, 78).addBox(-2.5F, -1.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-7.0F, -23.0F, 0.0F, 0.1309F, 0.0F, 0.0436F));
		PartDefinition orcRightLowerArm = orcRightArm.addOrReplaceChild("orcRightLowerArm", CubeListBuilder.create().texOffs(73, 73).addBox(-2.5F, 0.0F, -2.1F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
				// staff, baked into the geometry -- see OrcShaman#populateDefaultEquipmentSlots, no equippable item
				.texOffs(0, 0).addBox(-1.0F, 6.0F, -13.0F, 1.0F, 1.0F, 27.0F, new CubeDeformation(0.0F))
				.texOffs(21, 67).addBox(-2.0F, 5.0F, -14.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.2182F, 0.0F, 0.0F));
		PartDefinition orcRightBracer = orcRightLowerArm.addOrReplaceChild("orcRightBracer", CubeListBuilder.create().texOffs(78, 20).addBox(3.5F, -14.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(-6.0F, 18.0F, 0.0F));

		PartDefinition orcLeftLeg = partdefinition.addOrReplaceChild("orcLeftLeg", CubeListBuilder.create().texOffs(57, 20).addBox(-2.0F, 1.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 12.0F, 0.0F));
		PartDefinition left_boot = orcLeftLeg.addOrReplaceChild("left_boot", CubeListBuilder.create().texOffs(0, 67).addBox(0.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offset(-2.5F, 12.0F, 0.0F));
		PartDefinition orcRightLeg = partdefinition.addOrReplaceChild("orcRightLeg", CubeListBuilder.create().texOffs(35, 66).addBox(-3.0F, 1.0F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 12.0F, 0.0F));
		PartDefinition right_boot = orcRightLeg.addOrReplaceChild("right_boot", CubeListBuilder.create().texOffs(70, 37).addBox(-0.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offset(-2.5F, 12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// let HumanoidModel pose the (empty) vanilla parts -- walk cycle, attack swing,
		// bow/riding poses -- then read the arm rotations back out below.
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		// reset orc arm positions
		orcRightArm.x = rightArmX;
		orcLeftArm.x = leftArmX;

		// get the orc entity and determine what parts are visible (no hair/shoulder-pad toggles here --
		// this rig has neither, see class javadoc)
		Orc orc = (Orc) entity;
		rightBracer.visible = orc.hasBracers();
		leftBracer.visible = rightBracer.visible;

		// head
		this.orcHead.yRot = netHeadYaw * ((float) Math.PI / 180F);

		if (headPitch < 0) {
			this.orcHead.xRot = Math.max(-15, headPitch) * ((float) Math.PI / 180F);
		} else {
			this.orcHead.xRot = Math.min(35, headPitch) * ((float) Math.PI / 180F);
		}

		/*
		 * legs
		 */
		float radians = 0.6F;
		float walkSpeed = 0.9F; // half speed = 0.5
		this.orcRightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * radians * 1.4F * limbSwingAmount;
		this.orcLeftLeg.xRot = Mth.cos(limbSwing * walkSpeed + (float) Math.PI) * radians * 1.4F * limbSwingAmount;

		/*
		 * arms
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
