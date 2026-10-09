package mod.gottsch.neo.gmm.core.client.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Optional;

/**
 * @author by Mark Gottschling on 11/3/2025
 */
public interface IAnimalModel {
    default public Optional<ModelPart> getHead() {
        return Optional.empty();
    }
    default public Optional<ModelPart> getTail() {
        return Optional.empty();
    }
    default public Optional<ModelPart> getBody() {
        return Optional.empty();
    }

    default public Optional<ModelPart> getFrontRightLeg() {
        return Optional.empty();
    }
    default public Optional<ModelPart> getFrontLeftLeg() {
        return Optional.empty();
    }
    default public Optional<ModelPart> getRearRightLeg() {
        return Optional.empty();
    }
    default public Optional<ModelPart> getRearLeftLeg() {
        return Optional.empty();
    }
}
