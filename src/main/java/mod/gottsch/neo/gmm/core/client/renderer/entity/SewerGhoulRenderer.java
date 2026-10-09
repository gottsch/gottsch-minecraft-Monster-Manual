
package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.SewerGhoulModel;
import mod.gottsch.neo.gmm.core.entity.monster.ghoul.SewerGhoul;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on11/6/2025
 *
 */
public class SewerGhoulRenderer<T extends SewerGhoul> extends MobRenderer<T, SewerGhoulModel<T>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/sewer_ghoul.png");
    private ResourceLocation texture = TEXTURE;
    private float scale = 1.0F;

	/**
	 *
	 * @param context
	 */
	public SewerGhoulRenderer(EntityRendererProvider.Context context) {
        super(context, new SewerGhoulModel<>(context.bakeLayer(SewerGhoulModel.LAYER_LOCATION)), 0.1F);
    }

    public SewerGhoulRenderer(EntityRendererProvider.Context context, float scale) {
        this(context);
        this.scale = scale;
    }

    public SewerGhoulRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new SewerGhoulModel<>(context.bakeLayer(SewerGhoulModel.LAYER_LOCATION)), 0.1F);
        this.texture = texture;
    }

    @Override
    protected void scale(SewerGhoul mob, PoseStack pose, float scale) {
        pose.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(SewerGhoul entity) {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public float getScale() {
        return scale;
    }
}
