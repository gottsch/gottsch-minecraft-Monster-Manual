package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.construct.AnimatedArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Animated Armor as equipment with no wearer: the base body model always samples a fully
 * transparent texture ({@code blank.png}), so the only thing that ever actually draws is the standard
 * {@link HumanoidArmorLayer} rendering whatever's equipped. No dedicated rig — the equipped armor
 * pieces are the entity's entire visual identity, and {@code HumanoidModel}'s own stock walk/attack
 * animation drives them for free.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class AnimatedArmorRenderer<T extends AnimatedArmor> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
	private static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/blank.png");

	public AnimatedArmorRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
						new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
						context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return BLANK;
	}
}
