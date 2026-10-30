package mod.gottsch.forge.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.client.model.LeechModel;
import mod.gottsch.forge.gmm.core.entity.monster.Leech;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a {@link Leech} swimming, or clinging to its victim.
 *
 * <h2>Attached</h2>
 * <p>An attached leech is an ordinary entity drawn in the world, not a layer on the player model. A
 * layer would follow the legs' swing, but the player's own body is never drawn in first person, so a
 * player could never see what is on them. As a world entity, the leeches on the front of the legs
 * show when the player looks down.</p>
 * <p>Position comes from the victim, not the leech: {@link #setupRotations} moves the pose from the
 * leech's own interpolated position to {@link Leech#socketPosition} at this frame's partial tick. The
 * two entities tick in no fixed order, so drawing the leech where it last ticked would leave it a
 * frame behind a running player. Then it turns with the victim's body, lies flat against it head-up
 * (belly toward the body), and pitches forward with the victim when they swim.</p>
 *
 * <p>Constructor overloads match {@link RatRenderer}: the consumer draws the giant tier with
 * {@code new LeechRenderer<>(context, 2.0F)}. Each feeding stage adds 15% on top of that.</p>
 *
 * @author Mark Gottschling on 10/25/2026
 */
public class LeechRenderer<T extends Leech> extends MobRenderer<T, LeechModel<T>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GMM.MOD_ID, "textures/entity/leech.png");
    private static final float SHADOW = 0.2F;
    /** Extra size per feeding stage. */
    private static final float STAGE_GROWTH = 0.15F;

    private ResourceLocation texture = TEXTURE;
    private float scale = 1.0F;

    public LeechRenderer(EntityRendererProvider.Context context) {
        super(context, new LeechModel<>(context.bakeLayer(LeechModel.LAYER_LOCATION)), SHADOW);
    }

    public LeechRenderer(EntityRendererProvider.Context context, float scale) {
        this(context);
        this.scale = scale;
    }

    public LeechRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context);
        this.texture = texture;
    }

    public LeechRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        this(context, texture);
        this.scale = scale;
    }

    @Override
    public void render(T leech, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        // no shadow on a leech that is on someone; the shadow radius is read after render()
        this.shadowRadius = leech.isAttached() ? 0.0F : SHADOW * this.scale;
        super.render(leech, yaw, partialTick, poseStack, buffer, light);
    }

    private float totalScale(T leech) {
        return this.scale * (1.0F + STAGE_GROWTH * leech.getFeedStage());
    }

    @Override
    protected void scale(T leech, PoseStack poseStack, float partialTick) {
        float s = totalScale(leech);
        poseStack.scale(s, s, s);
    }

    @Override
    protected void setupRotations(T leech, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        LivingEntity victim = leech.isAttached() ? leech.getVictim() : null;
        if (victim == null) {
            super.setupRotations(leech, poseStack, ageInTicks, rotationYaw, partialTick);
            return;
        }
        // from where the leech is drawn to where it belongs on the victim this frame
        Vec3 socket = leech.socketPosition(victim, partialTick);
        double x = Mth.lerp(partialTick, leech.xo, leech.getX());
        double y = Mth.lerp(partialTick, leech.yo, leech.getY());
        double z = Mth.lerp(partialTick, leech.zo, leech.getZ());
        // socketPosition is the bottom of the leech's box; aim at the box's middle
        poseStack.translate(socket.x - x, socket.y - y + leech.getBbHeight() / 2.0D, socket.z - z);

        float bodyYaw = Mth.rotLerp(partialTick, victim.yBodyRotO, victim.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // as the victim swims, the body frame tips forward (up toward -z, the victim's forward)
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * victim.getSwimAmount(partialTick)));
        if (leech.isOnBack()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        // head up, belly toward the body (+z, behind the front face)
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        // the model's body centre, not its belly, sits on the socket (applied after the -1 flip and
        // this.scale(), so in the model's own y-up frame)
        poseStack.translate(0.0F, -LeechModel.CENTRE_HEIGHT * totalScale(leech), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T leech) {
        return this.texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public float getScale() {
        return this.scale;
    }
}
