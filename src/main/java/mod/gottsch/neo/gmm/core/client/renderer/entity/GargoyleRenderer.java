package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.GargoyleModel;
import mod.gottsch.neo.gmm.core.entity.monster.gargoyle.Gargoyle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on July 5, 2025
 *
 * @param <T>
 */
public class GargoyleRenderer<T extends Gargoyle> extends MobRenderer<T, GargoyleModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/gargoyle.png");

	public GargoyleRenderer(EntityRendererProvider.Context context) {
		super(context, new GargoyleModel<>(context.bakeLayer(GargoyleModel.LAYER_LOCATION)), 0.8F);
	}

	@Override
	public ResourceLocation getTextureLocation(Gargoyle entity) {
		return TEXTURE;
	}
}
