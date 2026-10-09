package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BoulderModel;
import mod.gottsch.neo.gmm.core.entity.monster.Boulder;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 */
public class BoulderRenderer extends MobRenderer<Boulder, BoulderModel<Boulder>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/boulder.png");

	public BoulderRenderer(EntityRendererProvider.Context context) {
		super(context, new BoulderModel<>(context.bakeLayer(BoulderModel.LAYER_LOCATION)), 0.4F);
	}

	@Override
	public void render(Boulder boulder, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(boulder, entityYaw, partialTicks, matrixStack, bufferSource, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(Boulder entity) {
		return TEXTURE;
	}
}
