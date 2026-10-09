package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.MagmaSkeletonModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.MagmaSkeleton;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Feb 4, 2024
 *
 * @param <T>
 */
public class MagmaSkeletonRenderer<T extends MagmaSkeleton> extends HumanoidMobRenderer<T, MagmaSkeletonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/magma_skeleton.png");
	private final float scale;

	public MagmaSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new MagmaSkeletonModel<>(context.bakeLayer(MagmaSkeletonModel.LAYER_LOCATION)), 0.6F);
		this.scale = 1.2F;
	}

	@Override
	protected void scale(MagmaSkeleton skeleton, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(MagmaSkeleton entity) {
		return TEXTURE;
	}
}
