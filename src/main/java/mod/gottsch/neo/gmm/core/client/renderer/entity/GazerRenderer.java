package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.GazerModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GazerEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Gazer;

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
public class GazerRenderer<T extends Gazer> extends MobRenderer<T, GazerModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/gazer.png");
	private final float scale;

	public GazerRenderer(EntityRendererProvider.Context context) {
		super(context, new GazerModel<>(context.bakeLayer(GazerModel.LAYER_LOCATION)), 0.8F);
		this.addLayer(new GazerEyeLayer<>(this));
		this.scale = 1F; // makes the body approx 16x16
	}

	@Override
	protected void scale(Gazer gazer, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(Gazer entity) {
		return TEXTURE;
	}
}
