package mod.gottsch.neo.gmm.core.entity.ownership;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The ownership contract: the raw, backing-agnostic state of "who owns this entity, in what way, and
 * (if summoned) for how much longer". Deliberately pure data -- no {@code Level}/resolution logic --
 * so it can be backed either by a GMM mob's own fields/synced data ({@code IGMMMonster}) or by a
 * capability/attachment on a non-GMM entity. All resolution, persistence and lifespan behavior lives
 * in {@link Ownership}, which operates through this contract.
 *
 * @author Mark Gottschling
 */
public interface IOwnable {

    /** @return the owner's UUID, or null if unowned. */
    @Nullable
    UUID getOwnerId();

    void setOwnerId(@Nullable UUID uuid);

    /** @return the kind of ownership; never null (defaults to {@link OwnershipType#NONE}). */
    OwnershipType getOwnershipType();

    void setOwnershipType(OwnershipType type);

    /**
     * Remaining lifespan in ticks for a {@link OwnershipType#SUMMONED} mob. A negative value means
     * "no lifespan" (permanent); 0 or below on a SUMMONED mob triggers unsummoning.
     */
    int getRemainingLifespan();

    void setRemainingLifespan(int ticks);

    /**
     * The standing order for a {@link OwnershipType#THRALL} mob; meaningless for other ownership
     * types. Never null (defaults to {@link ThrallOrder#FOLLOW}).
     */
    ThrallOrder getThrallOrder();

    void setThrallOrder(ThrallOrder order);

    /** The bound position for a {@link ThrallOrder#GUARD} order, or null if none has been set. */
    @Nullable
    BlockPos getGuardPos();

    void setGuardPos(@Nullable BlockPos pos);
}
