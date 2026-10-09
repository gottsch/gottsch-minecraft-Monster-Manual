package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.ShriekerModel;
import mod.gottsch.neo.gmm.core.entity.monster.plant.Shrieker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on 7/10/2026
 *
 * @param <T>
 */
public class ShriekerRenderer<T extends Shrieker> extends MobRenderer<T, ShriekerModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/shrieker.png");

    public ShriekerRenderer(EntityRendererProvider.Context context) {
        super(context, new ShriekerModel<>(context.bakeLayer(ShriekerModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
