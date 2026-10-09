package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BarrelMimicModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.mimic.BarrelMimic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's BarrelMimicRenderer
 *
 * @param <T>
 */
public class BarrelMimicRenderer<T extends BarrelMimic> extends MobRenderer<T, BarrelMimicModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/barrel_mimic.png");
	private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/barrel_mimic_eyes.png");

	public BarrelMimicRenderer(EntityRendererProvider.Context context) {
		super(context, new BarrelMimicModel<>(context.bakeLayer(BarrelMimicModel.LAYER_LOCATION)), 0.5F);
		this.addLayer(new GMMEyesLayer<>(this, EYES));
	}

	@Override
	public ResourceLocation getTextureLocation(BarrelMimic entity) {
		return TEXTURE;
	}
}
