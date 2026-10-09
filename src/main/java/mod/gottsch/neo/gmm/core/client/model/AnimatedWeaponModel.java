package mod.gottsch.neo.gmm.core.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.construct.AnimatedWeapon;
import net.minecraft.client.model.ArmedModel;
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
import net.minecraft.world.entity.HumanoidArm;

/**
 * Body-less model for Animated Weapon: the only part is {@code hand}, an invisible pivot with no
 * cubes of its own — {@link #renderToBuffer} literally draws nothing. The equipped weapon, rendered by
 * {@code ItemInHandLayer} at {@code hand}'s transform (see {@link #translateToHand}), is the entity's
 * entire visual identity.
 * <p>
 * {@link #setupAnim} blends {@code hand} toward a raised/pulled-back telegraph pose using
 * {@link #windupProgress} (0 = not winding up, 1 = fully wound, about to strike) — fed in by
 * {@code AnimatedWeaponRenderer#setupRotations} each frame from {@code AnimatedWeapon#getWindupProgress},
 * the same "renderer hands the model a partial-tick-interpolated float field" idiom
 * {@code GraveZombieRenderer}/{@code GraveZombieModel#riseAmount} already use. Rather than vanilla's
 * own {@code attackTime} — see {@code AnimatedWeapon}'s class doc for why that's too short to read as
 * a real telegraph. The entity itself (not this model) now handles facing its target (see
 * {@code AnimatedWeapon.HoverMoveControl}), so this model only ever needs to point the weapon "forward"
 * (local zero rotation) while engaged, reserving the idle bob/spin for when there's no target at all.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class AnimatedWeaponModel<T extends AnimatedWeapon> extends EntityModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "animated_weapon_model"), "main");

    // fully wound pose: pulled back and raised, ready to chop down
    private static final float WINDUP_XROT = -1.3F;
    private static final float WINDUP_YROT = -0.5F;

    /** 0..1, set by {@code AnimatedWeaponRenderer} each frame before {@link #setupAnim} runs. */
    public float windupProgress = 0.0F;

    private final ModelPart hand;

    public AnimatedWeaponModel(ModelPart root) {
        this.hand = root.getChild("hand");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("hand", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.windupProgress > 0.0F) {
            this.hand.y = 12.0F;
            this.hand.xRot = Mth.lerp(this.windupProgress, 0.0F, WINDUP_XROT);
            this.hand.yRot = Mth.lerp(this.windupProgress, 0.0F, WINDUP_YROT);
        } else if (entity.getTarget() != null) {
            // engaged but not yet winding up -- held level, pointed at whatever the entity itself
            // (HoverMoveControl) is already facing, no local rotation needed on top of that
            this.hand.y = 12.0F;
            this.hand.xRot = 0.0F;
            this.hand.yRot = 0.0F;
        } else {
            // idle hover: a gentle bob + slow spin so it reads as adrift, not parked
            this.hand.y = 12.0F + Mth.sin(ageInTicks * 0.1F) * 0.5F;
            this.hand.xRot = 0.0F;
            this.hand.yRot = ageInTicks * 0.05F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        this.hand.translateAndRotate(poseStack);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // intentionally empty -- hand has no cubes, ItemInHandLayer is this mob's only visual
    }
}
