package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.WoodGolemModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.GMMEyesLayer;
import mod.gottsch.neo.gmm.core.entity.monster.construct.WoodGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders Wood Golem on its ported {@link WoodGolemModel} rig. The walking side-to-side lean (ported
 * from Treasure2's {@code WitherwoodGolemRenderer#setupRotations}) is kept here rather than folded into
 * the model itself -- it rotates the whole {@code PoseStack}, not a single {@code ModelPart}, so it
 * belongs at the renderer level same as vanilla's own golem/strider lean effects.
 *
 * @author Mark Gottschling
 */
public class WoodGolemRenderer<T extends WoodGolem> extends MobRenderer<T, WoodGolemModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/wood_golem.png");
    private static final ResourceLocation EYES_TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/layer/wood_golem_eyes.png");

    public WoodGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new WoodGolemModel<>(context.bakeLayer(WoodGolemModel.LAYER_LOCATION)), 0.7F);
        this.addLayer(new GMMEyesLayer<>(this, EYES_TEXTURE));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(T golem, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(golem, poseStack, ageInTicks, rotationYaw, partialTicks);
        if (!(golem.walkAnimation.speed() < 0.01D)) {
            float f1 = golem.walkAnimation.position(partialTicks) + 6.0F;
            float f2 = (Math.abs(f1 % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * f2));
        }
    }
}
