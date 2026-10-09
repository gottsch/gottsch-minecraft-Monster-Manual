package mod.gottsch.neo.gmm.core.entity.ownership;

import mod.gottsch.neo.gmm.core.capability.GMMCapabilities;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.ThrallAttackOrderGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.thrall.ThrallFollowOwnerGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.thrall.ThrallGuardGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.thrall.ThrallStayGoal;
import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The single access point + shared behavior for entity ownership.
 * <p>
 * <b>The portability boundary.</b> Ownership state can be backed two ways: a GMM mob stores it in its
 * own fields/synced data (it implements {@link IOwnable} directly), while any other entity carries it
 * on a capability/attachment. {@link #of(Entity)} hides that split so every caller works the same for
 * a GMM mob or a vanilla one. When porting to NeoForge, only the non-GMM branch of {@link #of(Entity)}
 * (and {@link GMMCapabilities}) changes -- Forge capability becomes a Data Attachment; everything else
 * here is loader-agnostic.
 *
 * @author Mark Gottschling
 */
public final class Ownership {
    /** NBT keys (shared by GMM-mob save data and the capability provider). "Owner" matches vanilla. */
    public static final String TAG_OWNER = "Owner";
    public static final String TAG_TYPE = "OwnershipType";
    public static final String TAG_LIFESPAN = "SummonLifespan";
    public static final String TAG_THRALL_ORDER = "ThrallOrder";
    public static final String TAG_GUARD_POS = "GuardPos";

    /** No-op view returned for entities that can't be owned (players, non-living), so callers never NPE. */
    private static final IOwnable NULL = new IOwnable() {
        @Override public UUID getOwnerId() { return null; }
        @Override public void setOwnerId(UUID uuid) { }
        @Override public OwnershipType getOwnershipType() { return OwnershipType.NONE; }
        @Override public void setOwnershipType(OwnershipType type) { }
        @Override public int getRemainingLifespan() { return -1; }
        @Override public void setRemainingLifespan(int ticks) { }
        @Override public ThrallOrder getThrallOrder() { return ThrallOrder.FOLLOW; }
        @Override public void setThrallOrder(ThrallOrder order) { }
        @Override public BlockPos getGuardPos() { return null; }
        @Override public void setGuardPos(BlockPos pos) { }
    };

    private Ownership() { }

    /**
     * Resolves the ownership state for any entity: the entity itself if it's a GMM mob, otherwise its
     * ownership capability, otherwise a no-op view.
     */
    public static IOwnable of(Entity entity) {
        if (entity instanceof IOwnable ownable) {
            return ownable;
        }
        return entity.getCapability(GMMCapabilities.OWNERSHIP).orElse(NULL);
    }

    /**
     * Resolves the live owner entity (the summoner/conjurer) from the stored UUID. Handles both player
     * owners and mob owners (the latter only resolvable server-side).
     *
     * @return the owning LivingEntity, or null if there is no owner or it can't be resolved.
     */
    @Nullable
    public static LivingEntity resolveOwner(Entity mob) {
        UUID uuid = of(mob).getOwnerId();
        if (uuid == null) {
            return null;
        }
        Level level = mob.level();
        Player player = level.getPlayerByUUID(uuid);
        if (player != null) {
            return player;
        }
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /** @return true if {@code candidate} is {@code mob}'s owner. */
    public static boolean isOwnedBy(Entity mob, @Nullable Entity candidate) {
        UUID uuid = of(mob).getOwnerId();
        return uuid != null && candidate != null && uuid.equals(candidate.getUUID());
    }

    /** Stamps ownership onto a mob. Lifespan is only retained for {@link OwnershipType#SUMMONED}. */
    public static void stampOwnership(Entity mob, @Nullable LivingEntity owner, OwnershipType type, int lifespan) {
        IOwnable ownable = of(mob);
        ownable.setOwnerId(owner == null ? null : owner.getUUID());
        ownable.setOwnershipType(type);
        ownable.setRemainingLifespan(type == OwnershipType.SUMMONED ? lifespan : -1);
    }

    /**
     * Enthralls {@code target}: stamps it as a permanent {@link OwnershipType#THRALL} of
     * {@code owner} with a default standing order of {@link ThrallOrder#FOLLOW}. The <b>owner</b> is
     * always a mob (e.g. a Beholder enthralling a zombie) -- there is no player-facing enthrall path.
     * <p>
     * The first time a mob is enthralled, the reusable Thrall AI goals ({@link ThrallStayGoal},
     * {@link ThrallGuardGoal}, {@link ThrallFollowOwnerGoal}, {@link ThrallAttackOrderGoal}) are added
     * directly to its {@code goalSelector}/{@code targetSelector}. This works for any {@link Mob} --
     * GMM's own or a vanilla/other-mod one -- since goal injection needs no GMM interface on the
     * target; re-enthralling an existing thrall (e.g. a second cast) just re-stamps the owner without
     * adding duplicate goals.
     */
    public static void enthrall(Mob target, LivingEntity owner) {
        boolean alreadyThrall = of(target).getOwnershipType() == OwnershipType.THRALL;
        stampOwnership(target, owner, OwnershipType.THRALL, -1);
        IOwnable ownable = of(target);
        ownable.setThrallOrder(ThrallOrder.FOLLOW);
        ownable.setGuardPos(null);
        if (owner instanceof IGMMMonster enthraller && !enthraller.getThralls().contains(target.getUUID())) {
            enthraller.getThralls().add(target.getUUID());
        }
        if (!alreadyThrall) {
            target.goalSelector.addGoal(1, new ThrallStayGoal(target));
            target.goalSelector.addGoal(2, new ThrallGuardGoal(target, 1.0D));
            target.goalSelector.addGoal(3, new ThrallFollowOwnerGoal(target, 1.0D));
            target.targetSelector.addGoal(1, new ThrallAttackOrderGoal(target));
        }
    }

    /**
     * Resolves an owner's thrall list ({@link IGMMMonster#getThralls()}) to live {@link Mob}
     * references, pruning any UUID that no longer resolves to a living mob (e.g. it died) from the
     * owner's list as a side effect. Returns an empty list for a non-{@link IGMMMonster} owner or off
     * the server thread. Note: a thrall in an unloaded chunk is indistinguishable from a dead one here
     * and will be (harmlessly) pruned too -- same caveat vanilla has for any UUID-keyed entity list.
     */
    public static List<Mob> getLiveThralls(LivingEntity owner) {
        if (!(owner instanceof IGMMMonster enthraller) || !(owner.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        List<Mob> live = new ArrayList<>();
        Iterator<UUID> iterator = enthraller.getThralls().iterator();
        while (iterator.hasNext()) {
            Entity entity = serverLevel.getEntity(iterator.next());
            if (entity instanceof Mob mob && mob.isAlive()) {
                live.add(mob);
            } else {
                iterator.remove();
            }
        }
        return live;
    }

    /** Issues a standing {@link ThrallOrder#FOLLOW}/{@link ThrallOrder#STAY} order to a thrall. */
    public static void issueOrder(Mob thrall, ThrallOrder order) {
        of(thrall).setThrallOrder(order == null ? ThrallOrder.FOLLOW : order);
    }

    /** Orders a thrall to hold {@code pos}, returning to it once idle. */
    public static void issueGuardOrder(Mob thrall, BlockPos pos) {
        IOwnable ownable = of(thrall);
        ownable.setGuardPos(pos);
        ownable.setThrallOrder(ThrallOrder.GUARD);
    }

    /**
     * Orders a thrall to pursue and fight a specific target, overriding whatever it (or its owner)
     * was doing. One-shot: {@link ThrallAttackOrderGoal} reverts the order to FOLLOW once the target
     * dies or is otherwise lost.
     */
    public static void issueAttackOrder(Mob thrall, LivingEntity target) {
        of(thrall).setThrallOrder(ThrallOrder.ATTACK);
        thrall.setTarget(target);
    }

    /**
     * Server-tick hook for a summoned mob's lifespan: counts down and unsummons at expiry. A negative
     * lifespan means "permanent" and is left alone. No-op for non-SUMMONED ownership.
     */
    public static void tickLifespan(Mob mob) {
        IOwnable ownable = of(mob);
        if (ownable.getOwnershipType() != OwnershipType.SUMMONED) {
            return;
        }
        int remaining = ownable.getRemainingLifespan();
        if (remaining < 0) {
            return;
        }
        if (remaining <= 0) {
            unsummon(mob);
        } else {
            ownable.setRemainingLifespan(remaining - 1);
        }
    }

    /** "Unsummons" a mob: a puff of poof particles + a dispel sound, then removes it (no loot/death). */
    public static void unsummon(Mob mob) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            for (int p = 0; p < 20; p++) {
                double xSpeed = serverLevel.random.nextGaussian() * 0.02D;
                double ySpeed = serverLevel.random.nextGaussian() * 0.02D;
                double zSpeed = serverLevel.random.nextGaussian() * 0.02D;
                serverLevel.sendParticles(ParticleTypes.POOF, mob.getRandomX(0.5D), mob.getRandomY(), mob.getRandomZ(0.5D), 1, xSpeed, ySpeed, zSpeed, 0.15D);
            }
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, mob.getSoundSource(), 1.0F, 1.0F);
        }
        mob.discard();
    }

    /** Writes ownership state to {@code tag}. Only the "Owner" UUID (modern form) is written here. */
    public static void save(CompoundTag tag, IOwnable ownable) {
        if (ownable.getOwnerId() != null) {
            tag.putUUID(TAG_OWNER, ownable.getOwnerId());
        }
        if (ownable.getOwnershipType() != OwnershipType.NONE) {
            tag.putString(TAG_TYPE, ownable.getOwnershipType().name());
        }
        if (ownable.getRemainingLifespan() >= 0) {
            tag.putInt(TAG_LIFESPAN, ownable.getRemainingLifespan());
        }
        if (ownable.getOwnershipType() == OwnershipType.THRALL) {
            tag.putString(TAG_THRALL_ORDER, ownable.getThrallOrder().name());
            BlockPos guardPos = ownable.getGuardPos();
            if (guardPos != null) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("X", guardPos.getX());
                posTag.putInt("Y", guardPos.getY());
                posTag.putInt("Z", guardPos.getZ());
                tag.put(TAG_GUARD_POS, posTag);
            }
        }
    }

    /**
     * Reads ownership state from {@code tag}. Only the modern UUID form of "Owner" is handled here; a
     * caller that must support the legacy string-name owner (see {@code OldUsersConverter}) should
     * handle that itself after calling this.
     */
    public static void load(CompoundTag tag, IOwnable ownable) {
        if (tag.hasUUID(TAG_OWNER)) {
            ownable.setOwnerId(tag.getUUID(TAG_OWNER));
        }
        ownable.setOwnershipType(OwnershipType.byName(tag.getString(TAG_TYPE)));
        ownable.setRemainingLifespan(tag.contains(TAG_LIFESPAN) ? tag.getInt(TAG_LIFESPAN) : -1);
        ownable.setThrallOrder(tag.contains(TAG_THRALL_ORDER) ? ThrallOrder.byName(tag.getString(TAG_THRALL_ORDER)) : ThrallOrder.FOLLOW);
        if (tag.contains(TAG_GUARD_POS)) {
            CompoundTag posTag = tag.getCompound(TAG_GUARD_POS);
            ownable.setGuardPos(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
        } else {
            ownable.setGuardPos(null);
        }
    }

    /** Writes an enthraller's thrall-UUID list ({@link IGMMMonster#getThralls()}) to {@code tag}. */
    public static void saveThralls(CompoundTag tag, List<UUID> thralls) {
        if (thralls.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (UUID uuid : thralls) {
            list.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Thralls", list);
    }

    /** Reads an enthraller's thrall-UUID list from {@code tag} into {@code thralls} (cleared first). */
    public static void loadThralls(CompoundTag tag, List<UUID> thralls) {
        thralls.clear();
        if (tag.contains("Thralls", net.minecraft.nbt.Tag.TAG_LIST)) {
            ListTag list = tag.getList("Thralls", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            for (int i = 0; i < list.size(); i++) {
                thralls.add(NbtUtils.loadUUID(list.get(i)));
            }
        }
    }
}
