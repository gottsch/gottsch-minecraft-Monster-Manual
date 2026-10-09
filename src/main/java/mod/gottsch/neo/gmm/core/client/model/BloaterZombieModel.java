package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater;
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
 * The Bloater's dedicated rig, ported from the user's own Blockbench build at
 * {@code Blockbench/dungeon denizens/forge/bloater zombie/BloaterZombieModel2.bbmodel}/
 * {@code BloaterZombieModel2.java} (second revision — the first pass merged a parallel inflate layer
 * onto {@code body}'s own cube list, which read as "wearing a puffy coat" rather than genuinely
 * swollen; this one replaces that with two separate {@code frontBloat}/{@code backBloat} children of
 * {@code body}, each offset forward/back and tilted ~12.5 degrees so they poke past the torso's own
 * silhouette at an angle instead of just being a uniformly bigger box — both reuse {@code body}'s own
 * UV rectangle, so the overlap also reads as natural layered creases with no new texture art needed).
 * Replaces the shared {@link GMMZombieModel}/vanilla {@code ModelLayers.ZOMBIE} reuse — own
 * {@link #createBodyLayer()} now, since the swollen cube dimensions genuinely differ from vanilla
 * (same reason {@code BodakModel} got its own layer). No separate "hat" content survives porting —
 * the puffy outer layer is baked directly onto {@code head}'s own cube list (reusing the vanilla hat
 * UV region), so {@code hat} exists only to satisfy
 * {@link net.minecraft.client.model.HumanoidModel}'s required child and is permanently hidden.
 *
 * <p>Two behavior corrections from the shared shamble ({@link GMMZombieModel#animateArms}), both
 * per the user's request:
 * <ul>
 *   <li><b>No raised zombie arms</b> — {@link #animateArms} is a no-op (same idiom
 *       {@code WightModel} uses), leaving vanilla {@code HumanoidModel}'s own walk-swing/attack-swing
 *       machinery intact rather than swapping in {@code AnimationUtils.animateZombieArms}'s
 *       arms-out-front lurch.</li>
 *   <li><b>Slight swing, not a normal walk-swing</b> — the vanilla walk-swing amplitude left in place
 *       above is dampened ({@link #ARM_SWING_DAMPEN}), but only while not mid-attack (checked via
 *       {@code attackTime}), so a real attack swing (vanilla's own {@code setupAttackAnimation},
 *       already applied by the time our code runs) is never weakened.</li>
 * </ul>
 *
 * <p><b>Waddle:</b> the whole torso (body/head, arms following along) rolls side to side once per
 * step, bobbing down slightly at each weight-shift peak — see {@link #setupAnim}. Baked off the same
 * {@code limbSwing} phase the legs already swing on (not {@code ageInTicks}), so the roll always lines
 * up with the actual footstep cycle.
 *
 * <p><b>Re-baked resting pose every frame:</b> {@code HumanoidModel#setupAnim} resets {@code zRot} on
 * the arms as part of its own swing math, which would otherwise silently erase the rig's baked
 * outward-arm/splayed-leg stance after the very first render frame — the base {@code zRot} values are
 * captured once in the constructor and explicitly reapplied every frame in {@link #setupAnim} (same
 * "always set an absolute value, never assume it survives" lesson as {@code GraveZombieModel}).
 *
 * <p>Also hides the arm parts once the death-rupture has flung them loose (see
 * {@link Bloater#areArmsDetached()}) — folded in here (this replaces the old generic
 * {@code BloaterModel} specialization now that Bloater has its own dedicated rig).
 *
 * @author Mark Gottschling on 7/14/2026
 */
public class BloaterZombieModel<T extends Bloater> extends GMMZombieModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "bloater"), "main");

    // "slight swing" -- shrinks vanilla's own walk-swing amplitude down to a heavy, half-hearted sway.
    private static final float ARM_SWING_DAMPEN = 0.4F;
    // waddle: peak side-to-side roll (radians) and peak downward bob (model units) per step.
    private static final float WADDLE_ROLL = 0.09F;
    private static final float WADDLE_BOB = 0.6F;

    // baked resting zRot (arms/legs) and base Y (all parts) captured once, reapplied every frame.
    private final float baseRightArmZRot, baseLeftArmZRot, baseRightLegZRot, baseLeftLegZRot;
    private final float baseBodyY, baseHeadY, baseRightArmY, baseLeftArmY, baseRightLegY, baseLeftLegY;

    public BloaterZombieModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
        this.baseRightArmZRot = this.rightArm.zRot;
        this.baseLeftArmZRot = this.leftArm.zRot;
        this.baseRightLegZRot = this.rightLeg.zRot;
        this.baseLeftLegZRot = this.leftLeg.zRot;
        this.baseBodyY = this.body.y;
        this.baseHeadY = this.head.y;
        this.baseRightArmY = this.rightArm.y;
        this.baseLeftArmY = this.leftArm.y;
        this.baseRightLegY = this.rightLeg.y;
        this.baseLeftLegY = this.leftLeg.y;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // asymmetric, angled bulges (children of body, not a parallel inflate layer) -- offset
        // forward/back and tilted ~12.5 degrees so each one pokes past the torso's own silhouette at
        // an angle, rather than reading as a uniformly bigger box. Both reuse body's own UV rectangle
        // (no new texture region needed), which also gives the overlap a natural layered-crease look.
        body.addOrReplaceChild("frontBloat", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.201F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, -0.2182F, 0.0F, 0.0F));
        body.addOrReplaceChild("backBloat", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.2182F, 0.0F, 0.0F));

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE)
                        .texOffs(0, 33).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.3F))
                        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // required by HumanoidModel but unused -- the puffy outer layer is already baked onto head above.
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.48F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE).mirror(false),
                PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.48F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 16).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 16).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE).mirror(false)
                        .texOffs(0, 16).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false),
                PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, -0.0873F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * No-op: keeps vanilla {@code HumanoidModel}'s own walk-swing/attack-swing machinery intact
     * instead of swapping in the zombie arms-out-front lurch — see class doc.
     */
    @Override
    protected void animateArms(T entity, float ageInTicks) {
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // slight, heavy sway rather than a brisk walk-swing -- only while not mid-attack, so a real
        // attack swing (vanilla's own setupAttackAnimation, already applied above) is never weakened.
        if (this.attackTime <= 0.0F) {
            this.rightArm.xRot *= ARM_SWING_DAMPEN;
            this.leftArm.xRot *= ARM_SWING_DAMPEN;
        }

        // waddle: torso rolls side to side once per step (same limbSwing phase the legs already swing
        // on), bobbing down slightly at each weight-shift peak.
        float waddle = Mth.sin(limbSwing * 0.6662F) * limbSwingAmount;
        float roll = waddle * WADDLE_ROLL;
        float bob = Math.abs(waddle) * WADDLE_BOB;

        // re-bake each part's own resting zRot every frame -- HumanoidModel#setupAnim would otherwise
        // wipe the rig's baked bloated-arm/splayed-leg stance -- layering the waddle roll on top where
        // it applies (torso + arms follow the roll; legs stay planted).
        this.body.zRot = roll;
        this.head.zRot = roll;
        this.rightArm.zRot = baseRightArmZRot + roll;
        this.leftArm.zRot = baseLeftArmZRot + roll;
        this.rightLeg.zRot = baseRightLegZRot;
        this.leftLeg.zRot = baseLeftLegZRot;

        this.body.y = baseBodyY - bob;
        this.head.y = baseHeadY - bob;
        this.rightArm.y = baseRightArmY - bob;
        this.leftArm.y = baseLeftArmY - bob;
        this.rightLeg.y = baseRightLegY - bob;
        this.leftLeg.y = baseLeftLegY - bob;

        boolean armsDetached = entity.areArmsDetached();
        this.leftArm.visible = !armsDetached;
        this.rightArm.visible = !armsDetached;

        this.hat.copyFrom(this.head);
    }
}
