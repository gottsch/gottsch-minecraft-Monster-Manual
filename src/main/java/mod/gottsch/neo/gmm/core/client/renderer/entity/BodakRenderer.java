package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BodakModel;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bodak;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Bodak on its own dedicated {@link BodakModel#LAYER_LOCATION} rig -- a Blockbench-authored
 * variant of the vanilla zombie proportions adding a permanently agape "silent scream" mouth cube (see
 * {@link BodakModel}'s class doc), bare-chested (no baked-in shirt/sleeves, unlike the vanilla-zombie
 * texture every other GMM zombie variant recolors) -- which also overlays a Death-Gaze-charge-driven
 * head jerk on top of the shared {@code GMMZombieModel} pose. No glowing-eyes layer yet (Bodak's "milky
 * white" eyes from the 5e flavor text would need a new {@code layer/bodak_eyes.png} asset, not built
 * this pass).
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class BodakRenderer<T extends Bodak> extends HumanoidMobRenderer<T, BodakModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/bodak.png");

	public BodakRenderer(EntityRendererProvider.Context context) {
		super(context, new BodakModel<>(context.bakeLayer(BodakModel.LAYER_LOCATION)), 0.5F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
						new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
						context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
