package mod.gottsch.neo.gmm.core.client.renderer.entity.layer;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.DaemonModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
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
public class DaemonEyeLayer<T extends Entity, M extends DaemonModel<T>> extends EyesLayer<T, M> {
	private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID,"textures/entity/layer/daemon_eyes.png"));

	public DaemonEyeLayer(RenderLayerParent<T, M> layer) {
		super(layer);
	}

	public RenderType renderType() {
		return EYES;
	}

}
