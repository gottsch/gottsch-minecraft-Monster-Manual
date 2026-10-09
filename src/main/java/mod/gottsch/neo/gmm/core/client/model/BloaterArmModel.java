package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * A single severed-arm cuboid, sized and UV-mapped exactly like the vanilla humanoid/zombie right
 * arm (4x12x4, {@code texOffs(40, 16)}) so {@link mod.gottsch.neo.gmm.core.entity.projectile.BloaterArm}
 * reads as a real torn-off limb rather than generic shrapnel. Renders with the Bloater's own texture
 * (see {@code BloaterArmRenderer}) — no dedicated texture file needed, it samples the same arm
 * region the Bloater's own model already paints. Static geometry; the renderer applies the tumble.
 *
 * @author Mark Gottschling on 7/14/2026
 */
public class BloaterArmModel extends EntityModel<Entity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bloater_arm"), "main");

    private final ModelPart arm;

    public BloaterArmModel(ModelPart root) {
        this.arm = root.getChild("arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
