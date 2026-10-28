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
import mod.gottsch.forge.gmm.core.effect.GMMMobEffects;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The D&amp;D dire wolf: a wolf the size of a pony, hunting in packs. No breath; its trick is the
 * bite that knocks a target <b>prone</b>, here a brief full root ({@code gmm:paralyzed}) that holds
 * the victim in place for the rest of the pack. A target already rooted is not rooted again, and
 * each wolf has its own cooldown, so a pack can pin a player but not chain-lock one.
 *
 * <p>About 1.3x a vanilla wolf ({@link #WIDTH}/{@link #HEIGHT}, renderer scale 1.3). Stats sit
 * above a tamed wolf (20 HP, 4 bite): 30 HP, 6 bite, a little faster, a little armour.
 *
 * <p>{@code gmm:mob_config}: {@code prone} (flag, true), {@code proneChance} (0.3),
 * {@code proneTicks} (20), {@code proneCooldown} (100), plus the {@link GMMCanine} keys.
 *
 * @author Mark Gottschling on 10/28/2026
 */
public class DireWolf extends GMMCanine {
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 1.1F;

    private static final double DEFAULT_PRONE_CHANCE = 0.3D;
    private static final int DEFAULT_PRONE_TICKS = 20;
    private static final int DEFAULT_PRONE_COOLDOWN = 100;

    private int nextProneTick;

    public DireWolf(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected TagKey<EntityType<?>> getPackTag() {
        return GMMTags.EntityTypes.DIRE_WOLF_PACK;
    }

    @Override
    protected TagKey<EntityType<?>> getCompanionPool() {
        return GMMTags.EntityTypes.DIRE_WOLF_COMPANIONS;
    }

    @Override
    protected float getBaseVoicePitch() {
        return 0.8F;
    }

    /** Knock it prone: a short, full root, at a chance, off cooldown, and never on a rooted target. */
    @Override
    protected void onBite(LivingEntity target) {
        MobConfig config = MobConfigHelper.get(this);
        if (!config.flag("prone", true) || this.tickCount < this.nextProneTick
                || target.hasEffect(GMMMobEffects.PARALYZED.get())
                || this.random.nextDouble() >= config.number("proneChance", DEFAULT_PRONE_CHANCE)) {
            return;
        }
        int ticks = (int) config.number("proneTicks", DEFAULT_PRONE_TICKS);
        if (ticks <= 0) {
            return;
        }
        if (target.addEffect(new MobEffectInstance(GMMMobEffects.PARALYZED.get(), ticks, 0), this)) {
            this.nextProneTick = this.tickCount + (int) config.number("proneCooldown", DEFAULT_PRONE_COOLDOWN);
        }
    }
}
