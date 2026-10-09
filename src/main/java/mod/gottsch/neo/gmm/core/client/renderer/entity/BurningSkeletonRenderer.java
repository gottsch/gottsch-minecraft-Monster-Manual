package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.SkeletonWarriorModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BurningSkeleton;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Burning Skeleton on the shared vanilla-skeleton rig ({@link SkeletonWarriorModel},
 * charred/ember recolor texture) with full-brightness yellow-hot eyes via the reusable
 * {@link GMMEyesLayer}. The "aflame" look is a client-side flame/smoke particle wreath emitted by the
 * entity itself (see {@code BurningSkeleton#aiStep}), not a render layer. Armor layers reuse the
 * skeleton armor models (Burning spawns unarmored by default, but the layer is kept for parity with
 * the other skeleton variants + any picked-up gear).
 *
 * @author Mark Gottschling on 7/6/2026
 */
public class BurningSkeletonRenderer<T extends BurningSkeleton> extends HumanoidMobRenderer<T, SkeletonWarriorModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/burning_skeleton.png");
	private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/burning_skeleton_eyes.png");

	public BurningSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new SkeletonWarriorModel<>(context.bakeLayer(SkeletonWarriorModel.LAYER_LOCATION)), 0.8F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new SkeletonWarriorModel(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
						new SkeletonWarriorModel(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
						context.getModelManager()));
		this.addLayer(new GMMEyesLayer<>(this, EYES));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
