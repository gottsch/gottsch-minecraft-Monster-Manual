package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.MargoyleModel;
import mod.gottsch.neo.gmm.core.entity.monster.gargoyle.Margoyle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Margoyle using its own geometry (4 horns, smaller wings) and the
 * "stone" skin. Shares the winged-humanoid animation, so it flaps while hovering.
 *
 * @author Mark Gottschling on July 26, 2025
 */
public class MargoyleRenderer<T extends Margoyle> extends MobRenderer<T, MargoyleModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/margoyle.png");

	public MargoyleRenderer(EntityRendererProvider.Context context) {
		super(context, new MargoyleModel<>(context.bakeLayer(MargoyleModel.LAYER_LOCATION)), 0.9F);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
