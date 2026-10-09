package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.plant.Shrieker;
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

/**
 * The Shrieker rig: a net-new bespoke plant model (GMM's first) -- three mushroom clusters (stalk + a
 * two-tier cap + a 4-cube root "feet" flare) radiating from a shared hub. **Ported directly from the
 * user's own Blockbench build** (`Shrieker.bbmodel`, `modded_entity` format) via Blockbench's own
 * "Java Class" export codec, not hand-derived -- that format applies a Y-flip during compile that's easy
 * to get wrong porting by hand, so the geometry below is Blockbench's own generated coordinates, only
 * renamed/reorganized to fit this class's existing conventions (field names, the sway hook). The
 * dead-transform wrapper group Blockbench's own outliner grouping produced (`strieker2`, offset/rotation
 * both zero) was flattened away -- `stalk2`/`feet2` are direct children of `hub` here instead, a
 * zero-behavior-change simplification since that wrapper contributed no transform of its own.
 *
 * <p>Every stalk's own {@link PartPose} pivot sits at its base (where it meets the hub), not its center,
 * per the user's explicit design ask -- rotating a stalk therefore reads as it swaying/bending from where
 * it's rooted. The cap groups on this build are each individually tilted a few degrees off-axis
 * (baked into their own {@code PartPose} rotation, not animated) for an organic, not-perfectly-vertical
 * lean -- a detail from the user's own Blockbench pass, preserved verbatim.
 *
 * <p>Texture is still placeholder-quality (procedurally generated to match this geometry's actual UV
 * layout, not hand-painted) -- geometry is real, art is not, same "ship the mechanic first" arc every
 * other mob in this family has followed.
 *
 * <p>Stalks are still by default, swaying only while {@code Shrieker} is pulsing -- since it's meant to
 * read as inert scenery otherwise. {@link #setupAnim} always <em>sets</em> an absolute rotation (never
 * {@code +=}), per the {@code setupAnim}-runs-per-render-frame lesson {@code BodakModel}'s own head-jerk
 * bug already established, but the sway/rest transition itself is a smooth scale, not a boolean branch:
 * {@code Shrieker#getSwayIntensity()} (0..1) multiplies the oscillation directly, and the server eases
 * that value from 1 down to 0 over a short fixed window once a pulse ends rather than snapping the
 * synced flag straight to false -- so the stalks visibly settle back to their base pose instead of
 * cutting instantly (round-2 correction: the first pass gated the oscillation behind
 * {@code isAlerting()}, a hard boolean, which read as an abrupt snap the moment the pulse window closed).
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class ShriekerModel<T extends Shrieker> extends EntityModel<T> {
    public static final String MODEL_NAME = "shrieker_model";
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, MODEL_NAME), "main");

    private static final float SWAY_SPEED = 0.35F;
    private static final float SWAY_AMOUNT = 0.26F; // ~15 degrees

    private final ModelPart hub;
    private final ModelPart stalk1;
    private final ModelPart cap1;
    private final ModelPart feet1;
    private final ModelPart stalk2;
    private final ModelPart cap2;
    private final ModelPart feet2;
    private final ModelPart stalk3;
    private final ModelPart cap3;
    private final ModelPart feet3;

    // Base (baked) xRot/zRot for each swaying trunk, captured at construction time -- stalk2 in
    // particular ships with a deliberate ~2.5 degree lean baked into its own PartPose. The sway must
    // rotate *around* that baked pose, not overwrite it back to dead-vertical when idle -- see
    // applySway's doc.
    private final float baseStalk1XRot, baseStalk1ZRot;
    private final float baseStalk2XRot, baseStalk2ZRot;
    private final float baseStalk3XRot, baseStalk3ZRot;

    public ShriekerModel(ModelPart root) {
        this.hub = root.getChild("hub");
        this.stalk1 = hub.getChild("stalk1");
        this.cap1 = stalk1.getChild("cap");
        this.feet1 = stalk1.getChild("feet");
        this.stalk2 = hub.getChild("stalk2");
        this.cap2 = stalk2.getChild("cap2");
        this.feet2 = hub.getChild("feet2");
        this.stalk3 = hub.getChild("stalk3");
        this.cap3 = stalk3.getChild("cap3");
        this.feet3 = stalk3.getChild("feet3");

        this.baseStalk1XRot = stalk1.xRot;
        this.baseStalk1ZRot = stalk1.zRot;
        this.baseStalk2XRot = stalk2.xRot;
        this.baseStalk2ZRot = stalk2.zRot;
        this.baseStalk3XRot = stalk3.xRot;
        this.baseStalk3ZRot = stalk3.zRot;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition hub = partdefinition.addOrReplaceChild("hub", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition stalk1 = hub.addOrReplaceChild("stalk1", CubeListBuilder.create().texOffs(42, 35).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(5.0F, 0.0F, -5.0F));

        stalk1.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(33, 17).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

        stalk1.addOrReplaceChild("feet", CubeListBuilder.create().texOffs(42, 45).addBox(-3.0F, -2.0F, -2.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(35, 50).addBox(1.0F, -2.0F, -2.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(17, 47).addBox(-3.0F, -2.0F, 1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(26, 47).addBox(1.0F, -2.0F, 1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition stalk3 = hub.addOrReplaceChild("stalk3", CubeListBuilder.create().texOffs(0, 43).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(2.0F, 0.0F, 5.0F));

        stalk3.addOrReplaceChild("cap3", CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(33, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.0873F, 0.0F, -0.0873F));

        stalk3.addOrReplaceChild("feet3", CubeListBuilder.create().texOffs(0, 53).addBox(-3.0F, -2.0F, -2.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(53, 50).addBox(1.0F, -2.0F, -2.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(35, 55).addBox(-3.0F, -2.0F, 1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(44, 55).addBox(1.0F, -2.0F, 1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));

        // "strieker2" in the Blockbench source was a dead-transform wrapper (offset/rotation both zero)
        // around stalk2/feet2 -- flattened away, both are direct children of hub here (see class doc).
        PartDefinition stalk2 = hub.addOrReplaceChild("stalk2", CubeListBuilder.create().texOffs(25, 35).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 7.0F, 4.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-7.0F, 0.0F, -2.0F, 0.0436F, 0.0F, 0.0F));

        stalk2.addOrReplaceChild("cap2", CubeListBuilder.create().texOffs(33, 26).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(0, 17).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -7.0F, 0.0F));

        hub.addOrReplaceChild("feet2", CubeListBuilder.create().texOffs(44, 50).addBox(-10.0F, -2.0F, -4.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(51, 45).addBox(-6.0F, -2.0F, -4.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(17, 52).addBox(-10.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(26, 52).addBox(-6.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float intensity = entity.getSwayIntensity();
        applySway(stalk1, baseStalk1XRot, baseStalk1ZRot, ageInTicks, 0.0F, intensity);
        applySway(stalk2, baseStalk2XRot, baseStalk2ZRot, ageInTicks, 2.1F, intensity);
        applySway(stalk3, baseStalk3XRot, baseStalk3ZRot, ageInTicks, 4.2F, intensity);
    }

    /**
     * Always sets an absolute rotation -- see the class doc for why. Rotates around each stalk's own
     * baked base pose (captured in the constructor) rather than a hardcoded zero, so a stalk with a
     * deliberate rig-authored lean (e.g. {@code stalk2}) rights itself back to *that* lean, not to
     * dead-vertical. {@code intensity} (0..1, from {@code Shrieker#getSwayIntensity()}) scales the
     * oscillation directly rather than gating it behind a boolean branch -- as the server eases intensity
     * down from 1 to 0 after a pulse ends, the oscillation's amplitude shrinks smoothly to nothing right
     * along with it, so the stalk visibly settles to rest instead of snapping the instant the pulse ends.
     *
     * <p>Public/static so {@code VioletFungusModel} can reuse the exact same oscillation math on its own
     * (separately baked, but geometrically identical) stalk parts -- it can't subclass this model (its
     * generic bound is {@code Shrieker}-specific), but the math itself has no such dependency.
     */
    public static void applySway(ModelPart stalk, float baseXRot, float baseZRot, float ageInTicks, float phaseOffset, float intensity) {
        stalk.xRot = baseXRot + Mth.sin((ageInTicks + phaseOffset) * SWAY_SPEED) * SWAY_AMOUNT * intensity;
        stalk.zRot = baseZRot + Mth.cos((ageInTicks + phaseOffset) * SWAY_SPEED * 0.8F) * SWAY_AMOUNT * 0.6F * intensity;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        hub.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
