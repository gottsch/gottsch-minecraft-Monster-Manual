package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.HeadlessModel;
import mod.gottsch.neo.gmm.core.entity.monster.Headless;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Mark Gottschling on Apr 1, 2022
 */
public class HeadlessRenderer<T extends Headless> extends MobRenderer<T, HeadlessModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/headless.png");

	public HeadlessRenderer(EntityRendererProvider.Context context) {
        super(context, new HeadlessModel<>(context.bakeLayer(HeadlessModel.LAYER_LOCATION)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(Headless entity) {
        return TEXTURE;
    }
}
