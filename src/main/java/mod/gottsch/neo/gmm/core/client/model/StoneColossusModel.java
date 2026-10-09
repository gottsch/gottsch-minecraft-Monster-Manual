package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.IDormantMob;
import mod.gottsch.neo.gmm.core.entity.monster.IBurstingMob;
import mod.gottsch.neo.gmm.core.entity.monster.IHurlingMob;
import mod.gottsch.neo.gmm.core.entity.monster.ISlammingMob;
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
import net.minecraft.world.entity.Mob;

/**
 * Stone Colossus model -- a tall, lean armoured construct with a two-segment arm ending in a fist,
 * a two-segment leg, and six hanging cloth strips at the belt.
 *
 * <p><b>Why it extends {@link HumanoidModel}.</b> Same reason as {@code MinotaurModel}: the walk
 * cycle, idle arm bob, head tracking and attack swing are already written in
 * {@code HumanoidModel#setupAnim}. The seven vanilla part names are registered as <b>empty</b>
 * cube builders, {@code super.setupAnim} poses them, and the rotations are copied onto the real
 * rig. Empty parts carry no cubes, so they render nothing and cost nothing.
 *
 * <p><b>Mesh provenance.</b> {@link #createBodyLayer()} is the Blockbench export verbatim -- part
 * names, spelling ({@code shouldSpike2}) and all -- so a re-export from
 * {@code StoneColossusModel.bbmodel} diffs cleanly against it. The only additions are the seven
 * empty vanilla parts.
 *
 * <p><b>The rig is 44px tall and is drawn at 7 blocks by the consumer</b>, not here: GMM registers
 * nothing and owns no sizes. See Dungeons2's {@code ScaledStoneColossusRenderer}.
 *
 * @author Mark Gottschling on 9/13/2026
 */
public class StoneColossusModel<T extends Mob> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "stone_colossus"), "main");

    /** Baked-in poses from the export. Animation is applied ON TOP of these, never instead of them. */
    private static final float ARM_BASE_Z_ROT = 0.1309F;
    private static final float ELBOW_BASE_X_ROT = -0.48F;
    private static final float LEG_BASE_Z_ROT = 0.0436F;

    /** How much of the thigh swing the knee gives back, and only on the backswing. */
    private static final float KNEE_FOLLOW = 0.45F;
    /** The arm is heavy: the forearm lags the upper arm rather than travelling with it. */
    private static final float ELBOW_SWING_FOLLOW = 0.25F;

    /** Head tracking is damped -- a head this size does not snap round. */
    private static final float HEAD_TRACK = 0.6F;
    private static final float HEAD_PITCH_MIN = -25.0F;
    private static final float HEAD_PITCH_MAX = 30.0F;

    /** Cloth: a slow idle sway, plus the leg that is actually pushing each strip. */
    private static final float CLOTH_IDLE_SPEED = 0.05F;
    private static final float CLOTH_IDLE_SWAY = 0.06F;
    /**
     * How much of the thigh's swing a strip in front of it takes. The cloths hang off
     * {@code lowerBody} and the legs are its siblings, so nothing connects them but this -- which
     * is why they hung dead still while the mob walked (Mark, 2026-09-13).
     */
    private static final float CLOTH_LEG_PUSH = 0.85F;

    /**
     * THE STATUE POSE. Not "the idle pose with the animation switched off" -- that was the first
     * cut, and it meant a dormant colossus was indistinguishable from an awake one standing still,
     * so the rise appeared to come from nowhere (Mark, 2026-09-13: "the colossus is already
     * standing up, and so the wake up anim looks disjointed"). It now stands head bowed, shoulders
     * forward and forearms drawn up, so the rise has somewhere to rise FROM.
     *
     * <p><strong>The head bow was a pivot bug, not a number bug</strong> (2026-09-13). At 0.42 the
     * helmet swung off the shoulders, because the head's pivot was ten units below the neck --
     * level with the bottom of the chest -- so the box turned on a ten-unit arm. Mark moved the
     * pivot to the neck in the model; the bow is back to a real one because it now rotates where a
     * head rotates. Had it been "fixed" by shrinking the angle instead, the rig would still be
     * wrong and every later head animation would have inherited it.</p>
     */
    private static final float STATUE_HEAD_X_ROT = 0.30F;
    private static final float STATUE_CHEST_X_ROT = 0.12F;
    private static final float STATUE_ARM_X_ROT = -0.12F;
    private static final float STATUE_ARM_Z_EXTRA = 0.09F;
    private static final float STATUE_ELBOW_X_ROT = -0.70F;
    private static final float STATUE_KNEE_X_ROT = 0.08F;

    /** A shoulder roll partway through the rise, so it is not a straight interpolation. */
    private static final float WAKE_SHOULDER_ROLL = 0.30F;

    /**
     * THE SLAM. Fists overhead through the wind-up, then driven into the GROUND on the impact tick.
     *
     * <p><strong>The fall occupies the last {@link #SLAM_FALL_TICKS} of the wind-up, not the
     * recovery.</strong> The first cut spread the fall across the recovery, so the blow landed
     * while the fists were still overhead and they reached the floor most of a second later (Mark,
     * 2026-09-13). The raise now finishes early, holds -- that hold IS the warning -- and the fall
     * lands exactly on the tick the goal strikes.</p>
     *
     * <p><strong>And it is a slam, so the fists reach the floor</strong>, which a rig standing
     * upright cannot do: the shoulder is ~33 units up and the whole arm is ~20 long. It takes a
     * deep bend at the waist AND a crouch -- and a crouch in a Minecraft model is not leg rotation,
     * which moves nothing, but a translation of the root (see {@code SLAM_CROUCH_DROP}).</p>
     */
    private static final float SLAM_ARM_RAISED_X_ROT = -2.90F;
    /** Relative to a chest already bent forward, this points the arms straight down at the floor. */
    private static final float SLAM_ARM_STRUCK_X_ROT = -1.15F;
    private static final float SLAM_ELBOW_RAISED_X_ROT = -0.15F;
    private static final float SLAM_ELBOW_STRUCK_X_ROT = 0.0F;
    private static final float SLAM_LEAN_BACK_X_ROT = -0.22F;
    /** A deep fold at the waist -- about 70 degrees -- which is most of what gets the fists down. */
    private static final float SLAM_LEAN_INTO_X_ROT = 1.25F;
    private static final float SLAM_HEAD_DOWN_X_ROT = 0.45F;
    private static final float SLAM_THIGH_X_ROT = 0.50F;
    private static final float SLAM_KNEE_X_ROT = 0.50F;
    /**
     * How far the whole rig drops at impact, in model units. Rotating the legs does NOT lower a
     * Minecraft model -- the entity stays at its own Y -- so a crouch has to be a translation of
     * the root part. Kept in step with the leg bend above: too much and the feet sink through the
     * floor, too little and it looks like it is bowing rather than driving down.
     */
    private static final float SLAM_CROUCH_DROP = 5.0F;
    /** Ticks the fall takes, counted BACK from the impact tick. */
    private static final float SLAM_FALL_TICKS = 5.0F;

    /**
     * THE WALL BURST. One fist cocked back through the wind-up, then thrown HORIZONTALLY on the
     * impact tick -- the tick the goal takes the face out of the wall.
     *
     * <p>Same two-curve shape as the slam, and for the same reason: the cock eases out so the pose
     * is held at full draw before the punch, and the punch eases in so it accelerates into the wall.
     * Nothing crouches -- a punch lands at chest height, so the root stays where it is, and the
     * whole {@code SLAM_CROUCH_DROP} problem does not arise.</p>
     *
     * <p>The torso twist is what makes one fist read as a punch rather than a reach: the striking
     * shoulder is drawn BACK during the cock and comes THROUGH on the blow, and the head
     * counter-rotates so it keeps watching the wall while the body turns under it.</p>
     */
    private static final float BURST_ARM_COCKED_X_ROT = 0.55F;
    /** Straight out in front, at shoulder height -- the arm that is level is the arm that punches. */
    private static final float BURST_ARM_STRUCK_X_ROT = -1.50F;
    /** Folded almost shut at the draw; a punch starts at the shoulder and ends at the elbow. */
    private static final float BURST_ELBOW_COCKED_X_ROT = -1.60F;
    private static final float BURST_ELBOW_STRUCK_X_ROT = 0.0F;
    /** The off arm hangs back as a counterweight rather than mirroring the blow. */
    private static final float BURST_OFF_ARM_X_ROT = 0.35F;
    private static final float BURST_TWIST_Y_ROT = 0.45F;
    private static final float BURST_LEAN_X_ROT = 0.18F;
    private static final float BURST_BRACE_THIGH_X_ROT = 0.22F;
    private static final float BURST_BRACE_KNEE_X_ROT = 0.28F;
    /**
     * Ticks the punch itself takes, and how long the arm is HELD at full extension before the
     * blocks go.
     *
     * <p>The hold is the fix for "the wall goes after the fist arrives" (Mark, 2026-09-14). The
     * punch used to finish exactly on the impact tick, so the arm was at its fastest in the frame
     * the wall was supposed to come apart &mdash; and the break's own particles and sound reach the
     * client a tick or two behind, which puts the two events on visibly different frames. Reaching
     * full extension two ticks early and STOPPING there gives the eye a single unambiguous moment
     * of arrival for the wall to go with, the same way the slam's raise holds before it falls.</p>
     */
    private static final float BURST_PUNCH_TICKS = 6.0F;
    private static final float BURST_EXTENSION_HOLD_TICKS = 2.0F;

    /**
     * THE BOULDER HEAVE. Both arms overhead holding the rock, a lean back, then the whole body
     * through and the hands empty.
     *
     * <p>It shares the slam's raise on purpose &mdash; the rock goes up the same way the fists do,
     * because that is how something this size lifts anything &mdash; and then departs from it
     * completely: the slam folds at the waist and drives DOWN, the heave stays tall and drives
     * FORWARD, and the arms finish out in front rather than at the floor. The lean back during the
     * hold is what sells the weight, and it is also the silhouette the player reads from across a
     * room: fists over the head means get out of the circle, a rock over the head means get behind
     * something.</p>
     */
    private static final float HURL_ARM_RAISED_X_ROT = -2.80F;
    /** Finished out in front, slightly down &mdash; a throw follows through, it does not stop dead. */
    private static final float HURL_ARM_RELEASED_X_ROT = -1.15F;
    private static final float HURL_ELBOW_RAISED_X_ROT = -0.90F;
    private static final float HURL_ELBOW_RELEASED_X_ROT = 0.0F;
    private static final float HURL_LEAN_BACK_X_ROT = -0.35F;
    private static final float HURL_LEAN_INTO_X_ROT = 0.45F;
    private static final float HURL_HEAD_X_ROT = -0.20F;
    private static final float HURL_THIGH_X_ROT = 0.20F;
    private static final float HURL_KNEE_X_ROT = 0.25F;
    /** Ticks the throw itself takes, counted BACK from the release tick. */
    private static final float HURL_THROW_TICKS = 5.0F;

    // NOTE the rig* prefixes. HumanoidModel already declares head/body/arms/legs, and a field of
    // the same name here would HIDE rather than replace them -- leaving two parts one keystroke
    // apart, one of which draws nothing. The prefix makes "vanilla stub" and "real part" unmistakable.
    private final ModelPart colossus;
    private final ModelPart rigBody;
    private final ModelPart chest;
    private final ModelPart colossusHead;
    private final ModelPart rigLeftArm;
    private final ModelPart lowerLeftArm;
    private final ModelPart fist;
    private final ModelPart shoulderSpikes;
    private final ModelPart shouldSpike2;
    private final ModelPart shouldSpike3;
    private final ModelPart rigRightArm;
    private final ModelPart lowerRightArm;
    private final ModelPart rightFist;
    private final ModelPart shoulderSpikes2;
    private final ModelPart chestGem;
    private final ModelPart beltGem;
    private final ModelPart lowerBody;
    private final ModelPart[] cloths;
    private final ModelPart rigLeftLeg;
    private final ModelPart lowerLeftLeg;
    private final ModelPart rigRightLeg;
    private final ModelPart lowerRightLeg;

    /** The root's authored Y, so the slam's crouch can be undone every frame. */
    private final float colossusBaseY;

    public StoneColossusModel(ModelPart root) {
        super(root);
        this.colossus = root.getChild("colossus");
        this.rigBody = this.colossus.getChild("body");
        this.chest = this.rigBody.getChild("chest");
        this.colossusHead = this.chest.getChild("head");
        this.rigLeftArm = this.chest.getChild("leftArm");
        this.lowerLeftArm = this.rigLeftArm.getChild("lowerLeftArm");
        this.fist = this.lowerLeftArm.getChild("fist");
        this.shoulderSpikes = this.rigLeftArm.getChild("shoulderSpikes");
        this.shouldSpike2 = this.shoulderSpikes.getChild("shouldSpike2");
        this.shouldSpike3 = this.shoulderSpikes.getChild("shouldSpike3");
        this.rigRightArm = this.chest.getChild("rightArm");
        this.lowerRightArm = this.rigRightArm.getChild("lowerRightArm");
        this.rightFist = this.lowerRightArm.getChild("rightFist");
        this.shoulderSpikes2 = this.rigRightArm.getChild("shoulderSpikes2");
        this.chestGem = this.rigBody.getChild("chestGem");
        this.beltGem = this.rigBody.getChild("beltGem");
        this.lowerBody = this.rigBody.getChild("lowerBody");
        this.cloths = new ModelPart[]{
                this.lowerBody.getChild("beltCloth1"),
                this.lowerBody.getChild("beltCloth2"),
                this.lowerBody.getChild("beltCloth3"),
                this.lowerBody.getChild("beltCloth4"),
                this.lowerBody.getChild("beltCloth5"),
                this.lowerBody.getChild("beltCloth6"),
        };
        this.rigLeftLeg = this.colossus.getChild("leftLeg");
        this.lowerLeftLeg = this.rigLeftLeg.getChild("lowerLeftLeg");
        this.rigRightLeg = this.colossus.getChild("rightLeg");
        this.lowerRightLeg = this.rigRightLeg.getChild("lowerRightLeg");
        this.colossusBaseY = this.colossus.y;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // The seven vanilla parts, EMPTY. HumanoidModel's constructor looks them up by name and
        // super.setupAnim poses them; they hold no cubes, so they draw nothing.
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // ---- Blockbench export, verbatim ----
        PartDefinition colossus = partdefinition.addOrReplaceChild("colossus", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition body = colossus.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -14.0F, 0.0F));

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(29, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        // Head pivot at the NECK (2026-09-13). It used to sit ten units lower, level with the
        // bottom of the chest, so every head rotation swung the helmet on a ten-unit arm and pulled
        // the neck off the shoulders -- visible the first time the statue pose bowed it.
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -10.0F, -3.5F, 7.0F, 10.0F, 7.0F, new CubeDeformation(0.0F))
        .texOffs(0, 18).addBox(-3.5F, -10.0F, -3.5F, 7.0F, 10.0F, 7.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -8.0F, 0.0F));

        PartDefinition leftArm = chest.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(54, 0).addBox(-2.0F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 59).addBox(-2.0F, -1.0F, -2.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.7F))
        .texOffs(62, 56).addBox(-2.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(5.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.1309F));

        PartDefinition lowerLeftArm = leftArm.addOrReplaceChild("lowerLeftArm", CubeListBuilder.create().texOffs(17, 55).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-0.5F, 7.0F, 2.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition fist = lowerLeftArm.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(50, 29).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, -2.0F));

        PartDefinition shoulderSpikes = leftArm.addOrReplaceChild("shoulderSpikes", CubeListBuilder.create().texOffs(67, 29).addBox(-1.0F, -4.5F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.6109F));

        PartDefinition shouldSpike2 = shoulderSpikes.addOrReplaceChild("shouldSpike2", CubeListBuilder.create().texOffs(23, 36).addBox(0.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 0.5F, -1.75F, 0.2618F, 0.0F, 0.0F));

        PartDefinition shouldSpike3 = shoulderSpikes.addOrReplaceChild("shouldSpike3", CubeListBuilder.create().texOffs(39, 68).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, -0.2618F, 0.0F, 0.0F));

        PartDefinition rightArm = chest.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(32, 55).addBox(-2.0F, -1.0F, -2.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(62, 47).addBox(-2.0F, -1.0F, -2.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.7F))
        .texOffs(62, 63).addBox(-2.0F, 3.0F, -2.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

        PartDefinition lowerRightArm = rightArm.addOrReplaceChild("lowerRightArm", CubeListBuilder.create().texOffs(47, 55).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-0.5F, 7.0F, 2.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition rightFist = lowerRightArm.addOrReplaceChild("rightFist", CubeListBuilder.create().texOffs(57, 38).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, -2.0F));

        PartDefinition shoulderSpikes2 = rightArm.addOrReplaceChild("shoulderSpikes2", CubeListBuilder.create().texOffs(0, 68).addBox(-1.0F, -4.5F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(69, 5).addBox(-0.5F, -2.5F, -2.25F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(69, 10).addBox(-0.5F, -2.5F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.6109F));

        PartDefinition chestGem = body.addOrReplaceChild("chestGem", CubeListBuilder.create().texOffs(25, 68).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition beltGem = body.addOrReplaceChild("beltGem", CubeListBuilder.create().texOffs(32, 68).addBox(-1.0F, -1.0F, -1.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, 5.5F, -1.5F, 0.0F, 0.0F, 0.7854F));

        PartDefinition lowerBody = body.addOrReplaceChild("lowerBody", CubeListBuilder.create().texOffs(29, 28).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 36).addBox(-3.5F, 5.5F, -2.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition beltCloth5 = lowerBody.addOrReplaceChild("beltCloth5", CubeListBuilder.create().texOffs(54, 68).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 10.0F, 2.25F));

        PartDefinition beltCloth4 = lowerBody.addOrReplaceChild("beltCloth4", CubeListBuilder.create().texOffs(57, 47).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 2.25F));

        PartDefinition beltCloth1 = lowerBody.addOrReplaceChild("beltCloth1", CubeListBuilder.create().texOffs(17, 45).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, -2.25F));

        PartDefinition beltCloth2 = lowerBody.addOrReplaceChild("beltCloth2", CubeListBuilder.create().texOffs(44, 68).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 10.0F, -2.25F));

        PartDefinition beltCloth3 = lowerBody.addOrReplaceChild("beltCloth3", CubeListBuilder.create().texOffs(49, 68).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 10.0F, -2.25F));

        // This strip's HINGE was the bug (Mark, 2026-09-14: the belt cloth was not moving). It came
        // out of Blockbench with its box 4.5 units BEHIND a pivot on the mob's front (offset
        // z -2.25, box z +4.5), so the cloth hung at the back while its hinge sat at the belt
        // buckle: it swung on a four-unit arm through the body instead of hanging off the belt, and
        // the cloth code -- which reads the PIVOT to decide front-or-rear -- classified the one rear
        // strip as a front one and pushed it the wrong way. Fixed here first and then **in the
        // .bbmodel and the export** (Mark, same day), so this is now what a re-export produces
        // rather than a local divergence from it.
        PartDefinition beltCloth6 = lowerBody.addOrReplaceChild("beltCloth6", CubeListBuilder.create().texOffs(69, 0).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 10.0F, 2.25F));

        PartDefinition leftLeg = colossus.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(23, 41).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9F, -5.0F, 0.0F, 0.0F, 0.0F, -0.0436F));

        PartDefinition lowerLeftLeg = leftLeg.addOrReplaceChild("lowerLeftLeg", CubeListBuilder.create().texOffs(40, 41).addBox(-2.0F, -0.1F, 0.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, 8.0F, -2.0F));

        PartDefinition rightLeg = colossus.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(0, 45).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0436F));

        PartDefinition lowerRightLeg = rightLeg.addOrReplaceChild("lowerRightLeg", CubeListBuilder.create().texOffs(52, 15).addBox(-2.0F, -0.1F, 0.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offset(0.0F, 8.0F, -2.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        IDormantMob dormant = entity instanceof IDormantMob d ? d : null;
        // 0 while a statue, 1 once fully risen, and everything between during the rise. Measured
        // against ageInTicks, which the renderer supplies as tickCount + partialTick -- so this
        // moves with the frame rate rather than stepping twenty times a second. The start tick is
        // THIS side's, set by the entity the tick it first saw the synched flag; anything else is
        // comparing two unrelated clocks. See IDormantMob.
        float wake = 1.0F;
        if (dormant != null) {
            if (dormant.isDormant()) {
                statuePose();
                return;
            }
            if (dormant.isWaking()) {
                int start = dormant.getWakeAnimStartTick();
                if (start == IDormantMob.NOT_WAKING) {
                    // The flag is set but this client has not ticked the entity yet: hold the
                    // statue for the one frame rather than snapping to the risen pose.
                    wake = 0.0F;
                } else {
                    float p = Mth.clamp((ageInTicks - start) / dormant.getWakeDuration(), 0.0F, 1.0F);
                    wake = p * p * (3.0F - 2.0F * p);   // smoothstep: eases in and out of the rise
                }
            }
        }

        // ---- head: damped, and clamped so the helmet does not drive through the chest ----
        this.colossusHead.xRot = Mth.clamp(headPitch * HEAD_TRACK, HEAD_PITCH_MIN, HEAD_PITCH_MAX)
                * ((float) Math.PI / 180F);
        this.colossusHead.yRot = netHeadYaw * HEAD_TRACK * ((float) Math.PI / 180F);
        this.colossusHead.zRot = 0.0F;

        // ---- torso: the vanilla body pose, which is the attack twist and the riding lean ----
        this.chest.yRot = this.body.yRot;
        this.chest.xRot = this.body.xRot;

        // ---- arms: vanilla swing plus the baked-in outward set ----
        this.rigRightArm.xRot = this.rightArm.xRot;
        this.rigRightArm.yRot = this.rightArm.yRot;
        this.rigRightArm.zRot = ARM_BASE_Z_ROT + this.rightArm.zRot;
        this.rigLeftArm.xRot = this.leftArm.xRot;
        this.rigLeftArm.yRot = this.leftArm.yRot;
        this.rigLeftArm.zRot = -ARM_BASE_Z_ROT + this.leftArm.zRot;

        // The forearm lags: a stone arm swings from the shoulder and the elbow catches up.
        this.lowerRightArm.xRot = ELBOW_BASE_X_ROT - this.rigRightArm.xRot * ELBOW_SWING_FOLLOW;
        this.lowerLeftArm.xRot = ELBOW_BASE_X_ROT - this.rigLeftArm.xRot * ELBOW_SWING_FOLLOW;

        // ---- legs: vanilla stride plus the baked stance, knees only bend backwards ----
        this.rigRightLeg.xRot = this.rightLeg.xRot;
        this.rigRightLeg.zRot = LEG_BASE_Z_ROT;
        this.rigLeftLeg.xRot = this.leftLeg.xRot;
        this.rigLeftLeg.zRot = -LEG_BASE_Z_ROT;
        this.lowerRightLeg.xRot = Math.max(0.0F, -this.rigRightLeg.xRot) * KNEE_FOLLOW;
        this.lowerLeftLeg.xRot = Math.max(0.0F, -this.rigLeftLeg.xRot) * KNEE_FOLLOW;

        // ---- cloth: pushed by the LEG BEHIND IT, plus a slow idle sway ----
        //
        // The strips hang off lowerBody and the legs are its siblings, so nothing moves them unless
        // this does. Each strip takes the swing of the leg on ITS side, and only in the direction
        // that leg can actually push it: a front strip is pushed forward by a forward stride and
        // simply hangs on the backswing, and a rear strip the other way round.
        float idle = Mth.cos(ageInTicks * CLOTH_IDLE_SPEED) * CLOTH_IDLE_SWAY;
        for (int i = 0; i < this.cloths.length; i++) {
            ModelPart cloth = this.cloths[i];
            boolean front = cloth.z < 0.0F;
            float left = pushFrom(this.rigLeftLeg.xRot, front);
            float right = pushFrom(this.rigRightLeg.xRot, front);
            // +x is the mob's LEFT (leftLeg sits at x +1.9). A CENTRE strip hangs between the two
            // legs and is pushed by whichever of them is currently pushing -- NOT by their average,
            // which was the bug: a walk cycle keeps the legs in antiphase, so the mean of the two is
            // ~0 at every point in it and the two 6-long centre strips (the most visible cloth on
            // the mob) hung perfectly still while the outer four swung (Mark, 2026-09-14).
            float x = cloth.x;
            float push = x > 0.5F ? left
                    : x < -0.5F ? right
                    : (Math.abs(left) > Math.abs(right) ? left : right);
            cloth.xRot = idle + push * CLOTH_LEG_PUSH;
        }

        // ---- the slam, which overrides the arms, torso and stance entirely while it runs ----
        // The crouch is a translation of the root, so it has to be UNDONE on every frame that is
        // not slamming: a goal that ends mid-blow would otherwise leave the mob permanently sunk.
        this.colossus.y = this.colossusBaseY;
        if (entity instanceof ISlammingMob slammer && slammer.isSlamming()
                && slammer.getSlamAnimStartTick() != ISlammingMob.NOT_SLAMMING) {
            slamPose(slammer, ageInTicks);
        } else if (entity instanceof IHurlingMob hurler && hurler.isHurling()
                && hurler.getHurlAnimStartTick() != IHurlingMob.NOT_HURLING) {
            hurlPose(hurler, ageInTicks);
        } else if (entity instanceof IBurstingMob burster && burster.isBursting()
                && burster.getBurstAnimStartTick() != IBurstingMob.NOT_BURSTING) {
            // The two poses own the same parts, so exactly one of them may run, and the slam wins:
            // it is the attack that hurts somebody, and a wall that comes down a blow late costs
            // nothing. Resolved HERE rather than between the goals, because it is a fact about the
            // RIG -- a goal has no way to know that two animations share an arm.
            burstPose(burster, ageInTicks);
        }

        // ---- the rise: blend out of the statue pose rather than adding to the live one ----
        if (wake < 1.0F) {
            float roll = Mth.sin(wake * (float) Math.PI) * WAKE_SHOULDER_ROLL;
            this.colossusHead.xRot = Mth.lerp(wake, STATUE_HEAD_X_ROT, this.colossusHead.xRot);
            this.colossusHead.yRot *= wake;
            this.chest.xRot = Mth.lerp(wake, STATUE_CHEST_X_ROT, this.chest.xRot);
            this.chest.yRot *= wake;
            this.rigRightArm.xRot = Mth.lerp(wake, STATUE_ARM_X_ROT, this.rigRightArm.xRot) - roll;
            this.rigLeftArm.xRot = Mth.lerp(wake, STATUE_ARM_X_ROT, this.rigLeftArm.xRot) - roll;
            this.rigRightArm.zRot = Mth.lerp(wake,
                    ARM_BASE_Z_ROT + STATUE_ARM_Z_EXTRA, this.rigRightArm.zRot);
            this.rigLeftArm.zRot = Mth.lerp(wake,
                    -(ARM_BASE_Z_ROT + STATUE_ARM_Z_EXTRA), this.rigLeftArm.zRot);
            this.lowerRightArm.xRot = Mth.lerp(wake, STATUE_ELBOW_X_ROT, this.lowerRightArm.xRot);
            this.lowerLeftArm.xRot = Mth.lerp(wake, STATUE_ELBOW_X_ROT, this.lowerLeftArm.xRot);
            this.rigRightLeg.xRot *= wake;
            this.rigLeftLeg.xRot *= wake;
            this.lowerRightLeg.xRot = Mth.lerp(wake, STATUE_KNEE_X_ROT, this.lowerRightLeg.xRot);
            this.lowerLeftLeg.xRot = Mth.lerp(wake, STATUE_KNEE_X_ROT, this.lowerLeftLeg.xRot);
            for (ModelPart cloth : this.cloths) {
                cloth.xRot *= wake;
            }
        }
    }

    /**
     * How much of a leg's swing a strip in front of or behind it actually takes.
     *
     * <p>Only the direction that leg can push it: a front strip is carried forward by a forward
     * stride and simply hangs on the backswing, a rear strip the other way round. Cloth is pushed,
     * never pulled.</p>
     */
    private static float pushFrom(float legXRot, boolean front) {
        return front ? Math.min(0.0F, legXRot) : Math.max(0.0F, legXRot);
    }

    /**
     * Fists overhead through the wind-up, then down.
     *
     * <p>Two curves, not one. The raise is <strong>eased out</strong> -- fast at first, slowing as
     * the arms reach the top -- so the pose is unmistakably held at full height for the last few
     * ticks before impact: that hold is the warning, and a linear raise arrives at the top and
     * strikes in the same instant. The fall is <strong>eased in</strong>, so it accelerates into
     * the ground rather than drifting down.</p>
     *
     * <p>Measured against {@code ageInTicks}, so it moves between ticks like the rise does.</p>
     */
    private void slamPose(ISlammingMob slammer, float ageInTicks) {
        float elapsed = ageInTicks - slammer.getSlamAnimStartTick();
        float windup = Math.max(1.0F, slammer.getSlamWindupTicks());
        float recovery = Math.max(1.0F, slammer.getSlamRecoveryTicks());
        float fall = Math.min(SLAM_FALL_TICKS, windup - 1.0F);
        float raiseEnd = windup - fall;

        // t: 0 = neutral, 1 = fists overhead, 2 = fists on the floor. The phases are laid out so
        // that t reaches 2 exactly on the tick the goal strikes.
        float armX;
        float elbowX;
        float leanX;
        float legBlend;
        float crouch;

        if (elapsed <= raiseEnd) {
            // Ease OUT: up fast, then hold at the top. The hold is the tell.
            float p = Mth.clamp(elapsed / raiseEnd, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - p) * (1.0F - p);
            armX = Mth.lerp(eased, 0.0F, SLAM_ARM_RAISED_X_ROT);
            elbowX = Mth.lerp(eased, ELBOW_BASE_X_ROT, SLAM_ELBOW_RAISED_X_ROT);
            leanX = Mth.lerp(eased, 0.0F, SLAM_LEAN_BACK_X_ROT);
            legBlend = 0.0F;
            crouch = 0.0F;
        } else if (elapsed <= windup) {
            // Ease IN: the fall accelerates into the floor and arrives ON the impact tick.
            float p = Mth.clamp((elapsed - raiseEnd) / fall, 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, SLAM_ARM_RAISED_X_ROT, SLAM_ARM_STRUCK_X_ROT);
            elbowX = Mth.lerp(eased, SLAM_ELBOW_RAISED_X_ROT, SLAM_ELBOW_STRUCK_X_ROT);
            leanX = Mth.lerp(eased, SLAM_LEAN_BACK_X_ROT, SLAM_LEAN_INTO_X_ROT);
            legBlend = eased;
            crouch = eased;
        } else {
            // Recovery: straighten back up, slowest at the start -- it is heavy.
            float p = Mth.clamp((elapsed - windup) / recovery, 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, SLAM_ARM_STRUCK_X_ROT, 0.0F);
            elbowX = Mth.lerp(eased, SLAM_ELBOW_STRUCK_X_ROT, ELBOW_BASE_X_ROT);
            leanX = Mth.lerp(eased, SLAM_LEAN_INTO_X_ROT, 0.0F);
            legBlend = 1.0F - eased;
            crouch = 1.0F - eased;
        }

        this.rigRightArm.xRot = armX;
        this.rigLeftArm.xRot = armX;
        this.rigRightArm.yRot = 0.0F;
        this.rigLeftArm.yRot = 0.0F;
        this.rigRightArm.zRot = ARM_BASE_Z_ROT;
        this.rigLeftArm.zRot = -ARM_BASE_Z_ROT;
        // Elbows straighten on the way up and stay straight through the blow -- a fist driven down
        // with the forearm still folded reads as a shrug rather than as a hammer.
        this.lowerRightArm.xRot = elbowX;
        this.lowerLeftArm.xRot = elbowX;

        this.chest.xRot = leanX;
        this.chest.yRot = 0.0F;
        this.colossusHead.xRot = SLAM_HEAD_DOWN_X_ROT * legBlend;   // it watches what it is hitting
        this.colossusHead.yRot = 0.0F;

        // Planted and braced. The legs bend, and the ROOT drops -- rotation alone would leave the
        // body at full height with its legs folded under it.
        this.rigRightLeg.xRot = SLAM_THIGH_X_ROT * legBlend;
        this.rigLeftLeg.xRot = SLAM_THIGH_X_ROT * legBlend;
        this.lowerRightLeg.xRot = SLAM_KNEE_X_ROT * legBlend;
        this.lowerLeftLeg.xRot = SLAM_KNEE_X_ROT * legBlend;
        this.colossus.y = this.colossusBaseY + SLAM_CROUCH_DROP * crouch;

        for (ModelPart cloth : this.cloths) {
            cloth.xRot = leanX * 0.5F;
        }
    }

    /**
     * One fist cocked back, then thrown into the wall.
     *
     * <p>Measured against {@code ageInTicks} like the slam, so it moves between ticks, and laid out
     * so the fist arrives exactly on the tick the goal breaks the blocks &mdash; the wind-up length
     * is one number shared by the server and this pose (see {@link IBurstingMob}).</p>
     */
    private void burstPose(IBurstingMob burster, float ageInTicks) {
        float elapsed = ageInTicks - burster.getBurstAnimStartTick();
        float windup = Math.max(1.0F, burster.getBurstWindupTicks());
        float recovery = Math.max(1.0F, burster.getBurstRecoveryTicks());
        float punch = Math.min(BURST_PUNCH_TICKS, windup - 1.0F);
        float cockEnd = windup - punch;
        // The arm arrives here and waits for the wall; see BURST_EXTENSION_HOLD_TICKS.
        float hold = Math.min(BURST_EXTENSION_HOLD_TICKS, punch - 1.0F);
        float punchEnd = windup - hold;

        float armX;
        float elbowX;
        // -1 fully cocked, 0 neutral, +1 fully extended. The twist, the lean and the brace all ride
        // on it, so the whole body turns with the arm rather than being animated separately.
        float drive;

        if (elapsed <= cockEnd) {
            // Ease OUT: back fast, then hold at full draw. The hold is the tell, and it is what a
            // player standing in the doorway is meant to see before the wall arrives.
            float p = Mth.clamp(elapsed / cockEnd, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - p) * (1.0F - p);
            armX = Mth.lerp(eased, 0.0F, BURST_ARM_COCKED_X_ROT);
            elbowX = Mth.lerp(eased, ELBOW_BASE_X_ROT, BURST_ELBOW_COCKED_X_ROT);
            drive = -eased;
        } else if (elapsed <= windup) {
            // Ease IN: the punch accelerates into full extension, arrives a couple of ticks
            // BEFORE the blocks go, and holds there -- the clamp is what makes it hold.
            float p = Mth.clamp((elapsed - cockEnd) / (punchEnd - cockEnd), 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, BURST_ARM_COCKED_X_ROT, BURST_ARM_STRUCK_X_ROT);
            elbowX = Mth.lerp(eased, BURST_ELBOW_COCKED_X_ROT, BURST_ELBOW_STRUCK_X_ROT);
            drive = Mth.lerp(eased, -1.0F, 1.0F);
        } else {
            // Recovery: the arm comes back out of the hole, slowest at the start -- it is heavy.
            float p = Mth.clamp((elapsed - windup) / recovery, 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, BURST_ARM_STRUCK_X_ROT, 0.0F);
            elbowX = Mth.lerp(eased, BURST_ELBOW_STRUCK_X_ROT, ELBOW_BASE_X_ROT);
            drive = 1.0F - eased;
        }

        boolean left = burster.isBurstLeftFisted();
        // +1 when the LEFT fist is thrown. A positive chest yRot brings the +x (left) shoulder
        // forward, so the sign carries straight through to the twist.
        float side = left ? 1.0F : -1.0F;
        ModelPart arm = left ? this.rigLeftArm : this.rigRightArm;
        ModelPart elbow = left ? this.lowerLeftArm : this.lowerRightArm;
        ModelPart offArm = left ? this.rigRightArm : this.rigLeftArm;
        ModelPart offElbow = left ? this.lowerRightArm : this.lowerLeftArm;

        arm.xRot = armX;
        arm.yRot = 0.0F;
        arm.zRot = left ? -ARM_BASE_Z_ROT : ARM_BASE_Z_ROT;
        elbow.xRot = elbowX;

        offArm.xRot = BURST_OFF_ARM_X_ROT * Math.abs(drive);
        offArm.yRot = 0.0F;
        offArm.zRot = left ? ARM_BASE_Z_ROT : -ARM_BASE_Z_ROT;
        offElbow.xRot = ELBOW_BASE_X_ROT;

        this.chest.xRot = BURST_LEAN_X_ROT * Math.max(0.0F, drive);
        this.chest.yRot = BURST_TWIST_Y_ROT * side * drive;
        // It watches the wall while the torso turns under it, so the twist does not swing the head
        // off the thing it is punching.
        this.colossusHead.xRot = 0.0F;
        this.colossusHead.yRot = -this.chest.yRot;

        // Braced, not crouched: a punch lands at chest height, so the root stays where it is.
        float brace = Math.abs(drive);
        this.rigRightLeg.xRot = BURST_BRACE_THIGH_X_ROT * brace;
        this.rigLeftLeg.xRot = BURST_BRACE_THIGH_X_ROT * brace;
        this.lowerRightLeg.xRot = BURST_BRACE_KNEE_X_ROT * brace;
        this.lowerLeftLeg.xRot = BURST_BRACE_KNEE_X_ROT * brace;

        for (ModelPart cloth : this.cloths) {
            cloth.xRot = this.chest.xRot * 0.5F;
        }
    }

    /**
     * Rock overhead, lean back, then everything through it.
     *
     * <p>The same two curves as the slam and the burst: the raise eases out so the pose HOLDS at
     * full height &mdash; the hold is the warning, and it is the longest of the three because the
     * player it is aimed at is the one standing furthest away &mdash; and the throw eases in so the
     * arms accelerate through the release. The release lands on the tick the goal actually spawns
     * the boulder, so the rock appears as the hands pass overhead rather than after they stop.</p>
     */
    private void hurlPose(IHurlingMob hurler, float ageInTicks) {
        float elapsed = ageInTicks - hurler.getHurlAnimStartTick();
        float windup = Math.max(1.0F, hurler.getHurlWindupTicks());
        float recovery = Math.max(1.0F, hurler.getHurlRecoveryTicks());
        float throwTicks = Math.min(HURL_THROW_TICKS, windup - 1.0F);
        float raiseEnd = windup - throwTicks;

        float armX;
        float elbowX;
        float leanX;
        float brace;

        if (elapsed <= raiseEnd) {
            // Ease OUT: up fast, then hold the rock overhead, leaning back under its weight.
            float p = Mth.clamp(elapsed / raiseEnd, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - p) * (1.0F - p);
            armX = Mth.lerp(eased, 0.0F, HURL_ARM_RAISED_X_ROT);
            elbowX = Mth.lerp(eased, ELBOW_BASE_X_ROT, HURL_ELBOW_RAISED_X_ROT);
            leanX = Mth.lerp(eased, 0.0F, HURL_LEAN_BACK_X_ROT);
            brace = eased;
        } else if (elapsed <= windup) {
            // Ease IN: the arms come over and the rock leaves ON the release tick.
            float p = Mth.clamp((elapsed - raiseEnd) / throwTicks, 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, HURL_ARM_RAISED_X_ROT, HURL_ARM_RELEASED_X_ROT);
            elbowX = Mth.lerp(eased, HURL_ELBOW_RAISED_X_ROT, HURL_ELBOW_RELEASED_X_ROT);
            leanX = Mth.lerp(eased, HURL_LEAN_BACK_X_ROT, HURL_LEAN_INTO_X_ROT);
            brace = 1.0F;
        } else {
            // Follow through and straighten, slowest at the start.
            float p = Mth.clamp((elapsed - windup) / recovery, 0.0F, 1.0F);
            float eased = p * p;
            armX = Mth.lerp(eased, HURL_ARM_RELEASED_X_ROT, 0.0F);
            elbowX = Mth.lerp(eased, HURL_ELBOW_RELEASED_X_ROT, ELBOW_BASE_X_ROT);
            leanX = Mth.lerp(eased, HURL_LEAN_INTO_X_ROT, 0.0F);
            brace = 1.0F - eased;
        }

        this.rigRightArm.xRot = armX;
        this.rigLeftArm.xRot = armX;
        this.rigRightArm.yRot = 0.0F;
        this.rigLeftArm.yRot = 0.0F;
        // Elbows tucked IN while the rock is overhead: two arms holding one thing, not two arms
        // raised beside each other.
        this.rigRightArm.zRot = ARM_BASE_Z_ROT * 0.5F;
        this.rigLeftArm.zRot = -ARM_BASE_Z_ROT * 0.5F;
        this.lowerRightArm.xRot = elbowX;
        this.lowerLeftArm.xRot = elbowX;

        this.chest.xRot = leanX;
        this.chest.yRot = 0.0F;
        this.colossusHead.xRot = HURL_HEAD_X_ROT * brace;   // it watches where it is throwing
        this.colossusHead.yRot = 0.0F;

        // Planted, not crouched: it stays TALL through this one. The slam's crouch is what makes
        // that attack read as downward, and borrowing it here would make the two look alike.
        this.rigRightLeg.xRot = HURL_THIGH_X_ROT * brace;
        this.rigLeftLeg.xRot = -HURL_THIGH_X_ROT * brace;   // one foot forward, braced
        this.lowerRightLeg.xRot = HURL_KNEE_X_ROT * brace;
        this.lowerLeftLeg.xRot = HURL_KNEE_X_ROT * brace;

        for (ModelPart cloth : this.cloths) {
            cloth.xRot = leanX * 0.5F;
        }
    }

    /**
     * Scenery: nothing moves, nothing tracks, and the limbs sit exactly where the rig was authored.
     * The pose has to be written out rather than skipped, because {@code super.setupAnim} has
     * already posed the vanilla parts and the copies above would otherwise leave last tick's
     * rotations on the rig.
     */
    private void statuePose() {
        // The statue path returns before the per-frame reset below it, so it undoes the slam's
        // crouch itself. Belt and braces: a rig left translated is a mob standing in the floor.
        this.colossus.y = this.colossusBaseY;
        this.colossusHead.xRot = STATUE_HEAD_X_ROT;
        this.colossusHead.yRot = 0.0F;
        this.colossusHead.zRot = 0.0F;
        this.chest.xRot = STATUE_CHEST_X_ROT;
        this.chest.yRot = 0.0F;
        this.rigRightArm.xRot = STATUE_ARM_X_ROT;
        this.rigRightArm.yRot = 0.0F;
        this.rigRightArm.zRot = ARM_BASE_Z_ROT + STATUE_ARM_Z_EXTRA;
        this.rigLeftArm.xRot = STATUE_ARM_X_ROT;
        this.rigLeftArm.yRot = 0.0F;
        this.rigLeftArm.zRot = -(ARM_BASE_Z_ROT + STATUE_ARM_Z_EXTRA);
        this.lowerRightArm.xRot = STATUE_ELBOW_X_ROT;
        this.lowerLeftArm.xRot = STATUE_ELBOW_X_ROT;
        this.rigRightLeg.xRot = 0.0F;
        this.rigRightLeg.zRot = LEG_BASE_Z_ROT;
        this.rigLeftLeg.xRot = 0.0F;
        this.rigLeftLeg.zRot = -LEG_BASE_Z_ROT;
        this.lowerRightLeg.xRot = STATUE_KNEE_X_ROT;
        this.lowerLeftLeg.xRot = STATUE_KNEE_X_ROT;
        for (ModelPart cloth : this.cloths) {
            cloth.xRot = 0.0F;
        }
    }

    /**
     * Renders the colossus rig only. {@code super.renderToBuffer} is deliberately not called: the
     * seven vanilla parts hold no cubes, so it would walk them for nothing.
     */
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.colossus.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** The real head, not the empty vanilla stub, so head layers have something to sit on. */
    @Override
    public ModelPart getHead() {
        return this.colossusHead;
    }
}
