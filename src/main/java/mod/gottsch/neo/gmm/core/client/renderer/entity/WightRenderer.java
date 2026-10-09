package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.WightModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Wight;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Wight on the vanilla {@link ModelLayers#ZOMBIE} rig (gaunt recolor texture, no new
 * layer definition) via {@link WightModel}, plus an {@link ItemInHandLayer} so its carried sword/bow
 * (see {@code Wight#populateDefaultEquipmentSlots}) is actually visible, and full-brightness eyes.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class WightRenderer<T extends Wight> extends HumanoidMobRenderer<T, WightModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/wight.png");
	private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/wight_eyes.png");

	public WightRenderer(EntityRendererProvider.Context context) {
		super(context, new WightModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
						new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
						context.getModelManager()));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		this.addLayer(new GMMEyesLayer<>(this, EYES));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
