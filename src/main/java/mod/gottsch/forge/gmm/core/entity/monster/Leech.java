package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfig;
import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.forge.gmm.core.effect.GMMMobEffects;
import mod.gottsch.forge.gmm.core.entity.ai.goal.ReturnToWaterGoal;
import mod.gottsch.forge.gmm.core.entity.ai.navigation.FishPathNavigation;
import mod.gottsch.forge.gmm.core.particle.GMMParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static net.minecraftforge.common.ForgeMod.SWIM_SPEED;

/**
 * A water ambusher that does not bite. It <strong>attaches</strong> to a player who wades into its
 * pool, stays on when they climb out, and stops them healing until it comes off. The threat is not
 * the flooded room; it is arriving at the next fight unable to heal.
 *
 * <h2>Two tiers, one class</h2>
 * <p>The consumer registers this class twice, the {@code Rat} / {@code giant_rat} way: a small tier at
 * {@code sized(WIDTH, HEIGHT)} and a giant at {@code sized(WIDTH, HEIGHT * 2)}, rendered at 2x. The
 * tier is read back off the registered height ({@link #tier()}), so it is not cosmetic. It picks the
 * swimming box and the <em>defaults</em> of every gameplay value below; each value is still a
 * {@code gmm:mob_config} key, read per entity id, so a consumer can retune either tier.</p>
 * <ul>
 *   <li><b>Small:</b> up to 5 on one player; iron-or-better leggings make most bounce off; falls off
 *       fed after 60 s.</li>
 *   <li><b>Giant:</b> one per player; ignores armour; also slows the victim, drains 1 HP every 4 s,
 *       and costs 6 to rip off; falls off fed after 90 s.</li>
 * </ul>
 *
 * <h2>Attached</h2>
 * <p>No riding: a player carries exactly one passenger, and teleporting or changing dimension throws
 * riders off. Instead the leech holds its victim's UUID (persisted) and entity id (synced), stops its
 * own physics and AI ({@link #isImmobile()}), and snaps to a body socket on the victim every tick on
 * both sides. {@code LeechRenderer} then re-places it from the victim's <em>interpolated</em>
 * position, because two entities ticking in arbitrary order would otherwise lag a frame apart.</p>
 * <p>The leech is the only source of truth. Every second it renews a short {@code gmm:leeched} effect
 * on the victim (level = leeches on them); {@code LeechEvents} cancels all healing while that effect
 * is present. A leech that vanishes (chunk unload, logout) simply stops renewing it, and the effect
 * runs out two seconds later, so nothing can leave a player permanently unable to heal.</p>
 *
 * <h2>Getting it off</h2>
 * <p>Handled in {@code LeechEvents}: fire burns every leech off; sneak + use a {@code gmm:leech/burners}
 * item burns one; sneak + use an empty hand on a block rips one off for {@code ripDamage}; another
 * player right-clicking the victim pulls one off free. Or it falls off fed after {@code feedTicks}.
 * Leaving the water does not help; that is the point.</p>
 *
 * <h2>Registering it</h2>
 * <p>Swimming boxes are 0.75 (small) and 0.9 (giant) long. Not 1.0: the pathfinder sizes a mob as
 * {@code floor(width + 1)} blocks, so a 1.0 box could not swim a 1-wide channel (see {@link Orc}).
 * The giant's extra length lives in the model.</p>
 *
 * @author Mark Gottschling on 10/25/2026
 */
public class Leech extends WaterAnimal {
    public static final float WIDTH = 0.4F;
    public static final float HEIGHT = 0.3F;
    /** Small-tier swimming box length. */
    public static final float LENGTH = 0.75F;
    /** Longest swimming box any tier gets: under 1.0 so the pathfinder counts it as one block. */
    public static final float MAX_SWIM_LENGTH = 0.9F;

    /** Number of feeding stages the model grows through; see {@link #getFeedStage()}. */
    public static final int FEED_STAGES = 3;

    /**
     * Body sockets for the small tier, as {right, height, forward} in blocks from the victim's feet,
     * for a 1.8-tall player. Front and back only: a side socket would sit under the arm, and sockets
     * on the arm itself would not line up with the first-person arm. Several on the front of the legs
     * so a player sees them by looking down.
     */
    private static final float[][] SMALL_SOCKETS = {
            {-0.12F, 0.55F, 0.17F},   // left thigh, front
            {0.12F, 0.25F, 0.17F},    // right shin, front
            {0.12F, 0.62F, 0.17F},    // right thigh, front
            {-0.12F, 0.28F, 0.17F},   // left shin, front
            {0.08F, 1.0F, 0.17F},     // belly
            {-0.05F, 1.15F, -0.17F},  // back
            {0.12F, 0.5F, -0.17F},    // right thigh, back
    };
    /** The giant hangs down the victim's back, shoulders to knees. */
    private static final float[][] GIANT_SOCKETS = {
            {0.0F, 0.8F, -0.2F},
    };

    private static final EntityDataAccessor<Integer> VICTIM_ID =
            SynchedEntityData.defineId(Leech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> SOCKET =
            SynchedEntityData.defineId(Leech.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FEED_STAGE =
            SynchedEntityData.defineId(Leech.class, EntityDataSerializers.BYTE);

    private static final String VICTIM_TAG = "victim";
    private static final String SOCKET_TAG = "socket";
    private static final String FEED_TICKS_TAG = "feedTicks";
    private static final String SATED_TICKS_TAG = "satedTicks";

    /** How long a missing victim (logged out, unloaded, other dimension) is waited for before giving up. */
    private static final int VICTIM_LOST_TICKS = 40;
    /** Ticks between effect renewals; the effect lasts longer than this so it never flickers. */
    private static final int EFFECT_INTERVAL = 20;
    private static final int EFFECT_DURATION = 50;

    @Nullable
    private UUID victimUUID;
    private int feedTicks;
    /** After falling off fed, ticks during which it will not attach again. Also reused as the bounce/throw cooldown. */
    private int satedTicks;
    private int victimLostTicks;

    public Leech(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
        // AlligatorGar's maluses: a leech swims, it does not walk
        this.setPathfindingMalus(BlockPathTypes.WALKABLE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.OPEN, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 10.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                // 0 and never used: a leech does not bite. Present only so nothing that reads it crashes.
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                // FishPathNavigation is GroundPathNavigation-based, so MOVEMENT_SPEED is what moves it
                // (see AlligatorGar). Under the gar's: something you swim into, not something that runs
                // you down.
                .add(Attributes.MOVEMENT_SPEED, 0.8D)
                .add(SWIM_SPEED.get(), 1.2D);
    }

    public static AttributeSupplier.Builder createGiantAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D)
                .add(SWIM_SPEED.get(), 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VICTIM_ID, -1);
        this.entityData.define(SOCKET, (byte) 0);
        this.entityData.define(FEED_STAGE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ReturnToWaterGoal(this, 0.25D));
        // Swims at its target and nothing else: the attach happens on contact in aiStep, so the goal's
        // own attack is a no-op. Both must be in water, as for the gar.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            public boolean canUse() {
                return this.mob.isInWater() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return this.mob.isInWater() && super.canContinueToUse();
            }

            @Override
            protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
            }
        });
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> player.isInWater() && this.satedTicks <= 0));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FishPathNavigation(this, level);
    }

    @Override
    public float getPathfindingMalus(BlockPathTypes pathType) {
        // a beached leech may cross land to get back to water
        if (!this.isInWater() && pathType == BlockPathTypes.WALKABLE) {
            return 0.0F;
        }
        return super.getPathfindingMalus(pathType);
    }

    // ------------------------------------------------------------------------------------------------
    // tier
    // ------------------------------------------------------------------------------------------------

    /**
     * 1.0 for the small tier, 2.0 for a giant registered at {@code HEIGHT * 2}. Read off the REGISTERED
     * type, so a third size registered by a consumer gets a sensible box with no change here.
     */
    public float tier() {
        return this.getType().getDimensions().height / HEIGHT;
    }

    public boolean isGiant() {
        return tier() > 1.5F;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (pose == Pose.SWIMMING) {
            // long and flat, AlligatorGar's trick, but scaled by tier and capped below 1.0
            float tier = tier();
            return EntityDimensions.scalable(Math.min(LENGTH * tier, MAX_SWIM_LENGTH), HEIGHT * tier);
        }
        return super.getDimensions(pose);
    }

    // ------------------------------------------------------------------------------------------------
    // config (gmm:mob_config, per entity id; defaults by tier)
    // ------------------------------------------------------------------------------------------------

    private MobConfig config() {
        return MobConfigHelper.get(this);
    }

    /** Most leeches of this entity type one player can carry. */
    public int maxAttached() {
        return (int) config().number("maxAttached", isGiant() ? 1 : 5);
    }

    public boolean ignoresArmor() {
        return config().flag("ignoresArmor", isGiant());
    }

    /** Health a player loses ripping this leech off. Armour does not reduce it. */
    public float ripDamage() {
        return (float) config().number("ripDamage", isGiant() ? 6.0D : 2.0D);
    }

    private int feedTicksToSated() {
        return (int) config().number("feedTicks", isGiant() ? 1800 : 1200);
    }

    // ------------------------------------------------------------------------------------------------
    // attached state
    // ------------------------------------------------------------------------------------------------

    public boolean isAttached() {
        return this.level().isClientSide ? this.entityData.get(VICTIM_ID) >= 0 : this.victimUUID != null;
    }

    public boolean isAttachedTo(Entity entity) {
        return entity != null && (this.level().isClientSide
                ? this.entityData.get(VICTIM_ID) == entity.getId()
                : entity.getUUID().equals(this.victimUUID));
    }

    /** The victim, if it is loaded on this side. */
    @Nullable
    public LivingEntity getVictim() {
        if (this.level().isClientSide) {
            int id = this.entityData.get(VICTIM_ID);
            return id >= 0 && this.level().getEntity(id) instanceof LivingEntity living ? living : null;
        }
        if (this.victimUUID != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.victimUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public int getSocket() {
        return this.entityData.get(SOCKET);
    }

    /** 0 just attached, up to {@code FEED_STAGES - 1} nearly full. Drives the model's growth. */
    public int getFeedStage() {
        return this.entityData.get(FEED_STAGE);
    }

    /** Whether this leech's socket is on the victim's back (the renderer turns it to face in). */
    public boolean isOnBack() {
        return socketTable()[Math.min(getSocket(), socketTable().length - 1)][2] < 0;
    }

    private float[][] socketTable() {
        return isGiant() ? GIANT_SOCKETS : SMALL_SOCKETS;
    }

    /**
     * Where this leech sits on {@code victim} at {@code partialTick}. Shared by the server's per-tick
     * snap and the renderer, so what is drawn and what can be hit agree.
     *
     * <p>Follows crouching (scaled to the victim's height) and swimming (the body frame pitches
     * forward by {@code swimAmount * 90} degrees about the hips, so the front of the body faces down).</p>
     */
    public Vec3 socketPosition(LivingEntity victim, float partialTick) {
        float[] socket = socketTable()[Math.min(getSocket(), socketTable().length - 1)];
        float swim = victim.getSwimAmount(partialTick);
        float heightScale = victim.getBbHeight() / 1.8F;
        float right = socket[0];
        float height = Mth.lerp(swim, socket[1] * Math.min(1.0F, heightScale), socket[1]);
        float forward = socket[2];

        // pitch the body frame about its middle as the victim goes horizontal
        float pitch = swim * Mth.HALF_PI;
        float along = height - 0.9F;
        float up = along * Mth.cos(pitch) - forward * Mth.sin(pitch);
        float fwd = forward * Mth.cos(pitch) + along * Mth.sin(pitch);
        float base = Mth.lerp(swim, 0.9F * Math.min(1.0F, heightScale), 0.3F);

        float yaw = Mth.rotLerp(partialTick, victim.yBodyRotO, victim.yBodyRot) * Mth.DEG_TO_RAD;
        // Minecraft yaw: forward is (-sin, cos); the victim's right is (-cos, -sin)
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        Vec3 feet = victim.getPosition(partialTick);
        return new Vec3(feet.x + fx * fwd + rx * right,
                feet.y + base + up - this.getBbHeight() / 2.0D,
                feet.z + fz * fwd + rz * right);
    }

    /**
     * Latches on, if armour and the per-player cap allow it. Returns whether it attached; a bounce off
     * armour pushes the leech away and puts it on a short cooldown.
     */
    public boolean tryAttach(Player player) {
        if (this.level().isClientSide || isAttached() || this.satedTicks > 0
                || player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        List<Leech> onVictim = attachedTo(player);
        if (onVictim.stream().filter(l -> l.getType() == this.getType()).count() >= maxAttached()) {
            return false;
        }
        if (!ignoresArmor() && bouncesOff(player.getItemBySlot(EquipmentSlot.LEGS))) {
            this.satedTicks = 40;
            Vec3 away = this.position().subtract(player.position()).normalize().scale(0.4D);
            this.setDeltaMovement(away.x, 0.1D, away.z);
            this.playSound(SoundEvents.SLIME_JUMP_SMALL, 0.6F, 1.4F);
            return false;
        }

        // first free socket, so several leeches never share one
        int socket = 0;
        for (int i = 0; i < socketTable().length; i++) {
            final int candidate = i;
            if (onVictim.stream().noneMatch(l -> l.getType() == this.getType() && l.getSocket() == candidate)) {
                socket = i;
                break;
            }
        }
        if (onVictim.isEmpty()) {
            // the one-time how-to, so the counterplay is discoverable without a wiki
            player.displayClientMessage(Component.translatable("message.gmm.leech.attached"), true);
        }
        attach(player, socket);
        return true;
    }

    private boolean bouncesOff(ItemStack leggings) {
        if (!(leggings.getItem() instanceof ArmorItem armor)) {
            return false;
        }
        // iron leggings are 5; chainmail 4, gold 3, leather 2
        double threshold = config().number("armorThreshold", 5.0D);
        double chance = config().number("armorBounceChance", 0.8D);
        return armor.getDefense() >= threshold && this.random.nextDouble() < chance;
    }

    private void attach(LivingEntity victim, int socket) {
        this.victimUUID = victim.getUUID();
        this.entityData.set(VICTIM_ID, victim.getId());
        this.entityData.set(SOCKET, (byte) socket);
        this.feedTicks = 0;
        this.entityData.set(FEED_STAGE, (byte) 0);
        this.victimLostTicks = 0;
        this.setTarget(null);
        this.getNavigation().stop();
        startAttachedPhysics();
        this.setPos(socketPosition(victim, 1.0F));
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 0.8F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(GMMParticles.BLOOD_DRIP.get(), getX(), getY(), getZ(), 4, 0.1D, 0.1D, 0.1D, 0.0D);
        }
        renewEffect(victim);
    }

    private void startAttachedPhysics() {
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** Why a leech came off, which decides what happens to it next. */
    public enum DetachReason { BURNED, RIPPED, PULLED, SATED, VICTIM_LOST }

    /**
     * Comes off the victim. Burned leeches die; torn or pulled ones are thrown clear and wait before
     * trying again; sated ones swim off and leave players alone for a while.
     */
    public void detach(DetachReason reason) {
        LivingEntity victim = getVictim();
        this.victimUUID = null;
        this.entityData.set(VICTIM_ID, -1);
        this.noPhysics = false;
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
        this.feedTicks = 0;
        this.entityData.set(FEED_STAGE, (byte) 0);

        switch (reason) {
            case BURNED -> {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                            getX(), getY(), getZ(), 6, 0.1D, 0.1D, 0.1D, 0.01D);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            getX(), getY(), getZ(), 6, 0.1D, 0.1D, 0.1D, 0.01D);
                }
                this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.6F);
                this.kill();
            }
            case RIPPED, PULLED -> {
                this.satedTicks = (int) config().number("reattachCooldown", 100);
                throwClear(victim);
                this.playSound(SoundEvents.SLIME_SQUISH, 0.8F, 1.3F);
                if (reason == DetachReason.RIPPED && this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(GMMParticles.BLOOD_DRIP.get(), getX(), getY(), getZ(), 6, 0.1D, 0.1D, 0.1D, 0.0D);
                }
            }
            case SATED -> {
                this.satedTicks = (int) config().number("satedTicks", 1200);
                throwClear(victim);
                this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 0.6F);
            }
            case VICTIM_LOST -> { }
        }
    }

    private void throwClear(@Nullable LivingEntity victim) {
        if (victim == null) {
            return;
        }
        Vec3 away = this.position().subtract(victim.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }
        away = away.normalize().scale(0.35D);
        this.setDeltaMovement(away.x, 0.25D, away.z);
    }

    /** Every leech attached to {@code victim}, giants first, then the longest-feeding. */
    public static List<Leech> attachedTo(LivingEntity victim) {
        return victim.level().getEntitiesOfClass(Leech.class, victim.getBoundingBox().inflate(2.0D),
                        leech -> leech.isAlive() && leech.isAttachedTo(victim)).stream()
                .sorted(Comparator.comparing(Leech::isGiant).reversed()
                        .thenComparing(Comparator.comparingInt((Leech l) -> l.feedTicks).reversed()))
                .toList();
    }

    // ------------------------------------------------------------------------------------------------
    // ticking
    // ------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!isAttached()) {
            return;
        }
        LivingEntity victim = getVictim();
        if (victim != null) {
            this.setPos(socketPosition(victim, 1.0F));
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(victim.yBodyRot);
            this.yBodyRot = victim.yBodyRot;
        }
        if (!this.level().isClientSide) {
            serverAttachedTick(victim);
        }
    }

    private void serverAttachedTick(@Nullable LivingEntity victim) {
        if (victim == null) {
            if (++this.victimLostTicks > VICTIM_LOST_TICKS) {
                detach(DetachReason.VICTIM_LOST);
            }
            return;
        }
        this.victimLostTicks = 0;
        if (this.entityData.get(VICTIM_ID) != victim.getId()) {
            // first sight of the victim since a reload: tell the client who it is on
            this.entityData.set(VICTIM_ID, victim.getId());
        }
        if (!victim.isAlive() || (victim instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            detach(DetachReason.VICTIM_LOST);
            return;
        }
        if (victim.isOnFire()) {
            detach(DetachReason.BURNED);
            return;
        }

        this.feedTicks++;
        int sated = Math.max(1, feedTicksToSated());
        int stage = Math.min(FEED_STAGES - 1, this.feedTicks * FEED_STAGES / sated);
        if (stage != getFeedStage()) {
            this.entityData.set(FEED_STAGE, (byte) stage);
        }
        if (this.feedTicks >= sated) {
            detach(DetachReason.SATED);
            return;
        }

        double drain = config().number("drainAmount", isGiant() ? 1.0D : 0.0D);
        int drainInterval = Math.max(1, (int) config().number("drainInterval", 80));
        if (drain > 0.0D && this.feedTicks % drainInterval == 0) {
            victim.hurt(GMMDamageTypes.source(this.level(), GMMDamageTypes.LEECH_DRAIN, this, this), (float) drain);
        }
        // offset by entity id so a cluster does not renew in the same tick
        if ((this.feedTicks + this.getId()) % EFFECT_INTERVAL == 0) {
            renewEffect(victim);
        }
    }

    private void renewEffect(LivingEntity victim) {
        int count = attachedTo(victim).size();
        if (count <= 0) {
            return;
        }
        victim.addEffect(new MobEffectInstance(GMMMobEffects.LEECHED.get(), EFFECT_DURATION, count - 1,
                false, false, true), this);
        if (config().flag("slows", isGiant())) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 0,
                    false, false, true), this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || isAttached()) {
            return;
        }
        if (this.satedTicks > 0) {
            this.satedTicks--;
            return;
        }
        // contact attach: both in water, boxes touching
        LivingEntity target = this.getTarget();
        if (target instanceof Player player && this.isInWater() && player.isInWater()
                && this.getBoundingBox().inflate(0.3D).intersects(player.getBoundingBox())) {
            tryAttach(player);
        }
    }

    /** Attached: no AI, no movement input. The position is set in {@link #tick()}. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isAttached();
    }

    /** Attached leeches are carried out of the water and must not suffocate in air. */
    @Override
    protected void handleAirSupply(int air) {
        if (isAttached()) {
            this.setAirSupply(this.getMaxAirSupply());
            return;
        }
        super.handleAirSupply(air);
    }

    /** The client places an attached leech itself; the server's position updates would only fight it. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (isAttached()) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    @Override
    protected void pushEntities() {
        if (!isAttached()) {
            super.pushEntities();
        }
    }

    @Override
    public boolean isPushable() {
        return !isAttached() && super.isPushable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /**
     * Invisible to its own victim's crosshair. Otherwise a player looking down at the leeches on
     * their shins would target a leech instead of the floor, and could neither mine nor make the
     * empty-hand-on-a-block click that rips one off. Picking is client-side, so the local player is
     * the one to exclude; other players can still target it.
     */
    @Override
    public boolean isPickable() {
        if (isAttached() && getVictim() instanceof Player player && player.isLocalPlayer()) {
            return false;
        }
        return super.isPickable();
    }

    /**
     * Its own victim cannot hurt it, so a sweep or a stray swing is never a free removal. Another
     * player can: one punch kills a small leech.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isAttached() && source.getEntity() != null && isAttachedTo(source.getEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || isAttached();
    }

    @Override
    public void die(DamageSource source) {
        if (isAttached()) {
            this.victimUUID = null;
            this.entityData.set(VICTIM_ID, -1);
            this.noPhysics = false;
            this.setNoGravity(false);
        }
        super.die(source);
    }

    // ------------------------------------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.victimUUID != null) {
            tag.putUUID(VICTIM_TAG, this.victimUUID);
            tag.putByte(SOCKET_TAG, (byte) getSocket());
        }
        tag.putInt(FEED_TICKS_TAG, this.feedTicks);
        tag.putInt(SATED_TICKS_TAG, this.satedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.feedTicks = tag.getInt(FEED_TICKS_TAG);
        this.satedTicks = tag.getInt(SATED_TICKS_TAG);
        if (tag.hasUUID(VICTIM_TAG)) {
            // the victim's entity id is unknown until it is found again; tick() resolves it or gives up
            this.victimUUID = tag.getUUID(VICTIM_TAG);
            this.entityData.set(SOCKET, tag.getByte(SOCKET_TAG));
            startAttachedPhysics();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VICTIM_ID.equals(key) && this.level().isClientSide) {
            if (isAttached()) {
                startAttachedPhysics();
            } else {
                this.noPhysics = false;
                this.setNoGravity(false);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // sounds
    // ------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }
}
