package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.ShadowModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.ShadowEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.Shadow;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 12, 2022
 */
public class ShadowRenderer<T extends Shadow> extends HumanoidMobRenderer<T, ShadowModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/shadow.png");

	public ShadowRenderer(EntityRendererProvider.Context context) {
		super(context, new ShadowModel<>(context.bakeLayer(ShadowModel.LAYER_LOCATION)), 0F);
		this.addLayer(new ShadowEyeLayer<>(this));
	}

	@Override
	public ResourceLocation getTextureLocation(Shadow entity) {
		return TEXTURE;
	}
}
