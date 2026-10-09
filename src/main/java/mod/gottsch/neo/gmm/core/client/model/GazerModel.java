package mod.gottsch.neo.gmm.core.client.model;

import java.util.Random;

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

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public class GazerModel<T extends Entity> extends BeholderkinModel<T> {
	public static final String MODEL_NAME = "gazer_model";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart mouth;
	private final ModelPart eyeStalk1;
	private final ModelPart eyeStalk2;
	private final ModelPart tentacle3;
	private final ModelPart tentacle4;
	private final ModelPart eyeStalk5;
	private final ModelPart eye1;
	private final ModelPart eye2;
	private final ModelPart eye3;
	private final ModelPart eye4;
	private final ModelPart eye5;

	private final float bodyY;
	private final float headY;

	private int[] eyeOffsets = new int[5];

	public GazerModel(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.mouth = root.getChild("mouth");
		this.eyeStalk1 = root.getChild("tentacle1");
		this.eyeStalk2 = root.getChild("tentacle2");
		this.tentacle3 = root.getChild("tentacle3");
		this.tentacle4 = root.getChild("tentacle4");
		this.eyeStalk5 = root.getChild("tentacle5");
		this.eye1 = eyeStalk1.getChild("eye");
		this.eye2 = eyeStalk2.getChild("eye2");
		this.eye3 = tentacle3.getChild("eye3");
		this.eye4 = tentacle4.getChild("eye4");
		this.eye5 = eyeStalk5.getChild("eye5");

		Random random = new Random();
		for (int i = 0; i < 5; i++) {
			eyeOffsets[i] = random.nextInt(0, 180);
		}

		bodyY = body.y;
		headY = head.y;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(51, 45).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.0F, -5.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -19.0F, -9.0F, 18.0F, 18.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 55).addBox(-6.0F, -17.0F, -11.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 37).addBox(0.0F, 0.1F, -2.5F, 0.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -13.5F, 11.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 37).addBox(0.0F, -10.4F, -3.3F, 0.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -10.5F, 12.5F, 0.3054F, 0.0F, 0.0F));

		PartDefinition spike3_r1 = body.addOrReplaceChild("spike3_r1", CubeListBuilder.create().texOffs(0, 60).addBox(-1.0F, -1.0F, -0.1716F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -5.9792F, 8.5355F, -0.3054F, 0.0F, 0.0F));

		PartDefinition spike2_r1 = body.addOrReplaceChild("spike2_r1", CubeListBuilder.create().texOffs(38, 59).addBox(-1.0F, -1.0F, 0.7426F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -12.4038F, 7.8891F, 0.3491F, 0.0F, 0.0F));

		PartDefinition spike1_r1 = body.addOrReplaceChild("spike1_r1", CubeListBuilder.create().texOffs(55, 0).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.5F, -20.0F, 7.0F, 0.7854F, 0.0F, 0.0F));

		PartDefinition mouth = partdefinition.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(0, 37).addBox(-8.0F, -3.0F, -10.0F, 16.0F, 3.0F, 14.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition teeth3_r1 = mouth.addOrReplaceChild("teeth3_r1", CubeListBuilder.create().texOffs(10, 0).addBox(-2.0F, 4.0F, 2.2F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(10, 0).addBox(0.0F, 2.0F, 2.2F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(10, 0).addBox(6.0F, -4.0F, 2.2F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(10, 0).addBox(4.0F, -2.0F, 2.2F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(10, 0).addBox(2.0F, 0.0F, 2.2F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -6.0F, -12.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition bone = mouth.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition tentacle1 = partdefinition.addOrReplaceChild("tentacle1", CubeListBuilder.create().texOffs(47, 37).addBox(-1.0F, -1.5F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.0F, 19.5F, -0.5F, 0.0F, 0.0F, 0.4363F));
		PartDefinition eye = tentacle1.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(24, 55).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));
		PartDefinition tentacle2 = partdefinition.addOrReplaceChild("tentacle2", CubeListBuilder.create().texOffs(47, 37).addBox(-1.0F, -2.0F, -2.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.0F, 11.0F, 2.0F, 0.0F, 0.0F, -0.4363F));
		PartDefinition eye2 = tentacle2.addOrReplaceChild("eye2", CubeListBuilder.create().texOffs(24, 55).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, -0.5F, -0.5F, 0.0F, 0.0F, 0.4363F));
		PartDefinition tentacle3 = partdefinition.addOrReplaceChild("tentacle3", CubeListBuilder.create().texOffs(47, 37).addBox(-11.0F, -1.5F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0F, 19.5F, -1.5F, 0.0F, 0.0F, -0.4363F));
		PartDefinition eye3 = tentacle3.addOrReplaceChild("eye3", CubeListBuilder.create().texOffs(24, 55).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));
		PartDefinition tentacle4 = partdefinition.addOrReplaceChild("tentacle4", CubeListBuilder.create().texOffs(47, 37).addBox(-11.0F, -1.5F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0F, 10.5F, 1.5F, 0.0F, 0.0F, 0.4363F));
		PartDefinition eye4 = tentacle4.addOrReplaceChild("eye4", CubeListBuilder.create().texOffs(24, 55).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));
		PartDefinition tentacle5 = partdefinition.addOrReplaceChild("tentacle5", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -7.5F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.5F, 1.5F, 0.4363F, 0.0F, 0.0F));
		PartDefinition eye5 = tentacle5.addOrReplaceChild("eye5", CubeListBuilder.create().texOffs(24, 55).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.4363F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		setRestrictedEyeRotations(netHeadYaw, headPitch, -20, 20, -20, 20);

		// reset tentacle rotations before bobbing
		float tentacleRotation = 0.4363323F; // 25 degrees
		this.eyeStalk1.xRot = 0F;
		this.eyeStalk1.zRot = tentacleRotation;

		this.eyeStalk2.xRot = 0F;
		this.eyeStalk2.zRot = -tentacleRotation;

		this.tentacle3.xRot = 0F;
		this.tentacle3.zRot = -tentacleRotation;

		this.tentacle4.xRot = 0F;
		this.tentacle4.zRot = tentacleRotation;

		this.eyeStalk5.xRot = tentacleRotation;
		this.eyeStalk5.zRot = 0F;

		// bob tentacles
		bobSideTentaclePart(eyeStalk1, eye1, ageInTicks, 0.612F, 1.0F, eyeOffsets[0]);
		bobSideTentaclePart(eyeStalk2, eye2, ageInTicks, 0.35F, -1.0F, eyeOffsets[1]);
		bobSideTentaclePart(tentacle3, eye3, ageInTicks, 0.55F, 1.0F, eyeOffsets[2]);
		bobSideTentaclePart(tentacle4, eye4, ageInTicks, 0.45F, 1.0F, eyeOffsets[3]);
		bobTopEyeStalk(eyeStalk5, eye5, ageInTicks, 0.3F, 1.0F, 0, 0.05F);

		// reset mouth
		this.mouth.xRot = 0.2181662F;

		// bob mouth
		bobMouthPart(mouth, ageInTicks, 0.3F, -0.2181662F);

		// bob entire body
		body.y = bodyY + (Mth.cos(ageInTicks * 0.15F) * 0.5F + 0.05F);
		head.y = headY + (Mth.cos(ageInTicks * 0.15F) * 0.5F + 0.05F);
	}

	// TODO can completely replace as the eye shouldn't stay level as it does
	public static void bobSideTentaclePart(ModelPart stalk, ModelPart eye, float age, float radians, float direction, int eyeOffset) {
		float eyeSpeed = 0.1F;
		float speed = 0.05F; // 1/20th speed
		stalk.zRot += direction * (Mth.cos(age * speed) *  radians + 0.05F);
		eye.zRot = -stalk.zRot;
		eye.yRot = (Mth.cos((age + eyeOffset) * eyeSpeed) * 0.3490659F + 0.05F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		head.render(poseStack, buffer, packedLight, packedOverlay);
		body.render(poseStack, buffer, packedLight, packedOverlay);
		mouth.render(poseStack, buffer, packedLight, packedOverlay);
		eyeStalk1.render(poseStack, buffer, packedLight, packedOverlay);
		eyeStalk2.render(poseStack, buffer, packedLight, packedOverlay);
		tentacle3.render(poseStack, buffer, packedLight, packedOverlay);
		tentacle4.render(poseStack, buffer, packedLight, packedOverlay);
		eyeStalk5.render(poseStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public ModelPart getBody() {
		return this.body;
	}

	@Override
	public ModelPart getEye() {
		return this.head;
	}
}
