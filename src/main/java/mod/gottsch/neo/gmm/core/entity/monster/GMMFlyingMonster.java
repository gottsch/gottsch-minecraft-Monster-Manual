package mod.gottsch.neo.gmm.core.entity.monster;


import net.minecraft.core.Holder;
import mod.gottsch.neo.gmm.core.config.MobConfig;
import mod.gottsch.neo.gmm.core.config.MobConfigHelper;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import mod.gottsch.neo.gmm.core.util.Anchor;
import mod.gottsch.neo.gmm.core.entity.ownership.OwnershipType;
import mod.gottsch.neo.gmm.core.entity.ownership.ThrallOrder;
import mod.gottsch.neo.gmm.core.util.CompanionSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Flying counterpart to {@link GMMMonster}: a {@link FlyingMob} base with the same owner tracking (an
 * owner is any entity that summons/conjures this one). Mirrors GMMMonster's owner implementation
 * because FlyingMob and Monster can't share it via inheritance; the shared logic lives in
 * {@link Ownership}, so only the synced-data accessor and the delegating overrides are duplicated here.
 *
 * @author by Mark Gottschling
 */
public abstract class GMMFlyingMonster extends FlyingMob implements OwnableEntity, IGMMMonster {
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(GMMFlyingMonster.class, EntityDataSerializers.OPTIONAL_UUID);

    // ownership kind + summon lifespan are server-side only (see Ownership); the owner UUID above is synced.
    private OwnershipType ownershipType = OwnershipType.NONE;
    private int remainingLifespan = -1;
    private ThrallOrder thrallOrder = ThrallOrder.FOLLOW;
    @Nullable
    private BlockPos guardPos;
    private final List<UUID> thralls = new ArrayList<>();

    protected GMMFlyingMonster(EntityType<? extends FlyingMob> entityType, Level level) {
        super(entityType, level);
    }

    public final Predicate<LivingEntity> playerNotOwner = (entity) -> {
        if (entity instanceof Player) {
            return getOwnerId() == null || !getOwnerId().equals(entity.getUUID());
        }
        return true;
    };

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNERUUID_ID, Optional.empty());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Ownership.tickLifespan(this);
    }

    /**
     * Mirrors {@link GMMMonster#finalizeSpawn} one-for-one (config-attribute overrides + spawn-time
     * companions) — duplicated rather than shared for the same reason as the rest of this class:
     * {@code FlyingMob} and {@code Monster} can't share it via inheritance.
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        applyConfigAttributes(MobConfigHelper.get(level, EntityType.getKey(this.getType())));
        trySpawnCompanions(level, spawnType);
        return spawnGroupData;
    }

    private void applyConfigAttributes(MobConfig config) {
        applyAttribute(Attributes.MAX_HEALTH, config, "maxHealth", true);
        applyAttribute(Attributes.MOVEMENT_SPEED, config, "movementSpeed", false);
        applyAttribute(Attributes.ATTACK_DAMAGE, config, "attackDamage", false);
        applyAttribute(Attributes.ATTACK_KNOCKBACK, config, "attackKnockback", false);
        applyAttribute(Attributes.KNOCKBACK_RESISTANCE, config, "knockbackResistance", false);
        applyAttribute(Attributes.ARMOR, config, "armor", false);
        applyAttribute(Attributes.ARMOR_TOUGHNESS, config, "armorToughness", false);
    }

    private void applyAttribute(Holder<Attribute> attribute, MobConfig config, String key, boolean healToFull) {
        Double value = config.properties().get(key);
        if (value == null) {
            return;
        }
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.setBaseValue(value);
        if (healToFull) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /** See {@link GMMMonster#getCompanionPool()} — same contract, duplicated for the FlyingMob side. */
    @Nullable
    protected TagKey<EntityType<?>> getCompanionPool() {
        return null;
    }

    /** See {@link GMMMonster#triggersCompanions} — identical logic, duplicated for the FlyingMob side. */
    private static boolean triggersCompanions(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL;
    }

    /** See {@link GMMMonster#trySpawnCompanions} — identical logic (now delegating to {@link CompanionSpawner}), duplicated for the FlyingMob side. */
    private void trySpawnCompanions(ServerLevelAccessor level, MobSpawnType spawnType) {
        TagKey<EntityType<?>> pool = getCompanionPool();
        if (pool == null || !triggersCompanions(spawnType)) {
            return;
        }
        MobConfig config = MobConfigHelper.get(this);
        if (!config.flag("spawnCompanions", true) || this.random.nextDouble() >= config.number("companionChance", 1.0D)) {
            return;
        }
        int min = (int) config.number("companionMin", 1);
        int max = Math.max(min, (int) config.number("companionMax", 2));
        int count = min >= max ? min : min + this.random.nextInt(max - min + 1);
        CompanionSpawner.spawnNear(this, pool, count);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        Ownership.save(tag, this);
        Ownership.saveThralls(tag, thralls);
        Anchor.save(tag, this);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Ownership.load(tag, this);
        Ownership.loadThralls(tag, thralls);
        Anchor.load(tag, this);
        // legacy fallback: a pre-UUID "Owner" stored as a player name string.
        if (getOwnerId() == null && tag.contains(Ownership.TAG_OWNER)) {
            UUID uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), tag.getString(Ownership.TAG_OWNER));
            if (uuid != null) {
                try {
                    this.setOwnerId(uuid);
                } catch (Throwable throwable) {
                }
            }
        }
    }

    /** Satisfies vanilla {@link OwnableEntity}, which fixes this exact method name -- {@link #getOwnerId()}
     * (GMM's own {@link mod.gottsch.neo.gmm.core.entity.ownership.IOwnable} contract) just delegates here. */
    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNERUUID_ID).orElse((UUID) null);
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return getOwnerUUID();
    }

    @Override
    public void setOwnerId(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(uuid));
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

    @Override
    public List<UUID> getThralls() {
        return thralls;
    }

    /** Undead behaviour a class can give itself without a tag; see {@link IGMMMonster#isUndead}. */
    @Override
    public boolean isInvertedHealAndHarm() {
        return isUndead() || super.isInvertedHealAndHarm();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (isUndead() && (effect.is(MobEffects.POISON) || effect.is(MobEffects.REGENERATION))) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /**
     * Resolves the owner (the entity that summoned/conjured this mob) from the stored UUID.
     * Handles both player owners and mob owners (the latter only resolvable server-side).
     *
     * @return the owning LivingEntity, or null if there is no owner or it can't be resolved.
     */
    @Nullable
    public LivingEntity getSummonedOwner() {
        return Ownership.resolveOwner(this);
    }

    // --- anchoring (a mob posted to guard a position) ---------------------------------------------
    //
    // See Anchor for the whole of why this is not just restrictTo(): vanilla neither persists the
    // restriction nor lets it gate despawning. Everything here is inert on a mob nobody anchored.

    /**
     * An anchored mob never despawns. The restriction IS the persistence flag, so a consumer that
     * posts a guardian gets both from one call and there is no second thing to remember.
     */
    @Override
    public void checkDespawn() {
        if (this.hasRestriction()) {
            return;
        }
        super.checkDespawn();
    }

    /**
     * The anchor holds while idle and lets go while engaged.
     *
     * <p>{@code WaterAvoidingRandomStrollGoal} picks its candidate positions through this method, so
     * an unqualified restriction does not merely fail to leash a mob mid-fight &mdash; it makes one
     * that chased an intruder out of its room keep trying to wander back for the rest of its life.
     * {@code hasRestriction()}/{@code getRestrictCenter()}/{@code getRestrictRadius()} are left
     * untouched, so {@link #checkDespawn} and anything else reading the post keep working off the
     * original placement however far combat has dragged the mob since.</p>
     */
    @Override
    public boolean isWithinRestriction(BlockPos pos) {
        if (isAnchorSuspended()) {
            return true;
        }
        return super.isWithinRestriction(pos);
    }

    /**
     * Whether the anchor is currently suspended. Default: while the mob has a target.
     *
     * <p>Overridden by mobs with a dormant/active split of their own, where "engaged" is a phase
     * rather than a target &mdash; a buried grave zombie and an unactivated suit of armour both have
     * an anchor that must hold through more than just "no target yet".</p>
     */
    protected boolean isAnchorSuspended() {
        return this.getTarget() != null;
    }
}
