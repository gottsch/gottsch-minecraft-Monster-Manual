package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
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

import java.util.Optional;

/**
 * Wood Golem's rig -- a straight port of Treasure2's {@code WitherwoodGolemModel} (a bark/root-armed
 * construct, distinct from every other GMM golem-family candidate's planned villager/iron-golem-based
 * rigs, see the catalog's family note) onto GMM's {@link HumanlikeModel} base rather than Treasure2's
 * own bespoke {@code IHumanlikeModel}-implementing {@code EntityModel} -- GMM already carries an
 * equivalent {@link IHumanlikeModel}/{@link HumanlikeModel} pair (ported for exactly this kind of
 * "custom rig, standard biped attack swing" case, see {@code GargoyleModel}), so {@code
 * setupAttackAnimation} comes for free instead of being duplicated here.
 * <p>
 * Geometry, part hierarchy, and UV layout are unchanged from the source export (bent-forward
 * root/branch arms, a two-segment torso lean, root-flared legs) -- only the resting arm angle
 * (-0.3926991F, ~-22.5&deg;) and the leg swing (a triangle wave, not a cosine) are non-standard enough
 * that they're kept as explicit overrides in {@link #setupAnim} rather than routed through
 * {@link IHumanlikeModel}'s default {@code swingArms}/{@code swingLegs}.
 *
 * @author Mark Gottschling
 */
public class WoodGolemModel<T extends Mob> extends HumanlikeModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "wood_golem"), "main");

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart torso;
    private final ModelPart upperBody;
    private final ModelPart leftArm;
    private final ModelPart leftElbow;
    private final ModelPart rightArm;
    private final ModelPart rightElbow;
    private final ModelPart lowerBody;
    private final ModelPart legs;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    private final float rightArmX;
    private final float leftArmX;

    public WoodGolemModel(ModelPart root) {
        this.head = root.getChild("head");
        this.neck = head.getChild("neck");
        this.torso = root.getChild("torso");
        this.upperBody = torso.getChild("upperBody");
        this.leftArm = upperBody.getChild("leftArm");
        this.leftElbow = leftArm.getChild("leftElbow");
        this.rightArm = upperBody.getChild("rightArm");
        this.rightElbow = rightArm.getChild("rightElbow");
        this.lowerBody = torso.getChild("lowerBody");
        this.legs = root.getChild("legs");
        this.rightLeg = legs.getChild("rightLeg");
        this.leftLeg = legs.getChild("leftLeg");

        this.rightArmX = rightArm.x;
        this.leftArmX = leftArm.x;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(17, 51).addBox(0.0F, -15.0F, 3.0F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-3.0F, -13.0F, 3.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(17, 43).addBox(3.0F, -13.0F, -1.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(46, 55).addBox(-1.0F, -12.0F, 0.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, -10.0F, 0.1745F, 0.0F, 0.0F));

        head.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 63).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 2.5F, 0.4363F, 0.0F, 0.0F));

        PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(46, 39).mirror().addBox(4.0F, -16.0F, -1.0F, 2.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(46, 39).addBox(-6.0F, -16.0F, -1.0F, 2.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.3054F, 0.0F, 0.0F));

        PartDefinition upperBody = torso.addOrReplaceChild("upperBody", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -7.0F, -5.0F, 14.0F, 14.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(29, 51).addBox(3.0F, -8.0F, 4.0F, 0.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.2182F, 0.0F, 0.0F));

        upperBody.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(29, 51).addBox(0.0F, -8.0F, 0.0F, 0.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 6.0F, 4.0F, -0.2618F, 0.0F, 0.0F));

        PartDefinition leftArm = upperBody.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-1.0F, -2.5F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(49, 61).addBox(3.0F, -2.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -4.0F, -1.0F, -0.3927F, 0.0F, -0.1745F));

        leftArm.addOrReplaceChild("leftElbow", CubeListBuilder.create().texOffs(29, 39).mirror().addBox(-2.0F, -0.5F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 13.0F, 0.0F, -0.3491F, 0.0F, 0.0F));

        PartDefinition rightArm = upperBody.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(0, 43).addBox(-4.0F, -2.5F, -3.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -4.0F, 0.0F, -0.3927F, 0.0F, 0.1745F));

        PartDefinition rightElbow = rightArm.addOrReplaceChild("rightElbow", CubeListBuilder.create().texOffs(29, 39).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 13.0F, -1.0F, -0.3491F, 0.0F, 0.0F));

        rightElbow.addOrReplaceChild("branch_r1", CubeListBuilder.create().texOffs(49, 61).mirror().addBox(-8.0F, -6.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        torso.addOrReplaceChild("lowerBody", CubeListBuilder.create().texOffs(33, 24).addBox(-3.5F, 0.0F, -3.0F, 7.0F, 8.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        PartDefinition legs = partdefinition.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        legs.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(47, 0).addBox(-1.5F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 74).addBox(-2.0F, 9.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(56, 16).addBox(-1.0F, 16.0F, -4.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(56, 22).addBox(10.0F, 16.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -18.0F, 0.0F));

        legs.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(47, 0).mirror().addBox(-2.5F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 74).mirror().addBox(-3.0F, 9.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(56, 16).addBox(-2.0F, 16.0F, -4.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(56, 22).addBox(-12.0F, 16.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -18.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        swivelHead(netHeadYaw, headPitch, ageInTicks);

        // resting arm angle is a bent-forward branch pose (~-22.5deg), not straight down -- keep the
        // base offset that IHumanlikeModel's default swingArms doesn't carry.
        final float restingArmAngle = -0.3926991F;
        final float armSpeed = 0.25F;
        final float armSwingRadians = 0.5235988F;
        this.rightArm.xRot = restingArmAngle + Mth.cos(limbSwing * armSpeed) * armSwingRadians * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;
        this.leftArm.xRot = restingArmAngle + Mth.cos(limbSwing * armSpeed + (float) Math.PI) * armSwingRadians * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount;

        // root-flared legs swing as a triangle wave (a stiffer, snappier gait) rather than the default
        // cosine swing.
        this.rightLeg.xRot = -1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        this.leftLeg.xRot = 1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        setupAttackAnimation(entity, ageInTicks);
    }

    @Override
    public void resetSwing(T entity) {
        this.torso.yRot = 0.0F;
        this.rightArm.x = rightArmX;
        this.rightArm.xRot = -0.3926991F;
        this.rightArm.zRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.x = leftArmX;
        this.leftArm.xRot = -0.3926991F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        legs.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public Optional<ModelPart> getHead() {
        return Optional.of(this.head);
    }

    @Override
    public Optional<ModelPart> getBody() {
        return Optional.of(this.torso);
    }

    @Override
    public Optional<ModelPart> getRightArm() {
        return Optional.of(this.rightArm);
    }

    @Override
    public Optional<ModelPart> getLeftArm() {
        return Optional.of(this.leftArm);
    }
}
