package mod.gottsch.neo.gmm.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.projectile.SpikeGrowthSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * Renders a {@link SpikeGrowthSpell} as a tapered stalagmite — a {@code BASE} block at the ground, an
 * optional stretch of {@code MIDDLE} blocks, a {@code FRUSTUM} taper, and a {@code TIP} at the top —
 * using vanilla's own {@code pointed_dripstone} block states (block-entity rendering, no new texture,
 * see the spell dev guide §2) rather than stacking identical copies of one shape, which reads as
 * repeated cones instead of a single natural spike.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class SpikeGrowthSpellRenderer extends EntityRenderer<SpikeGrowthSpell> {

	public SpikeGrowthSpellRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SpikeGrowthSpell entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		MobConfig config = MobConfigHelper.get(entity);
		int telegraphTicks = (int) config.number("telegraphTicks", 40.0);
		int riseTicks = (int) config.number("riseTicks", 6.0);
		int columnHeight = Math.max(1, (int) config.number("columnHeight", 2.0));

		// nothing has erupted yet during the telegraph -- don't draw a spike that "already happened"
		float ticksSinceErupt = (entity.tickCount - telegraphTicks) + partialTick;
		if (ticksSinceErupt < 0.0F) {
			return;
		}
		float riseProgress = Mth.clamp(ticksSinceErupt / riseTicks, 0.0F, 1.0F);

		poseStack.pushPose();
		// BlockState models are drawn from a corner, not centered; the buried offset shrinks to 0 as
		// riseProgress goes 0 -> 1, so the stack visually pushes up out of the ground rather than
		// popping in at full height.
		poseStack.translate(-0.5D, -columnHeight * (1.0D - riseProgress), -0.5D);

		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		for (int i = 0; i < columnHeight; i++) {
			dispatcher.renderSingleBlock(thicknessAt(i, columnHeight), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
			poseStack.translate(0.0D, 1.0D, 0.0D);
		}

		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
	}

	/** Bottom layer = BASE, top layer = TIP, one FRUSTUM taper just under the tip, MIDDLE for any rest. */
	private static BlockState thicknessAt(int index, int total) {
		DripstoneThickness thickness;
		if (total == 1) {
			thickness = DripstoneThickness.TIP;
		} else if (index == 0) {
			thickness = DripstoneThickness.BASE;
		} else if (index == total - 1) {
			thickness = DripstoneThickness.TIP;
		} else if (index == total - 2) {
			thickness = DripstoneThickness.FRUSTUM;
		} else {
			thickness = DripstoneThickness.MIDDLE;
		}
		return Blocks.POINTED_DRIPSTONE.defaultBlockState()
				.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP)
				.setValue(PointedDripstoneBlock.THICKNESS, thickness);
	}

	@Override
	public ResourceLocation getTextureLocation(SpikeGrowthSpell entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
