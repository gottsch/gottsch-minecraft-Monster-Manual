package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.AnimatedWeaponModel;
import mod.gottsch.neo.gmm.core.entity.monster.construct.AnimatedWeapon;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Animated Weapon with {@link AnimatedWeaponModel} (a body-less rig — see that class's
 * doc) plus an {@link ItemInHandLayer} so the equipped weapon actually shows. The base model's texture
 * is never sampled (nothing in the model has any geometry), so it points at the same shared
 * {@code blank.png} {@link AnimatedArmorRenderer} uses.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class AnimatedWeaponRenderer<T extends AnimatedWeapon> extends MobRenderer<T, AnimatedWeaponModel<T>> {
	private static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/blank.png");

	public AnimatedWeaponRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedWeaponModel<>(context.bakeLayer(AnimatedWeaponModel.LAYER_LOCATION)), 0.3F);
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotYaw, float partialTicks) {
		super.setupRotations(entity, poseStack, ageInTicks, rotYaw, partialTicks);

		// hand the partial-tick-interpolated windup blend to the model -- it drives the
		// engaged-pose-to-raised-strike-pose lerp in setupAnim (which runs after this).
		this.getModel().windupProgress = entity.getWindupProgress(partialTicks);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return BLANK;
	}
}
