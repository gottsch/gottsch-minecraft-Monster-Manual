package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BloaterZombieModel;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Bloater on its own dedicated {@link BloaterZombieModel} rig (bloated body/leg geometry,
 * outward-rolled arm rest pose, waddle gait) — replaces the earlier shared
 * {@link ModelLayers#ZOMBIE}/{@code GMMZombieModel} reuse now that the user has built a real
 * swollen-parts model. Armor layers still reuse the vanilla zombie armor models for parity (the
 * Bloater spawns unarmored by default, but the layer keeps any picked-up gear rendering correctly).
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class BloaterRenderer<T extends Bloater> extends HumanoidMobRenderer<T, BloaterZombieModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/bloater.png");

	public BloaterRenderer(EntityRendererProvider.Context context) {
		super(context, new BloaterZombieModel<>(context.bakeLayer(BloaterZombieModel.LAYER_LOCATION)), 0.5F);
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
