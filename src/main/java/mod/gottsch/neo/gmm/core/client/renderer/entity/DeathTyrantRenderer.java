package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.DeathTyrantModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.DeathTyrantEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.DeathTyrant;
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
public class DeathTyrantRenderer<T extends DeathTyrant> extends MobRenderer<T, DeathTyrantModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/death_tyrant.png");
	private final float scale;

	public DeathTyrantRenderer(EntityRendererProvider.Context context) {
		super(context, new DeathTyrantModel<>(context.bakeLayer(DeathTyrantModel.LAYER_LOCATION)), 0.8F);
		this.addLayer(new DeathTyrantEyeLayer<>(this));
		this.scale = 2.00F; // makes the body approx 6-9 in diameter
	}

	@Override
	protected void scale(DeathTyrant deathTyrant, PoseStack pose, float scale) {
		pose.scale(this.scale, this.scale, this.scale);
	}

	@Override
	public ResourceLocation getTextureLocation(DeathTyrant entity) {
		return TEXTURE;
	}
}
