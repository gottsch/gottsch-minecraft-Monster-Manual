package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.VanillaChestMimicModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.mimic.VanillaChestMimic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's VanillaChestMimicRenderer
 *
 * @param <T>
 */
public class VanillaChestMimicRenderer<T extends VanillaChestMimic> extends MobRenderer<T, VanillaChestMimicModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/vanilla_chest_mimic.png");
	private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/vanilla_chest_mimic_eyes.png");

	public VanillaChestMimicRenderer(EntityRendererProvider.Context context) {
		super(context, new VanillaChestMimicModel<>(context.bakeLayer(VanillaChestMimicModel.LAYER_LOCATION)), 0.5F);
		this.addLayer(new GMMEyesLayer<>(this, EYES));
	}

	@Override
	public ResourceLocation getTextureLocation(VanillaChestMimic entity) {
		return TEXTURE;
	}
}
