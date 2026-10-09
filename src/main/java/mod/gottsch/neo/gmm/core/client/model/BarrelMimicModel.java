package mod.gottsch.neo.gmm.core.client.model;// Made with Blockbench 4.9.4

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.mimic.BarrelMimic;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported verbatim (geometry unchanged) from Treasure2's {@code BarrelMimicModel}; {@code openAnim} was
 * converted to {@code hiddenAnim} to match {@code MimicModel}'s new instant-reveal contract (see that
 * class's doc for why the gradual open animation was dropped).
 *
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's BarrelMimicModel
 */
public class BarrelMimicModel<T extends BarrelMimic> extends MimicModel<T> {
	private final float SIDE_EYE_64 = -1.11701F;

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "barrel_mimic"), "main");

	private final ModelPart body;
	private final ModelPart lid;
	private final ModelPart middleLid;
	private final ModelPart front;
	private final ModelPart tongue;
	private final ModelPart eye1;
	private final ModelPart eye2;
	private final ModelPart eye3;

	private float bodyY;

	public BarrelMimicModel(ModelPart root) {
		this.body = root.getChild("body");
		this.lid = body.getChild("lid");
		this.middleLid = lid.getChild("middle_lid");
		this.eye1 = lid.getChild("eye1");
		this.eye2 = lid.getChild("eye2");
		this.eye3 = middleLid.getChild("eye3");
		this.tongue = body.getChild("base").getChild("tongue");
		this.front = body.getChild("base").getChild("front");

		this.lid.xRot = 0;
		bodyY = body.y;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition lid = body.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(49, 11).addBox(4.0F, -6.0F, -16.0F, 4.0F, 6.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(33, 34).addBox(-8.0F, -6.0F, -16.0F, 4.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, 8.0F));
		PartDefinition eye1 = lid.addOrReplaceChild("eye1", CubeListBuilder.create().texOffs(0, 27).addBox(-1.5F, 0.1F, 0.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.5F, -3.1F, -16.1F));
		PartDefinition eye2 = lid.addOrReplaceChild("eye2", CubeListBuilder.create().texOffs(0, 27).addBox(-1.5F, 0.1F, 0.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.5F, -3.1F, -16.1F));
		PartDefinition smallTooth1 = lid.addOrReplaceChild("smallTooth1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -0.5F, -15.0F, 0.0F, 0.0F, 0.7854F));
		PartDefinition smallTooth2 = lid.addOrReplaceChild("smallTooth2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.25F, -0.5F, -15.0F, 0.0F, 0.0F, 0.7854F));
		PartDefinition middle_lid = lid.addOrReplaceChild("middle_lid", CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, -6.0F, -16.0F, 8.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition eye3 = middle_lid.addOrReplaceChild("eye3", CubeListBuilder.create().texOffs(35, 57).addBox(-3.5F, 0.0F, 0.0F, 6.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -5.1F, -16.1F));
		PartDefinition topTooth1 = middle_lid.addOrReplaceChild("topTooth1", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -15.5F));
		PartDefinition t_r1 = topTooth1.addOrReplaceChild("t_r1", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 3.5F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -1.0F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth8 = middle_lid.addOrReplaceChild("topTooth8", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, -15.75F));
		PartDefinition t_r2 = topTooth8.addOrReplaceChild("t_r2", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 3.5F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -1.0F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth2 = middle_lid.addOrReplaceChild("topTooth2", CubeListBuilder.create(), PartPose.offset(4.0F, -2.0F, -15.5F));
		PartDefinition t_r3 = topTooth2.addOrReplaceChild("t_r3", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 3.5F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -1.0F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth3 = middle_lid.addOrReplaceChild("topTooth3", CubeListBuilder.create(), PartPose.offset(7.0F, 0.0F, -15.5F));
		PartDefinition skin1 = lid.addOrReplaceChild("skin1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -4.0F, -2.5F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 2.0F, -4.5F, 0.5236F, 0.0F, 0.0F));
		PartDefinition skin2 = lid.addOrReplaceChild("skin2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -4.0F, -2.5F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, 2.0F, -4.5F, 0.5236F, 0.0F, 0.0F));
		PartDefinition topTooth6 = lid.addOrReplaceChild("topTooth6", CubeListBuilder.create(), PartPose.offsetAndRotation(7.1109F, 0.0178F, -13.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r4 = topTooth6.addOrReplaceChild("t_r4", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth5 = lid.addOrReplaceChild("topTooth5", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.3891F, 0.0178F, -13.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r5 = topTooth5.addOrReplaceChild("t_r5", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth4 = lid.addOrReplaceChild("topTooth4", CubeListBuilder.create(), PartPose.offsetAndRotation(7.1109F, 0.0178F, -10.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r6 = topTooth4.addOrReplaceChild("t_r6", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition topTooth7 = lid.addOrReplaceChild("topTooth7", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.6391F, 0.0178F, -10.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r7 = topTooth7.addOrReplaceChild("t_r7", CubeListBuilder.create().texOffs(7, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition base = body.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -10.0F, -8.0F, 16.0F, 10.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition front = base.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 54).addBox(-7.0F, -10.0F, -3.0F, 14.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -5.0F));
		PartDefinition bottomTooth5 = front.addOrReplaceChild("bottomTooth5", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, -9.75F, -1.25F, -0.2182F, -0.4363F, 0.0F));
		PartDefinition t_r8 = bottomTooth5.addOrReplaceChild("t_r8", CubeListBuilder.create().texOffs(7, 0).addBox(-8.0F, 5.5F, 0.0F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5F, -1.0F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth2 = front.addOrReplaceChild("bottomTooth2", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0F, -9.75F, -1.25F, -0.2182F, 0.4363F, 0.0F));
		PartDefinition t_r9 = bottomTooth2.addOrReplaceChild("t_r9", CubeListBuilder.create().texOffs(7, 0).addBox(-8.0F, 5.5F, 0.0F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5F, -1.0F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition tongue = base.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(49, 0).addBox(-3.0F, 0.9F, -12.0F, 6.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.0F, 7.0F, -0.1745F, 0.0F, 0.0F));
		PartDefinition bottomTooth6 = base.addOrReplaceChild("bottomTooth6", CubeListBuilder.create(), PartPose.offsetAndRotation(7.1109F, -9.9822F, -5.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r10 = bottomTooth6.addOrReplaceChild("t_r10", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth4 = base.addOrReplaceChild("bottomTooth4", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.3891F, -9.9822F, -5.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r11 = bottomTooth4.addOrReplaceChild("t_r11", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth3 = base.addOrReplaceChild("bottomTooth3", CubeListBuilder.create(), PartPose.offsetAndRotation(7.6109F, -9.9822F, -2.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r12 = bottomTooth3.addOrReplaceChild("t_r12", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth7 = base.addOrReplaceChild("bottomTooth7", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.8891F, -9.9822F, -2.4F, 0.0F, -1.5708F, 0.0F));
		PartDefinition t_r13 = bottomTooth7.addOrReplaceChild("t_r13", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 2.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8891F, -1.7678F, -0.6F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth8 = base.addOrReplaceChild("bottomTooth8", CubeListBuilder.create(), PartPose.offsetAndRotation(2.2F, -10.25F, -7.5F, -0.0873F, 0.0F, 0.0F));
		PartDefinition t2_r1 = bottomTooth8.addOrReplaceChild("t2_r1", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -1.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, -1.5F, -0.5F, 0.0F, 0.0F, 0.7854F));
		PartDefinition bottomTooth9 = base.addOrReplaceChild("bottomTooth9", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.8F, -10.25F, -7.5F, -0.0873F, 0.0F, 0.0F));
		PartDefinition t3_r1 = bottomTooth9.addOrReplaceChild("t3_r1", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -1.0F, 0.5F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, -1.5F, -0.5F, 0.0F, 0.0F, 0.7854F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	@Override
	public void activeAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.activeAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

		middleLid.xRot = -0.174533F;
		front.xRot = 0.174533F;
		eye3.xRot = -1.003564F;
		eye1.xRot = SIDE_EYE_64;
		eye2.xRot = eye1.xRot;
		tongue.xRot = -0.174533F; // 10
	}

	@Override
	public void hiddenAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.hiddenAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		middleLid.xRot = 0F;
		front.xRot = 0F;
		eye3.xRot = 0F;
		eye1.xRot = 0F;
		eye2.xRot = 0F;
		tongue.xRot = 0F;
	}

	@Override
	public void openAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float timeSlice) {
		body.xRot = timeSlice * 0.2618F;
		lid.xRot = timeSlice * getMaxOpenAngle();
		middleLid.xRot = timeSlice * -0.174533F;
		front.xRot = timeSlice * 0.174533F;
		eye3.xRot = timeSlice * -1.003564F;
		eye1.xRot = timeSlice * SIDE_EYE_64;
		eye2.xRot = eye1.xRot;
		tongue.xRot = timeSlice * -0.174533F;
	}

	@Override
	public ModelPart getBody() {
		return body;
	}

	@Override
	public float getBodyY() {
		return bodyY;
	}

	@Override
	public ModelPart getLid() {
		return lid;
	}
}
