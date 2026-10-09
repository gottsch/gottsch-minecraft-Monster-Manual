package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.OrcShamanModel;
import mod.gottsch.neo.gmm.core.entity.monster.OrcShaman;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Jul 10, 2026
 *
 * @param <T>
 */
public class OrcShamanRenderer<T extends OrcShaman> extends HumanoidMobRenderer<T, OrcShamanModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/orc_shaman.png");

	public OrcShamanRenderer(EntityRendererProvider.Context context) {
		super(context, new OrcShamanModel<>(context.bakeLayer(OrcShamanModel.LAYER_LOCATION)), 0.8F);
	}

	@Override
	public ResourceLocation getTextureLocation(OrcShaman entity) {
		return TEXTURE;
	}
}
