/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * gottsch's Monster Manual is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gottsch's Monster Manual is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with gottsch's Monster Manual.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.entity.monster.EmberHound;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * Ember Hound rig, imported unchanged from Mage Flame's {@code EmberHoundModel} (geometry, UVs and
 * wolf-style gait), made generic in the entity type.
 *
 * @author Mark Gottschling on 10/16/2026
 */
public class EmberHoundModel<T extends Entity> extends EntityModel<T> implements IAnimalModel {
	public static final String MODEL_NAME = "ember_hound";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(GMM.MOD_ID, MODEL_NAME), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart upperBody;
	private final ModelPart rightRearLeg;
	private final ModelPart leftRearLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart tail;

	// a doused hound's coat goes from glowing ember to dull cinder
	private static final float DOUSED_TINT = 0.55F;
	private float tint = 1.0F;

	public EmberHoundModel(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.upperBody = root.getChild("upperBody");
		this.rightRearLeg = root.getChild("rightRearLeg");
		this.leftRearLeg = root.getChild("leftRearLeg");
		this.rightFrontLeg = root.getChild("rightFrontLeg");
		this.leftFrontLeg = root.getChild("leftFrontLeg");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(25, 14).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(38, 36).addBox(-3.0F, -6.0F, -2.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(38, 36).addBox(1.0F, -6.0F, -2.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(25, 25).addBox(-1.5F, -1.0156F, -7.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 13.5F, -5.0F, 0.1745F, 0.0F, 0.0F));

		partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(33, 33).addBox(-1.0F, -2.0F, 3.0F, 0.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition upperBody = partdefinition.addOrReplaceChild("upperBody", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -3.0F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 14.0F, 2.0F, 1.5708F, 0.0F, 0.0F));
		// back ridge "flames"
		upperBody.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(38, 33).addBox(2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -8.0F, 4.0F, -0.7854F, 0.0F, 0.0F));
		upperBody.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(9, 32).addBox(0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -6.0F, 4.0F, -0.7854F, 0.0F, 0.0F));
		upperBody.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(31, 11).addBox(-2.0F, 0.0F, 0.0F, 6.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -4.0F, 4.0F, -0.7854F, 0.0F, 0.0F));
		upperBody.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 4.0F, -0.7854F, 0.0F, 0.0F));

		partdefinition.addOrReplaceChild("rightRearLeg", CubeListBuilder.create().texOffs(31, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.5F, 16.0F, 7.0F));
		partdefinition.addOrReplaceChild("leftRearLeg", CubeListBuilder.create().texOffs(31, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 16.0F, 7.0F));
		partdefinition.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(31, 0).mirror().addBox(-2.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.5F, 16.0F, -4.0F));
		partdefinition.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(31, 0).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(18, 36).addBox(1.0F, 5.0F, 1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(18, 36).addBox(-4.0F, 5.0F, 1.0F, 0.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 16.0F, -4.0F));
		partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(24, 33).addBox(0.0F, 5.0F, -2.0F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(9, 36).addBox(-2.0F, 5.0F, 0.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, 0.9599F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
		this.tint = entity instanceof EmberHound hound && hound.isDoused() ? DOUSED_TINT : 1.0F;

		// tail swish + wolf trot (diagonal leg pairs in phase), all absolute
		this.tail.yRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.rightRearLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leftRearLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.xRot = headPitch * ((float) Math.PI / 180F);
		this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		red *= tint;
		green *= tint;
		blue *= tint;
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		upperBody.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		rightRearLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leftRearLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		rightFrontLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leftFrontLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public Optional<ModelPart> getHead() {
		return Optional.of(head);
	}

	@Override
	public Optional<ModelPart> getTail() {
		return Optional.of(tail);
	}

	@Override
	public Optional<ModelPart> getBody() {
		return Optional.of(body);
	}

	@Override
	public Optional<ModelPart> getFrontRightLeg() {
		return Optional.of(rightFrontLeg);
	}

	@Override
	public Optional<ModelPart> getFrontLeftLeg() {
		return Optional.of(leftFrontLeg);
	}

	@Override
	public Optional<ModelPart> getRearRightLeg() {
		return Optional.of(rightRearLeg);
	}

	@Override
	public Optional<ModelPart> getRearLeftLeg() {
		return Optional.of(leftRearLeg);
	}
}
