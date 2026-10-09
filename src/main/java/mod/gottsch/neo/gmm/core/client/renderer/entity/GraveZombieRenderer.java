package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.GraveZombieModel;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.GraveZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Grave Zombie on the vanilla {@link ModelLayers#ZOMBIE} rig (dirt-caked recolor texture,
 * no new layer definition) via {@link GraveZombieModel}, which swaps in a "digging" arms-up pose for
 * the surfacing/reburrowing telegraph — see that class for the animation itself. This renderer's only
 * job is feeding it the partial-tick-interpolated rise amount each frame, the same pattern
 * {@link BloodyBonesRenderer} uses for {@code BloodyBonesModel#collapse}.
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class GraveZombieRenderer<T extends GraveZombie> extends HumanoidMobRenderer<T, GraveZombieModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/grave_zombie.png");

	public GraveZombieRenderer(EntityRendererProvider.Context context) {
		super(context, new GraveZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
		this.addLayer(
				new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
						new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
						context.getModelManager()));
	}

	@Override
	protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotYaw, float partialTicks) {
		super.setupRotations(entity, poseStack, ageInTicks, rotYaw, partialTicks);

		// hand the rise amount to the model -- it drives the arms-up-to-shamble blend + shudder in
		// setupAnim (which runs after this).
		this.getModel().riseAmount = entity.getRiseAmount(partialTicks);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return TEXTURE;
	}
}
