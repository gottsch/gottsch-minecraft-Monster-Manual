package mod.gottsch.neo.gmm.core.client.model.attribute;

import net.minecraft.client.model.geom.ModelPart;
/**
 * @author by Mark Gottschling on 11/6/2025
 */
public record Rotation(float x, float y, float z) {
    public Rotation(ModelPart part) {
        this(part.xRot, part.yRot, part.zRot);
    }
}
