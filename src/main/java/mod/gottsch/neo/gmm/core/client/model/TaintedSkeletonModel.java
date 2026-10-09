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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Tainted Skeleton model (authored in Blockbench). <b>Rebuilt 2026-07-12</b> from a genuinely new
 * Blockbench rig -- every limb is now two jointed segments (e.g. {@code rightArm}/{@code lowerRightArm})
 * with its own baked bend, and the torso is split into {@code upperBody} (the ribcage, where the taint
 * shard is fused) and {@code body} (the pelvis, a child of {@code upperBody}) -- a proper hunched,
 * crooked pose baked into nearly every part's own rotation, not just the head/arm tilts the previous
 * flat-limb rig had. {@link #setupAnim} only ever <em>adds</em> animated deltas on top of each part's
 * captured base rotation (never overwrites), so the decay pose survives everywhere, and only animates
 * the <em>upper</em> segment of each limb -- the lower segment is a child, so it swings along for free
 * via the normal parent-child transform while still keeping its own baked elbow/knee bend on top.
 *
 * <p>UV layout is unchanged from the previous rig despite the restructure -- {@code texOffs(16,16)}
 * (ribcage) now belongs to {@code upperBody} instead of the old flat {@code body}, and
 * {@code texOffs(17,30)} (pelvis) now belongs to {@code body} instead of the old {@code lower_body}, but
 * both still map to the same visual regions of {@code tainted_skeleton.png} -- the shard-glow/eye-glow
 * texture work from this same session needed no changes at all.
 *
 * <p>Melees via {@code MeleeAttackGoal}; {@link #setupAttackAnimation} (added 2026-07-12, carried
 * forward from the flat-limb rig, retargeted to {@code rightArm}) is deliberately a minimal,
 * rotation-only swing rather than a full port of vanilla's position-shifting version -- that formula's
 * arm x/z sign conventions assume vanilla's own pivot layout, which still doesn't hold for this rig's
 * pivots. Right-arm-only, since Tainted Skeleton has no bow to switch on.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class TaintedSkeletonModel<T extends Mob> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "tainted_skeleton_model"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart upperBody;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart lowerRightArm;
    private final ModelPart leftArm;
    private final ModelPart lowerLeftArm;
    private final ModelPart shard;
    private final ModelPart shard2;
    private final ModelPart rightLeg;
    private final ModelPart lowerRightLeg;
    private final ModelPart leftLeg;
    private final ModelPart lowerLeftLeg;

    // baked decay-pose rotations captured at construction time -- setupAnim adds onto these, never
    // overwrites, so the hunched/crooked pose survives every frame (see class doc).
    private final float baseHeadXRot;
    private final float baseRightArmXRot;
    private final float baseLeftArmXRot;
    private final float baseRightLegXRot;
    private final float baseLeftLegXRot;

    public TaintedSkeletonModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.upperBody = this.body.getChild("upperBody");
        this.head = this.upperBody.getChild("head");
        this.rightArm = this.upperBody.getChild("rightArm");
        this.lowerRightArm = this.rightArm.getChild("lowerRightArm");
        this.leftArm = this.upperBody.getChild("leftArm");
        this.lowerLeftArm = this.leftArm.getChild("lowerLeftArm");
        this.shard = this.upperBody.getChild("shard");
        this.shard2 = this.shard.getChild("shard2");
        this.rightLeg = this.root.getChild("rightLeg");
        this.lowerRightLeg = this.rightLeg.getChild("lowerRightLeg");
        this.leftLeg = this.root.getChild("leftLeg");
        this.lowerLeftLeg = this.leftLeg.getChild("lowerLeftLeg");

        this.baseHeadXRot = this.head.xRot;
        this.baseRightArmXRot = this.rightArm.xRot;
        this.baseLeftArmXRot = this.leftArm.xRot;
        this.baseRightLegXRot = this.rightLeg.xRot;
        this.baseLeftLegXRot = this.leftLeg.xRot;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 26.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(17, 30).addBox(-4.0F, -5.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.0F, 0.0F, 0.1309F, 0.0F, 0.0F));

        PartDefinition upperBody = body.addOrReplaceChild("upperBody", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, -7.2F, -4.2F, 8.0F, 7.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, 0.2618F, 0.0F, 0.0F));

        PartDefinition head = upperBody.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(0.0F, -7.0F, -2.0F, -0.1745F, 0.0F, 0.0F));

        PartDefinition rightArm = upperBody.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -6.0F, -2.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition lowerRightArm = rightArm.addOrReplaceChild("lowerRightArm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.6545F, 0.0F, 0.0F));

        PartDefinition leftArm = upperBody.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.0F, -5.0F, -2.0F, -0.4363F, 0.0F, 0.0F));

        PartDefinition lowerLeftArm = leftArm.addOrReplaceChild("lowerLeftArm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

        PartDefinition shard = upperBody.addOrReplaceChild("shard", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.0F, -2.5F, -0.7854F, 0.0F, 0.0F));

        PartDefinition shard2 = shard.addOrReplaceChild("shard2", CubeListBuilder.create().texOffs(0, 31).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -14.0F, 0.0F, 0.1309F, 0.0F, 0.0F));

        PartDefinition lowerRightLeg = rightLeg.addOrReplaceChild("lowerRightLeg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, -14.0F, 0.0F, -0.3054F, 0.0F, 0.0F));

        PartDefinition lowerLeftLeg = leftLeg.addOrReplaceChild("lowerLeftLeg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    // Walk-swing amplitudes deliberately much smaller than the old flat-limb rig's (and than vanilla's).
    // This rig ships with the arms already posed forward/bent by the artist (baked xRot ~ -0.48) plus a
    // jointed elbow child, so the old full-size vanilla swing (~1.0 rad on arms, 1.4 on legs) shoved the
    // already-raised arms far past a natural range and the elbow child compounded it into a flail (the
    // 2026-07-12 "arms are messed up" bug -- proven to be animation, not geometry, by a no-op setupAnim
    // rendering the baked pose correctly). Everything is still *added* onto each captured base rotation,
    // so at rest (limbSwingAmount -> 0) the pose is exactly the baked one.
    private static final float ARM_SWING = 0.35F;   // subtle sway that preserves the reaching pose
    private static final float LEG_SWING = 0.9F;    // legs are near-straight, can take a fuller stride

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // head look -- add onto the baked forward-hunch tilt rather than overwrite it
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = this.baseHeadXRot + headPitch * ((float) Math.PI / 180F);

        // walk: swing only the upper arm/leg segments -- each lower segment is a child, so it swings
        // along for free via the normal parent-child transform while keeping its own baked elbow/knee
        // bend on top; no separate animation needed for the lower segments at all.
        this.rightArm.xRot = this.baseRightArmXRot + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * ARM_SWING * limbSwingAmount;
        this.leftArm.xRot = this.baseLeftArmXRot + Mth.cos(limbSwing * 0.6662F) * ARM_SWING * limbSwingAmount;

        this.rightLeg.xRot = this.baseRightLegXRot + Mth.cos(limbSwing * 0.6662F) * LEG_SWING * limbSwingAmount;
        this.leftLeg.xRot = this.baseLeftLegXRot + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * LEG_SWING * limbSwingAmount;

        // pulsating taint shard — grows and shrinks like a beating heart
        float pulse = 1.0F + 0.15F * Mth.sin(ageInTicks * 0.3F);
        this.shard2.xScale = pulse;
        this.shard2.yScale = pulse;
        this.shard2.zScale = pulse;

        setupAttackAnimation();
    }

    /**
     * A rotation-only two-arm attack lunge -- see the class doc for why this is deliberately not a
     * full port of vanilla's position-shifting version. Applied additively on top of the walk-cycle
     * xRot already set above, so it composes rather than overwrites, and naturally falls back to the
     * plain walk pose once {@code attackTime} decays to 0 (nothing left "stuck").
     *
     * <p><b>Timing curve corrected 2026-07-12 (round 2, "I don't see any animation"):</b> the first
     * pass kept vanilla's eased curve ({@code sin(pi * (1-(1-t)^4))}), which peaks at ~16% through the
     * swing -- inside the first 1-2 ticks of a 6-tick swing -- then decays; combined with a halved
     * amplitude on a single axis (vanilla compensates with ~1.2 rad across xRot+zRot+body-yRot), the
     * whole jab was an invisible sub-tenth-of-a-second flick. Replaced with a plain symmetric
     * {@code sin(t * pi)} pulse (peak mid-swing, meaningful displacement across every tick of the
     * window) at a larger amplitude, on <em>both</em> arms (off-hand at 0.7x) -- reads as a two-handed
     * lunging claw, fitting a mob whose whole design is rushing you down to detonate. Peak right-arm
     * total is ~-79 degrees (baked -27.5 plus the pulse) -- thrust forward/up, still well short of
     * flipping overhead.
     */
    private static final float ATTACK_JAB = 0.9F;

    private void setupAttackAnimation() {
        if (this.attackTime <= 0.0F) {
            return;
        }
        float swing = Mth.sin(this.attackTime * (float) Math.PI);
        // negative xRot rotates the already-forward arms further out/up into the lunge
        this.rightArm.xRot -= swing * ATTACK_JAB;
        this.leftArm.xRot -= swing * ATTACK_JAB * 0.7F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
