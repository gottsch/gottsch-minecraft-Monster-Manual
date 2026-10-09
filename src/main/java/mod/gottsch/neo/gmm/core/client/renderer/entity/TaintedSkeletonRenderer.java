package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.TaintedSkeletonModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.TaintedSkeleton;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Tainted Skeleton with the {@link TaintedSkeletonModel} (shard-core rig with the
 * pulsating taint shard). Plain {@link MobRenderer} — the model is a custom {@code EntityModel},
 * not a humanoid, and this variant wears no armor.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class TaintedSkeletonRenderer<T extends TaintedSkeleton> extends MobRenderer<T, TaintedSkeletonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/tainted_skeleton.png");

	public TaintedSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new TaintedSkeletonModel<>(context.bakeLayer(TaintedSkeletonModel.LAYER_LOCATION)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
