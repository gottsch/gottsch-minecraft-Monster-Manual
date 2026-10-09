package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.OrcModel;
import mod.gottsch.neo.gmm.core.entity.monster.Orc;
import mod.gottsch.neo.gmm.core.entity.monster.OrcWarlord;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Orc Warlord: the orc rig at 1.2x, in the chief's colours.
 *
 * <p>Extends {@link OrcRenderer} and re-bakes {@code OrcModel.LAYER_LOCATION} rather than declaring
 * a layer of its own &mdash; the warlord is an {@link Orc} and uses the orc geometry unchanged, so a
 * second {@code ModelLayerLocation} would be a second thing to register for no difference in what is
 * drawn. It also means a consumer registering this renderer needs no extra layer registration, which
 * is the failure mode that crashes on first sight of the mob rather than at load.</p>
 *
 * <p>1.2x, matching what SkeletonChampion does to a skeleton: the chief has to read as the leader
 * across a room, and colour alone does not carry at distance. {@link OrcWarlord#getStandingEyeHeight}
 * applies the same factor so the eye line follows the render.</p>
 *
 * @author Mark Gottschling on Sep 5, 2026
 */
public class OrcWarlordRenderer extends OrcRenderer<OrcWarlord> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/orc_warlord.png");

	private static final float SCALE = 1.2F;

	public OrcWarlordRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(Orc entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(OrcWarlord entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
		poseStack.scale(SCALE, SCALE, SCALE);
		super.scale(entity, poseStack, partialTick);
	}
}
