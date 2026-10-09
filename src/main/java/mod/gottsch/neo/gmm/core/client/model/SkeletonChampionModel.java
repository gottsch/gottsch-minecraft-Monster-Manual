package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;

/**
 * Skeleton Champion model (authored in Blockbench) — a plated elite skeleton, its armor (boots,
 * bracers/pauldron caps, cuirass, belt) baked as static child cubes of the same {@code main} rig
 * a plain skeleton uses, so the plate follows each limb's swing for free via the normal
 * parent-child transform — no separate animated armor layer needed. Single texture, single render
 * pass, same idiom as {@link TaintedSkeletonModel}. Implements {@link ArmedModel} (see
 * {@link #translateToHand}) so {@code ItemInHandLayer} can render the tag-rolled weapon
 * {@code SkeletonChampion#populateDefaultEquipmentSlots} equips into the mainhand.
 *
 * @author Mark Gottschling on 7/12/2026
 */
public class SkeletonChampionModel<T extends Mob> extends EntityModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "skeleton_champion_model"), "main");

    private final ModelPart main;
    private final ModelPart left_leg;
    private final ModelPart right_leg;
    private final ModelPart left_arm;
    private final ModelPart right_arm;
    private final ModelPart body;
    private final ModelPart head;

    public SkeletonChampionModel(ModelPart root) {
        this.main = root.getChild("main");
        this.left_leg = this.main.getChild("left_leg");
        this.right_leg = this.main.getChild("right_leg");
        this.left_arm = this.main.getChild("left_arm");
        this.right_arm = this.main.getChild("right_arm");
        this.body = this.main.getChild("body");
        this.head = this.main.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition left_leg = main.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(26, 39).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(2.0F, -12.0F, 0.0F));

        left_leg.addOrReplaceChild("armorLeftBoot", CubeListBuilder.create().texOffs(0, 52).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition right_leg = main.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(26, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -12.0F, 0.0F));

        right_leg.addOrReplaceChild("armorRightBoot", CubeListBuilder.create().texOffs(0, 52).mirror().addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition left_arm = main.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(17, 39).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(5.0F, -22.0F, 0.0F));

        left_arm.addOrReplaceChild("leftItem", CubeListBuilder.create(), PartPose.offset(1.0F, 7.0F, 1.0F));

        PartDefinition armorLeftArm = left_arm.addOrReplaceChild("armorLeftArm", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(0.0F, -3.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(-1.0F, 0.0F, 0.0F));

        armorLeftArm.addOrReplaceChild("leftCap1", CubeListBuilder.create().texOffs(35, 39).mirror().addBox(0.5F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false), PartPose.offsetAndRotation(0.5F, -2.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

        PartDefinition leftCap2 = armorLeftArm.addOrReplaceChild("leftCap2", CubeListBuilder.create(), PartPose.offset(-8.5F, -2.0F, 0.0F));

        leftCap2.addOrReplaceChild("cap3_r1", CubeListBuilder.create().texOffs(35, 39).mirror().addBox(0.5F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.4F)).mirror(false), PartPose.offsetAndRotation(9.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

        armorLeftArm.addOrReplaceChild("leftCap3", CubeListBuilder.create().texOffs(33, 8).mirror().addBox(-4.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false), PartPose.offset(5.5F, -2.0F, 0.0F));

        PartDefinition right_arm = main.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(17, 39).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -22.0F, 0.0F));

        PartDefinition armorRightArm = right_arm.addOrReplaceChild("armorRightArm", CubeListBuilder.create().texOffs(0, 34).addBox(-4.0F, -3.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(1.0F, 0.0F, 0.0F));

        armorRightArm.addOrReplaceChild("rightCap1", CubeListBuilder.create().texOffs(35, 39).addBox(-4.5F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(-0.5F, -2.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

        PartDefinition rightCap2 = armorRightArm.addOrReplaceChild("rightCap2", CubeListBuilder.create(), PartPose.offset(-0.5F, -2.0F, 0.0F));

        rightCap2.addOrReplaceChild("cap2_r1", CubeListBuilder.create().texOffs(35, 39).addBox(-4.5F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

        armorRightArm.addOrReplaceChild("rightCap3", CubeListBuilder.create().texOffs(33, 8).addBox(-4.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.offset(-0.5F, -2.0F, 0.0F));

        PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

        body.addOrReplaceChild("armorBody", CubeListBuilder.create().texOffs(25, 17).addBox(-4.0F, 0.5F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.95F))
                .texOffs(25, 28).addBox(-4.0F, 6.5F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.35F))
                .texOffs(35, 46).addBox(-1.5F, 8.0F, -3.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(33, 0).addBox(-4.0F, 8.8F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;

        this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        setupAttackAnimation();

        // a raised shield (see RaiseShieldGoal) bends the offhand (left) arm up -- nothing in vanilla
        // does this for a Mob automatically, see GMMAnimationUtils#poseBlockingArm.
        if (entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.OFF_HAND) {
            GMMAnimationUtils.poseBlockingArm(this.left_arm, this.head, false);
        }
    }

    /** Rotation-only sword swing, right arm only (mainhand — the champion's blade). */
    private static final float ATTACK_JAB = 1.1F;

    private void setupAttackAnimation() {
        if (this.attackTime <= 0.0F) {
            return;
        }
        this.right_arm.xRot -= Mth.sin(this.attackTime * (float) Math.PI) * ATTACK_JAB;
    }

    /**
     * Positions the {@code PoseStack} at the mainhand/offhand arm so {@code ItemInHandLayer} can
     * render the equipped weapon there.
     *
     * <p><b>Bug fixed 2026-07-13:</b> unlike vanilla {@code HumanoidModel} (whose root sits at zero
     * offset, so a direct child's own local transform is already the full transform), this
     * Blockbench export wraps everything in a {@code main} group carrying its own
     * {@code PartPose.offset(0, 24, 0)} — {@code right_arm}/{@code left_arm} are children of
     * {@code main}, not of an unoffset root. The first version only applied the arm's own local
     * transform, skipping {@code main}'s wrapper offset entirely, so the item rendered ~1.5 blocks
     * too high (floating near the head instead of in the hand). Applying {@code main}'s transform
     * first reconstructs the same composed transform {@code main.render()} builds when walking the
     * real part hierarchy.
     *
     * <p><b>Bug fixed 2026-07-13 (second pass):</b> the side nudge was copied verbatim from
     * {@code SkeletonWarriorModel} (+1 right / -1 left), which centers a held item on that rig's bare
     * 2px bone. This rig isn't bare — {@code armorRightArm}/{@code armorLeftArm} are a thicker 4px
     * sleeve built as a child of the bone, and that sleeve is itself offset ~1px off the bone's own
     * pivot ({@code armorRightArm} spans local x -3..+1, center -1; {@code armorLeftArm} spans -1..+3,
     * center +1). The borrowed nudge pushed the item a further +1/-1 *away* from those centers instead
     * of onto them, landing it visibly off to the side of the hand. Flipped to -1 right / +1 left so
     * the item lands on the sleeve's actual center, not the hidden bone's.
     */
    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        this.main.translateAndRotate(poseStack);
        float sideOffset = (side == HumanoidArm.RIGHT) ? -1.0F : 1.0F;
        ModelPart arm = (side == HumanoidArm.RIGHT) ? this.right_arm : this.left_arm;
        arm.x += sideOffset;
        arm.translateAndRotate(poseStack);
        arm.x -= sideOffset;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
