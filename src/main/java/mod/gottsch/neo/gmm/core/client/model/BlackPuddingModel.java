package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.client.renderer.TendrilRenderer;
import mod.gottsch.neo.gmm.core.entity.monster.BlackPudding;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A single body cube, built to vanilla slime's exact dimensions ({@code 8x8x8} at {@code -4,16,-4}) so
 * the Black Pudding sits in the ooze family's silhouette alongside {@code GelatinousCube},
 * {@code OchreJelly} and {@code GrayOoze} — which all reuse vanilla's {@code SlimeModel} directly.
 * <p>
 * This mob needs its own model class rather than reusing {@code SlimeModel} for one reason: <b>the
 * lash</b>. It is drawn procedurally by {@link TendrilRenderer} rather than being cuboid geometry,
 * because a stretched {@code ModelPart} can only ever be a straight line and this one whips out along
 * an arc. It appears only while striking, and only on an adult ({@code BlackPudding#hasLash}), so the
 * strike itself is the tell that this pudding out-reaches you.
 * <p>
 * Earlier passes hung extra cuboids off the body — four corner "tendrils", then a pair of trailing
 * slime shapes plus a blob on the leading face. All were tried in game and cut: the tendrils read as
 * legs poking through the floor, and the trailing slime did not read as slime. The mob is
 * deliberately a plain black cube whose only distinguishing feature is what it does, not what it
 * grows. Do not reintroduce decorative geometry without testing it in the client first.
 * <p>
 * No translucent outer layer, unlike the rest of the family: a black pudding is opaque, and vanilla's
 * {@code SlimeOuterLayer} is hard-typed to {@code SlimeModel} anyway.
 *
 * @author Mark Gottschling on 8/24/2026
 */
public class BlackPuddingModel extends EntityModel<BlackPudding> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "black_pudding"), "main");

    /**
     * Render scale for this mob. Lives here rather than in the renderer because the lash maths needs
     * it to convert a world distance into model units — {@code BlackPuddingRenderer} reads it from
     * here so there is a single source of truth.
     */
    public static final float RENDER_SCALE = 1.7F;

    /** Lash shape. Procedural, so these are free parameters rather than modelled geometry. */
    private static final float LASH_START_RADIUS = 1.6F;
    private static final float LASH_END_RADIUS = 0.5F;
    private static final int LASH_SEGMENTS = 10;
    private static final int LASH_SIDES = 5;
    /**
     * Peak bow of the whip, in model units, reached mid-strike and easing to nearly straight at full
     * extension — which is what reads as a crack rather than a pole being pushed out.
     */
    private static final float LASH_ARC = 5.0F;
    /** Normalised UV rect for the lash skin — the 8x8 patch at (32,12) on a 64x32 sheet. */
    private static final float LASH_U0 = 32.0F / 64.0F;
    private static final float LASH_V0 = 12.0F / 32.0F;
    private static final float LASH_U1 = 40.0F / 64.0F;
    private static final float LASH_V1 = 20.0F / 32.0F;
    /** Where the lash leaves the body: centre of the front face. */
    private static final float LASH_ORIGIN_Y = 20.0F;
    private static final float LASH_ORIGIN_Z = -4.0F;
    /** Half the body's width in blocks (4 units * RENDER_SCALE / 16) — where the lash starts. */
    private static final float BODY_HALF_BLOCKS = 4.0F * RENDER_SCALE / 16.0F;
    /** Fraction of the lash spent extending; the rest retracts. 0.25 of 8 ticks = out in 2, back in 6. */
    private static final float EXTEND_FRACTION = 0.25F;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Captured in {@link #prepareMobModel}, which is the only hook handed the real partial tick —
     * {@code setupAnim} receives {@code ageInTicks} instead, and the lash needs sub-tick precision to
     * stay smooth over its 8-tick window.
     */
    private float partialTick;

    private final ModelPart body;

    /**
     * Lash state computed in {@link #setupAnim} and consumed in {@link #renderToBuffer}, which is not
     * handed the entity. Same stash-then-use pattern vanilla uses for {@code attackTime}.
     */
    private boolean lashActive;
    private float lashLength;
    private float lashArc;
    private float lashPitch;
    private float lashYaw;

    public BlackPuddingModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 16.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void prepareMobModel(BlackPudding entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(BlackPudding entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // The body itself is never posed here — the renderer's idle pulse handles its only movement.
        // Everything this model animates is the lash.
        setupLash(entity, netHeadYaw, headPitch);
    }

    /**
     * Drives the lash. No wind-up: the damage has already landed by the time this first runs, so the
     * tube snaps out over {@link #EXTEND_FRACTION} of the window and retracts over the remainder —
     * follow-through that reads as feedback, never as a warning.
     * <p>
     * No aiming maths is needed for yaw: the attack goal turns the mob to face its target and the
     * model renders inside body-rotated space, so -Z already points at the victim. Only the head
     * yaw/pitch deltas are applied on top, for a target above, below, or slightly off-centre.
     */
    private void setupLash(BlackPudding entity, float netHeadYaw, float headPitch) {
        float remaining = entity.getLashProgress(this.partialTick);
        if (remaining <= 0.0F || !entity.hasLash()) {
            lashActive = false;
            return;
        }
        lashActive = true;

        float elapsed = 1.0F - remaining;
        float extension = elapsed < EXTEND_FRACTION
                ? elapsed / EXTEND_FRACTION
                : 1.0F - (elapsed - EXTEND_FRACTION) / (1.0F - EXTEND_FRACTION);
        extension = Mth.clamp(extension, 0.0F, 1.0F);

        // the lash leaves the body's surface, so only the air beyond that has to be covered
        float reachBlocks = Math.max(0.0F, entity.getLashDistance() - BODY_HALF_BLOCKS);
        lashLength = reachBlocks * 16.0F / RENDER_SCALE * extension;
        // bow hardest while it is on its way out and straighten as it lands — a crack, not a pole
        lashArc = LASH_ARC * Mth.sin(extension * Mth.PI);
        lashPitch = headPitch * DEG_TO_RAD;
        lashYaw = netHeadYaw * DEG_TO_RAD;
    }

    /** Emits the lash as procedural geometry in the model's own space. */
    private void renderLash(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                            int color) {
        float cosPitch = Mth.cos(lashPitch);
        Vector3f from = new Vector3f(0.0F, LASH_ORIGIN_Y, LASH_ORIGIN_Z);
        Vector3f to = new Vector3f(
                from.x - Mth.sin(lashYaw) * cosPitch * lashLength,
                from.y + Mth.sin(lashPitch) * lashLength,
                from.z - Mth.cos(lashYaw) * cosPitch * lashLength);

        TendrilRenderer.render(poseStack, consumer, packedLight, packedOverlay, color,
                from, to, lashArc, LASH_START_RADIUS, LASH_END_RADIUS,
                LASH_SEGMENTS, LASH_SIDES, LASH_U0, LASH_V0, LASH_U1, LASH_V1);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        if (lashActive) {
            renderLash(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
    }
}
