package mod.gottsch.neo.gmm.core.client.model;

import mod.gottsch.neo.gmm.core.client.model.attribute.Rotation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * A custom humanlike model with wings + a tail (demon/gargoyle silhouette). Subclasses supply the
 * geometry and wire the wing/tail {@link ModelPart}s + their rest rotations; this base drives the
 * wing-flap and tail-swing animation. Extends {@link HumanlikeModel} for the shared arm/leg/head
 * helpers and attack animation.
 *
 * @author by Mark Gottschling on 7/15/2025
 */
public abstract class DemonlikeModel<T extends Entity> extends HumanlikeModel<T> implements IDemonlikeModel {
    protected ModelPart tail;
    protected ModelPart upperTail;
    protected ModelPart lowerTail;
    protected ModelPart rightWing;
    protected ModelPart rightWingAxis;
    protected ModelPart rightWingMedius;
    protected ModelPart leftWing;
    protected ModelPart leftWingAxis;
    protected ModelPart leftWingMedius;

    protected Rotation rightWingRots;
    protected Rotation leftWingRots;

    protected Rotation tailRots;

    protected DemonlikeModel(ModelPart root) {
    }

    @Override
    public void swingTail(float ageInTicks) {
        Optional.ofNullable(getTail()).ifPresent(tail -> {
            tail.xRot = getTailOriginalRotation().x() + Mth.cos(ageInTicks * getTailSpeeds().x()) * getTailSwings().x();
            tail.zRot = Mth.cos(ageInTicks * getTailSpeeds().z()) * getTailSwings().z();
        });
    }

    @Override
    public void flapWings(float age) {
        float flapRotation = (Mth.cos(age * getWingSpeed()) * getWingSwing() + 0.05F);
        Optional.ofNullable(getRightWing()).ifPresent(wing -> {
            wing.yRot = getRightWingOriginalRotation().y() + flapRotation;
        });
        Optional.ofNullable(getLeftWing()).ifPresent(wing -> {
            wing.yRot = getLeftWingOriginalRotation().y() - flapRotation;
        });
    }

    @Override
    public ModelPart getRightWing() {
        return this.rightWing;
    }

    @Override
    public ModelPart getLeftWing() {
        return this.leftWing;
    }

    @Override
    public Rotation getRightWingOriginalRotation() {
        return rightWingRots;
    }

    @Override
    public Rotation getLeftWingOriginalRotation() {
        return leftWingRots;
    }

    @Override
    public ModelPart getTail() {
        return this.tail;
    }

    @Override
    public Rotation getTailOriginalRotation() {
        return this.tailRots;
    }

    public ModelPart getLowerTail() {
        return this.lowerTail;
    }
}
