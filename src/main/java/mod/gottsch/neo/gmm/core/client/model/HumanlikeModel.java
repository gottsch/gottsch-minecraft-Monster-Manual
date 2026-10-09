package mod.gottsch.neo.gmm.core.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author by Mark Gottschling on 11/6/2025
 */
public abstract class HumanlikeModel<T extends Entity> extends EntityModel<T> implements IHumanlikeModel {

    public HumanlikeModel() {
        super();
    }

    public HumanlikeModel(Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
    }

    public abstract void resetSwing(T entity);

    public Optional<ModelPart> getAttackArm() {
        return getRightArm();
    }

    /**
     * from vanilla
     * @param entity
     * @param age
     */
    protected void setupAttackAnimation(T entity, float age) {
        resetSwing(entity);
        if (!(this.attackTime <= 0.0F)) {
            getAttackArm().ifPresent(attachArm -> {
                float f = this.attackTime;
                getBody().ifPresent(body -> {
                    body.yRot = Mth.sin(Mth.sqrt(f) * ((float)Math.PI * 2F)) * 0.2F;
                    getLeftArm().ifPresent(leftArm -> {
                        if (attachArm == leftArm) {
                            body.yRot *= -1.0F;
                        }
                    });
                });
            });

            getRightArm().ifPresent(rightArm -> {
                getBody().ifPresent(body -> {
                    rightArm.z = Mth.sin(body.yRot) * 5.0F;
                    rightArm.x = -Mth.cos(body.yRot) * 5.0F;
                    rightArm.yRot += body.yRot;
                });
            });

            getLeftArm().ifPresent(leftArm -> {
                getBody().ifPresent(body -> {
                    leftArm.z = -Mth.sin(body.yRot) * 5.0F;
                    leftArm.x = Mth.cos(body.yRot) * 5.0F;
                    leftArm.yRot += body.yRot;
                    leftArm.xRot += body.yRot;
                });
            });

            float f = 1.0F - this.attackTime;
            f *= f;
            f *= f;
            f = 1.0F - f;
            float f1 = Mth.sin(f * (float)Math.PI);
            getAttackArm().ifPresent(attackArm -> {
                getHead().ifPresent(head -> {
                    float f2 = Mth.sin(this.attackTime * (float)Math.PI) * -(head.xRot - 0.7F) * 0.75F;
                    attackArm.xRot = (float)((double)attackArm.xRot - ((double)f1 * 1.2D + (double)f2));
                    getBody().ifPresent(body -> attackArm.yRot += body.yRot * 2.0F);
                    attackArm.zRot += Mth.sin(this.attackTime * (float)Math.PI) * -0.4F;
                });
            });
        }
    }
}
