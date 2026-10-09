package mod.gottsch.neo.gmm.core.effect;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * gmm-owned status effects. Like {@link mod.gottsch.neo.gmm.core.sound.GMMSounds} /
 * {@link mod.gottsch.neo.gmm.core.particle.GMMParticles}, this is registry <em>infrastructure</em> a
 * mob AI needs to be usable (a {@link MobEffect} must be registered before
 * {@code LivingEntity#addEffect} can apply it) rather than player-facing content -- nothing ever
 * grants it via a potion, item, or command; only GMM mob code applies it directly.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public class GMMMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GMM.MOD_ID);

    /**
     * Stacking, temporary max-health reduction -- the Wight's life-drain bite. Each amplifier level
     * scales the attribute modifier automatically (vanilla's built-in {@code MobEffect} attribute-
     * modifier scaling, the same mechanism {@code MobEffects.HEALTH_BOOST} uses); vanilla's own
     * {@code MobEffectInstance} duration/removal machinery re-clamps the target's health when the
     * effect expires, so no bespoke expiry tracking is needed (see {@code Wight#applyWither}).
     */
    public static final RegistryObject<MobEffect> WITHERED = EFFECTS.register("withered",
            // MobEffect's constructor is protected (package-private-ish); the empty {} anonymous
            // subclass body is what lets a caller outside net.minecraft.world.effect invoke it.
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4B3B47) {}
                    .addAttributeModifier(Attributes.MAX_HEALTH, "f22b1e0a-6d1e-4e0a-9b1a-6b2c1a9d5e3a",
                            -2.0D, AttributeModifier.Operation.ADDITION));

    /**
     * A pure marker effect -- no attribute modifiers, no vanilla behavior -- applied by {@code Shrieker}
     * instead of vanilla {@code MobEffects.DARKNESS}. Vanilla Darkness has no partial-intensity control:
     * its client-side screen shader always ramps to full peak opacity while active, regardless of
     * amplifier. Using GMM's own marker effect instead lets {@code core.client.ShriekerDarknessOverlay}
     * draw a capped-opacity black overlay keyed off this effect's presence/remaining duration, without
     * vanilla's own Darkness renderer ever getting involved.
     */
    public static final RegistryObject<MobEffect> SHRIEKER_DARKNESS = EFFECTS.register("shrieker_darkness",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x1A0D24) {});

    /**
     * A brief, full movement root -- Beholderkin's Paralysis eye ray (see {@code ParalysisSpell}).
     * Unlike vanilla Slowness (a partial speed reduction that still reads as "Slowness" in the HUD),
     * this zeroes {@link Attributes#MOVEMENT_SPEED} outright via a {@code MULTIPLY_TOTAL -1.0}
     * modifier -- the standard "full stop" attribute trick, same mechanism as {@link #WITHERED}'s
     * health drain, just applied to a different attribute -- so a struck target genuinely can't walk
     * for the duration. Deliberately its own effect/name rather than vanilla Slowness dressed up:
     * a target-can't-move root is a materially different mechanic, fitting for the "generally
     * powerful beings" (Beholder/Gazer/DeathTyrant/Spectator) that cast it.
     */
    public static final RegistryObject<MobEffect> PARALYZED = EFFECTS.register("paralyzed",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8B2FC9) {}
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "b7b0b1b2-9c3d-4e2a-8f1a-2d6e4c9a7b1f",
                            -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** Called from the GMM constructor to attach the registry to the mod event bus. */
    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
