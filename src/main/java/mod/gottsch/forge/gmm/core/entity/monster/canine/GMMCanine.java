/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.gmm.core.entity.monster.canine;

import mod.gottsch.forge.gmm.core.config.MobConfig;
import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.AllyAlertHurtByTargetGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.AllyAlertNearestAttackableTargetGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.AllyAlertUtil;
import mod.gottsch.forge.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.forge.gmm.core.entity.monster.GMMMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared base for GMM's canine family: wolf-rigged quadrupeds that hunt in packs. The members are
 * the D&amp;D trio {@link DireWolf}, {@link Hellhound} and {@link WinterWolf}. The Ember Hound is
 * deliberately <em>not</em> one of them: it is a fire elemental in hound shape, with none of the
 * pack's abilities.
 *
 * <p>What a canine gets from here:
 * <ul>
 *     <li><b>Pack spawning</b> - {@link #getCompanionPool()} returns the subclass's
 *     {@code gmm:<mob>/companions} tag, so a natural spawn brings its pack (GMMMonster's companion
 *     hook, finalized as {@code MOB_SUMMONED}, so a companion never spawns a pack of its own).</li>
 *     <li><b>The howl</b> - when a canine picks a player out or is hurt, it howls and every idle
 *     pack mate in {@code howlRange} takes the same target (AllyAlertUtil, via the
 *     {@code gmm:<mob>/pack} tag). The pack is always alerted; only the sound is held to
 *     {@code howlCooldown}, so a running fight is not a running howl.</li>
 *     <li><b>Pack Tactics</b> - a bite does {@code packTacticsMultiplier} times its damage while
 *     another living pack mate is within {@code packTacticsRange} blocks of the target.</li>
 *     <li>The synched breathing flag that {@link mod.gottsch.forge.gmm.core.entity.monster.IBreathingMob}
 *     needs, so the two breathers share it and their model can read it.</li>
 * </ul>
 *
 * <p>Pack membership is only ever the {@link #getPackTag()} tag, never the class, so a consumer
 * decides who runs with whom.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public abstract class GMMCanine extends GMMMonster {
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(GMMCanine.class, EntityDataSerializers.BOOLEAN);
    private static final UUID PACK_TACTICS_ID = UUID.fromString("3c9f6a52-1d7e-4b8a-a6f4-8e2b5d1c7a90");

    private static final double DEFAULT_PACK_TACTICS_MULTIPLIER = 1.25D;
    private static final double DEFAULT_PACK_TACTICS_RANGE = 3.0D;
    private static final double DEFAULT_HOWL_RANGE = 16.0D;
    private static final int DEFAULT_HOWL_COOLDOWN = 200;

    private int nextHowlTick;

    protected GMMCanine(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BREATHING, false);
    }

    /** Who this canine runs with: howl recipients, Pack Tactics partners, spared by its breath. */
    protected abstract TagKey<EntityType<?>> getPackTag();

    /** The base pitch of its voice; a big wolf sounds deeper than a vanilla one. */
    protected abstract float getBaseVoicePitch();

    @Override
    protected abstract TagKey<EntityType<?>> getCompanionPool();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // 3 is left for a breath weapon, which must outrank the leap and the bite
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new SummonedOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new AllyAlertHurtByTargetGoal(this, getPackTag()) {
            @Override
            protected void alertOthers() {
                howl(this.mob.getTarget());
            }
        });
        this.targetSelector.addGoal(3, new AllyAlertNearestAttackableTargetGoal<>(this, Player.class, true, getPackTag()) {
            {
                // read lazily: registerGoals runs inside Mob's constructor, before playerNotOwner is set
                this.targetConditions.selector(entity -> playerNotOwner == null || playerNotOwner.test(entity));
            }

            @Override
            protected void alertOthers() {
                howl(this.mob.getTarget());
            }
        });
    }

    /**
     * The pack call: alert every idle pack mate in range to {@code target}, howling if the howl is
     * off cooldown. The {@code howl} flag turns both off, leaving each canine to find its own fight.
     */
    protected void howl(@Nullable LivingEntity target) {
        MobConfig config = MobConfigHelper.get(this);
        if (target == null || !config.flag("howl", true)) {
            return;
        }
        if (this.tickCount >= this.nextHowlTick) {
            this.nextHowlTick = this.tickCount + (int) config.number("howlCooldown", DEFAULT_HOWL_COOLDOWN);
            this.playSound(SoundEvents.WOLF_HOWL, 1.5F, this.getVoicePitch());
        }
        AllyAlertUtil.alertNearby(this, target, config.number("howlRange", DEFAULT_HOWL_RANGE), getPackTag());
    }

    public boolean isPackMate(Entity entity) {
        return entity != this && entity.getType().is(getPackTag());
    }

    /** Pack Tactics: a living pack mate (not this one) close enough to the target to flank it. */
    private boolean hasPackTactics(LivingEntity target, MobConfig config) {
        if (!config.flag("packTactics", true)) {
            return false;
        }
        double range = config.number("packTacticsRange", DEFAULT_PACK_TACTICS_RANGE);
        return !this.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(range),
                mob -> mob.isAlive() && isPackMate(mob)).isEmpty();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        MobConfig config = MobConfigHelper.get(this);
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        double multiplier = config.number("packTacticsMultiplier", DEFAULT_PACK_TACTICS_MULTIPLIER);
        boolean flanked = attack != null && multiplier != 1.0D
                && target instanceof LivingEntity living && hasPackTactics(living, config);

        // the bonus rides on the attribute for exactly this one bite, so vanilla's damage maths
        // (enchantments, knockback, difficulty) all see the boosted number
        if (flanked) {
            attack.removeModifier(PACK_TACTICS_ID);
            attack.addTransientModifier(new AttributeModifier(PACK_TACTICS_ID, "Pack tactics",
                    multiplier - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        boolean hurt;
        try {
            hurt = super.doHurtTarget(target);
        } finally {
            if (flanked) {
                attack.removeModifier(PACK_TACTICS_ID);
            }
        }
        if (hurt && target instanceof LivingEntity living) {
            onBite(living);
        }
        return hurt;
    }

    /** Called after each bite that landed. Nothing by default. */
    protected void onBite(LivingEntity target) {
    }

    public boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    public void setBreathing(boolean breathing) {
        this.entityData.set(DATA_BREATHING, breathing);
    }

    /** Pack mates and whoever owns this canine are never caught in its breath. */
    public boolean isBreathAlly(LivingEntity entity) {
        return isPackMate(entity) || entity.isAlliedTo(this)
                || (getOwnerId() != null && getOwnerId().equals(entity.getUUID()));
    }

    @Override
    public float getVoicePitch() {
        return getBaseVoicePitch() + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
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
