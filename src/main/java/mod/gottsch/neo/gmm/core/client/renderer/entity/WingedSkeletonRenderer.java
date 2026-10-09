package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.WingedSkeletonModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.WingedSkeleton;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 18, 2022
 *
 * @param <T>
 */
public class WingedSkeletonRenderer<T extends WingedSkeleton> extends HumanoidMobRenderer<T, WingedSkeletonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/winged_skeleton.png");

	public WingedSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new WingedSkeletonModel<>(context.bakeLayer(WingedSkeletonModel.LAYER_LOCATION)), 0.8F);
	}

	@Override
	public ResourceLocation getTextureLocation(WingedSkeleton entity) {
		return TEXTURE;
	}
}
