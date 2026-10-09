package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.client.model.attribute.Position;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * @author by Mark Gottschling on 7/16/2025
 */
public interface IHumanlikeModel {
    public static final float BIPEDAL_MAGIC_SWING_MULTIPLIER = 1.4F;

    default public void swingLegs(float limbSwing, float limbSwingAmount, float age, float walkSpeed, float maxAngle) {
        getRightLeg().ifPresent(leg -> leg.xRot = Mth.cos(limbSwing * walkSpeed) * maxAngle  * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount);
        getLeftLeg().ifPresent(leg -> leg.xRot = Mth.cos(limbSwing  * walkSpeed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount);
    }

    default public void swingArms(float limbSwing, float limbSwingAmount, float age, float speed, float maxAngle)	{
        getRightArm().ifPresent(arm -> arm.xRot = Mth.cos(limbSwing * speed) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount);
        getLeftArm().ifPresent(arm -> arm.xRot = Mth.cos(limbSwing * speed + (float)Math.PI) * maxAngle * BIPEDAL_MAGIC_SWING_MULTIPLIER * limbSwingAmount);

    }

    default public void swivelHead(float netHeadYaw, float headPitch, float ageInTicks) {
        getHead().ifPresent(head -> {
            head.yRot = netHeadYaw * ((float)Math.PI / 180F);
            head.xRot = headPitch * ((float)Math.PI / 180F);
        });
    }

    default public void swivelHead(float netYaw, float pitch, float minYaw, float maxYaw, float minPitch, float maxPitch, float ageInTicks) {
        swivelHead(netYaw, pitch, ageInTicks);
    }

    /*
     * this is a y-position small bob
     */
    default public void bobUpperBody(ModelPart part, Position original, float ageInTicks, float direction) {
        part.y = original.y() + (Mth.cos(ageInTicks * 0.15F) * 0.25F + 0.05F);
    }

    default public Optional<ModelPart> getHead() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getBody() {
        return Optional.empty();
    }

    /*
     * entire torso - chest, stomach, pelvis, sometimes head. ie not arms & legs.
     * depends on the needs of the model
     */
    default public Optional<ModelPart> getTorso() {
        return Optional.empty();
    }

    /*
     * upper torso - chest and sometimes head
     * depends on the needs of the model.
     */
    default public Optional<ModelPart> getChest() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getRightArm() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getLeftArm() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getRightLeg() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getLeftLeg() {
        return Optional.empty();
    }
}
