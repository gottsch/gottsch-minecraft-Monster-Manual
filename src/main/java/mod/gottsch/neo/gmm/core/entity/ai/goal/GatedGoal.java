package mod.gottsch.neo.gmm.core.entity.ai.goal;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.BooleanSupplier;

/**
 * Delegates every {@link Goal} method to a wrapped instance, but only lets {@code canUse}/
 * {@code canContinueToUse} succeed while {@code gate} is true — a single reusable wrapper instead of a
 * bespoke gated subclass per vanilla goal type (compare Gray Ooze's {@code GrayOozeStrollGoal}/
 * {@code GrayOozeMeleeAttackGoal}, which solve the exact same "don't act while disguised" problem with
 * a hand-written subclass per goal type).
 * <p>
 * Extracted from {@code Mimic} (its original, only consumer) once a second consumer needed the exact
 * same "every goal is inert until some trigger flips a flag" shape — see {@code AnimatedArmor}, which
 * mirrors the ambush idea but activates on proximity rather than a hit/interact.
 *
 * @author Mark Gottschling on 7/13/2026 -- extracted from Mimic
 */
public class GatedGoal extends Goal {
    private final Goal delegate;
    private final BooleanSupplier gate;

    public GatedGoal(Goal delegate, BooleanSupplier gate) {
        this.delegate = delegate;
        this.gate = gate;
        this.setFlags(delegate.getFlags());
    }

    @Override
    public boolean canUse() {
        return this.gate.getAsBoolean() && this.delegate.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.gate.getAsBoolean() && this.delegate.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return this.delegate.isInterruptable();
    }

    @Override
    public void start() {
        this.delegate.start();
    }

    @Override
    public void stop() {
        this.delegate.stop();
    }

    @Override
    public void tick() {
        this.delegate.tick();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return this.delegate.requiresUpdateEveryTick();
    }
}
