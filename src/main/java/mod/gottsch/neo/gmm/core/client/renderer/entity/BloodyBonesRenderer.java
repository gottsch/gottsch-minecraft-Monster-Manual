package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BloodyBonesModel;
import mod.gottsch.neo.gmm.core.client.model.SkeletonWarriorModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BloodyBones;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders Bloody Bones on the shared vanilla-skeleton rig ({@link BloodyBonesModel}, bloody recolor
 * texture) with full-brightness red eyes. The collapse (limbs flung off, only the skull left, dropping
 * to the ground) and the resurrect (parts made visible again and raised back up from the ground) are
 * both done in the model via part visibility + y-position; this renderer just feeds it the collapse
 * amount. Reuses {@link SkeletonWarriorModel#LAYER_LOCATION} — no new layer definition.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class BloodyBonesRenderer<T extends BloodyBones> extends HumanoidMobRenderer<T, BloodyBonesModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/bloody_skeleton.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/bloody_skeleton_eyes.png");

    public BloodyBonesRenderer(EntityRendererProvider.Context context) {
        super(context, new BloodyBonesModel<>(context.bakeLayer(SkeletonWarriorModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(
                new HumanoidArmorLayer<>(this, new SkeletonWarriorModel(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                        new SkeletonWarriorModel(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                        context.getModelManager()));
        this.addLayer(new GMMEyesLayer<>(this, EYES));
    }

    @Override
    protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotYaw, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotYaw, partialTicks, scale);

        // hand the collapse amount to the model — it drives the skull drop / whole-body rise in
        // setupAnim (which runs after this) via part visibility + y-position.
        this.getModel().collapse = entity.getCollapseAmount(partialTicks);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
