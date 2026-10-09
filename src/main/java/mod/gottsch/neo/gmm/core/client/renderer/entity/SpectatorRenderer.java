package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.SpectatorModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.SpectatorEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Spectator;
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
public class SpectatorRenderer<T extends Spectator> extends MobRenderer<T, SpectatorModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/spectator.png");
	private final float scale;

	public SpectatorRenderer(EntityRendererProvider.Context context) {
		super(context, new SpectatorModel<>(context.bakeLayer(SpectatorModel.LAYER_LOCATION)), 0.8F);
		this.addLayer(new SpectatorEyeLayer<>(this));
		this.scale = 0.75F; // makes the body approx 6-9 in diameter
	}

	@Override
	protected void scale(Spectator spectator, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(Spectator entity) {
		return TEXTURE;
	}
}
