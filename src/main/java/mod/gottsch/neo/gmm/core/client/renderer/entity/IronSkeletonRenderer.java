package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.IronSkeletonModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.IronSkeleton;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Feb 3, 2024
 *
 * @param <T>
 */
public class IronSkeletonRenderer<T extends IronSkeleton> extends HumanoidMobRenderer<T, IronSkeletonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/iron_skeleton.png");

	private final float scale;

	public IronSkeletonRenderer(EntityRendererProvider.Context context) {
		this(context,
				IronSkeletonModel.LAYER_LOCATION,
				ModelLayers.SKELETON_INNER_ARMOR,
				ModelLayers.SKELETON_OUTER_ARMOR);
	}

	public IronSkeletonRenderer(EntityRendererProvider.Context context,
                                ModelLayerLocation modelLocation, ModelLayerLocation innerArmor, ModelLayerLocation outerArmor) {

		super(context, new IronSkeletonModel<>(context.bakeLayer(modelLocation)), 0.6F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new IronSkeletonModel(context.bakeLayer(innerArmor)),
				new IronSkeletonModel(context.bakeLayer(outerArmor)),
						context.getModelManager()));
		this.scale = 1.05F;
	}

	@Override
	protected void scale(IronSkeleton ironSkeleton, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(IronSkeleton entity) {
		return TEXTURE;
	}
}
