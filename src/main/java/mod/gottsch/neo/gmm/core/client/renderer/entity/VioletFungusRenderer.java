package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.VioletFungusModel;
import mod.gottsch.neo.gmm.core.entity.monster.plant.VioletFungus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on 7/10/2026
 *
 * @param <T>
 */
public class VioletFungusRenderer<T extends VioletFungus> extends MobRenderer<T, VioletFungusModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/violet_fungus.png");

    public VioletFungusRenderer(EntityRendererProvider.Context context) {
        super(context, new VioletFungusModel<>(context.bakeLayer(VioletFungusModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
