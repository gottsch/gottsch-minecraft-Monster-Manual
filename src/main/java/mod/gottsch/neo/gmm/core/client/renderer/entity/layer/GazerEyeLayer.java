package mod.gottsch.neo.gmm.core.client.renderer.entity.layer;


import net.minecraft.util.FastColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.GazerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 * @param <M>
 */
@OnlyIn(Dist.CLIENT)
public class GazerEyeLayer<T extends Entity, M extends GazerModel<T>> extends EyesLayer<T, M> {
	private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID,"textures/entity/layer/gazer_eyes.png"));

	public GazerEyeLayer(RenderLayerParent<T, M> layer) {
		super(layer);
	}

	public RenderType renderType() {
		return EYES;
	}

	public void render(PoseStack p_116983_, MultiBufferSource p_116984_, int p_116985_, T p_116986_, float p_116987_, float p_116988_, float p_116989_, float p_116990_, float p_116991_, float p_116992_) {
		VertexConsumer vertexconsumer = p_116984_.getBuffer(this.renderType());
		this.getParentModel().renderToBuffer(p_116983_, vertexconsumer, 15728640, OverlayTexture.NO_OVERLAY,FastColor.ARGB32.colorFromFloat(1.0F, 0.5F, 0.5F, 0.5F));
	}
}
