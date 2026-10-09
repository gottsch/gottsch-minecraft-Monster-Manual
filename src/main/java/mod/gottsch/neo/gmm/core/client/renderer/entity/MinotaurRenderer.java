package mod.gottsch.neo.gmm.core.client.renderer.entity;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.model.MinotaurModel;
import mod.gottsch.neo.gmm.core.entity.monster.Minotaur;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Minotaur. {@link HumanoidMobRenderer} is used rather than a plain {@code MobRenderer}
 * purely for the layers it brings -- chiefly the {@code ItemInHandLayer} that draws the weapon
 * {@code Minotaur#populateDefaultEquipmentSlots} rolls into the mainhand. It is applicable here
 * only because {@link MinotaurModel} extends {@code HumanoidModel}; the placement of that item is
 * the model's {@code translateToHand} override, not anything this class does.
 *
 * <p>No {@code HumanoidArmorLayer} is added (as with {@code OrcRenderer}): vanilla-shaped armor
 * has nothing to sit on over a bull-shouldered rig. The Minotaur equips no armor as a result.
 *
 * @author Mark Gottschling on 9/7/2026
 */
public class MinotaurRenderer<T extends Minotaur> extends HumanoidMobRenderer<T, MinotaurModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "textures/entity/minotaur.png");

    public MinotaurRenderer(EntityRendererProvider.Context context) {
        super(context, new MinotaurModel<>(context.bakeLayer(MinotaurModel.LAYER_LOCATION)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
