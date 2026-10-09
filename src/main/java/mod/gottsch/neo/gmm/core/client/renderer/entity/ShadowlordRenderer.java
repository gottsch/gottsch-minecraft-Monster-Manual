package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.ShadowlordModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.ShadowlordEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.Shadowlord;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public class ShadowlordRenderer<T extends Shadowlord> extends HumanoidMobRenderer<T, ShadowlordModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/shadowlord_2.png");

	private final float scale;

	public ShadowlordRenderer(EntityRendererProvider.Context context) {
		super(context, new ShadowlordModel<>(context.bakeLayer(ShadowlordModel.LAYER_LOCATION)), 0F);

		this.addLayer(new ShadowlordEyeLayer<>(this));
		// TODO make another 'eye' layer for the trim on the robes, but not full bright.
		this.scale = 1.25F; // makes the body approx 3 blocks in height
	}

	@Override
	protected void scale(T shadowlord, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
