package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BloaterArmModel;
import mod.gottsch.neo.gmm.core.entity.projectile.BloaterArm;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders a {@link BloaterArm} as a tumbling severed limb: oriented roughly along its flight and
 * spun on its long axis while airborne, frozen once it sticks — same tumble idiom as
 * {@link BoneShardRenderer}, but a single fixed zombie-arm shape textured from the Bloater's own
 * skin rather than a bone-splinter shape.
 *
 * @author Mark Gottschling on 7/14/2026
 */
public class BloaterArmRenderer extends EntityRenderer<BloaterArm> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/bloater.png");
	private final BloaterArmModel model;

	public BloaterArmRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new BloaterArmModel(context.bakeLayer(BloaterArmModel.LAYER_LOCATION));
	}

	@Override
	public void render(BloaterArm entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();
		// orient toward motion
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
		// tumble on the long axis — frozen once the arm sticks/stops (spinTicks stops advancing)
		float spin = (entity.getSpinTicks() + (entity.isStuck() ? 0.0F : partialTick)) * 40.0F;
		poseStack.mulPose(Axis.XP.rotationDegrees(spin));
		VertexConsumer vertexConsumer = buffer.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(BloaterArm entity) {
		return TEXTURE;
	}
}
