package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.GrayOoze;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Reuses vanilla's {@link SlimeModel} geometry like the other oozes, but its appearance interpolates
 * with {@link GrayOoze#getRevealProgress()} instead of being fixed: fully hidden (0) it's squashed to
 * barely poke above the ground (less than a pressure plate — see {@link #MIN_HEIGHT_SCALE}) and wears
 * {@code gray_ooze_dormant.png}, a real crop of vanilla's own {@code stone.png} rather than a recolor,
 * masked onto every UV face of {@link SlimeModel} — since it's squashed nearly flat, only the top face
 * reads anyway, so a uniform stone fill (rather than needing to know the model's exact per-face UV
 * layout) is enough to look like an actual stone tile. Fully revealed (1) it's the normal ooze cube
 * proportions in the glossier {@code gray_ooze.png}. Width is left unchanged at every reveal stage —
 * {@link GrayOoze} also snaps its own facing to a cardinal direction while hidden (see that class), so
 * together this reads as a flush, axis-aligned stone tile rather than a squashed blob.
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class GrayOozeRenderer extends MobRenderer<GrayOoze, SlimeModel<GrayOoze>> {
    private static final ResourceLocation DORMANT_TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/gray_ooze_dormant.png");
    private static final ResourceLocation ACTIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/gray_ooze.png");
    // Big Slime (full-sized) renders at ~2.04 blocks; this keeps the ooze well under that when revealed.
    private static final float SCALE = 1.8F;
    // native model height is 0.5 block; 0.5 * SCALE * 0.05 =~ 0.045 block, under a pressure plate's 0.0625
    private static final float MIN_HEIGHT_SCALE = 0.05F;

    public GrayOozeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F * SCALE);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
    }

    @Override
    protected void scale(GrayOoze entity, PoseStack poseStack, float partialTick) {
        float heightScale = Mth.lerp(entity.getRevealProgress(), MIN_HEIGHT_SCALE, 1.0F);
        poseStack.scale(SCALE, SCALE * heightScale, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(GrayOoze entity) {
        return entity.getRevealProgress() <= 0.0F ? DORMANT_TEXTURE : ACTIVE_TEXTURE;
    }
}
