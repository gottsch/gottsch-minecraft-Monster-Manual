package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.Boulder;

import net.minecraft.client.model.HierarchicalModel;
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

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public class BoulderModel<T extends Entity> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "boulder"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart frontLeftLeg;
	private final ModelPart frontRightLeg;
	private final ModelPart backLeftLeg;
	private final ModelPart backRightLeg;

	private final static PartPose FL_LEG_POSE = PartPose.offset(7.0F, 19.0F, -7.0F);
	private final static PartPose FR_LEG_POSE = PartPose.offset(-7.0F, 19.0F, -7.0F);
	private final static PartPose BL_LEG_POSE = PartPose.offset(7.0F, 19.0F, 7.0F);
	private final static PartPose BR_LEG_POSE = PartPose.offset(-7.0F, 19.0F, 7.0F);
	private final static PartPose BODY_POSE = PartPose.offset(0.0F, 24.0F, 0.0F);

	public BoulderModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.frontLeftLeg = root.getChild("front_left_leg");
		this.frontRightLeg = root.getChild("front_right_leg");
		this.backLeftLeg = root.getChild("back_left_leg");
		this.backRightLeg = root.getChild("back_right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -18.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), BODY_POSE);
		PartDefinition front_left_leg = partdefinition.addOrReplaceChild("front_left_leg", CubeListBuilder.create().texOffs(0, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), FL_LEG_POSE);
		PartDefinition front_right_leg = partdefinition.addOrReplaceChild("front_right_leg", CubeListBuilder.create().texOffs(34, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), FR_LEG_POSE);
		PartDefinition back_left_leg = partdefinition.addOrReplaceChild("back_left_leg", CubeListBuilder.create().texOffs(17, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), BL_LEG_POSE);
		PartDefinition back_right_leg = partdefinition.addOrReplaceChild("back_right_leg", CubeListBuilder.create().texOffs(0, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), BR_LEG_POSE);

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

		Boulder boulder = (Boulder) entity;

		// legs
		float f = 1.0F;
		float radians = 0.6662F; // 30 degrees
		float walkSpeed = 1.0F; // half speed = 0.5

		// awake - animate regular walk
		if (boulder.isActive()) {
			// reset positions
			this.frontRightLeg.x = FR_LEG_POSE.x;
			this.frontRightLeg.z = FR_LEG_POSE.z;
			this.frontLeftLeg.x = FL_LEG_POSE.x;
			this.frontLeftLeg.z = FL_LEG_POSE.z;

			this.backRightLeg.x = BR_LEG_POSE.x;
			this.backRightLeg.z = BR_LEG_POSE.z;
			this.backLeftLeg.x = BL_LEG_POSE.x;
			this.backLeftLeg.z = BL_LEG_POSE.z;

			this.body.y = BODY_POSE.y;

			this.frontRightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * radians  * 1.4F * limbSwingAmount / f;
			this.frontLeftLeg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount / f;

			this.backRightLeg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * radians * 1.4F * limbSwingAmount / f;
			this.backLeftLeg.xRot = Mth.cos(limbSwing * walkSpeed) * radians  * 1.4F * limbSwingAmount / f;
		}
		else {
			if (boulder.getAmount() <= 1.0F) {
				// update poses
				this.frontLeftLeg.x = FL_LEG_POSE.x - (boulder.getAmount() * Boulder.MAX_LEG_AMOUNT);
				this.frontLeftLeg.z = FL_LEG_POSE.z + (boulder.getAmount() * 2.0F);
				this.frontLeftLeg.xRot = 0;

				this.frontRightLeg.x = FR_LEG_POSE.x + (boulder.getAmount() * 2.0F);
				this.frontRightLeg.z = FR_LEG_POSE.z + (boulder.getAmount() * 2.0F);
				this.frontRightLeg.xRot = 0;

				this.backLeftLeg.x = BL_LEG_POSE.x - (boulder.getAmount() * 2.0F);
				this.backLeftLeg.z = BL_LEG_POSE.z - (boulder.getAmount() * 2.0F);
				this.backLeftLeg.xRot = 0;

				this.backRightLeg.x = BR_LEG_POSE.x + (boulder.getAmount() * 2.0F);
				this.backRightLeg.z = BR_LEG_POSE.z - (boulder.getAmount() * 2.0F);
				this.backRightLeg.xRot = 0;
			}
			if (boulder.getBodyAmount() <= 1.0F) {
				this.body.y = BODY_POSE.y + (boulder.getBodyAmount() * 2.0F);
			}
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		body.render(poseStack, buffer, packedLight, packedOverlay);
		frontLeftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		frontRightLeg.render(poseStack, buffer, packedLight, packedOverlay);
		backLeftLeg.render(poseStack, buffer, packedLight, packedOverlay);
		backRightLeg.render(poseStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public ModelPart root() {
		return root;
	}
}
