package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.plant.VioletFungus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The Violet Fungus rig: reuses {@link ShriekerModel}'s exact hub+3-stalk+cap geometry verbatim (delegates
 * straight to {@link ShriekerModel#createBodyLayer()}) rather than duplicating it -- **and, per an
 * in-game test round, this is now the permanent design, not a placeholder**: the two mobs are meant to be
 * visually identical at a glance (a meta-disguise -- players learn "Shriekers are noisy but harmless,"
 * and an identical-looking Violet Fungus punishes that habit; see the catalog's "Family: Plants" entry
 * for the full reasoning). Model geometry is deliberately never going to diverge from Shrieker's; only
 * the texture differs (smaller pore holes, same palette). Deliberately a sibling class rather than a
 * generic subclass of {@code ShriekerModel<T extends Shrieker>} -- that type bound is
 * {@code Shrieker}-specific, and {@code VioletFungus} isn't a {@code Shrieker} -- but it reuses
 * {@link ShriekerModel#applySway} directly (public/static, no entity-type dependency) rather than
 * duplicating the oscillation math.
 *
 * <p>Sways on the same proximity-based schedule as Shrieker's own pulse-sway -- {@code VioletFungus}
 * keeps the two mobs' idle behavior matching too, another point of the disguise -- driven by
 * {@code VioletFungus#getSwayIntensity()} instead of Shrieker's own accessor.
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class VioletFungusModel<T extends VioletFungus> extends EntityModel<T> {
    public static final String MODEL_NAME = "violet_fungus_model";
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

    private final ModelPart hub;
    private final ModelPart stalk1;
    private final ModelPart stalk2;
    private final ModelPart stalk3;

    private final float baseStalk1XRot, baseStalk1ZRot;
    private final float baseStalk2XRot, baseStalk2ZRot;
    private final float baseStalk3XRot, baseStalk3ZRot;

    public VioletFungusModel(ModelPart root) {
        this.hub = root.getChild("hub");
        this.stalk1 = hub.getChild("stalk1");
        this.stalk2 = hub.getChild("stalk2");
        this.stalk3 = hub.getChild("stalk3");

        this.baseStalk1XRot = stalk1.xRot;
        this.baseStalk1ZRot = stalk1.zRot;
        this.baseStalk2XRot = stalk2.xRot;
        this.baseStalk2ZRot = stalk2.zRot;
        this.baseStalk3XRot = stalk3.xRot;
        this.baseStalk3ZRot = stalk3.zRot;
    }

    /** Reuses Shrieker's geometry verbatim -- see the class doc. */
    public static LayerDefinition createBodyLayer() {
        return ShriekerModel.createBodyLayer();
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float intensity = entity.getSwayIntensity();
        ShriekerModel.applySway(stalk1, baseStalk1XRot, baseStalk1ZRot, ageInTicks, 0.0F, intensity);
        ShriekerModel.applySway(stalk2, baseStalk2XRot, baseStalk2ZRot, ageInTicks, 2.1F, intensity);
        ShriekerModel.applySway(stalk3, baseStalk3XRot, baseStalk3ZRot, ageInTicks, 4.2F, intensity);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        hub.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
