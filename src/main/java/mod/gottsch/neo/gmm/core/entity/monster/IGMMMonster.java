package mod.gottsch.neo.gmm.core.entity.monster;

import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The common contract for all GMM monsters. Extends {@link IOwnable} so every GMM mob carries
 * ownership (owner UUID + type + lifespan) directly on its own fields/synced data -- the "GMM half"
 * of the ownership system; non-GMM mobs get the same contract via a capability (see
 * {@link mod.gottsch.neo.gmm.core.entity.ownership.Ownership}).
 *
 * @author by Mark Gottschling on 11/2/2025
 */
public interface IGMMMonster extends IOwnable {
    // /// NOTE
    // these methods are all from other vanilla interfaces or abstract classes
    // but they are added here to ensure that this singular interface can be used
    // for all GMM monsters. (getOwnerId/setOwnerId now come from IOwnable.)
    // ///
    public void defineSynchedData();
    public void addAdditionalSaveData(CompoundTag tag);
    public void readAdditionalSaveData(CompoundTag tag);
    // /// END of NOTE

    /**
     * Resolves the owner (the entity that summoned/conjured this mob) from the stored UUID.
     */
    @Nullable
    LivingEntity getSummonedOwner();

    /**
     * Whether a mob summoned via {@code SummonGoal} should have its owner set to the summoner.
     * Most GMM mobs are never chance-summoned by another mob, so this defaults to false.
     */
    default boolean canSummonedHaveOwner() {
        return false;
    }

    /**
     * The live list of UUIDs this mob currently enthralls via {@link Ownership#enthrall}. Mutable --
     * callers add/remove UUIDs directly. Only meaningful for a mob that actually casts Enthrall (e.g.
     * Beholder); every other GMM mob just carries an always-empty list.
     */
    List<UUID> getThralls();
}
