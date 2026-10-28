/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
import mod.gottsch.forge.gmm.core.entity.monster.canine.GMMCanine;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * The canine family's rig: vanilla's wolf geometry and UVs (64x32, so a recoloured vanilla wolf
 * texture maps straight on), copied rather than extending {@code WolfModel}, which is bound to
 * {@code Wolf}. One rig for all three; size is the renderer's scale, colour is the texture.
 *
 * <p>Posture is a hunting wolf's: tail carried low while it roams, raised stiff once it is
 * aggressive (vanilla's angry-wolf tail). While a breath is in progress the head drops and braces
 * and shudders slightly, so the breath has a tell.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class CanineModel<T extends GMMCanine> extends EntityModel<T> implements IAnimalModel {
	public static final String MODEL_NAME = "canine";
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(GMM.MOD_ID, MODEL_NAME), "main");

	// vanilla wolf tail angles: wild and roaming, and angry
	private static final float TAIL_IDLE = (float) Math.PI / 5F;
	private static final float TAIL_AGGRESSIVE = 1.5393804F;
	// breath tell: head dipped and braced, with a small shudder
	private static final float BREATH_HEAD_DIP = 0.2F;
	private static final float BREATH_SHUDDER = 0.06F;

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart realHead;
	private final ModelPart body;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart tail;

	public CanineModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
		this.realHead = this.head.getChild("real_head");
		this.body = root.getChild("body");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.tail = root.getChild("tail");
	}

	/** Vanilla {@code WolfModel#createBodyLayer}, part for part. */
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
		head.addOrReplaceChild("real_head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F)
				.texOffs(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F)
				.texOffs(16, 14).addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F)
				.texOffs(0, 10).addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F), PartPose.ZERO);

		partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) Math.PI / 2F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F),
				PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) Math.PI / 2F, 0.0F, 0.0F));

		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);
		partdefinition.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 16.0F, 7.0F));
		partdefinition.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(0.5F, 16.0F, 7.0F));
		partdefinition.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 16.0F, -4.0F));
		partdefinition.addOrReplaceChild("left_front_leg", leg, PartPose.offset(0.5F, 16.0F, -4.0F));

		PartDefinition tail = partdefinition.addOrReplaceChild("tail", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, (float) Math.PI / 5F, 0.0F, 0.0F));
		tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.ZERO);

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
		// a hunting wolf holds its tail still; otherwise vanilla's swish. Wolf trot: diagonal pairs in phase
		this.tail.yRot = entity.isAggressive() ? 0.0F : Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		float pitch = headPitch * ((float) Math.PI / 180F);
		this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
		this.tail.xRot = entity.isAggressive() ? TAIL_AGGRESSIVE : TAIL_IDLE;
		if (entity.isBreathing()) {
			this.head.xRot = pitch + BREATH_HEAD_DIP;
			this.realHead.zRot = Mth.sin(ageInTicks * 1.7F) * BREATH_SHUDDER;
		} else {
			this.head.xRot = pitch;
			this.realHead.zRot = 0.0F;
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
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
		return Optional.of(rightHindLeg);
	}

	@Override
	public Optional<ModelPart> getRearLeftLeg() {
		return Optional.of(leftHindLeg);
	}
}
