package mod.gottsch.neo.gmm.core.capability;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Plain {@link IOwnable} backing store carried by the ownership capability on non-GMM entities. GMM
 * mobs implement {@link IOwnable} on themselves and never use this.
 *
 * @author Mark Gottschling
 */
public class OwnershipData implements IOwnable {
    @Nullable
    private UUID ownerUUID;
    private OwnershipType ownershipType = OwnershipType.NONE;
    private int remainingLifespan = -1;
    private ThrallOrder thrallOrder = ThrallOrder.FOLLOW;
    @Nullable
    private BlockPos guardPos;

    @Nullable
    @Override
    public UUID getOwnerId() {
        return ownerUUID;
    }

    @Override
    public void setOwnerId(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }

    @Override
    public OwnershipType getOwnershipType() {
        return ownershipType;
    }

    @Override
    public void setOwnershipType(OwnershipType type) {
        this.ownershipType = type == null ? OwnershipType.NONE : type;
    }

    @Override
    public int getRemainingLifespan() {
        return remainingLifespan;
    }

    @Override
    public void setRemainingLifespan(int ticks) {
        this.remainingLifespan = ticks;
    }

    @Override
    public ThrallOrder getThrallOrder() {
        return thrallOrder;
    }

    @Override
    public void setThrallOrder(ThrallOrder order) {
        this.thrallOrder = order == null ? ThrallOrder.FOLLOW : order;
    }

    @Nullable
    @Override
    public BlockPos getGuardPos() {
        return guardPos;
    }

    @Override
    public void setGuardPos(@Nullable BlockPos pos) {
        this.guardPos = pos;
    }
}
