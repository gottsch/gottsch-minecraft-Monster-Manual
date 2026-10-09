package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.OchreJelly;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Reuses vanilla's {@link SlimeModel} geometry, same trick as {@code GelatinousCubeRenderer} —
 * {@link SlimeModel} is generic over any {@code Entity}. The texture is a distinct yellow-ochre
 * recolor of vanilla's {@code slime.png} (a different hue/saturation than the Cube's tan/amber, so the
 * two oozes read as different mobs at a glance). Fixed scale — {@link OchreJelly} has no size-config
 * mechanism (its children are already smaller via a reduced max-health attribute, not a visual scale).
 *
 * @author Mark Gottschling on 7/8/2026
 */
public class OchreJellyRenderer extends MobRenderer<OchreJelly, SlimeModel<OchreJelly>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/ochre_jelly.png");
    // Big Slime (full-sized) renders at ~2.04 blocks; this keeps the jelly well under that.
    private static final float SCALE = 1.8F;

    public OchreJellyRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F * SCALE);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
    }

    @Override
    protected void scale(OchreJelly entity, PoseStack poseStack, float partialTick) {
        // gentle idle pulse in place of the jump-triggered squish it never plays
        float pulse = 1.0F + 0.03F * Mth.sin((entity.tickCount + partialTick) * 0.1F);
        poseStack.scale(SCALE * pulse, SCALE / pulse, SCALE * pulse);
    }

    @Override
    public ResourceLocation getTextureLocation(OchreJelly entity) {
        return TEXTURE;
    }
}
