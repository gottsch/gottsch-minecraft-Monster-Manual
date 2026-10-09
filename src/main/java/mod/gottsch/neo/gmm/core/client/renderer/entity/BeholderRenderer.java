package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BeholderModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.BeholderEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Beholder;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public class BeholderRenderer<T extends Beholder> extends MobRenderer<T, BeholderModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/beholder.png");

	private final float scale;

	public BeholderRenderer(EntityRendererProvider.Context context) {
		super(context, new BeholderModel<>(context.bakeLayer(BeholderModel.LAYER_LOCATION)), 0.8F);
		this.addLayer(new BeholderEyeLayer<>(this));
		this.scale = 2.00F; // makes the body approx 6-9 in diameter
	}

	@Override
	protected void scale(Beholder beholder, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(Beholder entity) {
		return TEXTURE;
	}
}
