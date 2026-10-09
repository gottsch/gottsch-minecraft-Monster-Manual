package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BoneShardModel;
import mod.gottsch.neo.gmm.core.entity.projectile.BoneShard;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders a {@link BoneShard} as a tumbling bone splinter: oriented roughly along its flight and
 * spun on its long axis while airborne. The spin is driven by the shard's own spin counter (which
 * freezes on contact), so a stuck/landed shard holds still instead of spinning forever. Picks the
 * model matching the shard's shape variant.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class BoneShardRenderer extends EntityRenderer<BoneShard> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/bone_shard.png");
	private final BoneShardModel[] models;

	public BoneShardRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.models = new BoneShardModel[BoneShardModel.LAYERS.length];
		for (int i = 0; i < this.models.length; i++) {
			this.models[i] = new BoneShardModel(context.bakeLayer(BoneShardModel.LAYERS[i]));
		}
	}

	@Override
	public void render(BoneShard entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();
		// orient toward motion
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
		// tumble on the long axis — frozen once the shard sticks/stops (spinTicks stops advancing)
		float spin = (entity.getSpinTicks() + (entity.isStuck() ? 0.0F : partialTick)) * 40.0F;
		poseStack.mulPose(Axis.XP.rotationDegrees(spin));
		// caller-supplied chunkiness (default 1.0); lets Bloody Bones throw bigger limb pieces.
		float scale = entity.getScale();
		if (scale != 1.0F) {
			poseStack.scale(scale, scale, scale);
		}
		BoneShardModel model = this.models[Math.floorMod(entity.getVariant(), this.models.length)];
		VertexConsumer vertexConsumer = buffer.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(BoneShard entity) {
		return TEXTURE;
	}
}
