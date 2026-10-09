
package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.AlligatorGarModel;
import mod.gottsch.neo.gmm.core.entity.monster.AlligatorGar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 1, 2022
 *
 */
public class AlligatorGarRenderer<T extends AlligatorGar> extends MobRenderer<T, AlligatorGarModel<T>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/alligator_gar.png");
    private ResourceLocation texture = TEXTURE;

	/**
	 *
	 * @param context
	 */
	public AlligatorGarRenderer(EntityRendererProvider.Context context) {
        super(context, new AlligatorGarModel<>(context.bakeLayer(AlligatorGarModel.LAYER_LOCATION)), 0.1F);
    }

    public AlligatorGarRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new AlligatorGarModel<>(context.bakeLayer(AlligatorGarModel.LAYER_LOCATION)), 0.1F);
        this.texture = texture;
    }


    @Override
    public ResourceLocation getTextureLocation(AlligatorGar entity) {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }
}
