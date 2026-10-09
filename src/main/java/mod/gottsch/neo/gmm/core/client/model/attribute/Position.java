package mod.gottsch.neo.gmm.core.client.model.attribute;

import net.minecraft.client.model.geom.ModelPart;
/**
 * @author by Mark Gottschling on 11/6/2025
 */
public record Position(float x, float y, float z) {
    public Position(ModelPart part) {
        this(part.x, part.y, part.z);
    }
}
