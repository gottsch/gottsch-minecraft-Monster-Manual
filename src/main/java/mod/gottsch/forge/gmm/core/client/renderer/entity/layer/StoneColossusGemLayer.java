package mod.gottsch.forge.gmm.core.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.client.model.StoneColossusModel;
import mod.gottsch.forge.gmm.core.entity.monster.IDormantMob;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The colossus's chest and belt gems, lit full-bright.
 *
 * <h2>Why this is gated and the other eye layers are not</h2>
 * <p>Every other {@link EyesLayer} in GMM draws unconditionally, because those mobs are always
 * awake. This one is <strong>the tell for dormancy</strong>: the whole point of a statue is that a
 * player should be able to look across a room and know whether the stone figure in the corner is
 * scenery or a boss that has already noticed them. Dark gems say scenery; the gems coming alight
 * as the thing stands up is the warning. Drawing them while dormant would give the ambush away,
 * and drawing them <em>only</em> while dormant would be backwards.
 *
 * <p>So: nothing is drawn while {@link IDormantMob#isDormant()}, and the gems are lit from the
 * first tick of the rise onward. It deliberately does not fade in with the wake blend &mdash;
 * {@link RenderType#eyes} has no alpha to animate, and a hard light-up on the tick the mob comes
 * round reads as the eyes opening rather than as a pop.
 *
 * @author Mark Gottschling on 9/14/2026
 */
@OnlyIn(Dist.CLIENT)
public class StoneColossusGemLayer<T extends Mob, M extends StoneColossusModel<T>> extends EyesLayer<T, M> {
    private static final RenderType GEMS =
            RenderType.eyes(new ResourceLocation(GMM.MOD_ID, "textures/entity/layer/stone_colossus_gems.png"));

    public StoneColossusGemLayer(RenderLayerParent<T, M> layer) {
        super(layer);
    }

    @Override
    public RenderType renderType() {
        return GEMS;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity instanceof IDormantMob dormant && dormant.isDormant()) {
            return;
        }
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                partialTicks, ageInTicks, netHeadYaw, headPitch);
    }
}
