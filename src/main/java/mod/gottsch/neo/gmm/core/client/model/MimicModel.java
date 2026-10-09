package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.entity.monster.mimic.Mimic;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Shared hidden / opening / idle-chomp animation state machine for the Mimic family. A Mimic stays in
 * the fully closed, motionless {@link #hiddenAnim} pose — reading as an ordinary placed chest/barrel/
 * etc — until {@link Mimic#isActive()} flips (server-side, on being hit or interacted with, see
 * {@link Mimic}). It doesn't jump straight to fully open: {@link #openAnim} interpolates the lid swing
 * using {@link Mimic#getOpenProgress()} (a synced float that ramps 0 {@literal ->} 1 over about a
 * second once activated), and only once that reaches 1 does it settle into the steady-state
 * {@link #activeAnim} idle, which opens wider while it {@link Mimic#hasTarget()}.
 * <p>
 * This differs from Treasure2's original {@code MimicModel} in where the open-progress value comes
 * from: Treasure2 derived it from raw {@code ageInTicks} (time since spawn), which only worked because
 * its Mimic always opened a fixed ~1s after spawning. Since this Mimic can stay disguised indefinitely
 * before anything triggers it, "time since spawn" doesn't mean "time since activation" — so the
 * progress is tracked as its own synced field on {@link Mimic} instead, incremented server-side only
 * once active.
 *
 * @param <T> the concrete Mimic entity this model renders
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's MimicModel
 */
public abstract class MimicModel<T extends Mimic> extends EntityModel<T> {
    protected static final float MAX_OPEN_RADIANS = -0.3926991F;
    protected static final float BODY_RADIANS = 0.2618F;

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isActive()) {
            hiddenAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        float openProgress = entity.getOpenProgress();
        if (openProgress < 1F) {
            openAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, openProgress);
        } else {
            activeAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
    }

    /**
     * Fully closed and motionless. Concrete classes with extra parts beyond body/lid (eyes, latch,
     * tongue, ...) should override to zero those too — see {@code VanillaChestMimicModel}.
     */
    public void hiddenAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        getBody().xRot = 0F;
        getLid().xRot = 0F;
    }

    /** Interpolates from the closed pose to the fully-open pose as {@code timeSlice} runs 0 {@literal ->} 1. */
    public abstract void openAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float timeSlice);

    public void activeAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        getBody().xRot = BODY_RADIANS;

        if (entity.hasTarget()) {
            bobMouth(getLid(), 22.5f, 22.5f, ageInTicks);
        } else {
            bobMouth(getLid(), 22.5f, 3f, ageInTicks);
        }

        bob(getBody(), getBodyY(), ageInTicks);
        // concrete classes should override to layer on any additional animation
    }

    public abstract ModelPart getBody();
    public abstract float getBodyY();
    public abstract ModelPart getLid();

    public float getMaxOpenAngle() {
        return MAX_OPEN_RADIANS;
    }

    public void bob(ModelPart part, float originY, float age) {
        part.y = originY + (Mth.cos(age * 0.25F) * 0.5F + 0.05F);
    }

    public void bobMouth(ModelPart mouth, float originRot, float maxRot, float age) {
        mouth.xRot = -(degToRad(originRot + Mth.cos(age * 0.25f) * maxRot));
    }

    protected static float degToRad(float degrees) {
        return degrees * (float) Math.PI / 180;
    }
}
