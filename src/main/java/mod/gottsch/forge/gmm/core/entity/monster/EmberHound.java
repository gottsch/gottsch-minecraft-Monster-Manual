/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * gottsch's Monster Manual is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gottsch's Monster Manual is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with gottsch's Monster Manual.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A minor fire elemental in the shape of a hound. Imported from Mage Flame, where it is a summoned
 * companion; here it is a plain GMM mob, with only the rig and texture carried over. It hunts like a
 * wolf (leaps in, then bites) and its bite sets the target alight. Deliberately not a Hell Hound
 * (a fiend, with breath and pack tactics, reserved for GMM's canine family): its kit is the D&D fire
 * elemental's, translated.
 *
 * <ul>
 *     <li><b>Fire Form</b> - a creature that hits it in melee is set alight. Ranged attacks are safe.</li>
 *     <li><b>Stoked</b> - standing in fire or lava heals it, and dries it off.</li>
 *     <li><b>Doused</b> - water, rain or a splash water bottle douses it for a while: its bite and
 *     Fire Form stop igniting, it slows, and its glow dims. This replaces a Blaze's water damage,
 *     which would kill an overworld mob in the first rain shower ({@code hurtByWater} brings the
 *     damage back).</li>
 * </ul>
 *
 * <p>Every number and each ability is codec-driven ({@code gmm:mob_config}).
 *
 * @author Mark Gottschling on 10/16/2026
 */
public class EmberHound extends GMMMonster {
    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 0.85F;

    private static final EntityDataAccessor<Boolean> DATA_DOUSED = SynchedEntityData.defineId(EmberHound.class, EntityDataSerializers.BOOLEAN);
    private static final UUID DOUSED_SPEED_ID = UUID.fromString("7b3e1d2a-4c55-4f6e-9a1b-2e8d6c0f5a31");

    private static final int DEFAULT_MELEE_FIRE_SECONDS = 4;
    // Fire Form: burn a melee attacker briefly - a deterrent, not a second bite
    private static final int DEFAULT_FIRE_FORM_SECONDS = 3;
    // A melee hit from farther than this is a reach weapon, and the attacker is out of the flames
    private static final double FIRE_FORM_REACH = 3.0D;
    // Stoked: half a heart a second while standing in fire or lava
    private static final double DEFAULT_STOKE_HEAL = 1.0D;
    private static final int DEFAULT_STOKE_INTERVAL = 20;
    // Doused: rain/water keeps refreshing it, so this is how long it stays wet once out of the water;
    // a thrown splash bottle is a deliberate act and soaks it for longer
    private static final int DEFAULT_WET_DOUSED_TICKS = 100;
    private static final int DEFAULT_SPLASH_DOUSED_TICKS = 200;
    private static final double DEFAULT_DOUSED_SLOW = 0.3D;

    // vanilla's water-sensitivity tick is 1.0 DROWN; real drowning is 2.0
    private static final float WATER_SENSITIVITY_DAMAGE = 1.0F;

    private int dousedTicks;

    public EmberHound(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DOUSED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner));
    }

    /*
     * default attributes. Anchored to a vanilla wolf (tamed 20 HP, 4 bite) rather than Mage Flame's
     * 40 HP / 8 summon, which was tuned as a player's ally; the ignition is the extra threat.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    private boolean ignitesOnHit() {
        return MobConfigHelper.get(this).flag("igniteOnHit", true);
    }

    private int meleeFireSeconds() {
        return (int) MobConfigHelper.get(this).number("meleeFireSeconds", DEFAULT_MELEE_FIRE_SECONDS);
    }

    private boolean hasFireForm() {
        return MobConfigHelper.get(this).flag("fireForm", true);
    }

    private int fireFormSeconds() {
        return (int) MobConfigHelper.get(this).number("fireFormSeconds", DEFAULT_FIRE_FORM_SECONDS);
    }

    private boolean isStokable() {
        return MobConfigHelper.get(this).flag("stoked", true);
    }

    private double stokeHeal() {
        return MobConfigHelper.get(this).number("stokeHeal", DEFAULT_STOKE_HEAL);
    }

    private int stokeInterval() {
        return (int) MobConfigHelper.get(this).number("stokeInterval", DEFAULT_STOKE_INTERVAL);
    }

    private boolean isDousable() {
        return MobConfigHelper.get(this).flag("doused", true);
    }

    private int wetDousedTicks() {
        return (int) MobConfigHelper.get(this).number("wetDousedTicks", DEFAULT_WET_DOUSED_TICKS);
    }

    private int splashDousedTicks() {
        return (int) MobConfigHelper.get(this).number("splashDousedTicks", DEFAULT_SPLASH_DOUSED_TICKS);
    }

    private double dousedSlow() {
        return MobConfigHelper.get(this).number("dousedSlow", DEFAULT_DOUSED_SLOW);
    }

    private boolean hurtByWater() {
        return MobConfigHelper.get(this).flag("hurtByWater", false);
    }

    public boolean isDoused() {
        return this.entityData.get(DATA_DOUSED);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !isDoused() && ignitesOnHit() && target instanceof LivingEntity living) {
            living.setSecondsOnFire(meleeFireSeconds());
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // vanilla's per-tick water/rain damage (only reaches us because isSensitiveToWater is true,
        // which it must be for a splash bottle to find us). Wetness itself is picked up in
        // customServerAiStep; here we only decide whether it also hurts.
        if (source.is(DamageTypes.DROWN) && amount <= WATER_SENSITIVITY_DAMAGE && this.isInWaterRainOrBubble()) {
            return hurtByWater() && super.hurt(source, amount);
        }

        if (isSplashWater(source)) {
            douse(splashDousedTicks());
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            burnMeleeAttacker(source);
        }
        return hurt;
    }

    private static boolean isSplashWater(DamageSource source) {
        return source.getDirectEntity() instanceof ThrownPotion potion
                && PotionUtils.getPotion(potion.getItem()) == Potions.WATER;
    }

    /** Fire Form: whoever struck us by hand, from up close, catches fire. */
    private void burnMeleeAttacker(DamageSource source) {
        if (!hasFireForm() || isDoused() || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker.isAlive() && this.distanceToSqr(attacker) <= FIRE_FORM_REACH * FIRE_FORM_REACH) {
            attacker.setSecondsOnFire(fireFormSeconds());
        }
    }

    /**
     * Fire-immune by nature, so a consumer that forgets {@code .fireImmune()} on its
     * {@code EntityType.Builder} still gets a hound that doesn't burn.
     */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Always true: it is how vanilla's splash water bottle finds us. Damage is gated in {@link #hurt}. */
    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    private void douse(int ticks) {
        if (!isDousable() || ticks <= 0) {
            return;
        }
        if (!isDoused()) {
            setDoused(true);
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5D), this.getZ(),
                        8, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
        this.dousedTicks = Math.max(this.dousedTicks, ticks);
    }

    private void dryOff() {
        this.dousedTicks = 0;
        if (isDoused()) {
            setDoused(false);
            this.playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 1.2F);
        }
    }

    private void setDoused(boolean doused) {
        this.entityData.set(DATA_DOUSED, doused);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(DOUSED_SPEED_ID);
        if (doused && dousedSlow() > 0) {
            speed.addTransientModifier(new AttributeModifier(DOUSED_SPEED_ID, "Doused slow",
                    -dousedSlow(), AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private boolean isInFireOrLava() {
        return this.isInLava() || this.level().getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-3D))
                .anyMatch(state -> state.is(BlockTags.FIRE));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Doused: wet keeps it doused; dry, it counts back down
        if (this.isInWaterRainOrBubble()) {
            douse(wetDousedTicks());
        } else if (this.dousedTicks > 0 && --this.dousedTicks == 0) {
            dryOff();
        }

        // Stoked: fire and lava dry it at once, and mend it on the interval
        if (isStokable() && isInFireOrLava()) {
            dryOff();
            if (this.tickCount % Math.max(1, stokeInterval()) == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal((float) stokeHeal());
                if (this.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5D), this.getZ(),
                            6, 0.3D, 0.3D, 0.3D, 0.02D);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide) {
            double y = this.getRandomY();
            if (isDoused()) {
                // doused: the coat only steams
                if (this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.SMOKE,
                            this.getRandomX(0.5D), y, this.getRandomZ(0.75D), 0.0D, 0.02D, 0.0D);
                }
            } else {
                // ember body: flame licking off the coat, a wisp of smoke now and then (Mage Flame's look)
                for (int i = 0; i < 3; i++) {
                    this.level().addParticle(ParticleTypes.FLAME,
                            this.getRandomX(0.5D), y, this.getRandomZ(0.75D), 0.0D, 0.0D, 0.0D);
                }
                if (this.tickCount % 4 == 0) {
                    this.level().addParticle(ParticleTypes.SMOKE,
                            this.getX(0.5D), y, this.getZ(0.75D), 0.0D, 0.0D, 0.0D);
                }
            }
        }
        super.aiStep();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DousedTicks", this.dousedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dousedTicks = tag.getInt("DousedTicks");
        if (this.dousedTicks > 0) {
            setDoused(true);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.WOLF_PANT : SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }
}
