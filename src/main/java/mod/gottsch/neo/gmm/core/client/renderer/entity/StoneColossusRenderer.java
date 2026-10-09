package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.StoneColossusModel;
import mod.gottsch.neo.gmm.core.client.renderer.entity.layer.StoneColossusGemLayer;
import mod.gottsch.neo.gmm.core.entity.monster.StoneColossus;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Stone Colossus <strong>at the size its rig was authored</strong> (44px, i.e. 2.75
 * blocks), applying no scale of its own. GMM registers no entities and owns no hitboxes, so the
 * size a consumer draws it at is the consumer's decision -- see Dungeons2's
 * {@code ScaledStoneColossusRenderer}, and {@code MinotaurRenderer} for the same split.
 *
 * <p>{@link HumanoidMobRenderer} rather than a plain {@code MobRenderer} because
 * {@link StoneColossusModel} extends {@code HumanoidModel} for its animation math. The colossus
 * carries no weapon, so the {@code ItemInHandLayer} it brings has nothing to draw -- and
 * deliberately no {@code HumanoidArmorLayer}: vanilla-shaped armour has nothing to sit on over a
 * rig this proportioned, the same call {@code OrcRenderer} and {@code MinotaurRenderer} make.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class StoneColossusRenderer<T extends StoneColossus> extends HumanoidMobRenderer<T, StoneColossusModel<T>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/stone_colossus.png");

    public StoneColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new StoneColossusModel<>(context.bakeLayer(StoneColossusModel.LAYER_LOCATION)), 0.9F);
        // The dormancy tell: dark gems while it is scenery, lit from the first tick of the rise.
        this.addLayer(new StoneColossusGemLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
