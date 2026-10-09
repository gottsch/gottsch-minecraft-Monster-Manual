package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.DaemonModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.DaemonEyeLayer;
import mod.gottsch.neo.gmm.core.entity.monster.Daemon;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public class DaemonRenderer<T extends Daemon> extends MobRenderer<T, DaemonModel<T>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/daemon.png");

	public DaemonRenderer(EntityRendererProvider.Context context) {
        super(context, new DaemonModel<>(context.bakeLayer(DaemonModel.LAYER_LOCATION)), 0F);
        this.addLayer(new DaemonEyeLayer<>(this));
    }

     @Override
    public ResourceLocation getTextureLocation(Daemon entity) {
        return TEXTURE;
    }
}
