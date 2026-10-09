package mod.gottsch.neo.gmm.core.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

/**
 * Generalized from Dungeon Denizens.
 *
 * @author Mark Gottschling on 7/1/2026
 *
 * @param <T>
 */
public abstract class BeholderkinModel<T extends Entity> extends EntityModel<T> {

    public BeholderkinModel() {
        super();
    }

    public BeholderkinModel(Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
    }

    public void setRestrictedEyeRotations(float netYaw, float pitch, float minYaw, float maxYaw, float minPitch, float maxPitch) {
        // reset head
        getEye().xRot = getBody().xRot;

        if (netYaw < 0) {
            getEye().yRot = Math.max(minYaw, netYaw) * ((float)Math.PI / 180F);
        }
        else {
            getEye().yRot = Math.min(maxYaw, netYaw) * ((float)Math.PI / 180F);
        }

        if (pitch < 0) {
            getEye().xRot = Math.max(minPitch, pitch) * ((float)Math.PI / 180F);
        }
        else {
            getEye().xRot = Math.min(maxPitch, pitch) * ((float)Math.PI / 180F);
        }
    }

    public void resetTopEyeStalk(ModelPart eyeStalk, float xRot, float zRot) {
        eyeStalk.xRot = xRot;
        eyeStalk.zRot = zRot;
    }

    public void resetSideEyeStalk(ModelPart eyeStalk, float yRot, float zRot) {
        eyeStalk.yRot = yRot;
        eyeStalk.zRot = zRot;
    }

    public void bobTopEyeStalk(ModelPart stalk, ModelPart eye, float age, float radians, float direction, int stalkOffset, float stalkSpeed) {
        stalk.xRot += direction * (Mth.cos((age + stalkOffset) * stalkSpeed) * radians + 0.05F);
        stalk.zRot += direction * (Mth.cos((age + stalkOffset) * stalkSpeed) * radians + 0.05F);

        float eyeSpeed = 0.1F;
        eye.yRot = -direction * (Mth.cos((age + stalkOffset) * eyeSpeed) * 0.3490659F + 0.05F);
    }

    public void bobSideEyeStalk(ModelPart stalk, ModelPart eye, float age, float radians, float direction, int stalkOffset, float stalkSpeed, float eyeDirection) {
        stalk.yRot += direction * (Mth.sin((age + stalkOffset) * stalkSpeed) * radians + 0.05F);
        stalk.zRot += direction * (Mth.sin((age + stalkOffset) * stalkSpeed) *  radians + 0.05F);

        float eyeSpeed = 0.1F;
        eye.yRot = eyeDirection * (Mth.cos((age + stalkOffset) * eyeSpeed) * 0.3490659F + 0.05F);
    }

    public void bobMouthPart(ModelPart mouth, float age, float speed, float radians) {
        mouth.xRot -= Mth.cos(age * speed) * radians + 0.05F;
    }

    public abstract ModelPart getBody();
    public abstract ModelPart getEye();
}
