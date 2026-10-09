package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.SkeletonWarriorModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonWarrior;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Jan 19, 2024
 */
public class SkeletonWarriorRenderer<T extends SkeletonWarrior> extends HumanoidMobRenderer<T, SkeletonWarriorModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/skeleton_warrior.png");

	public SkeletonWarriorRenderer(EntityRendererProvider.Context context) {
		this(context,
				SkeletonWarriorModel.LAYER_LOCATION,
				ModelLayers.SKELETON_INNER_ARMOR,
				ModelLayers.SKELETON_OUTER_ARMOR);
	}

	public SkeletonWarriorRenderer(EntityRendererProvider.Context context,
								   ModelLayerLocation modelLocation, ModelLayerLocation innerArmor, ModelLayerLocation outerArmor) {
		super(context, new SkeletonWarriorModel<>(context.bakeLayer(modelLocation)), 0.8F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new SkeletonWarriorModel(context.bakeLayer(innerArmor)),
				new SkeletonWarriorModel(context.bakeLayer(outerArmor)),
						context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(SkeletonWarrior entity) {
		return TEXTURE;
	}
}
