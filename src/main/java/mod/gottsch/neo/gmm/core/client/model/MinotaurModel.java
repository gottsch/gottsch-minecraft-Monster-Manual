package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.IChargingMob;
import net.minecraft.client.model.HumanoidModel;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;

/**
 * Minotaur model -- a bull-headed brute with a digitigrade (hoofed) leg and a two-segment arm
 * ending in an oversized fist.
 *
 * <p><b>Why it extends {@link HumanoidModel} rather than a bare {@code EntityModel}.</b> Everything
 * a weapon-carrying humanoid needs -- the walk cycle, the idle arm bob, the weapon swing on
 * {@code attackTime}, the bow/crossbow/spyglass arm poses, the riding and swimming poses -- is
 * already written in {@code HumanoidModel#setupAnim}. Rather than reimplement it, this rig
 * registers the seven vanilla part names ({@code head}, {@code hat}, {@code body},
 * {@code right_arm}, {@code left_arm}, {@code right_leg}, {@code left_leg}) as <b>empty</b>
 * {@code CubeListBuilder}s, lets {@code super.setupAnim} pose them, and then copies the resulting
 * rotations onto the minotaur parts. The empty parts carry no cubes, so they render nothing and
 * cost nothing -- unlike {@link OrcModel}, which registers real vanilla cubes and then has to
 * remember to set {@code visible = false} on every one of them.
 *
 * <p><b>Held items.</b> The inherited {@code translateToHand} walks only the vanilla arm part,
 * which here is empty and sits at the root -- an item rendered off it would float at the
 * minotaur's feet. {@link #translateToHand} is overridden to walk the real part chain instead; see
 * that method for the offset arithmetic.
 *
 * <p><b>Mesh provenance.</b> {@link #createBodyLayer()} is the Blockbench export verbatim
 * (part names, spelling and all -- {@code lowerRightArm2}, {@code rigthFoot}) so that a re-export
 * from {@code MinotaurModel.bbmodel} diffs cleanly against it. The only additions are the seven
 * empty vanilla parts at the top.
 *
 * @author Mark Gottschling on 9/7/2026
 */
public class MinotaurModel<T extends Mob> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "minotaur"), "main");

    /** Baked-in torso lean from the export; the vanilla body pose is added on top of it. */
    private static final float BODY_BASE_X_ROT = 0.1745F;
    /** Baked-in thigh tilt that gives the leg its digitigrade set; the walk swing adds to it. */
    private static final float LEG_BASE_X_ROT = -0.48F;
    /**
     * How much of the thigh swing the foot gives back. The digitigrade leg is one rigid tilted
     * chain here (only the thigh is animated), so without this the hoof pitches with every stride
     * and the minotaur walks on its toes, then its heels. Half-cancelling at the foot keeps the
     * hoof roughly level through the stride without needing a real two-bone IK pass.
     */
    private static final float HOOF_LEVELLING = 0.5F;
    /**
     * Head pitch clamp, in degrees. The muzzle is slung well forward of the neck pivot (the snout
     * cube reaches z -14), so an unclamped downward look drives it through the chest.
     */
    private static final float HEAD_PITCH_MIN = -20.0F;
    private static final float HEAD_PITCH_MAX = 25.0F;
    /**
     * The head does not track like a vanilla humanoid's. A bull skull with a metre of horn through
     * it, sat on a thick neck, should not whip around to follow a player the way a zombie's does --
     * at full vanilla yaw this rig reads as a head on a swivel. Both axes are damped and the yaw is
     * clamped as well, so the head leads a turn and the body follows, rather than the head doing all
     * the work. The mob still faces its target: {@code LookAtPlayerGoal} turns the whole entity.
     */
    private static final float HEAD_YAW_LIMIT = 55.0F;
    private static final float HEAD_YAW_DAMPING = 0.6F;
    private static final float HEAD_PITCH_DAMPING = 0.7F;

    /** Rest rotations of the two tail segments, from the export; the motion is added on top. */
    private static final float TAIL_BASE_X_ROT = 0.3927F;
    private static final float TAIL_LOWER_BASE_X_ROT = 0.2182F;
    /** Standing-still swish: slow, shallow, and driven by ageInTicks so it never fully stops. */
    private static final float TAIL_IDLE_SPEED = 0.08F;
    private static final float TAIL_IDLE_SWAY = 0.18F;
    /**
     * Walk swing. Driven off {@code limbSwing} at the same frequency as the legs so the tail is
     * locked to the stride rather than drifting against it, and scaled by {@code limbSwingAmount}
     * so it fades out to the idle swish when the mob stops.
     */
    private static final float TAIL_STRIDE_SPEED = 0.6662F;
    private static final float TAIL_STRIDE_SWAY = 0.35F;
    /** How far the tail lifts away from the body as the mob picks up speed. */
    private static final float TAIL_LIFT = 0.25F;
    /**
     * Phase, in radians, by which the lower segment trails the base -- the whole reason the tail
     * has two animated joints. Swing both in step and it reads as one stiff rod; let the tip arrive
     * late and it reads as something with weight on the end of it.
     */
    private static final float TAIL_LAG = 0.9F;
    /** How much of that lagged angle the lower segment actually takes up. */
    private static final float TAIL_FOLLOW = 0.6F;
    /**
     * The charge pose, applied while {@code IChargingMob.isCharging()}. Head dropped and body
     * pitched forward so the horns lead -- this IS the telegraph {@code ChargeAttackGoal}'s windup
     * buys, and without it the mob just stands still for a second and then teleports at you.
     */
    private static final float CHARGE_HEAD_PITCH = 0.55F;
    private static final float CHARGE_BODY_PITCH = 0.30F;
    /** Ticks to blend into and out of the charge pose, so the head drops rather than snaps. */
    private static final float CHARGE_POSE_BLEND = 0.18F;

    /** A single hard lash across the swing of an attack -- a bull swishes when it is agitated. */
    private static final float TAIL_ATTACK_FLICK = 0.3F;
    /**
     * Hard limit on the tail's total sideways swing, because the tail hangs between the thighs and
     * there is not much room down there. Measured against the rig rather than guessed: sweeping both
     * tail segments through every angle and testing their corners inside each thigh's own local box.
     *
     * <p>Re-measured after the 2026-09-07 re-export moved the tail pivot 1 unit further back, which
     * bought a lot: contact was at 0.42 rad standing / 0.70 running before, and is 0.69 / 0.68 now.
     * The split between standing and running existed only because a standing mob's legs were in the
     * way; with the tail sitting behind them that no longer holds, so this is a single number again.
     *
     * <p>It still earns its place -- idle sway + stride + attack flick compose to 0.83 rad, so
     * without the clamp the tail would lash through a thigh at the moment all three peak together.
     * Clamping the composed value rather than shrinking each term keeps the motions readable.
     *
     * <p>Clipping the LOINCLOTH is explicitly accepted (Mark, 2026-09-07) and is not measured here.
     */
    private static final float TAIL_YAW_LIMIT = 0.60F;

    private final ModelPart minotaur;
    private final ModelPart minoBody;
    private final ModelPart minoHead;
    private final ModelPart upperTorso;
    private final ModelPart minoLeftArm;
    private final ModelPart minoLowerLeftArm;
    private final ModelPart minoRightArm;
    private final ModelPart minoLowerRightArm;
    private final ModelPart minoLeftLeg;
    private final ModelPart minoLeftFoot;
    private final ModelPart minoRightLeg;
    private final ModelPart minoRightFoot;
    private final ModelPart tail;
    private final ModelPart lowerTail;
    /** Eased 0..1 toward the charge pose; per-model-instance, which is fine -- a model instance is
     *  shared across entities of the type, but setupAnim runs immediately before each render, so
     *  the value is always re-derived for the entity being drawn before it is used. */
    private float chargePose;

    public MinotaurModel(ModelPart root) {
        super(root);
        this.minotaur = root.getChild("minotaur");
        this.minoBody = this.minotaur.getChild("body");
        this.minoHead = this.minoBody.getChild("head");
        this.upperTorso = this.minoBody.getChild("upperTorso");
        this.minoLeftArm = this.upperTorso.getChild("leftArm");
        this.minoLowerLeftArm = this.minoLeftArm.getChild("lowerLeftArm");
        this.minoRightArm = this.upperTorso.getChild("rightArm");
        this.minoLowerRightArm = this.minoRightArm.getChild("lowerRightArm2");
        this.minoLeftLeg = this.minotaur.getChild("leftLeg");
        this.minoLeftFoot = this.minoLeftLeg.getChild("leftLowerLeg").getChild("leftFoot");
        this.minoRightLeg = this.minotaur.getChild("rightLeg");
        this.minoRightFoot = this.minoRightLeg.getChild("rightLowerLeg").getChild("rigthFoot");
        this.tail = this.minotaur.getChild("tail");
        this.lowerTail = this.tail.getChild("lowerTail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // The seven part names the HumanoidModel constructor demands. Registered with no cubes:
        // they exist purely so super.setupAnim() has somewhere to write the vanilla pose that
        // setupAnim() below reads back out. Nothing here ever renders.
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        ///////// Blockbench export below this line /////////


        PartDefinition minotaur = partdefinition.addOrReplaceChild("minotaur", CubeListBuilder.create(), PartPose.offset(2.0F, 7.0F, 0.0F));

        PartDefinition body = minotaur.addOrReplaceChild("body", CubeListBuilder.create().texOffs(29, 31).addBox(-4.0F, -5.0F, -2.5F, 8.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(17, 52).addBox(-3.0F, 1.0F, -2.0F, 6.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -1.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -9.0F, -9.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(29, 42).addBox(-2.0F, -6.0F, -14.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
        .texOffs(0, 44).addBox(-3.5F, -10.0F, -8.0F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(45, 64).addBox(-2.0F, -8.0F, -10.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.5F, 3.0F));

        PartDefinition hair1_r1 = head.addOrReplaceChild("hair1_r1", CubeListBuilder.create().texOffs(58, 26).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -8.0F, -2.5F, 0.6545F, 0.0F, 0.0F));

        PartDefinition mainHorn = head.addOrReplaceChild("mainHorn", CubeListBuilder.create().texOffs(0, 15).addBox(-12.5F, -2.6F, -1.3F, 20.0F, 2.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offset(3.0F, -7.0F, -6.2F));

        PartDefinition leftHorn = mainHorn.addOrReplaceChild("leftHorn", CubeListBuilder.create().texOffs(58, 18).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.5F, -1.6F, -0.3F, -0.2618F, -0.1309F, 0.0F));

        PartDefinition rightHorn = mainHorn.addOrReplaceChild("rightHorn", CubeListBuilder.create().texOffs(58, 18).mirror().addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-11.5F, -1.6F, -0.3F, -0.2618F, 0.1309F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(29, 20).addBox(-4.0F, 0.0F, -5.0F, 9.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.0F, 0.5F, 0.48F, 0.0F, 0.0F));

        PartDefinition hair3_r1 = neck.addOrReplaceChild("hair3_r1", CubeListBuilder.create().texOffs(56, 8).addBox(-6.0F, 0.0F, 0.0F, 10.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 2.5F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition hair2_r1 = neck.addOrReplaceChild("hair2_r1", CubeListBuilder.create().texOffs(45, 14).addBox(-6.0F, 0.0F, 0.0F, 10.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition upperTorso = body.addOrReplaceChild("upperTorso", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -11.0F, -3.5F, 12.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition hair4_r1 = upperTorso.addOrReplaceChild("hair4_r1", CubeListBuilder.create().texOffs(32, 64).addBox(-2.0F, 0.0F, 0.0F, 6.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -11.0F, 3.5F, 0.3054F, 0.0F, 0.0F));

        PartDefinition leftArm = upperTorso.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(50, 42).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(7.0F, -8.0F, 0.0F));

        PartDefinition lowerLeftArm = leftArm.addOrReplaceChild("lowerLeftArm", CubeListBuilder.create().texOffs(0, 51).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(55, 55).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(1.0F, 6.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

        PartDefinition rightArm = upperTorso.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(50, 42).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offset(-9.0F, -8.0F, 0.0F));

        PartDefinition lowerRightArm2 = rightArm.addOrReplaceChild("lowerRightArm2", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-2.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
        .texOffs(55, 55).mirror().addBox(-2.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offsetAndRotation(1.0F, 6.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

        PartDefinition loinCloth = minotaur.addOrReplaceChild("loinCloth", CubeListBuilder.create().texOffs(0, 35).addBox(-4.5F, -14.0F, -2.5F, 9.0F, 3.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offset(-2.0F, 12.0F, 0.0F));

        PartDefinition frontCloth = loinCloth.addOrReplaceChild("frontCloth", CubeListBuilder.create().texOffs(17, 59).addBox(-3.5F, 0.1F, 0.05F, 7.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.0F, -2.7F, -0.1309F, 0.0F, 0.0F));

        PartDefinition backCloth = loinCloth.addOrReplaceChild("backCloth", CubeListBuilder.create().texOffs(0, 64).addBox(-3.5F, 0.0F, 2.5F, 7.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition leftLeg = minotaur.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(39, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition leftLowerLeg = leftLeg.addOrReplaceChild("leftLowerLeg", CubeListBuilder.create().texOffs(38, 55).addBox(-1.5F, 9.0F, -2.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition leftFoot = leftLowerLeg.addOrReplaceChild("leftFoot", CubeListBuilder.create().texOffs(56, 31).addBox(-2.0F, -3.0F, 4.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 12.0F, -1.0F));

        PartDefinition leftHoof = leftFoot.addOrReplaceChild("leftHoof", CubeListBuilder.create().texOffs(56, 0).addBox(-1.5F, -0.5F, -6.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-1.0F, 1.0F, 7.0F, 0.48F, 0.0F, 0.0F));

        PartDefinition rightLeg = minotaur.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(39, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.5F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition rightLowerLeg = rightLeg.addOrReplaceChild("rightLowerLeg", CubeListBuilder.create().texOffs(38, 55).mirror().addBox(-1.5F, 9.0F, -2.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition rigthFoot = rightLowerLeg.addOrReplaceChild("rigthFoot", CubeListBuilder.create().texOffs(56, 31).mirror().addBox(-2.0F, -3.0F, 4.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.5F, 12.0F, -1.0F));

        PartDefinition rightHoof = rigthFoot.addOrReplaceChild("rightHoof", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.5F, -0.5F, -6.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 1.0F, 7.0F, 0.48F, 0.0F, 0.0F));

        PartDefinition tail = minotaur.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, 1.0F, 3.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition upperTail = tail.addOrReplaceChild("upperTail", CubeListBuilder.create().texOffs(15, 66).addBox(-1.0F, -10.0F, 3.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 8.5412F, -4.3066F));

        PartDefinition lowerTail = tail.addOrReplaceChild("lowerTail", CubeListBuilder.create().texOffs(20, 66).addBox(0.0F, -4.25F, -0.1F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 64).addBox(-0.5F, -1.0F, -0.6F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 10.5412F, -0.3066F, 0.2182F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // let HumanoidModel pose the (empty) vanilla parts: walk cycle, idle arm bob, weapon swing
        // off attackTime, bow/crossbow/riding/swimming poses -- then read the result back out.
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // head -- damped and clamped rather than tracking one-for-one; see HEAD_YAW_DAMPING.
        // Taken from netHeadYaw/headPitch directly instead of from the vanilla head part, so the
        // damping is not applied on top of a pose something else may already have adjusted.
        this.minoHead.yRot = Mth.clamp(netHeadYaw, -HEAD_YAW_LIMIT, HEAD_YAW_LIMIT)
                * HEAD_YAW_DAMPING * ((float) Math.PI / 180F);
        this.minoHead.xRot = Mth.clamp(headPitch, HEAD_PITCH_MIN, HEAD_PITCH_MAX)
                * HEAD_PITCH_DAMPING * ((float) Math.PI / 180F);

        // torso -- the lean from the export is the rest pose, the vanilla pose is added on top
        this.minoBody.xRot = BODY_BASE_X_ROT + this.body.xRot;
        this.minoBody.yRot = this.body.yRot;

        // arms. The minotaur uppers pivot at the shoulder and hang down exactly as the vanilla arms
        // do, so the vanilla rotations transfer one-for-one. The forearms keep their static elbow
        // bend from the export.
        this.minoRightArm.xRot = this.rightArm.xRot;
        this.minoRightArm.yRot = this.rightArm.yRot;
        this.minoRightArm.zRot = this.rightArm.zRot;
        this.minoLeftArm.xRot = this.leftArm.xRot;
        this.minoLeftArm.yRot = this.leftArm.yRot;
        this.minoLeftArm.zRot = this.leftArm.zRot;

        // legs -- swing added to the digitigrade rest tilt, hoof half-levelled at the foot
        this.minoRightLeg.xRot = LEG_BASE_X_ROT + this.rightLeg.xRot;
        this.minoLeftLeg.xRot = LEG_BASE_X_ROT + this.leftLeg.xRot;
        this.minoRightFoot.xRot = -this.rightLeg.xRot * HOOF_LEVELLING;
        this.minoLeftFoot.xRot = -this.leftLeg.xRot * HOOF_LEVELLING;

        setupChargePose(entity);

        setupTailAnimation(limbSwing, limbSwingAmount, ageInTicks);
    }

    /**
     * Drops the head and pitches the body forward while the mob is charging, blended in and out so
     * the transition reads as the animal setting itself rather than a pose snap. Applied AFTER the
     * normal head/body pose above, because it deliberately overrides the look-at.
     */
    private void setupChargePose(T entity) {
        boolean charging = entity instanceof IChargingMob charger && charger.isCharging();
        this.chargePose = Mth.clamp(this.chargePose + (charging ? CHARGE_POSE_BLEND : -CHARGE_POSE_BLEND), 0.0F, 1.0F);
        if (this.chargePose <= 0.0F) {
            return;
        }
        this.minoHead.xRot = Mth.lerp(this.chargePose, this.minoHead.xRot, CHARGE_HEAD_PITCH);
        this.minoHead.yRot = Mth.lerp(this.chargePose, this.minoHead.yRot, 0.0F);
        this.minoBody.xRot = Mth.lerp(this.chargePose, this.minoBody.xRot, BODY_BASE_X_ROT + CHARGE_BODY_PITCH);
    }

    /**
     * Two-segment tail: a base that swishes when idle and swings with the stride when moving, and a
     * lower segment that arrives late.
     *
     * <p>The lag is the point. {@code lowerTail} is a CHILD of {@code tail}, so its rotation is
     * relative to whatever the base is already doing -- setting both to the same curve would just
     * double the angle and still read as one rigid rod. Instead the lower segment is given the
     * angle the base held {@link #TAIL_LAG} radians of phase ago, minus the base's current angle,
     * which is the relative rotation that puts it where a trailing joint would actually be.
     */
    private void setupTailAnimation(float limbSwing, float limbSwingAmount, float ageInTicks) {
        float idle = Mth.cos(ageInTicks * TAIL_IDLE_SPEED) * TAIL_IDLE_SWAY;
        float stride = Mth.cos(limbSwing * TAIL_STRIDE_SPEED) * TAIL_STRIDE_SWAY * limbSwingAmount;
        float baseYaw = idle + stride;

        if (this.attackTime > 0.0F) {
            baseYaw += Mth.sin(this.attackTime * (float) Math.PI) * TAIL_ATTACK_FLICK;
        }

        baseYaw = Mth.clamp(baseYaw, -TAIL_YAW_LIMIT, TAIL_YAW_LIMIT);

        this.tail.yRot = baseYaw;
        this.tail.xRot = TAIL_BASE_X_ROT - limbSwingAmount * TAIL_LIFT;

        // the same two curves, rewound by TAIL_LAG, then expressed relative to the parent
        float laggedYaw = Mth.cos(ageInTicks * TAIL_IDLE_SPEED - TAIL_LAG) * TAIL_IDLE_SWAY
                + Mth.cos(limbSwing * TAIL_STRIDE_SPEED - TAIL_LAG) * TAIL_STRIDE_SWAY * limbSwingAmount;
        this.lowerTail.yRot = (laggedYaw - baseYaw) * TAIL_FOLLOW;
        this.lowerTail.xRot = TAIL_LOWER_BASE_X_ROT;
    }

    /**
     * Positions the {@code PoseStack} at the mainhand/offhand fist so {@code ItemInHandLayer} can
     * render the equipped weapon there.
     *
     * <p>The inherited {@code HumanoidModel} version applies only the vanilla arm part's own local
     * transform, which works for vanilla because that arm is a direct child of an unoffset root.
     * Here the real arm is five deep -- {@code minotaur > body > upperTorso > *Arm > lower*Arm} --
     * and the vanilla arm is an empty stub at the root, so the inherited version would drop the
     * whole chain and render the weapon at the minotaur's feet. Walking the chain by hand
     * reconstructs the same composed transform {@code minotaur.render()} builds.
     *
     * <p>The trailing nudge lines the item up with the fist. {@code ItemInHandLayer} places the
     * item, in the frame this method leaves behind, at {@code (-1, 10, -2)} for the right hand and
     * {@code (+1, 10, -2)} for the left -- the bottom-front-centre of a <i>vanilla</i> 4x12x4 arm
     * whose pivot is at the shoulder. This forearm is only 8 long, with the fist cube spanning
     * y 4..8, z -2..2, so its bottom-front-centre is {@code (0, 8, -2)}: two units higher than
     * vanilla aims, and one unit off to the side of it. Hence {@code (+/-1, -2, 0)}.
     */
    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        boolean right = side == HumanoidArm.RIGHT;
        this.minotaur.translateAndRotate(poseStack);
        this.minoBody.translateAndRotate(poseStack);
        this.upperTorso.translateAndRotate(poseStack);
        (right ? this.minoRightArm : this.minoLeftArm).translateAndRotate(poseStack);
        (right ? this.minoLowerRightArm : this.minoLowerLeftArm).translateAndRotate(poseStack);
        poseStack.translate((right ? 1.0F : -1.0F) / 16.0F, -2.0F / 16.0F, 0.0F);
    }

    /**
     * Renders the minotaur rig only. {@code super.renderToBuffer} is deliberately not called: the
     * seven vanilla parts hold no cubes, so it would walk them for nothing.
     */
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.minotaur.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** The bull head, not the empty vanilla stub -- so {@code CustomHeadLayer} has something real. */
    @Override
    public ModelPart getHead() {
        return this.minoHead;
    }
}
