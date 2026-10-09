package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.OrcModel;
import mod.gottsch.neo.gmm.core.entity.monster.Orc;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 28, 2022
 *
 * @param <T>
 */
public class OrcRenderer<T extends Orc> extends HumanoidMobRenderer<T, OrcModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/orc.png");

	public OrcRenderer(EntityRendererProvider.Context context) {
		super(context, new OrcModel<>(context.bakeLayer(OrcModel.LAYER_LOCATION)), 0.8F);
	}

	@Override
	public ResourceLocation getTextureLocation(Orc entity) {
		return TEXTURE;
	}
}
