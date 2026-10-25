package mod.gottsch.forge.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.entity.monster.Leech;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * A segmented leech, 13px long, head toward -z: oral sucker, front segment, swollen middle, back
 * segment and the wide tail sucker it clings with. Hand-authored (no Blockbench source).
 *
 * <p>Swimming, the body undulates side to side, the wave travelling head to tail. Attached, it lies
 * still and pulses: the middle swells and relaxes as it feeds. Growth between feeding stages is a
 * renderer scale, not a model change.</p>
 *
 * <p>Geometry is centred on y = 22.5px, so the body's centre is {@link #CENTRE_HEIGHT} above the
 * model's ground; {@code LeechRenderer} uses that to put the body, not the belly, on a socket.</p>
 *
 * @author Mark Gottschling on 10/25/2026
 */
public class LeechModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(GMM.MOD_ID, "leech"), "main");

    /** Height of the body's centre above the model's ground, in blocks. */
    public static final float CENTRE_HEIGHT = 1.5F / 16.0F;

    private final ModelPart body;
    private final ModelPart front;
    private final ModelPart head;
    private final ModelPart back;
    private final ModelPart tail;

    public LeechModel(ModelPart root) {
        this.body = root.getChild("body");
        this.front = this.body.getChild("front");
        this.head = this.front.getChild("head");
        this.back = this.body.getChild("back");
        this.tail = this.back.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 22.5F, 0.0F));
        PartDefinition front = body.addOrReplaceChild("front", CubeListBuilder.create()
                .texOffs(0, 6).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        front.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(12, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.35F)),
                PartPose.offset(0.0F, 0.2F, -2.7F));
        PartDefinition back = body.addOrReplaceChild("back", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 0.0F, 2.0F));
        back.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(-0.3F, -0.5F, -0.2F)),
                PartPose.offset(0.0F, 0.3F, 2.6F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean attached = entity instanceof Leech leech && leech.isAttached();
        if (attached) {
            // still, feeding: the middle swells and relaxes, about once every two seconds
            float pulse = 1.0F + 0.08F * Mth.sin(ageInTicks * 0.15F);
            this.body.xScale = pulse;
            this.body.yScale = pulse;
            this.front.yRot = 0.0F;
            this.back.yRot = 0.0F;
            this.tail.yRot = 0.0F;
            this.head.xRot = 0.15F * Mth.sin(ageInTicks * 0.3F);
            return;
        }
        this.body.xScale = 1.0F;
        this.body.yScale = 1.0F;
        // undulate: a wave travelling head to tail, faster and wider on land, where it thrashes
        float speed = entity.isInWater() ? 0.35F : 0.7F;
        float amplitude = entity.isInWater() ? 0.3F : 0.5F;
        this.front.yRot = amplitude * Mth.sin(ageInTicks * speed);
        this.back.yRot = amplitude * Mth.sin(ageInTicks * speed - 1.2F);
        this.tail.yRot = amplitude * Mth.sin(ageInTicks * speed - 2.4F);
        this.head.xRot = 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
