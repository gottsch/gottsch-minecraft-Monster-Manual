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
 * Bone-fragment models for the {@link mod.gottsch.neo.gmm.core.entity.projectile.BoneShard}
 * projectile. Several shape {@link #LAYERS variants} (a flat chip, a skinnier/longer splinter, a
 * stubbier chunk); the shard's synced variant selects one. Static geometry — the renderer applies
 * the tumble.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class BoneShardModel extends EntityModel<Entity> {

    /** One baked layer per shard shape variant (order matches BoneShard variant indices). */
    public static final ModelLayerLocation[] LAYERS = {
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bone_shard_0"), "main"),
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bone_shard_1"), "main"),
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bone_shard_2"), "main"),
    };

    // per-variant {width, height, length}
    private static final float[][] DIMS = {
            {2.0F, 1.0F, 5.0F},  // flat chip
            {1.0F, 1.0F, 7.0F},  // skinnier + longer splinter
            {3.0F, 1.0F, 4.0F},  // stubbier chunk
    };

    private final ModelPart root;

    public BoneShardModel(ModelPart root) {
        this.root = root.getChild("root");
    }

    public static LayerDefinition createBodyLayer(int variant) {
        float w = DIMS[variant][0];
        float h = DIMS[variant][1];
        float d = DIMS[variant][2];
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("root",
                CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2.0F, -h / 2.0F, -d / 2.0F, w, h, d),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
