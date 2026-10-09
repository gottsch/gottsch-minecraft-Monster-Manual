package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Frost Skeleton model: the shared {@link SkeletonWarriorModel} rig plus ice growths on the skull —
 * a front and back "beard" of ice and four angled icicle spikes rising off the top of the head —
 * added as children of the head part so they ride the vanilla skeleton head animation for free.
 * Uses a 64x64 texture (the base skeleton UVs occupy the top 64x32; the ice cubes are mapped into
 * the freed lower half).
 * <p>
 * Geometry authored in Blockbench (ddenizens-forge-1.20.1-FrostSkeleton.bbmodel) and mirrored here.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class FrostSkeletonModel<T extends Mob> extends SkeletonWarriorModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "frost_skeleton_model"), "main");

    public FrostSkeletonModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition part = mesh.getRoot();

        // thin skeleton arms + legs (identical to SkeletonWarriorModel)
        part.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        part.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        part.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        part.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        PartDefinition head = part.getChild("head");

        // --- skull ice (children of the head; from the Blockbench edit) ---

        // beard: flat ice planes hanging from the front and back of the jaw
        head.addOrReplaceChild("front_beard",
                CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 3.0F, 0.0F), PartPose.ZERO);
        head.addOrReplaceChild("back_beard",
                CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-4.0F, 0.0F, 4.0F, 8.0F, 3.0F, 0.0F), PartPose.ZERO);

        // four angled icicle spikes rising off the top corners of the skull
        head.addOrReplaceChild("left_icicle_1",
                CubeListBuilder.create().texOffs(10, 40).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.9F, -8.0F, -4.0F, -0.3927F, 0.0F, -0.3927F));
        head.addOrReplaceChild("left_icicle_2",
                CubeListBuilder.create().texOffs(10, 40).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.9F, -8.0F, -2.0F, -0.3927F, 0.0F, -0.3927F));
        head.addOrReplaceChild("right_icicle_1",
                CubeListBuilder.create().texOffs(10, 40).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.9F, -8.0F, -4.0F, -0.3927F, 0.0F, 0.3927F));
        head.addOrReplaceChild("right_icicle_3",
                CubeListBuilder.create().texOffs(10, 40).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.9F, -8.0F, 0.0F, -0.3927F, 0.0F, 0.3927F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
