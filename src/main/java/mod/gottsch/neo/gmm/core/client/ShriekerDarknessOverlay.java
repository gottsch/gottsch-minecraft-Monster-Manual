package mod.gottsch.neo.gmm.core.client;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.effect.GMMMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Draws a capped-opacity black screen overlay while the local player has
 * {@link GMMMobEffects#SHRIEKER_DARKNESS}, in place of vanilla {@code MobEffects.DARKNESS} -- vanilla's
 * own Darkness shader always ramps to full peak opacity while active with no partial-intensity control
 * (amplifier doesn't change how dark it gets, only duration changes how long it lasts), which read as
 * "absolute blindness" rather than the oppressive-but-not-total dark the user asked for.
 *
 * <p>The target opacity is decoded from the effect instance's own amplifier (0..100, reconstructed to a
 * 0..1 fraction below) rather than a hardcoded constant here -- {@code Shrieker} encodes its
 * {@code gmm:mob_config}-driven {@code darknessOpacity} into the amplifier when it applies the effect
 * (see that class's {@code triggerAlert}), the same "repurpose an existing numeric field to carry data"
 * trick {@code GMMParticles.SLIME_TRAIL} already uses for its own RGB tint. This overlay class itself
 * stays fully generic -- it never references {@code gmm:mob_config} or any consumer namespace, it just
 * decodes whatever amplifier its own registered effect happens to carry.
 *
 * <p>No fade-in: the darkness snaps to its target opacity the instant the effect lands (reads as
 * "startled by a shriek," matching the sound cue landing at the same moment) and fades back to normal
 * over the last {@link #FADE_TICKS} of the effect's remaining duration -- computed purely from
 * {@link MobEffectInstance#getDuration()} (ticks remaining), since a {@code MobEffectInstance} doesn't
 * retain its own original total duration once applied, so a symmetric fade-in would need extra
 * bookkeeping this effect doesn't need for the fade-out-only shape it actually wants.
 *
 * @author Mark Gottschling on 7/10/2026
 */
@EventBusSubscriber(modid = GMM.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ShriekerDarknessOverlay {

    private static final float FADE_TICKS = 10.0F; // fade back to normal over the last ~0.5s

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MobEffectInstance instance = mc.player.getEffect(GMMMobEffects.SHRIEKER_DARKNESS);
        if (instance == null) {
            return;
        }
        float targetOpacity = Mth.clamp(instance.getAmplifier() / 100.0F, 0.0F, 1.0F);
        float alpha = Math.min(1.0F, instance.getDuration() / FADE_TICKS) * targetOpacity;
        int color = (int) (alpha * 255.0F) << 24; // black, alpha channel only

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
    }
}
