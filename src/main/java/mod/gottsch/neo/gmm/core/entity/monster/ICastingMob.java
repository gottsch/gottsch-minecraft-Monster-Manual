package mod.gottsch.neo.gmm.core.entity.monster;

/**
 * Implemented by a GMM mob that wants a visible "channeling a spell" tell -- a pose swap in its
 * model -- while an AI goal is charging up a cast (e.g. Wight's Summon/Enthrall abilities). A
 * charge-then-execute goal (see {@code EnthrallGoal}, {@code SummonThrallGoal}) calls
 * {@link #setCasting(boolean)} at {@code start()}/{@code stop()} if the caster implements this;
 * a caster that doesn't (e.g. Beholder, which has its own established cast feel) is unaffected --
 * the check is always {@code mob instanceof ICastingMob}, never a hard cast.
 *
 * @author Mark Gottschling on 7/9/2026
 */
public interface ICastingMob {
    void setCasting(boolean casting);
}
