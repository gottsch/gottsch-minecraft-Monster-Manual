package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.SkeletonChampionModel;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonChampion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Skeleton Champion with the {@link SkeletonChampionModel} (plated elite-skeleton rig,
 * armor baked as static child cubes of the main body), plus an {@link ItemInHandLayer} so the
 * tag-rolled diamond/netherite blade {@code SkeletonChampion#populateDefaultEquipmentSlots} equips
 * is actually visible — this variant wears no separate equippable armor layer (the plate is baked
 * into the texture/rig itself). A visual-only {@link #scale} (same idiom as
 * {@code IronSkeletonRenderer}) makes the elite pack leader read as physically bigger than the
 * rank-and-file skeletons it rallies — DD's {@code ModEntities} bounding box is widened to match.
 *
 * @author Mark Gottschling on 7/12/2026
 */
public class SkeletonChampionRenderer<T extends SkeletonChampion> extends MobRenderer<T, SkeletonChampionModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/skeleton_champion.png");

	private final float scale = 1.2F;

	public SkeletonChampionRenderer(EntityRendererProvider.Context context) {
		super(context, new SkeletonChampionModel<>(context.bakeLayer(SkeletonChampionModel.LAYER_LOCATION)), 0.5F);
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	protected void scale(T entity, PoseStack pose, float partialTick) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
