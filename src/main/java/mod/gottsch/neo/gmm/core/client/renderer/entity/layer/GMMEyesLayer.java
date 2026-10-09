package mod.gottsch.neo.gmm.core.client.renderer.entity.layer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Reusable full-brightness eye overlay for any GMM mob whose emissive detail lives in a separate
 * {@code layer/<name>_eyes.png} texture (transparent except the glowing pixels). Unlike the per-mob
 * eye layers (e.g. {@link GazerEyeLayer}), the texture is supplied per instance, so a variant's
 * renderer wires glowing eyes in one line — {@code addLayer(new GMMEyesLayer<>(this, EYES))} — with
 * no bespoke subclass. Rendering uses vanilla {@link EyesLayer}'s full-bright pass.
 *
 * @author Mark Gottschling on 7/3/2026
 */
@OnlyIn(Dist.CLIENT)
public class GMMEyesLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
	private final RenderType renderType;

	public GMMEyesLayer(RenderLayerParent<T, M> parent, ResourceLocation eyesTexture) {
		super(parent);
		this.renderType = RenderType.eyes(eyesTexture);
	}

	@Override
	public RenderType renderType() {
		return renderType;
	}
}
