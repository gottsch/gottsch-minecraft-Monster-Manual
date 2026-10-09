package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.ElectricSkeletonModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.ElectricSkeleton;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Electric Skeleton with its dedicated {@link ElectricSkeletonModel} (bent/jointed-arm
 * rig with independently-spinning forearm electricity cuffs), plus full-brightness cyan-white eyes
 * via the reusable {@link GMMEyesLayer}. No armor layer — Electric Skeleton never equips gear.
 *
 * @author Mark Gottschling on 7/4/2026
 */
public class ElectricSkeletonRenderer<T extends ElectricSkeleton> extends MobRenderer<T, ElectricSkeletonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/electric_skeleton.png");
	private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/electric_skeleton_eyes.png");

	public ElectricSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new ElectricSkeletonModel<>(context.bakeLayer(ElectricSkeletonModel.LAYER_LOCATION)), 0.5F);
		this.addLayer(new GMMEyesLayer<>(this, EYES));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
