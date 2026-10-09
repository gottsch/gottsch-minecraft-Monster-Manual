package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.BlackPuddingModel;
import mod.gottsch.neo.gmm.core.entity.monster.BlackPudding;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Unlike the rest of the ooze family this renders a dedicated {@link BlackPuddingModel} rather than
 * vanilla's {@code SlimeModel}, and adds no translucent outer layer — a black pudding is opaque.
 * <p>
 * The base scale sits just under {@code OchreJellyRenderer}'s 1.8 and is then multiplied by the
 * entity's own generation-derived scale, so each split generation renders visibly smaller. That is the
 * same value {@code BlackPudding#getDimensions} feeds the hitbox, so what you see and what you can hit
 * stay in agreement.
 *
 * @author Mark Gottschling on 8/24/2026
 */
public class BlackPuddingRenderer extends MobRenderer<BlackPudding, BlackPuddingModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/black_pudding.png");
    // single source of truth: the model needs this too, to convert a world distance into model
    // units when it stretches the pseudopod
    private static final float SCALE = BlackPuddingModel.RENDER_SCALE;

    public BlackPuddingRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackPuddingModel(context.bakeLayer(BlackPuddingModel.LAYER_LOCATION)), 0.25F * SCALE);
    }

    @Override
    protected void scale(BlackPudding entity, PoseStack poseStack, float partialTick) {
        // same gentle idle pulse the rest of the family uses in place of a jump squish, a touch quicker
        // to match how much faster this one moves
        float pulse = 1.0F + 0.03F * Mth.sin((entity.tickCount + partialTick) * 0.14F);
        float scale = SCALE * entity.getSizeScale();
        poseStack.scale(scale * pulse, scale / pulse, scale * pulse);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackPudding entity) {
        return TEXTURE;
    }
}
