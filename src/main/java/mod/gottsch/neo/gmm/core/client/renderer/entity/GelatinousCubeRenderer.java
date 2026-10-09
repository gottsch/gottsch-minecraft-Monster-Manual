package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.GelatinousCube;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Reuses vanilla's {@link SlimeModel} geometry rather than a bespoke model — {@link SlimeModel} is
 * generic over any {@code Entity}, so it isn't tied to the vanilla {@code Slime} class. The texture is
 * a hue-shifted (amber/gold) recolor of vanilla's {@code slime.png} — same UV layout, so it drops onto
 * the reused model without remapping — so it isn't mistaken for a vanilla Slime at a glance.
 * {@link #BASE_SCALE} is the size at {@link GelatinousCube}'s default {@code size} config (1.0) — well
 * under a full-sized ("Big") vanilla Slime — further multiplied by the entity's
 * {@link GelatinousCube#getSizeScale()} so an optional {@code gmm:mob_config} override is reflected
 * both here and in the entity's own hitbox.
 *
 * @author Mark Gottschling on 7/7/2026
 */
public class GelatinousCubeRenderer extends MobRenderer<GelatinousCube, SlimeModel<GelatinousCube>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/gelatinous_cube.png");
    // Big Slime (full-sized) renders at ~2.04 blocks; this keeps the cube well under that.
    private static final float BASE_SCALE = 2.2F;

    public GelatinousCubeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F * BASE_SCALE);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
    }

    @Override
    public void render(GelatinousCube entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = 0.25F * BASE_SCALE * entity.getSizeScale();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(GelatinousCube entity, PoseStack poseStack, float partialTick) {
        float scale = BASE_SCALE * entity.getSizeScale();
        // gentle idle pulse in place of the jump-triggered squish it never plays
        float pulse = 1.0F + 0.03F * Mth.sin((entity.tickCount + partialTick) * 0.1F);
        poseStack.scale(scale * pulse, scale / pulse, scale * pulse);
    }

    @Override
    public ResourceLocation getTextureLocation(GelatinousCube entity) {
        return TEXTURE;
    }
}
