package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bodak;
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
 * The Bodak rig: ported from the user's own Blockbench build (bare-chested, no shirt/sleeves -- the
 * vanilla zombie's baked-in clothing is gone entirely) at
 * {@code Blockbench/dungeon denizens/forge/bodak/Bodak.bbmodel}/{@code BodakModel.java}. Same vanilla
 * zombie proportions as {@link GMMZombieModel} (this still extends it, keeping the shared arms-out
 * shamble via {@link #animateArms}), but now with its **own** {@link #createBodyLayer()} rather than
 * reusing {@code ModelLayers.ZOMBIE} -- the source rig adds one genuinely new cube, a permanently
 * agape "silent scream" mouth (the D&amp;D flavor text's other defining feature, alongside the
 * jerky-puppet movement below). Ported as a real child of {@code head} (not merged onto the same
 * {@code CubeListBuilder} the raw export used) so it automatically inherits both vanilla's
 * look-tracking rotation and this class's own jerk {@code zRot} below with zero extra code -- no
 * separate copy-pose call needed, unlike {@code hat} (see {@link #setupAnim}).
 *
 * <p>The mouth UV rect tracks the source {@code .bbmodel} exactly and has moved twice already --
 * {@code (-1, 32)} in the original export, then {@code (0, 33)} after the user enlarged the jaw in
 * Blockbench (which pushed the box's auto-laid-out UV off the texture map), found the mistake, and
 * repositioned both the texture content and the UV offset (2026-07-12). Always pull this value from the
 * current {@code .bbmodel}/exported {@code BodakModel.java} at
 * {@code Blockbench/dungeon denizens/forge/bodak/} rather than assuming it's stable -- this is the one
 * coordinate in this class that must stay byte-for-byte identical to whatever Blockbench currently
 * exports, not a place to "clean up" what looks like an odd offset.
 *
 * <p>Also overlays a head-only jerk/twitch on top of the normal look-tracking pose while a Death Gaze
 * charge is building -- the "jerky and puppet-like, as though guided by an unseen, malevolent force"
 * movement is part of the creature's actual D&amp;D description, not an invented tell.
 *
 * <p>Deliberately isolated to a single axis (head roll, {@code zRot}) on a single part: vanilla's own
 * look-tracking already drives the head's yaw/pitch, so "eye contact" with whoever it's charging at is
 * preserved, and confining the jerk to one part avoids the "different parts rotating independently
 * looks broken" trap {@code GraveZombieModel}'s first shudder pass hit (see that class's doc) -- there's
 * no risk of a twisted-looking rig when only one part ever gets the extra rotation.
 *
 * <p>Motion is a stepped snap, not a smooth oscillation: the head-roll target flips sign once every
 * {@link Bodak#getJerkPeriodTicks()} and holds there with no interpolation between snaps -- "jerk one
 * way, pause, jerk, pause." Only the angle range ({@link Bodak#getJerkMinAngleDegrees()} /
 * {@link Bodak#getJerkMaxAngleDegrees()}) scales with gaze charge (a mild build in intensity); the
 * period is a fixed, config-tunable cadence, not charge-scaled -- per the user's own correction after
 * the first in-game look (round-2 feedback: a period that sped up toward full charge, paired with a
 * wide angle range, read as "rolling all over the place," not a controlled twitch). All three numbers
 * live in {@code gmm:mob_config} (read via {@code Bodak}, not hardcoded here) so they can be retuned
 * without a code change.
 *
 * <p><b>Round-2 correction:</b> the very first pass used {@code head.zRot +=} inside the
 * charge-building branch only. {@code setupAnim} runs once per <em>render frame</em>, not once per game
 * tick (many times more often at typical framerates), so that accumulated the same angle over and over
 * within a single unchanging snap step -- the head visibly spun away rather than snapping to a fixed
 * pose, and since the reset only ever happened inside the {@code charge > 0} branch, the roll never
 * returned to neutral once charging stopped ("doesn't right itself after I stop looking"). Fixed by
 * always <em>setting</em> {@code head.zRot} to an absolute target every frame (charging or not) instead
 * of incrementing it -- see {@link #setupAnim}.
 *
 * @author Mark Gottschling on 7/10/2026
 */
public class BodakModel<T extends Bodak> extends GMMZombieModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bodak"), "main");

    public BodakModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // agape "silent scream" mouth -- a real child of head, see class doc for why (auto-inherits
        // both look-tracking and the jerk zRot below, unlike the raw export's merged-box approach).
        head.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, 0.0F, -4.0F, 5.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE.extend(0.5F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float charge = entity.getGazeCharge();
        if (charge > 0.0F) {
            float minAngle = (float) Math.toRadians(entity.getJerkMinAngleDegrees());
            float maxAngle = (float) Math.toRadians(entity.getJerkMaxAngleDegrees());
            float angle = Mth.lerp(charge, minAngle, maxAngle);
            int period = Math.max(1, entity.getJerkPeriodTicks());
            long step = (long) (ageInTicks / period);
            this.head.zRot = (step % 2 == 0) ? angle : -angle;
        } else {
            // rights itself the instant charging stops, rather than freezing at whatever angle it last
            // snapped to.
            this.head.zRot = 0.0F;
        }
        // vanilla HumanoidModel#setupAnim already copies hat = head as its last internal step, but that
        // happened inside super.setupAnim() above, before this mutation -- re-copy unconditionally (both
        // branches) so the hat layer never lags a frame stale (same gotcha GraveZombieModel's pose
        // mutations hit).
        this.hat.copyFrom(this.head);
    }
}
